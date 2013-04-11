package fr.thumbnailsdb;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class DuplicateMediaFinder {

    protected ThumbStore thumbstore;

    protected DuplicateFileList duplicateFileList;

    public DuplicateMediaFinder(ThumbStore c) {
        this.thumbstore = c;
    }

    public ArrayList<MediaFileDescriptor> findDuplicateMedia() {
        return thumbstore.getMFDOrderedByMD5();
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

    public DuplicateFileList computeDuplicateSets(ArrayList<MediaFileDescriptor> r) {
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
                if (md5.equals(currentMd5)) {
                    // add to current group
                    dg.add(mfd.getSize(), mfd.getPath());
                } else {
                    if (dg.size() > 1) {
                        duplicateFileList.add(dg);
                    }
                    dg = new DuplicateFileGroup();
                    dg.add(mfd.getSize(), mfd.getPath());
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
    public DuplicateFolderList computeDuplicateFolderSets(ArrayList<MediaFileDescriptor> r) {
        //  DuplicateFileList list = new DuplicateFileList();
        DuplicateFileGroup dg = new DuplicateFileGroup();
        String currentMd5 = "";
        //The table to maintain the tree of folder-couples and the
        //the number of common files they have
        DuplicateFolderList dfl = new DuplicateFolderList();
        Iterator<MediaFileDescriptor> it = r.iterator();
        while (it.hasNext()) {
            MediaFileDescriptor mfd = it.next();
            String md5 = mfd.getMD5();
            if (md5.equals(currentMd5)) {
                dg.add(mfd.getSize(), mfd.getPath());
            } else {
                if (dg.size() > 1) {
                    dfl.addOrIncrement(dg);
                }
                dg = new DuplicateFileGroup();
                dg.add(mfd.getSize(), mfd.getPath());
                currentMd5 = md5;

            }
        }
        if (dg.size() > 1) {
            //ok we have found a tree of duplicate files
            //let's add their parent folder to the tree
            //first compute the tree of folders
            dfl.addOrIncrement(dg);
        }
        System.out.println("DuplicateMediaFinder.computeDuplicateFolderSets has " + dfl.size() + " entries");
        return dfl;
    }

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
