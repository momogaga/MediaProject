package fr.inria.oasis;

import java.util.ArrayList;
import java.util.Iterator;

public class Bucket {

	protected int number; 
	protected ArrayList<String> parts = new ArrayList<String>();
	
	public Bucket(int i) {
		this.number=i;
	}
	
	public void addPart(String s) {
		this.parts.add(s);
	}

	@Override
	public int hashCode() {
		return number;
	}

	@Override
	public String toString() {
		String r = "Bucket [number=" + number + ", parts=";
		for (String part : parts) {
			r+=part;
			r+=" ";
		}
		
		return r+  "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Bucket other = (Bucket) obj;
		if (number != other.number)
			return false;
		return true;
	}
	

	
}
