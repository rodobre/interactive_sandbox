package sysjail;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class CSVWriter {
    public static void WriteLog(String filename, String log) throws RuntimeException {
        Path filepath = Paths.get("/tmp/" + filename);

        try {
            BufferedWriter bw = Files.newBufferedWriter(filepath, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            String log_formatted = String.format("%s,%s\n", new SimpleDateFormat("dd/MM/yyyy,HH:mm:ss").format(new Date()), log);
            bw.write(log_formatted);
            bw.close();
        } catch (IOException e) {
        }
    }

    public static void WriteArray(String filename, ArrayList<String> info) throws RuntimeException {
        Path filepath = Paths.get(filename);

        try {
            BufferedWriter bw = Files.newBufferedWriter(filepath, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            String log_formatted = String.format("%s", new SimpleDateFormat("dd/MM/yyyy,HH:mm:ss").format(new Date()));
            bw.write(log_formatted);

            for(String array_element : info) {
                bw.write(String.format(",%s", array_element));
            }
            bw.write("\n");
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to the file!", e);
        }
    }
}