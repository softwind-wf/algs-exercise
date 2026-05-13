package test3;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 优化版线性探测散列表
 * 改进：
 * 1. 容量固定为2的幂，使用位运算取模 (hash & (m-1))
 * 2. 双散列探测解决主聚集 (步长 = 1 + (hash2 & (m-1))，其中hash2保证奇数)
 * 3. 惰性删除标记 (tombstone)，避免删除时的重哈希风暴
 * 4. 调整负载因子阈值：扩容阈值0.75，缩容阈值0.15
 */
public class LinearProbingHashST<Key, Value> {
    private static final int INIT_CAPACITY = 16;                // 初始容量（必须为2的幂）
    private static final int TOMBSTONE_HASH = Integer.MIN_VALUE; // 墓碑标记的特殊hash值

    private int n;          // 实际键值对数量（不包括墓碑）
    private int m;          // 数组容量（恒为2的幂）
    private int tombstones; // 墓碑数量（用于负载计算）
    private Object[] keys;  // 键数组（使用Object[]避免泛型数组警告）
    private Object[] vals;  // 值数组
    private int[] hashes;   // 存储键的原始hashCode（用于快速比较和探测）

    public LinearProbingHashST() {
        this(INIT_CAPACITY);
    }

    public LinearProbingHashST(int cap) {
        // 保证容量为2的幂
        m = roundUpToPowerOfTwo(cap);
        n = 0;
        tombstones = 0;
        keys = new Object[m];
        vals = new Object[m];
        hashes = new int[m];
    }

    private static int roundUpToPowerOfTwo(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : n + 1;
    }

    public int size() {
        return n;
    }

    public boolean isEmpty() {
        return n == 0;
    }

    /**
     * 位运算取模：仅当 m 为 2 的幂时有效
     */
    private int indexFor(int hash) {
        return hash & (m - 1);
    }

    /**
     * 计算双散列的步长：必须是 1 到 m-1 之间的奇数，保证与 m 互质
     */
    private int stepSize(int hash) {
        // 使用 hash >>> 16 混合高位信息，然后确保结果为奇数
        int h2 = (hash >>> 16) | 1;
        return indexFor(h2);
    }

    /**
     * 判断指定位置是否为墓碑
     */
    private boolean isTombstone(int i) {
        return hashes[i] == TOMBSTONE_HASH;
    }

    /**
     * 扩容/缩容：重新计算所有元素的索引
     */
    private void resize(int newCap) {
        LinearProbingHashST<Key, Value> temp = new LinearProbingHashST<>(newCap);
        for (int i = 0; i < m; i++) {
            if (keys[i] != null && !isTombstone(i)) {
                @SuppressWarnings("unchecked")
                Key k = (Key) keys[i];
                @SuppressWarnings("unchecked")
                Value v = (Value) vals[i];
                temp.put(k, v);
            }
        }
        // 拷贝回原对象
        this.m = temp.m;
        this.n = temp.n;
        this.tombstones = temp.tombstones;
        this.keys = temp.keys;
        this.vals = temp.vals;
        this.hashes = temp.hashes;
    }

    public void put(Key key, Value val) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        if (val == null) {
            delete(key);
            return;
        }

        // 负载因子阈值设为 0.75（包含墓碑在内的占用率）
        if ((n + tombstones) >= m * 0.75) {
            resize(m * 2);
        }

        int hash = key.hashCode();
        int i = indexFor(hash);
        int step = stepSize(hash);
        int firstTombstone = -1;  // 记录沿途遇到的第一个墓碑位置

        while (keys[i] != null) {
            if (isTombstone(i)) {
                if (firstTombstone == -1) firstTombstone = i;
            } else if (hashes[i] == hash && keys[i].equals(key)) {
                // 键已存在，直接更新
                vals[i] = val;
                return;
            }
            i = indexFor(i + step);
        }

        // 如果中途遇到墓碑，优先复用墓碑位置
        if (firstTombstone != -1) {
            i = firstTombstone;
            tombstones--;
        }

        keys[i] = key;
        vals[i] = val;
        hashes[i] = hash;
        n++;
    }

    @SuppressWarnings("unchecked")
    public Value get(Key key) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        int hash = key.hashCode();
        int i = indexFor(hash);
        int step = stepSize(hash);

        while (keys[i] != null) {
            if (!isTombstone(i) && hashes[i] == hash && keys[i].equals(key)) {
                return (Value) vals[i];
            }
            i = indexFor(i + step);
        }
        return null;
    }

    public boolean contains(Key key) {
        return get(key) != null;
    }

    public void delete(Key key) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        int hash = key.hashCode();
        int i = indexFor(hash);
        int step = stepSize(hash);

        while (keys[i] != null) {
            if (!isTombstone(i) && hashes[i] == hash && keys[i].equals(key)) {
                // 惰性删除：置为墓碑
                keys[i] = null;
                vals[i] = null;
                hashes[i] = TOMBSTONE_HASH;
                n--;
                tombstones++;
                break;
            }
            i = indexFor(i + step);
        }

        // 缩容：当实际元素少于 m/8 且容量 > 16 时
        if (n > 0 && n <= m / 8 && m > INIT_CAPACITY) {
            resize(m / 2);
        }
    }

    @SuppressWarnings("unchecked")
    public Iterable<Key> keys() {
        Queue<Key> queue = new LinkedList<>();
        for (int i = 0; i < m; i++) {
            if (keys[i] != null && !isTombstone(i)) {
                queue.add((Key) keys[i]);
            }
        }
        return queue;
    }

    // 测试入口保持不变
    public static void main(String[] args) {
        LinearProbingHashST<String, Integer> st = new LinearProbingHashST<>();
        String[] keys = {"E", "A", "R", "C", "H", "E", "X", "A", "M", "P", "L", "E"};
        for (int i = 0; i < keys.length; i++) {
            st.put(keys[i], i);
        }

        System.out.println("符号表中所有键值对：");
        for (String key : st.keys()) {
            System.out.println(key + " -> " + st.get(key));
        }

        System.out.println("\n查询键 'X' 的值：" + st.get("X"));
        System.out.println("符号表中是否包含键 'Z'：" + st.contains("Z"));

        st.delete("E");
        System.out.println("\n删除键 'E' 后，查询 'E' 的值：" + st.get("E"));
        System.out.println("当前符号表大小：" + st.size());
    }
}