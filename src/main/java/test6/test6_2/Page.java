package test6.test6_2;

import java.util.List;

public class Page<Key extends Comparable<Key>> {
    public static final int M = 4;
    final int pageIndex;
    final boolean isBottom;
    final SimpleST<Key, Integer> st;

    public Page(boolean bottom, int pageIndex) {
        this.isBottom = bottom;
        this.pageIndex = pageIndex;
        st = new SimpleST<>();
    }

    public void add(Key key) { st.put(key, null); }

    public boolean isExternal() { return isBottom; }

    public boolean contains(Key key) {
        boolean res = st.contains(key);
        if (isBottom) System.out.println("叶子页" + pageIndex + "检查[" + key + "] 结果:" + res);
        return res;
    }

    public Page<Key> next(Key key, DiskManager<Key> disk) {
        List<Key> keyArr = st.keyList();
        if (keyArr.isEmpty()) return null;

        for (int i = 0; i < keyArr.size(); i++) {
            Key sepKey = keyArr.get(i);
            int cmp = key.compareTo(sepKey);
            if (cmp < 0) {
                if (i == 0) return null;
                Integer childIdx = st.get(keyArr.get(i - 1));
                return childIdx != null ? disk.getPage(childIdx) : null;
            } else if (cmp == 0) {
                Integer childIdx = st.get(sepKey);
                return childIdx != null ? disk.getPage(childIdx) : null;
            }
        }
        Integer lastIdx = st.get(keyArr.get(keyArr.size() - 1));
        return lastIdx != null ? disk.getPage(lastIdx) : null;
    }

    public boolean isFull() { return st.size() >= M; }

    public Page<Key> split(DiskManager<Key> disk) {
        int newIdx = disk.allocatePage(this.isBottom);
        Page<Key> newPage = disk.getPage(newIdx);
        List<Key> allKeys = st.keyList();
        int mid = M / 2;

        if (isBottom) {
            for (int i = mid + 1; i < allKeys.size(); i++) {
                Key k = allKeys.get(i);
                newPage.st.put(k, null);
                st.delete(k);
            }
        } else {
            for (int i = mid + 1; i < allKeys.size(); i++) {
                Key k = allKeys.get(i);
                Integer childIdx = st.get(k);
                newPage.st.put(k, childIdx);
                st.delete(k);
            }
        }

        disk.writePage(this);
        disk.writePage(newPage);
        return newPage;
    }

    public Iterable<Key> keys() { return st; }

    public void printPage(String prefix) {
        System.out.print(prefix + "页#" + pageIndex + "[" + (isBottom ? "叶子" : "内部") + "]键：");
        for (Key k : st) System.out.print(k + " ");
        System.out.println();
    }
}
