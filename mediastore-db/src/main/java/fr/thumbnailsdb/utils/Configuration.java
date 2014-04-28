package fr.thumbnailsdb.utils;

import java.util.Enumeration;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: fhuet
 * Date: 10/02/2014
 * Time: 12:29
 * To change this template use File | Settings | File Templates.
 */
public class Configuration {

    static {
        Configuration.dump();
    }

    private static String MAX_THREADS = "mediastore.mediaindexer.maxthreads";
    private static String LOGGER_STDOUT = "mediastore.logger.out";
    private static String LOGGER_ERR = "mediastore.logger.err";
    private static String DRY_RUN = "mediastore.dry.run";
    private static String FORCE_GPS = "mediastore.force.gps";
    private static String FORCE_UPDATE = "mediastore.force.update";
    private static String TIMING="mediastore.timing";

    public static boolean loggerOutEnabled() {
        String out = System.getProperty(LOGGER_STDOUT);
        return (out != null);
    }

    public static boolean loggerErrEnabled() {
        String err = System.getProperty(LOGGER_ERR);
        return (err != null);
    }

    public static boolean dryRun() {
        String err = System.getProperty(DRY_RUN);
        return (err != null);
    }

    public static boolean forceGPS() {
        return (System.getProperty(FORCE_GPS) != null);
    }

    public static boolean forceUpdate() {
        return (System.getProperty(FORCE_UPDATE) != null);
    }

    public static boolean timing(){
        return (System.getProperty(TIMING) != null);
    }

    public static int getMaxIndexerThreads() {
        int maxThreads = 4;
        String maxT = System.getProperty(MAX_THREADS);
        if (maxT != null) {
            maxThreads = Integer.parseInt(maxT);

        }
        return maxThreads;
    }

    public static void dump() {
        System.out.println(" ----- Configuration   ---------");
        Properties props = System.getProperties();
        Enumeration names = props.propertyNames();
        while (names.hasMoreElements()) {
            String s = (String) names.nextElement();
            if (s.startsWith("mediastore")) {
                System.out.println("   " + s + "  " + props.getProperty(s));
            }

        }
        System.out.println(" -------------------------------");
    }


}
