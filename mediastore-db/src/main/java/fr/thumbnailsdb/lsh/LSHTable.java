package fr.thumbnailsdb.lsh;


import com.google.common.collect.ArrayListMultimap;
import fr.thumbnailsdb.utils.FixedBitSet;

import java.util.HashMap;
import java.util.List;

public class LSHTable {

    private KbitLSH hashFunction;
    private ArrayListMultimap<FixedBitSet, Integer> table;


    public LSHTable(int k, int maxExcluded) {
        hashFunction = new KbitLSH(k, maxExcluded);
        table =   ArrayListMultimap.<FixedBitSet, Integer>create((int) Math.pow(2,k), 100);
    }

    public void add(String key, int value) {
//        String hv = hashFunction.hash(key);
        table.put(hashFunction.hash(key), value);

    }

    public List<Integer> get(String key) {
        return table.get(hashFunction.hash(key));
    }

    public int size() {
        return table.size();
    }


}
