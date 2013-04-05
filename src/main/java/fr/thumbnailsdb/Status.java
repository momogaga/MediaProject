package fr.thumbnailsdb;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created with IntelliJ IDEA.
 * User: fhuet
 * Date: 08/11/12
 * Time: 16:50
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement
public class Status {


    public static synchronized Status getStatus() {
       return status;
    }

    private static Status status = new Status("idle");

    public static String IDLE="idle";

    public static String FIND_SIMILAR="Searching similar images";

    @XmlElement
    private String stringStatus;

    private Status(String status) {
        this.stringStatus = status;
    }

    public String getStringStatus() {
        return stringStatus;

    }

    public void setStringStatus(String status) {
        //System.out.println("Status changed to " + status);
        this.stringStatus = status;
    }
}
