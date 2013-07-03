package fr.thumbnailsdb;

import java.io.File;
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
     * @param image image for validation
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


    public static String fileToDirectory(String n) {
        int folderIndex = n.lastIndexOf('/');
        if (folderIndex < 0) {
            //it's probably a windows path
            folderIndex = n.lastIndexOf('\\');
        }
        // File file = new File(n);
//            //File parentDir = file.getParentFile(); // to get the parent dir
        return n.substring(0, folderIndex);//file.getParent(); // to get the parent dir name
    }


    public static int folderSize(String folder) {

        String[] folders = new File(folder).list();
        if (folders != null) {
            return folders.length;
        } else {
            return 0;
        }
    }

}
