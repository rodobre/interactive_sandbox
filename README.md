## Interactive Sandbox designed for Linux amd-64

The project here implements a minimal sandbox leveraging Linux syscalls in order to run a process in a separate namespace to test functionality and behaviour. This project is still WIP.
The sandbox only provides a minimal layer of security, do not use it for anything else than personal projects

### Types

The project exports multiple Java classes, such as LogType, Logger, Argument, MultiArgument, ArgumentParser, NativeInterfaceConnector, CStdLib, and many others.

### How to build

Under Linux x86-64, if running Ubuntu/Debian, run the following:

```
sudo apt-get -y install libjna-java libjni-java libjna-platform-java
git clone https://github.com/rodobre/interactive_sandbox
cd interactive_sandbox
chmod +x build.sh
./build.sh
```

To add the command to the path, run the following:

```
chmod +x sysjail.sh
ln run.sh /bin/sysjail
```

### Demo

![Sandboxing /bin/bash](/demo/sandbox_demo.PNG)