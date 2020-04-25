package sysjail;
import java.text.SimpleDateFormat;  
import java.util.Date;

class PrettyDate {
    public static String get_date() {
        SimpleDateFormat formatter= new SimpleDateFormat("[dd-MM-yyyy 'at' HH:mm:ss z] ");
        Date date = new Date(System.currentTimeMillis());
        String date_fmt = formatter.format(date);
        return date_fmt;
    }
}

class PrintColor {
    static final String green = "\u001B[32m";
    static final String lblue = "\u001B[36m";
    static final String orange= "\u001B[33m";
    static final String red   = "\u001B[31m";
    static final String bolds = "\u001B[97;1m";
    static final String bolde = "\u001B[0m";
}

public class Logger {
    public static void Log(LogType log_type, String log) {
        String date_fmt = PrettyDate.get_date();
        String color_switch = "";

        switch(log_type) {
            case INFO:
                color_switch = PrintColor.bolds + "[" + PrintColor.green + "   INFO\u001B[97m]: " + PrintColor.bolde;
                break;

            case DEBUG:
                color_switch = PrintColor.bolds + "[" + PrintColor.lblue + "  DEBUG\u001B[97m]: " + PrintColor.bolde;
                break;

            case WARNING:
                color_switch = PrintColor.bolds + "[" + PrintColor.orange + "WARNING\u001B[97m]:  " + PrintColor.bolde;
                break;
            
            case ERROR:
                color_switch = PrintColor.bolds + "[" + PrintColor.red + "  ERROR\u001B[97m]:  " + PrintColor.bolde;
                break;
        }

        String log_string = date_fmt + color_switch + log;
        System.out.println(log_string);
        CSVPersistenceProvider.WriteLog("logger_log.csv", log_string);
    }
}
