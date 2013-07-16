package fr.thumbnailsdb;

import fr.thumbnailsdb.duplicate.DuplicateFileGroup;
import fr.thumbnailsdb.duplicate.DuplicateFileList;
import fr.thumbnailsdb.duplicate.DuplicateFolderList;
import fr.thumbnailsdb.utils.Logger;

import java.sql.ResultSet;
import java.util.Iterator;
import java.util.List;

public class DuplicateMediaFinder {

    protected ThumbStore thumbstore;

    protected DuplicateFileList duplicateFileList;

    public DuplicateMediaFinder(ThumbStore c) {
        this.thumbstore = c;
    }

    public PreloadedDescriptors findDuplicateMedia() {
//        return thumbstore.getMFDOrderedByMD5();
        return thumbstore.getPreloadedDescriptors();
    }

    public void prettyPrintDuplicate(ResultSet r) {
//        DuplicateFileList list = computeDuplicateSets(r);
//        for (DuplicateFileGroup dg : list) {
//            System.out.println(dg.fileSize + " (" + dg.fileSize * (dg.size() - 1) + " to save) ");
//            System.out.println(dg);
//        }
    }

    public void prettyPrintDuplicateFolder(ResultSet r) {
//       Collection<DuplicateFolderGroup> map = computeDuplicateFolderSets(r).asSortedCollection(null);
//        Iterator<DuplicateFolderGroup> it = map.iterator();
//            //System.out.println(k + "  have " + map.get(k).occurences + " common files");
//        while (it.hasNext()) {
//        DuplicateFolderGroup df = it.next();
//           System.out.println(df.occurences + " " + df.folder1 +   "  " + df.folder2);
//        }
//        for (DuplicateFileGroup dg : tree) {
//            System.out.println(dg.fileSize + " (" + dg.fileSize * (dg.size() - 1) + " to save) ");
//            System.out.println(dg);
//        }

    }

    public DuplicateFileList computeDuplicateSets(PreloadedDescriptors r) {
        if (duplicateFileList != null) {
            return duplicateFileList;
        }
        duplicateFileList = new DuplicateFileList();
        DuplicateFileGroup dg = new DuplicateFileGroup();
        String currentMd5 = "";
        Iterator<MediaFileDescriptor> it = r.iterator();
        while (it.hasNext()) {
            MediaFileDescriptor mfd = it.next();
            String md5 = mfd.getMD5();
            if (md5 != null) {
                //TODO : this should be done in the DB directly
                int index = mfd.getId();
                String path = ThumbStore.getPath(mfd.getConnection(), index);
                if (md5.equals(currentMd5)) {
                    // add to current group
                    dg.add(mfd.getSize(), path);
                } else {
                    if (dg.size() > 1) {
                        duplicateFileList.add(dg);
                    }
                    dg = new DuplicateFileGroup();
                    dg.add(mfd.getSize(), path);
                    currentMd5 = md5;

                }
            }
        }
        if (dg.size() > 1) {
            duplicateFileList.add(dg);
        }

        return duplicateFileList;
    }

    /**
     * @param r the set of files sorted by md5 value
     * @return
     */
    public DuplicateFolderList computeDuplicateFolderSets(PreloadedDescriptors r) {
        Logger.getLogger().log("DuplicateMediaFinder.computeDuplicateFolderSets preloadedDescriptors  V2 " + r.size());
        long t0 = System.currentTimeMillis();
        DuplicateFileGroup dg = new DuplicateFileGroup();
        String currentMd5 = "";
        //The table to maintain the tree of folder-couples and the
        //the number of common files they have
        DuplicateFolderList dfl = new DuplicateFolderList();
        Iterator<String> it = r.keyIterator();
        while (it.hasNext()) {
            List<MediaFileDescriptor> mList = r.get(it.next());
            if (mList.size() > 1) {
                dg = new DuplicateFileGroup();
                Iterator<MediaFileDescriptor> itMedia = mList.iterator();
                while (itMedia.hasNext()) {
                    MediaFileDescriptor mfd = itMedia.next();
                    int index = mfd.getId();
                    String path = thumbstore.getPath(mfd.getConnection(), index);
                    mfd.setPath(path);
                    dg.add(mfd.getSize(), mfd.getPath());
                }
                if (dg.size() > 1) {
                    //ok we have found a tree of duplicate files
                    //let's add their parent folder to the tree
                    //first compute the tree of folders
                    dfl.addOrIncrement(dg);
                }
            }
        }

        long t1 = System.currentTimeMillis();
        System.out.println("DuplicateMediaFinder.computeDuplicateFolderSets took "  + (t1-t0) + "  ms");
        Logger.getLogger().log("DuplicateMediaFinder.computeDuplicateFolderSets has " + dfl.size() + " entries");
        return dfl;
    }


//    public DuplicateFolderList computeDuplicateFolderSets(PreloadedDescriptors r) {
//        Logger.getLogger().log("DuplicateMediaFinder.computeDuplicateFolderSets preloadedDescriptors  "+ r.size());
//        DuplicateFileGroup dg = new DuplicateFileGroup();
//        String currentMd5 = "";
//        long t0 = System.currentTimeMillis();
//        //The table to maintain the tree of folder-couples and the
//        //the number of common files they have
//        DuplicateFolderList dfl = new DuplicateFolderList();
//        Iterator<MediaFileDescriptor> it = r.iterator();
//        while (it.hasNext()) {
//            MediaFileDescriptor mfd = it.next();
//            String md5 = mfd.getMD5();
//            //TODO : this should be done in the DB directly
//            int index = mfd.getId();
//            String path = thumbstore.getPath(mfd.getConnection(), index);
//            mfd.setPath(path);
//            //TODO : use multimaps to avoid this part. Simply get keys with multiple values
//            if (md5.equals(currentMd5)) {
//                dg.add(mfd.getSize(), mfd.getPath());
//            } else {
//                if (dg.size() > 1) {
//                    dfl.addOrIncrement(dg);
//                }
//                dg = new DuplicateFileGroup();
//                dg.add(mfd.getSize(), mfd.getPath());
//                currentMd5 = md5;
//            }
//        }
//        if (dg.size() > 1) {
//            //ok we have found a tree of duplicate files
//            //let's add their parent folder to the tree
//            //first compute the tree of folders
//            dfl.addOrIncrement(dg);
//        }
//        long t1 = System.currentTimeMillis();
//        System.out.println("DuplicateMediaFinder.computeDuplicateFolderSets took "  + (t1-t0) + "  ms");
//        Logger.getLogger().log("DuplicateMediaFinder.computeDuplicateFolderSets has " + dfl.size() + " entries");
//        return dfl;
//    }

    // TODO : save result for subsequent requests
    public String prettyHTMLDuplicate(ResultSet r, int max) {
//        DuplicateFileList list = computeDuplicateSets(r);
//        System.out.println("DuplicateMediaFinder.prettyHTMLDuplicate() " + max);
//        String result = "";
//        int i = 0;
//        for (DuplicateFileGroup dg : list) {
//            result += dg.fileSize + " (" + dg.fileSize * (dg.size() - 1) + " to save)\n";
//            result += dg + "\n";
//            i++;
//            if (i > max) {
//                break;
//            }
//        }
//        System.out.println("DuplicateMediaFinder.prettyHTMLDuplicate() " + result);
//        return result;
        return "";
    }


}
