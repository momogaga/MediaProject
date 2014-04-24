package fr.thumbnailsdb.persistentLSH;

import fr.thumbnailsdb.lsh.KbitLSH;
import org.mapdb.DB;
import org.mapdb.Fun;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;

/**
 * Created by fhuet on 23/04/2014.
 */
public class PersistentLSHTable {

    private KbitLSH hashFunction;
    NavigableSet<Fun.Tuple2<String, Integer>> multiMap;

    public PersistentLSHTable(int k, int maxExcluded, int index, DB db) {

         hashFunction = new KbitLSH(k, maxExcluded, index);
        // table =   ArrayListMultimap.<String, Integer>create((int) Math.pow(2,k), 100);
        multiMap = db.getTreeSet("lsh"+index);
    }

    public void add(String key, int value) {
        String hv = hashFunction.hash(key);
        multiMap.add(Fun.t2(hv,value));

    }

    public List<Integer> get(String key) {
        List<Integer> list = new ArrayList<Integer>();
        for(Integer l: Fun.filter(multiMap, hashFunction.hash(key))){
            list.add(l);
        }
        //return multiMap.
        return list;
    }

    public void clear() {
        multiMap.clear();
    }

    public int size() {
        //return table.size();
        return multiMap.size();
    }
}
