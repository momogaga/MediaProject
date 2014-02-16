package fr.thumbnailsdb.lsh;


import com.google.common.collect.ArrayListMultimap;

import java.util.HashMap;
import java.util.List;

public class LSHTable {

    private KbitLSH hashFunction;
    private ArrayListMultimap<String, Integer> table;


    public LSHTable(int k, int maxExcluded) {
        hashFunction = new KbitLSH(k, maxExcluded);
        table =   ArrayListMultimap.<String, Integer>create((int) Math.pow(2,k), 100);
    }

    public void add(String key, int value) {
        String hv = hashFunction.hash(key);
        table.put(hv, value);

    }

    public List<Integer> get(String key) {
        return table.get(hashFunction.hash(key));
    }

    public int size() {
        return table.size();
    }


}
