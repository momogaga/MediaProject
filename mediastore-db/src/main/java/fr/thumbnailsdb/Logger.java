package fr.thumbnailsdb;

public class Logger {

	private static Logger log = new Logger();
	private static boolean enabled = true;

	
	public static Logger getLogger() {
		return log;
	}
	
	private Logger() {
		
	}

    public boolean isEnabled() {
        return getLogger().enabled;
    }

	public void log(String s) {
		System.out.println(s);
	}
	
	
}
