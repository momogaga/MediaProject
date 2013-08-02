package fr.inria.oasis;

import fr.thumbnailsdb.ImageComparator;
import fr.thumbnailsdb.Utils;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: fhuet
 * Date: 02/08/13
 * Time: 13:57
 * To change this template use File | Settings | File Templates.
 */
public class TestSignature {

    private String signatureFileName;

    public TestSignature(String n) {
        this.signatureFileName = n;
    }


    public void test() {

        File f = new File(this.signatureFileName);
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));

            String s1 = br.readLine();
            String s2 = br.readLine();

            String base64s1 = s1.split(",")[1];
            String base64s2 = s2.split(",")[1];

            System.out.println("TestSignature.test base64s1 = " + base64s1);
            System.out.println("TestSignature.test base64s2 = " + base64s2);

            double result = ImageComparator.compareRGBUsingRMSE(Utils.base64ImgToIntArray(base64s1), Utils.base64ImgToIntArray(base64s2));

            System.out.println("Distance is " + result);

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: " + TestSignature.class + " <signatureFile>");
        }
        TestSignature t = new TestSignature(args[0]);
        t.test();
    }

}
