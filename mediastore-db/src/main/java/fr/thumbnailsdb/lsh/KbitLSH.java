package fr.thumbnailsdb.lsh;


import fr.thumbnailsdb.utils.FixedBitSet;

import java.util.Random;

/**
 * A k-bit locality sensitive hash function
 *
 */
public class KbitLSH {


    int[] indexes;

    public KbitLSH(int nbBits, int keyLength){
        indexes = new int[nbBits];

        Random r = new Random();
        for (int i = 0; i < nbBits; i++) {
             indexes[i]=r.nextInt(keyLength);
        }
    }

    public KbitLSH(int nbBits, int keyLength, long seed){
        indexes = new int[nbBits];
        Random r = new Random(seed);
        for (int i = 0; i < nbBits; i++) {
            indexes[i]=r.nextInt(keyLength);
        }
    }


    public FixedBitSet hash(String s) {
        FixedBitSet result=new FixedBitSet(indexes.length);
        for (int i = 0; i < indexes.length; i++) {
           if (s.charAt(indexes[i])=='1') {
               result.set(indexes[i]);
           }
        }
     //   System.out.println("KbitLSH " + s + " -> " + result);
        return result; //.hashCode();
    }

    public static void main(String[] args) {
        KbitLSH klsh = new KbitLSH(5,10);
        String test = "1010111010";
        klsh.hash(test);


    }

}
