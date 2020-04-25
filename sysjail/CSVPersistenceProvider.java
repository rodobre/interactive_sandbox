package sysjail;

import java.util.ArrayList;
import java.util.HashMap;

public class CSVPersistenceProvider {
    private static CSVPersistenceProvider singleton_instance = null;

    private CSVPersistenceProvider() {
        System.out.println("Initializing CSVPersistenceProvider...");
    }

    public static CSVPersistenceProvider CSVPersistenceProvider() {
        if (singleton_instance == null) {
            singleton_instance = new CSVPersistenceProvider();
        }
        return singleton_instance;
    }

    public static void WriteLog(String filename, String action) throws RuntimeException {
        CSVWriter.WriteLog(filename, action);
    }

    public static void Write(String filename, ArrayList<String> content) throws RuntimeException {
        CSVWriter.WriteArray(filename, content);
    }

    public static HashMap<String, String> ReadSettings(String filename) throws RuntimeException {
        return CSVReader.ReadSettings(filename);
    }
}