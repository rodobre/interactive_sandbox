package sysjail;
import com.sun.jna.Native;
import com.sun.jna.Library;
import com.sun.jna.Platform;

import java.lang.ProcessBuilder;
import java.lang.ProcessBuilder.Redirect;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import java.io.IOException;
import java.lang.InterruptedException;

import sysjail.LogType;
import sysjail.Logger;
import sysjail.ArgumentParser;

interface CStdLib extends Library {
    int syscall(int number, Object... args);
    int system(String path);
    int execl(String path, String args, Object... args2);
    int execlp(String path, String args, Object... args2);
}

class PathUtils {
    public static String get_processname_from_path(String proc_path) {
        assert ( proc_path != null && !proc_path.isEmpty() );
        int last_path_idx = proc_path.lastIndexOf('/');
        assert last_path_idx != (proc_path.length() - 1) : "Invalid process";

        if ( last_path_idx == -1 ) {
            return proc_path;
        }
        else {
            return proc_path.substring(last_path_idx + 1);
        }
    }
}

public class NativeInterfaceConnector {
    private CStdLib c;
    private final String sandbox_hostname = "sysjail";
    private String sandbox_mtdir          = "./.sysjail_root";
    private String old_root               = ".oldroot";
    private int sysjail_user_id;

    /* Sandbox limitations */
    private static final int MAX_CPU_SHARES        = 1024;
    private static final int MAX_CPU_PERIOD        = 100000;
    private static final int MAX_CPU_QUOTA_PERCENT = 20;
    private static final int MAX_CPU_QUOTA_CPUS    = 1;
    private static final int MAX_MEMORY_LIMIT      = (1<<24);

    /* Syscalls */
    private static final int SYS_UNSHARE     = 272;
    private static final int SYS_SETHOSTNAME = 170;
    private static final int SYS_MOUNT       = 165;
    private static final int SYS_CHDIR       =  80;
    private static final int SYS_MKDIR       =  83;
    private static final int SYS_PIVOTROOT   = 155;
    private static final int SYS_UMOUNT2     = 166;
    private static final int SYS_RMDIR       =  84;
    private static final int SYS_EXECVE      =  59;
    private static final int SYS_GETPID      =  39;
    private static final int SYS_WRITE       =   1;
    private static final int SYS_SYMLINK     =  88;
    private static final int SYS_CHROOT      = 161;
    private static final int SYS_SETUID      = 105;
    private static final int SYS_SETGID      = 106;
    private static final int SYS_SETREUID    = 113;
    private static final int SYS_SETREGID    = 114;
    private static final int SYS_SETGROUPS   = 116;
    private static final int SYS_SETRESUID   = 118;
    private static final int SYS_SETRESGID   = 119;
    private static final int SYS_LINK        =  86;
    private static final int SYS_LINKAT      = 265;
    private static final int SYS_CHOWN       =  92;
    private static final int SYS_CHMOD       =  90;
    private static final int SYS_UMASK       =  95;

    /* Unshare flags */
    private static final int CLONE_NEWUTS = 0x04000000;
    private static final int CLONE_NEWPID = 0x20000000;
    private static final int CLONE_NEWNS  = 0x00020000;
    private static final int CLONE_FS     = 0x00000200;
    private static final int CLONE_NEWIPC = 0x08000000;
    private static final int CLONE_NEWNET = 0x40000000;
    private static final int CLONE_NEWUSER= 0x10000000;
    private static final int CLONE_SYSVEM = 0x00040000;

    /* Mount flags */
    private static final int MS_RDONLY    = 1;
    private static final int MS_NOSUID    = 2;
    private static final int MS_NODEV     = 4;
    private static final int MS_REMOUNT   = 32;
    private static final int MS_BIND      = 4096;
    private static final int MS_REC       = 16384;
    private static final int MS_PRIVATE   = (1<<18);
    private static final int MNT_DETACH   = 0x00000002;

    /* File permissions */
    private static final int S_IRWXU      = 0700;
    private static final int S_IRXGR      = 0050;
    private static final int S_IRXOT      = 0005;

    public NativeInterfaceConnector() {
        c = (CStdLib)Native.loadLibrary("c", CStdLib.class);
    }

    private void set_user_id(int id) {
        sysjail_user_id = id;
    }

    private int get_user_id() {
        return sysjail_user_id;
    }

    public void set_mount_dir_path(String new_path) {
        sandbox_mtdir = new_path;
    }

    public String get_mount_dir_path() {
        return sandbox_mtdir;
    }

    public void set_oldroot_path(String new_path) {
        old_root = new_path;
    }

    public String get_oldroot_path() {
        return old_root;
    }

    public CStdLib get_std_inst() {
        return c;
    }

    private void create_sandbox_user_if_not_exists() {
        Process p;
        try {
            p = Runtime.getRuntime().exec("id -u sysjail");
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            
            String cmd_output = "";
            String line = null;

            while((line = reader.readLine()) != null) {
                cmd_output += line;
            }

            Logger.Log(LogType.DEBUG, String.format("Cmd output: %s", cmd_output));
            if ( cmd_output.contains("no such user") || cmd_output == null || cmd_output == "" ) {
                Logger.Log(LogType.DEBUG, "Instantiating user \"sysjail\"");
                c.system("useradd -M -N -l sysjail && passwd -d sysjail");
                create_sandbox_user_if_not_exists();
            }
            else {
                int u_id = Integer.parseInt(cmd_output);
                Logger.Log(LogType.DEBUG, String.format("Found user \"sysjail\" with ID [%d]", u_id));
                set_user_id(u_id);
                return;
            }
        } catch(IOException except) {
            Logger.Log(LogType.ERROR, "Could not query users.");
            return;
        }
    }

    private void own_process(String proc_path, int uid, int gid) {
        Logger.Log(LogType.DEBUG, "Dropping umask privileges...");
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_UMASK, 0777)));

        Logger.Log(LogType.DEBUG, String.format("Getting ownership on process at path [%s]", proc_path));
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_CHOWN, proc_path, uid, gid)));
        Logger.Log(LogType.DEBUG, String.format("Making process executable"));
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_CHMOD, proc_path, S_IRWXU | S_IRXGR | S_IRXOT)));
        Logger.Log(LogType.DEBUG, String.format("Changing user to [%d]", uid));

        int[] groups_list = { 65534 };
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_SETGID, 65534)));
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_SETRESGID, 65534, 65534, 65534)));
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_SETGROUPS, 1, groups_list)));
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_SETREUID, uid, uid)));
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_SETUID, uid)));
    }

    private void symlink_dependencies(String old_root, String new_root) {
        List<String> dependencies = new ArrayList<String>(List.of( "lib", "lib64", "bin", "etc" ));

        Logger.Log(LogType.DEBUG, "Linking dependencies...");
        for (String dependency : dependencies) {
            Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_SYMLINK, String.format("%s/%s", old_root, dependency), String.format("%s/%s", new_root, dependency))));
        }
        Logger.Log(LogType.DEBUG, "Finished linking dependencies...");
    }

    private void enter_sandbox(String proc_path) {

        // Unshare the namespace to avoid confusion with host
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_UNSHARE, CLONE_NEWUTS | CLONE_NEWPID | CLONE_NEWNS | CLONE_NEWIPC |
                                                CLONE_FS | CLONE_NEWNET)));

        // Change hostname to indicate the process that it is being sandboxed
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_SETHOSTNAME, sandbox_hostname, sandbox_hostname.length())));

        // Create a new root directory
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_MKDIR, sandbox_mtdir, 0755)));

        // Mount MS_PRIVATE on the root FS
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_MOUNT, "none", "/", 0, MS_REC | MS_PRIVATE, 0)));

        // Mount the new root directory
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_MOUNT, sandbox_mtdir, sandbox_mtdir, 0, MS_NODEV | MS_NOSUID | MS_RDONLY | MS_REC | MS_BIND | MS_PRIVATE , 0)));

        // Change directory to the new root directory
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_CHDIR, sandbox_mtdir)));

        // Make the directory for the new root
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_MKDIR, old_root, 0755)));

        // Pivot root
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_PIVOTROOT, ".", old_root)));

        // Change root directory
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_CHROOT, ".")));

        // Symlink the process here
        if (proc_path.charAt(0) == '/') {
            proc_path = String.format("%s%s", old_root, proc_path);
        }
        else {
            proc_path = String.format("%s/%s", old_root, proc_path);
        }

        Logger.Log(LogType.INFO, String.format("Symlinking path [%s] to [%s].", proc_path, PathUtils.get_processname_from_path(proc_path)));
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_SYMLINK, proc_path, String.format("./%s", PathUtils.get_processname_from_path(proc_path)))));

        // Create /proc
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_MKDIR, "/proc", 0755)));

        // Mount /proc
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_MOUNT, "proc", "/proc", "proc", 0, 0)));

        // Symlink dependencies
        symlink_dependencies(old_root, ".");
    }

    private void exit_sandbox() {
        Logger.Log(LogType.DEBUG, "Removing old root directory...");
        Logger.Log(LogType.DEBUG, String.format("SYSCALL RESULT: %d", c.syscall(SYS_UMOUNT2, "/" + get_oldroot_path())));
        Logger.Log(LogType.DEBUG, String.format("Sandbox finished"));
    }

    public void sandbox_process(final String process_path) {
        
        Logger.Log(LogType.INFO, String.format("Preparing to sandbox process [%s]", process_path));
        Logger.Log(LogType.DEBUG, "Entering sandbox mode...");
        
        // Acquire sandbox user
        create_sandbox_user_if_not_exists();

        // Enter sandboxing mode, detach from the namespace and pivot root
        enter_sandbox(process_path);

        String process = String.format("%s", PathUtils.get_processname_from_path(process_path));

        Logger.Log(LogType.DEBUG, String.format("Sandboxing proces [%s]...", process));

        // Own the process symlink
        own_process(process, get_user_id(), 65534);

        final ProcessBuilder p = new ProcessBuilder(process);
        p.redirectInput(Redirect.INHERIT);
        p.redirectOutput(Redirect.INHERIT);
        p.redirectError(Redirect.INHERIT);

        try {
            Logger.Log(LogType.DEBUG, String.format("Started process [%d] succesfully in sandbox mode.", c.syscall(SYS_GETPID)));
            p.start().waitFor();
        }
        catch(IOException | InterruptedException except) {
            Logger.Log(LogType.ERROR, String.format("Encountered error while starting process: [%s]", except.getMessage()));
            exit_sandbox();
            return;
        }

        exit_sandbox();
    }

    public void sandbox_processes(ArrayList<Argument> processes) {
        for(Argument arg : processes) {
            if (arg instanceof MultiArgument) {
                MultiArgument m_arg = (MultiArgument)arg;
                
                for (Map.Entry<String, String> entry : m_arg.get_values().entrySet()) {
                    if(entry.getKey() == "proc") {
                        sandbox_process(entry.getValue());
                    }
                }
            } else {
                if ( arg.get_name().startsWith("proc") ) {
                    this.sandbox_process(arg.get_value());
                }
            }
        }
    }
}
