package test3;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 基于线性探测的散列表（开放地址法）实现符号表
 * 对应《算法》第3.4节内容
 */
public class LinearProbingHashST1<Key, Value> {
    private static final int INIT_CAPACITY = 16; // 默认初始容量

    private int n; // 符号表中键值对的总数
    private int m; // 线性探测表的大小（数组容量）
    private Key[] keys;    // 键数组
    private Value[] vals;  // 值数组

    /**
     * 构造函数：使用默认容量初始化散列表
     */
    public LinearProbingHashST1() {
        this(INIT_CAPACITY);
    }

    /**
     * 构造函数：指定初始容量
     * @param cap 初始容量
     */
    public LinearProbingHashST1(int cap) {
        m = cap;
        n = 0;
        keys = (Key[]) new Object[m];
        vals = (Value[]) new Object[m];
    }

    /**
     * 获取键值对数量
     * @return 键值对数量
     */
    public int size() {
        return n;
    }

    /**
     * 判断符号表是否为空
     * @return true 为空，false 不为空
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * 计算键的散列值（保证非负）
     * @param key 键
     * @return 散列索引
     */
    private int hash(Key key) {
        return (key.hashCode() & 0x7fffffff) % m;
    }

    /**
     * 调整散列表大小（扩容/缩容）
     * @param cap 新的容量
     */
    private void resize(int cap) {
        LinearProbingHashST1<Key, Value> t = new LinearProbingHashST1<>(cap);
        for (int i = 0; i < m; i++) {
            if (keys[i] != null) {
                t.put(keys[i], vals[i]);
            }
        }
        keys = t.keys;
        vals = t.vals;
        m = t.m;
    }

    /**
     * 向符号表中插入键值对
     * 若键已存在则更新值，若值为null则删除该键
     * @param key 键
     * @param val 值
     */
    public void put(Key key, Value val) {
        if (key == null) throw new IllegalArgumentException("key不能为null");

        // 若值为null，相当于删除该键
        if (val == null) {
            delete(key);
            return;
        }

        // 使用率超过1/2时扩容，避免性能下降
        if (n >= m / 2) {
            resize(2 * m);
        }

        // 线性探测找空位置或已存在的键
        int i;
        for (i = hash(key); keys[i] != null; i = (i + 1) % m) {
            if (keys[i].equals(key)) {
                vals[i] = val; // 键已存在，更新值
                return;
            }
        }

        // 找到空位置，插入新键值对
        keys[i] = key;
        vals[i] = val;
        n++;
    }

    /**
     * 根据键获取对应的值
     * @param key 键
     * @return 对应的值，不存在则返回null
     */
    public Value get(Key key) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        if (isEmpty()) return null;

        // 线性探测找键
        for (int i = hash(key); keys[i] != null; i = (i + 1) % m) {
            if (keys[i].equals(key)) {
                return vals[i];
            }
        }
        return null; // 未找到
    }

    /**
     * 判断符号表中是否包含指定键
     * @param key 键
     * @return true 包含，false 不包含
     */
    public boolean contains(Key key) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        return get(key) != null;
    }

    /**
     * 从符号表中删除指定键
     * @param key 键
     */
    public void delete(Key key) {
        if (key == null) throw new IllegalArgumentException("key不能为null");
        if (!contains(key)) return;

        // 找到键的位置
        int i = hash(key);
        while (!key.equals(keys[i])) {
            i = (i + 1) % m;
        }

        // 删除该键值对
        keys[i] = null;
        vals[i] = null;

        // 探测后续的键，重新插入到散列表中（处理“聚集”问题）
        i = (i + 1) % m;
        while (keys[i] != null) {
            Key keyToRedo = keys[i];
            Value valToRedo = vals[i];
            keys[i] = null;
            vals[i] = null;
            n--;
            put(keyToRedo, valToRedo);
            i = (i + 1) % m;
        }

        n--;

        // 使用率低于1/8时缩容，节省空间
        if (n > 0 && n == m / 8) {
            resize(m / 2);
        }
    }

    /**
     * 获取符号表中所有键的集合（用于遍历）
     * @return 所有键的Iterable
     */
    public Iterable<Key> keys() {
        Queue<Key> queue = new LinkedList<>();
        for (int i = 0; i < m; i++) {
            if (keys[i] != null) {
                queue.add(keys[i]);
            }
        }
        return queue;
    }

    // 测试用例
    public static void main(String[] args) {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();

        // 插入测试（和图3.4.6的例子一致）
        String[] keys = {"E", "A", "R", "C", "H", "E", "X", "A", "M", "P", "L", "E"};
        for (int i = 0; i < keys.length; i++) {
            st.put(keys[i], i);
        }

        // 遍历所有键值对
        System.out.println("符号表中所有键值对：");
        for (String key : st.keys()) {
            System.out.println(key + " -> " + st.get(key));
        }

        // 测试get和contains
        System.out.println("\n查询键 'X' 的值：" + st.get("X"));
        System.out.println("符号表中是否包含键 'Z'：" + st.contains("Z"));

        // 测试delete
        st.delete("E");
        System.out.println("\n删除键 'E' 后，查询 'E' 的值：" + st.get("E"));
        System.out.println("当前符号表大小：" + st.size());
    }
}