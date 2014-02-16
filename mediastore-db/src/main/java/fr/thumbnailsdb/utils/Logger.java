package fr.thumbnailsdb.utils;

public class Logger {

    private static Logger log = new Logger();
    private static boolean logEnabled = Configuration.loggerOutEnabled();
    private static boolean errEnabled = Configuration.loggerErrEnabled();


    public static Logger getLogger() {
        return log;
    }

    private Logger() {

    }

    public boolean isEnabled() {
        return getLogger().logEnabled;
    }

    public void log(String s) {
        if (logEnabled) {
            System.out.println(s);
        }
    }

    public void err(String s) {
        if (errEnabled) {
            System.err.println(s);
        }
    }


}
