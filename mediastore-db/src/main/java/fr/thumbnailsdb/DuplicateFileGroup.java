package fr.thumbnailsdb;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DuplicateFileGroup {
	long fileSize;
	@XmlElement
	ArrayList<String> al = new ArrayList<String>();

	public DuplicateFileGroup() {
		super();
		// this.individualSize = individualSize;
	}

	public void add(long size, String path) {
		this.fileSize = size;
		al.add(path);
	}

	public long getFileSize() {
		return fileSize;
	}

	public int size() {
		return al.size();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Iterator iterator = al.iterator(); iterator.hasNext();) {
			sb.append(iterator.next() + "\n");
		}

		return sb.toString();
	}

    public void sort() {
        Collections.sort(al);
    }

    public String get(int i) {
        return al.get(i);
    }


    public boolean match(String filter) {
        for (String s : al) {
            if (s.contains(filter)) {
                return true;
            }
        }
        return false;
    }

}