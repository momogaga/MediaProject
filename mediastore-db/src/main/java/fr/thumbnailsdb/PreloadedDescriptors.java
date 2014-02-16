package fr.thumbnailsdb;

import com.google.common.collect.ArrayListMultimap;


import java.util.*;

public class PreloadedDescriptors {

    protected ArrayListMultimap<String, MediaFileDescriptor> list;


    protected Comparator comp;

    public PreloadedDescriptors(int size, Comparator comp) {
        this.comp = comp;
        this.list = ArrayListMultimap.<String, MediaFileDescriptor>create(size, 10);
    }


    public void add(MediaFileDescriptor t) {
        this.list.put(t.getMD5(),t);
    }

    public void remove(MediaFileDescriptor t) {
        this.list.remove(t.getMD5(),t);

    }

    public int size() {
        return list.size();
    }

    public void clear() {
        this.list.clear();
    }

    public Iterator iterator() {
        return list.values().iterator();
    }

    public Iterator keyIterator() {
        return list.keySet().iterator();
    }

    public List<MediaFileDescriptor> get(String key) {
        return list.get(key);
    }


    public void sort() {
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
