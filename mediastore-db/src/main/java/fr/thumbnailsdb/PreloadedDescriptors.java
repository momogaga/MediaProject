package fr.thumbnailsdb;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: fhuet
 * Date: 18/06/13
 * Time: 17:45
 * To change this template use File | Settings | File Templates.
 */
public class PreloadedDescriptors<MediaFileDescriptor> {
                  //TODO : use google multimap

   protected ArrayList<MediaFileDescriptor> list;
    protected Comparator comp;
    //protected TreeMap<String,MediaFileDescriptor> list;

    public PreloadedDescriptors(int size, Comparator comp) {
        this.comp=comp;
        this.list = new ArrayList<MediaFileDescriptor>();//new TreeMap<String,MediaFileDescriptor>(comp); //new ArrayList<T>(size);
    }


    public void add(MediaFileDescriptor t) {
      //  Logger.getLogger().log("PreloadedDescriptors.add " + t);
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
        Collections.sort(list, comp);
    }

}
