package fr.thumbnailsdb;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: fhuet
 * Date: 18/06/13
 * Time: 17:45
 * To change this template use File | Settings | File Templates.
 */
public class PreloadedDescriptors<T> {


   // protected ArrayList<T> list;
    protected TreeSet<T> list;

    public PreloadedDescriptors(int size, Comparator comp) {
        this.list = new TreeSet<T>(comp); //new ArrayList<T>(size);
    }


    public void add(T t) {
        list.add(t);
    }

    public int size() {
        return list.size();
    }
    public void clear() {
        this.clear();
    }

    public Iterator iterator() {
        return list.iterator();
    }


    public void sort() {
        //Collections.sort(list, comp);
    }

}
