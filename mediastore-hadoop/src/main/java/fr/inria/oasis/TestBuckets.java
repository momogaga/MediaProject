package fr.inria.oasis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

public class TestBuckets {

	protected int partitions;

	protected int buckets;

	protected HashMap<Integer, Bucket> map = new HashMap<Integer, Bucket>();

	public TestBuckets(int p) {
		this.partitions = p;
		this.buckets = total(p);
		for (int i=0;i<this.buckets;i++) {
			map.put(i,new Bucket(i));
		}
	}

	protected int total(int p) {
		return p * (p + 1) / 2;
	}

	protected int maxColumnIndexForLine(int i) {
		// return partitions-i-1;
		return partitions - i - 1;
	}

	protected int maxLineIndexForColumn(int j) {

		// return partitions -1 -j;
		return partitions - j - 1;
	}

	protected int index(int i, int j) {
		if (j > maxColumnIndexForLine(i) || i > maxColumnIndexForLine(j)) {
			return -1;
		}

		// compute how many we have before the current line
		// consider the whole triangle and remove a subset
//		int before = this.buckets - total(maxColumnIndexForLine(i) + 1);
//		return before + j;
		
		int before = this.buckets - total(this.partitions-i);
		return before + j;
		
	}

	protected void dumpBucketsNumForLine(int i) {
		if (i > partitions - 1) {
			return;
		}

		String s = "";
		for (int j = 0; j < this.maxColumnIndexForLine(i); j++) {
			s = s + index(i, j) + " ";
		}
		System.out.println("TestBuckets.dumpBucketsNumForLine() i= " + i
				+ " : " + s);
	}
	
	protected void addToBucket(String s, int i, int j) {
		int ind = index(i,j);
//		System.out.println("TestBuckets.addToBucket() looking for bucket " + ind);
		map.get(ind).addPart(s);
	}
	

	public void simulatePartitioning() {
		String data[] = new String[] { "a", "b", "c", "d" };
		Random r = new Random(0);
		System.out.println("TestBuckets.simulatePartitioning() with "
				+ this.partitions + " partitions");
		for (int d = 0; d < data.length; d++) {
			// select a partition
			int par = d; //r.nextInt(this.partitions);
			// output lines
			System.out.println("----- data " + data[d] + " --- partition "
					+ par);
		//	System.out.println("lines");
			for (int j = 0; j <= maxColumnIndexForLine(par); j++) {
		//		System.out.println("   (" + par + ";" + j + ")");
				this.addToBucket(data[d], par, j);
			}
		//	System.out.println("columns ");

			for (int i = par - 1; i >= 0; i--) {
		//		System.out.println("   (" + i + ";" + (par - i - 1) + ")");
				this.addToBucket(data[d], i, par-i-1);
			}

			// output columns
		}
	}

	@Override
	public String toString() {
		return "TestBuckets [partitions=" + partitions + ", buckets=" + buckets
				+ ", maxColonne=" + maxColumnIndexForLine(0) + ", maxLigne="
				+ maxLineIndexForColumn(0) + "]";
	}

	public void dumpBuckets() {
		for (Integer key : map.keySet()) {
			System.out.println(map.get(key));
		}
	}
	
	public static void main(String[] args) {

		int nbPartitions = 4;
		TestBuckets tb = new TestBuckets(nbPartitions);
		System.out.println(tb);
		tb.simulatePartitioning();
		tb.dumpBuckets();

		// System.out.println(tb);
		// for (int i = 0; i < nbPartitions-1; i++) {
		// tb.dumpBucketsNumForLine(i);
		// }

	}

}
