package test3;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 基于有序数组的二分查找符号表 (BinarySearchST)
 * 核心特性：键有序存储，利用二分查找将查找效率优化至 O(logN)
 * @param <Key> 键类型，必须可比较 (Comparable)
 * @param <Value> 值类型
 */
public class BinarySearchST<Key extends Comparable<Key>, Value> {
    private Key[] keys;      // 存储键的有序数组
    private Value[] vals;    // 存储值的数组，与keys一一对应
    private int n;           // 符号表中键值对的数量

    /**
     * 构造函数：初始化指定容量的空符号表
     * @param capacity 初始容量
     */
    @SuppressWarnings("unchecked")
    public BinarySearchST(int capacity) {
        // 强制类型转换，创建Comparable和Object的泛型数组
        keys = (Key[]) new Comparable[capacity];
        vals = (Value[]) new Object[capacity];
        n = 0;
    }

    /**
     * 默认构造函数：初始容量设为8
     */
    public BinarySearchST() {
        this(8);
    }

    /**
     * 返回符号表中键值对的数量
     * @return 元素个数
     */
    public int size() {
        return n;
    }

    /**
     * 判断符号表是否为空
     * @return true if empty
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * 查找指定键，返回对应的值
     * @param key 待查找的键
     * @return 键对应的值，若不存在返回null
     */
    public Value get(Key key) {
        if (key == null) throw new IllegalArgumentException("argument to get() is null");
        if (isEmpty()) return null;
        
        int i = rank(key);
        // 找到且索引有效，则返回值
        if (i < n && keys[i].compareTo(key) == 0) {
            return vals[i];
        }
        return null;
    }

    /**
     * 插入/更新键值对
     * @param key 待插入的键
     * @param val 待插入的值
     */
    public void put(Key key, Value val) {
        if (key == null) throw new IllegalArgumentException("first argument to put() is null");
        if (val == null) {
            delete(key);
            return;
        }

        int i = rank(key);
        
        // 1. 键已存在，直接更新值
        if (i < n && keys[i].compareTo(key) == 0) {
            vals[i] = val;
            return;
        }

        // 2. 数组已满，进行扩容 (2倍)
        if (n == keys.length) {
            resize(2 * keys.length);
        }

        // 3. 元素后移，腾出位置插入新元素
        // 从最后一个元素开始向前移动，避免覆盖
        for (int j = n; j > i; j--) {
            keys[j] = keys[j-1];
            vals[j] = vals[j-1];
        }
        keys[i] = key;
        vals[i] = val;
        n++;
    }

    /**
     * 删除指定键及其对应的值
     * @param key 待删除的键
     */
    public void delete(Key key) {
        if (key == null) throw new IllegalArgumentException("argument to delete() is null");
        if (isEmpty()) return;

        int i = rank(key);
        
        // 键不存在，直接返回
        if (i >= n || keys[i].compareTo(key) != 0) {
            return;
        }

        // 元素前移，覆盖待删除元素
        for (int j = i; j < n-1; j++) {
            keys[j] = keys[j+1];
            vals[j] = vals[j+1];
        }
        
        n--;
        // 手动清空引用，避免内存泄漏
        keys[n] = null;
        vals[n] = null;

        // 缩容：如果元素数量小于数组长度的1/4，缩小至1/2
        if (n > 0 && n == keys.length / 4) {
            resize(keys.length / 2);
        }
    }

    /**
     * 判断符号表是否包含指定键
     * @param key 待检查的键
     * @return true if contains
     */
    public boolean contains(Key key) {
        if (key == null) throw new IllegalArgumentException("argument to contains() is null");
        return get(key) != null;
    }

    /**
     * 二分查找核心：返回键应该插入的索引位置 (rank)
     * @param key 目标键
     * @return 索引位置
     */
    public int rank(Key key) {
        if (key == null) throw new IllegalArgumentException("argument to rank() is null");
        
        int lo = 0, hi = n - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2; // 防止溢出
            int cmp = key.compareTo(keys[mid]);
            if (cmp < 0) {
                hi = mid - 1; // 目标在左半部分
            } else if (cmp > 0) {
                lo = mid + 1; // 目标在右半部分
            } else {
                return mid;   // 找到，返回索引
            }
        }
        return lo; // 未找到，返回插入点
    }

    /**
     * 调整数组大小 (扩容/缩容)
     * @param max 新的容量
     */
    @SuppressWarnings("unchecked")
    private void resize(int max) {
        if (max < n) throw new IllegalArgumentException("New size must be larger than current size");
        
        Key[] tempKeys = (Key[]) new Comparable[max];
        Value[] tempVals = (Value[]) new Object[max];
        
        // 复制原有元素
        System.arraycopy(keys, 0, tempKeys, 0, n);
        System.arraycopy(vals, 0, tempVals, 0, n);
        
        keys = tempKeys;
        vals = tempVals;
    }

    // ----------- 以下是获取极值和键集合的方法 -----------

    /**
     * 返回最小的键
     */
    public Key min() {
        if (isEmpty()) throw new NoSuchElementException("called min() with empty symbol table");
        return keys[0];
    }

    /**
     * 返回最大的键
     */
    public Key max() {
        if (isEmpty()) throw new NoSuchElementException("called max() with empty symbol table");
        return keys[n-1];
    }

    /**
     * 返回排名为k的键 (第k个小的键)
     */
    public Key select(int k) {
        if (k < 0 || k >= n) throw new IllegalArgumentException("called select() with invalid index");
        return keys[k];
    }

    /**
     * 返回小于等于key的最大键
     */
    public Key floor(Key key) {
        if (key == null) throw new IllegalArgumentException("argument to floor() is null");
        int i = rank(key);
        if (i < n && keys[i].compareTo(key) == 0) return keys[i];
        if (i == 0) return null;
        return keys[i-1];
    }

    /**
     * 返回大于等于key的最小键
     */
    public Key ceiling(Key key) {
        if (key == null) throw new IllegalArgumentException("argument to ceiling() is null");
        int i = rank(key);
        if (i == n) return null;
        return keys[i];
    }

    /**
     * 返回所有键的可迭代集合 (升序)
     * 复用之前的技巧：直接返回自定义Iterable，无需额外容器
     */
    public Iterable<Key> keys() {
        return new Iterable<Key>() {
            @Override
            public Iterator<Key> iterator() {
                return new Iterator<Key>() {
                    private int current = 0;

                    @Override
                    public boolean hasNext() {
                        return current < n;
                    }

                    @Override
                    public Key next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        return keys[current++];
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /**
     * 测试用例
     */
    public static void main(String[] args) {
        BinarySearchST<String, Integer> st = new BinarySearchST<>();
        
        // 模拟图片中的插入轨迹
        st.put("S", 0);
        st.put("E", 1);
        st.put("A", 2);
        st.put("R", 3);
        st.put("C", 4);
        st.put("H", 5);
        st.put("E", 6); // 更新
        st.put("X", 7);
        st.put("A", 8); // 更新

        System.out.println("Size: " + st.size()); // 预期 7
        
        // 遍历打印
        for (String key : st.keys()) {
            System.out.println(key + " : " + st.get(key));
        }

        System.out.println("\nAfter delete E:");
        st.delete("E");
        System.out.println("size: "+st.size());
        for (String key : st.keys()) {
            System.out.println(key + " : " + st.get(key));
        }
    }
}