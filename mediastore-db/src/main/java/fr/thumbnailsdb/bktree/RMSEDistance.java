package fr.thumbnailsdb.bktree;

import fr.thumbnailsdb.ImageComparator;
import fr.thumbnailsdb.MediaFileDescriptor;

/**
 * Created with IntelliJ IDEA.
 * User: fhuet
 * Date: 31/10/12
 * Time: 14:54
 * To change this template use File | Settings | File Templates.
 */
public class RMSEDistance implements Distance{

    public int getDistance(Object object1, Object object2) {
        MediaFileDescriptor mf1 = (MediaFileDescriptor) object1;
        MediaFileDescriptor mf2 = (MediaFileDescriptor) object2 ;
        //TODO fixe for hash
        return 0;
       // return (int) ImageComparator.compareARGBUsingRMSE(mf1.getData(), mf2.getData());

        //return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
