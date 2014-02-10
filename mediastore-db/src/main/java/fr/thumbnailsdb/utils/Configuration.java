package fr.thumbnailsdb.utils;

/**
 * Created with IntelliJ IDEA.
 * User: fhuet
 * Date: 10/02/2014
 * Time: 12:29
 * To change this template use File | Settings | File Templates.
 */
public class Configuration {


    private static String MAX_THREADS= "mediastore.mediaindexer.maxthreads";

    private static String LOGGER_STDOUT="mediastore.logger.out";

    private static String LOGGER_ERR= "mediastore.logger.err" ;


    public static boolean loggerOutEnabled() {
        String out = System.getProperty(LOGGER_STDOUT);
        return (out!=null);
    }

    public static boolean loggerErrEnabled() {
        String err = System.getProperty(LOGGER_ERR);
        return (err!=null);
    }



    public static int getMaxIndexerThreads() {
        int maxThreads = 4;
        String maxT = System.getProperty(MAX_THREADS);
        if (maxT!=null) {
            maxThreads = Integer.parseInt(maxT);

        }
        return maxThreads;
    }

}
