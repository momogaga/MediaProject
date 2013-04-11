package fr.thumbnailsdb;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: fhuet
 * Date: 25/10/12
 * Time: 16:56
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement
public class DuplicateFolderGroup {
    @XmlElement
    String folder1;
    @XmlElement
    String folder2;
    @XmlElement
     int occurences;
    @XmlElement
    long totalSize;

    public ArrayList<String> getFile1() {
        return file1;
    }

    public ArrayList<String> getFile2() {
        return file2;
    }

    // @XmlElement
    ArrayList<String> file1 = new ArrayList<String>();

   // @XmlElement
    ArrayList<String> file2 = new ArrayList<String>();

    public DuplicateFolderGroup(String f1, String f2) {
        folder1=f1;
        folder2=f2;
         occurences=1;
     }

    public void increase(){
       occurences++;
       //System.out.println("occurences was "+ (occurences-1) + " now " +occurences);
    }

    public void addFiles(String f1, String f2) {
       file1.add(f1);
       file2.add(f2);
    }

    public void addSize(long s){
        totalSize+=s;
    }

}
