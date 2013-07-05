package fr.thumbnailsdb.duplicate;

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
    int filesInFolder1;


    @XmlElement
    int filesInFolder2;

    @XmlElement
     int occurences;
    @XmlElement
    long totalSize;

    @XmlElement
    ArrayList<FileWithSize> file1 = new ArrayList<FileWithSize>();

    @XmlElement
    ArrayList<FileWithSize> file2 = new ArrayList<FileWithSize>();


    public ArrayList<FileWithSize> getFile1() {
        return file1;
    }

    public ArrayList<FileWithSize> getFile2() {
        return file2;
    }



    public DuplicateFolderGroup(String f1, String f2) {
        folder1=f1;
        folder2=f2;
        occurences=1;
     }

    public void increase(){
       occurences++;
       //System.out.println("occurences was "+ (occurences-1) + " now " +occurences);
    }

    public void addFiles(String f1, String f2, long size) {
       file1.add(new FileWithSize(f1,size));
       file2.add(new FileWithSize(f2,size));
    }

    public void addSize(long s){
        totalSize+=s;
    }


    public void setFilesInFolder1(int filesInFolder1) {
        this.filesInFolder1 = filesInFolder1;
    }

    public void setFilesInFolder2(int filesInFolder2) {
        this.filesInFolder2 = filesInFolder2;
    }

}
