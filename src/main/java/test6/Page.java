// Page.java
package test6;

import java.util.List;

public class Page<Key extends Comparable<Key>> {
    public static final int M = 4;
    private final boolean isBottom;
    private final SimpleST<Key, Page<Key>> st;

    public Page(boolean bottom) {
        this.isBottom = bottom;
        st = new SimpleST<>();
    }
    public void close() {}
    public void add(Key key) { st.put(key, null); }
    public void add(Page<Key> child) {
        Key minK = child.st.min();
        st.put(minK, child);
    }
    public boolean isExternal() { return isBottom; }
    public boolean contains(Key key) {
        boolean res = st.contains(key);
        if(isBottom) System.out.println("叶子页检查["+key+"] 结果:"+res);
        return res;
    }

    public Page<Key> next(Key key) {
        List<Key> keyArr = st.keyList();
        // 如果 key 小于第一个键，说明不在这个页的任何子树中
        if (keyArr.isEmpty()) return null;
        
        // 在内部节点中，键和子节点的关系：
        // 键 k_i 对应子树 child_i，其中 child_i 中的所有键都 <= k_i
        // 查找时，找到第一个 >= key 的键，返回它前面的那个子树
        for (int i = 0; i < keyArr.size(); i++) {
            Key sepKey = keyArr.get(i);
            int cmp = key.compareTo(sepKey);
            if (cmp < 0) {
                // key 小于当前分隔键，应该去前一个子树
                if (i == 0) return null; // 没有更小的子树了
                return st.get(keyArr.get(i - 1));
            } else if (cmp == 0) {
                // key 等于当前分隔键，应该去这个键对应的子树
                return st.get(sepKey);
            }
        }
        // key 大于所有分隔键，去最后一个子树
        return st.get(keyArr.get(keyArr.size() - 1));
    }

    public boolean isFull() { return st.size() >= M; }

    // Page.java - 修复后的split方法
    public Page<Key> split() {
        Page<Key> newPage = new Page<>(this.isBottom);
        List<Key> allKeys = st.keyList();
        int mid = M / 2;

        if (isBottom) {
            // 叶子节点：从mid+1位置开始，将后半部分移到新页
            // mid位置的键保留在原页，确保不丢失任何数据键
            for (int i = mid + 1; i < allKeys.size(); i++) {
                Key k = allKeys.get(i);
                newPage.st.put(k, null);
                st.delete(k);
            }
        } else {
            // 内部节点：从mid+1位置开始，将后半部分移到新页
            // 保留mid位置的键在原页作为路由键
            for (int i = mid + 1; i < allKeys.size(); i++) {
                Key k = allKeys.get(i);
                Page<Key> child = st.get(k);
                newPage.st.put(k, child);
                st.delete(k);
            }
        }

        return newPage;
    }
    
    public Iterable<Key> keys() { return st; }
    
    public void printPage(String prefix) {
        System.out.print(prefix + "页[" + (isBottom ? "叶子" : "内部") + "]键：");
        for (Key k : st) System.out.print(k + " ");
        System.out.println();
    }
}