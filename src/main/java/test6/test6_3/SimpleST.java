// D:\downloads\algs4-master\algs4-master\src\main\java\test6\test6_3\SimpleST.java
package test6.test6_3;

import java.util.*;

class SimpleST<Key extends Comparable<Key>, Value> implements Iterable<Key> {
    private final List<Key> keys = new ArrayList<>();
    private final List<Value> vals = new ArrayList<>();

    public void put(Key k, Value v) {
        int idx = rank(k);
        if (idx < keys.size() && keys.get(idx).compareTo(k) == 0) {
            vals.set(idx, v);
            return;
        }
        keys.add(idx, k);
        vals.add(idx, v);
    }

    public Value get(Key k) {
        int idx = rank(k);
        if (idx >= keys.size()) return null;
        if (keys.get(idx).compareTo(k) == 0) return vals.get(idx);
        return null;
    }

    public boolean contains(Key k) {
        int idx = rank(k);
        return idx < keys.size() && keys.get(idx).compareTo(k) == 0;
    }

    public int size() { return keys.size(); }
    public Key min() { return keys.isEmpty() ? null : keys.get(0); }
    public List<Key> keyList() { return new ArrayList<>(keys); }

    public void delete(Key k) {
        int idx = rank(k);
        if (idx < keys.size() && keys.get(idx).compareTo(k) == 0) {
            keys.remove(idx);
            vals.remove(idx);
        }
    }

    private int rank(Key k) {
        int lo = 0, hi = keys.size() - 1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            int cmp = k.compareTo(keys.get(mid));
            if (cmp < 0) hi = mid - 1;
            else if (cmp > 0) lo = mid + 1;
            else return mid;
        }
        return lo;
    }

    @Override public Iterator<Key> iterator() { return keys.iterator(); }
}
