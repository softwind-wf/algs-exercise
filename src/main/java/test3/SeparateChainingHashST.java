package test3;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 基于拉链法的散列表，实现完整的符号表API
 */
public class SeparateChainingHashST<Key, Value> {
    // 默认初始桶数量（和书中一致）
    private static final int INIT_CAPACITY = 997;
    // 键值对总数
    private int N;
    // 散列表桶的数量
    private int M;
    // 存放链表符号表的数组（每个元素是一个桶）
    private SequentialSearchST<Key, Value>[] st;

    /**
     * 无参构造方法，使用默认初始容量
     */
    public SeparateChainingHashST() {
        this(INIT_CAPACITY);
    }

    /**
     * 构造方法，指定初始桶数量
     * @param M 初始桶数量
     */
    @SuppressWarnings("unchecked")
    public SeparateChainingHashST(int M) {
        this.M = M;
        // Java不支持泛型数组，强制类型转换
        st = (SequentialSearchST<Key, Value>[]) new SequentialSearchST[M];
        // 初始化每个桶
        for (int i = 0; i < M; i++) {
            st[i] = new SequentialSearchST<>();
        }
    }

    /**
     * 哈希函数：将键转换为 0~M-1 的数组索引
     * @param key 键
     * @return 数组索引
     */
    private int hash(Key key) {
        // 消除hashCode的负数，再取模得到合法索引
        return (key.hashCode() & 0x7fffffff) % M;
    }

    /**
     * 动态调整散列表的桶数量，重新哈希所有键值对
     * @param newM 新的桶数量
     */
    private void resize(int newM) {
        SeparateChainingHashST<Key, Value> temp = new SeparateChainingHashST<>(newM);
        // 把原表的所有键值对重新插入新表
        for (int i = 0; i < M; i++) {
            for (Key key : st[i].keys()) {
                temp.put(key, st[i].get(key));
            }
        }
        // 更新当前表的属性
        this.M = temp.M;
        this.N = temp.N;
        this.st = temp.st;
    }

    // ------------------- 符号表核心API -------------------

    /**
     * 获取键值对总数
     * @return 键值对数量
     */
    public int size() {
        return N;
    }

    /**
     * 判断符号表是否为空
     * @return 空返回true，否则false
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * 判断是否包含指定键
     * @param key 键
     * @return 包含返回true，否则false
     */
    public boolean contains(Key key) {
        if (key == null) throw new IllegalArgumentException("键不能为null");
        return get(key) != null;
    }

    /**
     * 获取键对应的值
     * @param key 键
     * @return 键对应的值，不存在返回null
     */
    public Value get(Key key) {
        if (key == null) throw new IllegalArgumentException("键不能为null");
        int index = hash(key);
        return st[index].get(key);
    }

    /**
     * 插入/更新键值对
     * @param key 键
     * @param val 值，若为null则删除该键
     */
    public void put(Key key, Value val) {
        if (key == null) throw new IllegalArgumentException("键不能为null");
        // 值为null，执行删除
        if (val == null) {
            delete(key);
            return;
        }
        // 平均链表长度>=8时，扩容为2倍，保证查询效率
        if (N >= 8 * M) {
            resize(2 * M);
        }
        int index = hash(key);
        // 键不存在，总数+1
        if (!st[index].contains(key)) {
            N++;
        }
        // 插入/更新对应桶的键值对
        st[index].put(key, val);
    }

    /**
     * 删除指定键值对
     * @param key 键
     */
    public void delete(Key key) {
        if (key == null) throw new IllegalArgumentException("键不能为null");
        int index = hash(key);
        // 键存在，总数-1
        if (st[index].contains(key)) {
            N--;
            st[index].delete(key);
        }
        // 平均链表长度<=2 且 桶数量大于初始值时，缩容为1/2
        if (M > INIT_CAPACITY && N <= 2 * M) {
            resize(M / 2);
        }
    }

    /**
     * 返回所有键的可迭代对象，支持for-each遍历
     * @return 所有键的集合
     */
    public Iterable<Key> keys() {
        Queue<Key> queue = new LinkedList<>();
        for (int i = 0; i < M; i++) {
            for (Key key : st[i].keys()) {
                queue.add(key);
            }
        }
        return queue;
    }

    // ------------------- 测试用例 -------------------
    public static void main(String[] args) {
        SeparateChainingHashST<String, Integer> st = new SeparateChainingHashST<>();

        // 插入书中示例的键值对
        st.put("S", 0);
        st.put("E", 1);
        st.put("A", 2);
        st.put("R", 3);
        st.put("C", 4);
        st.put("H", 5);
        st.put("E", 6);
        st.put("X", 7);
        st.put("A", 8);
        st.put("M", 9);
        st.put("P", 10);
        st.put("L", 11);
        st.put("E", 12);

        // 核心API测试
        System.out.println("E 对应的值: " + st.get("E"));       // 输出 12
        System.out.println("X 对应的值: " + st.get("X"));       // 输出 7
        System.out.println("是否包含键 M: " + st.contains("M")); // 输出 true
        System.out.println("键值对总数: " + st.size());          // 输出 10（重复键去重）

        // 删除测试
        st.delete("A");
        System.out.println("删除A后，是否包含A: " + st.contains("A")); // 输出 false
        System.out.println("删除A后的总数: " + st.size());              // 输出 9

        // 遍历所有键值对
        System.out.println("\n所有键值对:");
        for (String key : st.keys()) {
            System.out.println(key + " : " + st.get(key));
        }
    }
}