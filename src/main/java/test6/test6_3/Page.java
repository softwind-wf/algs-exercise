// D:\downloads\algs4-master\algs4-master\src\main\java\test6\test6_3\Page.java
package test6.test6_3;

import java.io.*;
import java.util.*;

public class Page {
    public static final int M = 4;

    final int index;
    final boolean isBottom;
    final String nodeName;
    final SimpleST<String, Object> st;

    Page(boolean bottom, int index, int m) {
        this.isBottom = bottom;
        this.index = index;
        this.nodeName = String.format("BTreeNode%0" + m + "d", index);
        this.st = new SimpleST<>();
    }

    boolean isExternal() { return isBottom; }
    boolean isFull() { return st.size() >= M; }

    void addKey(String key) {
        st.put(key, null);
    }

    void addChild(String sepKey, int childIdx) {
        st.put(sepKey, childIdx);
    }

    boolean contains(String key) {
        return st.contains(key);
    }

    String getLeafValue(String key) {
        Object v = st.get(key);
        return v instanceof String ? (String) v : null;
    }

    Page next(String key, Map<Integer, Page> cache) {
        List<String> keyArr = st.keyList();
        if (keyArr.isEmpty()) return null;
        for (int i = 0; i < keyArr.size(); i++) {
            String sepKey = keyArr.get(i);
            int cmp = key.compareTo(sepKey);
            if (cmp < 0) {
                if (i == 0) return null;
                return cache.get((Integer) st.get(keyArr.get(i - 1)));
            } else if (cmp == 0) {
                return cache.get((Integer) st.get(sepKey));
            }
        }
        return cache.get((Integer) st.get(keyArr.get(keyArr.size() - 1)));
    }

    Page split(Map<Integer, Page> cache, int newIndex, int m) {
        Page newPage = new Page(this.isBottom, newIndex, m);
        cache.put(newIndex, newPage);
        List<String> allKeys = st.keyList();
        int mid = M / 2;
        for (int i = mid + 1; i < allKeys.size(); i++) {
            String k = allKeys.get(i);
            newPage.st.put(k, st.get(k));
            st.delete(k);
        }
        return newPage;
    }

    void print(String prefix) {
        System.out.print(prefix + " " + nodeName + (isBottom ? "[叶子]" : "[内部]") + ": ");
        for (String k : st) System.out.print(k + " ");
        System.out.println();
    }

    // ===== 序列化/反序列化 =====

    static byte[] serialize(Page p) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeInt(p.index);
            dos.writeBoolean(p.isBottom);
            List<String> keys = p.st.keyList();
            dos.writeInt(keys.size());
            for (String k : keys) {
                dos.writeUTF(k);
                Object v = p.st.get(k);
                if (p.isBottom) {
                    dos.writeUTF(v instanceof String ? (String) v : "");
                } else {
                    dos.writeInt(v instanceof Integer ? (Integer) v : -1);
                }
            }
            dos.flush();
            return bos.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    static Page deserialize(byte[] data, int m) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            int idx = dis.readInt();
            boolean isLeaf = dis.readBoolean();
            int n = dis.readInt();
            Page p = new Page(isLeaf, idx, m);
            for (int i = 0; i < n; i++) {
                String k = dis.readUTF();
                if (isLeaf) {
                    String v = dis.readUTF();
                    p.st.put(k, v.isEmpty() ? null : v);
                } else {
                    int childIdx = dis.readInt();
                    p.st.put(k, childIdx);
                }
            }
            return p;
        } catch (IOException e) { throw new RuntimeException(e); }
    }
}
