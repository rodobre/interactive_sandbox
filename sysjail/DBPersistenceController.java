package sysjail;

import java.sql.*;
import java.util.ArrayList;

public class DBPersistenceController {

    private static DBPersistenceController instance;
    private static Connection conn;
    private static Statement stmt;
    private static ResultSet rs;

    private DBPersistenceController() {
        try {
            Class.forName("org.sqlite.JDBC");

            conn = DriverManager.getConnection("jdbc:sqlite:/tmp/database.db");
            stmt = conn.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS native_settings (sandbox_hostname TEXT NOT NULL, sandbox_mtdir TEXT NOT NULL, old_root TEXT NOT NULL);");
            stmt.executeUpdate("INSERT INTO native_settings (sandbox_hostname, sandbox_mtdir, old_root) SELECT \"sysjail\", \"./.sysjail_root\", \".oldroot\" WHERE NOT EXISTS (SELECT 1 FROM native_settings);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS main_logs (log TEXT);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS native_logs (log TEXT);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS parser_logs (log TEXT);");
        } catch(ClassNotFoundException e) {
            System.err.println("Couldn't find the JDBC driver. Error: " + e);
        } catch (SQLException e) {
            System.err.println("Couldn't connect to the database. Error: " + e);
        }
    }

    static public DBPersistenceController GetInstance() {

        if (instance == null) {
            instance = new DBPersistenceController();
        }

        return instance;
    }

    static public ArrayList<String> FetchNativeOptions() {
        ArrayList<String> arr = new ArrayList<String>();

        try {
            rs = conn.createStatement().executeQuery("SELECT * FROM native_settings;");

            arr.add(rs.getString(1));
            arr.add(rs.getString(2));
            arr.add(rs.getString(3));
        } catch (SQLException e) {
            Logger.Log(LogType.ERROR, "SQLite error encountered when trying to fetch options from the database. Error: " + e);
        }
        
        return arr;
    }

    static public void WriteLog(DBLogEnum log_dest, String log) {
        try {
            PreparedStatement prep_stmt;
            switch(log_dest) {
                case MAIN:
                    prep_stmt = conn.prepareStatement("INSERT INTO main_logs (log) VALUES (?)");
                    prep_stmt.setString(1, log);
                    prep_stmt.executeUpdate();
                    break;
                case NATIVE:
                    prep_stmt = conn.prepareStatement("INSERT INTO native_logs (log) VALUES (?)");
                    prep_stmt.setString(1, log);
                    prep_stmt.executeUpdate();
                    break;
                case PARSER:
                    prep_stmt = conn.prepareStatement("INSERT INTO parser_logs (log) VALUES (?)");
                    prep_stmt.setString(1, log);
                    prep_stmt.executeUpdate();
                    break;
            }
        } catch (SQLException e) {
            Logger.Log(LogType.ERROR, "SQLite error encountered when trying to log events to the databse. Error: " + e);
        }
    }
}
