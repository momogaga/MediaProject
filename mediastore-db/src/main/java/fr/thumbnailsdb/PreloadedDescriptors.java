package fr.thumbnailsdb;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.TreeMultimap;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: fhuet
 * Date: 18/06/13
 * Time: 17:45
 * To change this template use File | Settings | File Templates.
 */
public class PreloadedDescriptors {
    //TODO : use google multimap
    protected ArrayListMultimap<String, MediaFileDescriptor> list;


    //protected ArrayList<MediaFileDescriptor> list;
    protected Comparator comp;
    //protected TreeMap<String,MediaFileDescriptor> list;

    public PreloadedDescriptors(int size, Comparator comp) {
        this.comp = comp;
        //this.list = new ArrayList<MediaFileDescriptor>();//new TreeMap<String,MediaFileDescriptor>(comp); //new ArrayList<T>(size);
        this.list = ArrayListMultimap.<String, MediaFileDescriptor>create(size, 10);
    }


    public void add(MediaFileDescriptor t) {
        //  Logger.getLogger().log("PreloadedDescriptors.add " + t);
     //   list.add(t);
        this.list.put(t.getMD5(),t);
    }

    public void remove(MediaFileDescriptor t) {
      //  System.out.println("PreloadedDescriptors.remove " +t);
        this.list.remove(t.getMD5(),t);

    }

    public int size() {
        return list.size();
    }

    public void clear() {
        this.clear();
    }

    public Iterator iterator() {
     //   return list.iterator();
        return list.values().iterator();
       // return null;
    }


    public void sort() {
      //  Collections.sort(list, comp);
    }


    public String toString() {
        StringBuffer str = new StringBuffer();
        for (String key : list.keySet()) {
            str.append("[ ");
            str.append(key);
            str.append(" : ");
            str.append(java.util.Arrays.toString(list.get(key).toArray()));
            str.append(" ]");
        }
        return str.toString();
    }

}
