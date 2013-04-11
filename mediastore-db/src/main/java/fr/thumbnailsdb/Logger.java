package fr.thumbnailsdb;

public class Logger {

	private static Logger log = new Logger();
	
	
	public static Logger getLogger() {
		return log;
	}
	
	private Logger() {
		
	}
	
	public void log(String s) {
		System.out.println(s);
	}
	
	
}
