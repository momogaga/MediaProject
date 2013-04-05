package fr.thumbnailsdb;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
	private static final String IMAGE_PATTERN = "(.+(\\.(?i)(jpg|jpeg|png|gif|bmp))$)";
	private static final String VIDEO_PATTERN = "(.+(\\.(?i)(avi|flv|mpg|mp4|wmv))$)";

	private static Pattern imagePattern = Pattern.compile(IMAGE_PATTERN);
	private static Matcher imageMatcher;
	private static Pattern videoPattern = Pattern.compile(VIDEO_PATTERN);
	private static Matcher videoMatcher;

	
	/**
	 * Validate image with regular expression
	 * 
	 * @param image
	 *            image for validation
	 * @return true valid image, false invalid image
	 */
	public static boolean isValideImageName(final String image) {
		imageMatcher = imagePattern.matcher(image);
		return imageMatcher.matches();
	}

	public static boolean isValideVideoName(final String video) {
		videoMatcher = videoPattern.matcher(video);
		return videoMatcher.matches();
	}
	
}
