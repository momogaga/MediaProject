package fr.thumbnailsdb.vptree.distances;

import fr.thumbnailsdb.ImageComparator;
import fr.thumbnailsdb.MediaFileDescriptor;

/**
 * Created with IntelliJ IDEA.
 * User: fhuet
 * Date: 31/10/12
 * Time: 16:41
 * To change this template use File | Settings | File Templates.
 */
public class VPRMSEDistance extends Distance {

    @Override
    public double d(Object x, Object y) {
        MediaFileDescriptor mf1 = (MediaFileDescriptor) x;
        MediaFileDescriptor mf2 = (MediaFileDescriptor) y ;
//        System.out.println("VPRMSEDistance.d " + x + "<-> " + y + " RMSE : " + ImageComparator.compareARGBUsingRMSE(mf1.getData(), mf2.getData()));

        return  ImageComparator.compareARGBUsingRMSE(mf1.getData(), mf2.getData());
    }
}
