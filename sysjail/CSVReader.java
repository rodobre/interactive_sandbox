package sysjail;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.HashMap;


public class CSVReader {
    public static HashMap<String, String> ReadSettings(String filename) throws RuntimeException {
        HashMap<String, String> settings = new HashMap<String, String>();

        String line = "", splitBy = ",";

        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null) {
                String[] options = line.split(splitBy);
                settings.put(options[0], options[1]);
                System.out.println("Read setting [" + options[0] + "] = \"" + options[1] + "\"");
            }
            br.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not find " + filename);
        } catch (IOException e) {
            throw new RuntimeException("Could not read from file!", e);
        }
        
        return settings;
    }
}