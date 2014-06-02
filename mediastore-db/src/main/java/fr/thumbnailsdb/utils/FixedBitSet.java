package fr.thumbnailsdb.utils;

import java.io.Serializable;
import java.util.BitSet;

/**
 * A bitset with a fixed length
 */
public class FixedBitSet implements Serializable, Comparable<FixedBitSet> {


    private int size;
    private BitSet b;

    public FixedBitSet(int size) {
        this.size =size;
        this.b=new BitSet();
    }

    public  FixedBitSet(String s) {
        this(s.length());
        for(int i =0;i<s.length();i++) {
            if (s.charAt(i)=='1') {
                b.set(i);
            }
        }
    }


    public void set(int index) {
      b.set(index);
    }

    public String toString(){
            String r = new String();
            for(int i=0;i<size;i++) {
                if(b.get(i)) {
                    r+="1";
                }else {
                    r+="0";
                }
            }
            return r;
    }


    public static void main(String[] args) {
        String s = "1100000000111100000011111000001100011000111011000101111010001111111100110111010000001110100011101000";
        String s2= "1100000000111100000011111000001100011000111011000101111010001111111100110111010000001110100011101";
        FixedBitSet b = new FixedBitSet(s);
        System.out.println("fr.thumbnailsdb.utils.FixedBitSet fromString " + b );
    }


    @Override
    public int compareTo(FixedBitSet o) {
        if(this.size > this.size) {
            return 1;
        } else if(o.size > this.size) {
            return -1;
        } else {
            for(int i = 0; i < size; i++) {
                if(this.b.get(i) != o.b.get(i)) {
                    if(this.b.get(i)) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            }
            return 0;
        }
    }
}
