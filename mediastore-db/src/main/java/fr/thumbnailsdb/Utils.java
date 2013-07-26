package fr.thumbnailsdb;

import java.io.*;
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
        if (n == null) return null;

        int folderIndex = n.lastIndexOf('/');
        if (folderIndex < 0) {
            //it's probably a windows path
            folderIndex = n.lastIndexOf('\\');
        }
        return n.substring(0, folderIndex);//file.getParent(); // to get the parent dir name
    }


    public static int folderSize(String folder) {

        String[] folders = new File(folder).list();
        if (folders != null) {
            int total = 0;
            for (String s : folders) {
                if (isValideImageName(s) || isValideVideoName(s)) {
                    total++;
                }
            }
            return total;
        } else {
            return 0;
        }
    }

    public static int[] toIntArray(byte[] d) {
        int[] idata = null;
        if (d != null) {
            ObjectInputStream oi = null;
            try {
                oi = new ObjectInputStream(new ByteArrayInputStream(d));
                idata = (int[]) oi.readObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }
        return idata;
    }

    public static byte[] toByteArray(int[] data) {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        ObjectOutputStream oi = null;
        try {
            oi = new ObjectOutputStream(ba);
            oi.writeObject(data);
            oi.close();
            return ba.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
