package fr.thumbnailsdb;

import java.util.*;


public class DuplicateFileList implements Iterable<DuplicateFileGroup> {

    TreeSet<DuplicateFileGroup> tree = new TreeSet<DuplicateFileGroup>(new Comparator<DuplicateFileGroup>() {
        // @Override
        public int compare(DuplicateFileGroup o1, DuplicateFileGroup o2) {
            return Double.compare(o2.getFileSize(), o1.getFileSize());
        }
    });


    public void add(DuplicateFileGroup dg) {
        tree.add(dg);
    }


    public Iterator<DuplicateFileGroup> iterator() {
        // TODO Auto-generated method stub
        return tree.iterator();
    }

    public DuplicateFileGroup[] toArray() {
        return tree.toArray(new DuplicateFileGroup[]{});
    }


    public DuplicateFileGroup getFirst() {
        return tree.first();
    }

    public Collection<DuplicateFileGroup> toCollection(int max, String[] filter) {
        System.out.println("DuplicateFileList.toCollection filters : " + filter[0]);
        ArrayList<DuplicateFileGroup> al = new ArrayList<DuplicateFileGroup>(max);
        Iterator<DuplicateFileGroup> it = tree.iterator();
        int i = 0;
        while (it.hasNext() && i < max) {
            DuplicateFileGroup dfg = it.next();
            if (this.match(dfg, filter)) {
                al.add(dfg);

                i++;
            }
        }
        return al;
    }

    private boolean match(DuplicateFileGroup d, String[] filter) {
        for (String s : filter) {
            System.out.println("DuplicateFileList.match " + d + " with filter " +s);
            if (d.match(s)) {
                return true;
            }
        }
        return false;
    }


}
