package dev.murad.shipping.util;

import java.util.*;

public class MultiMap<K, V> extends HashMap<K, List<V>> {

    public void putInsert(K key, V value) {
        this.computeIfAbsent(key, (k) -> new ArrayList<>()).add(value);
    }
}
