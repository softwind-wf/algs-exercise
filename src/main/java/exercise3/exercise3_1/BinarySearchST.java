package exercise3.exercise3_1;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 基于有序Item数组的二分查找符号表 (BinarySearchST 3.1.12 题解)
 * 用单个Item对象数组替代两个平行数组，新增归并排序构造函数
 * @param <Key> 键类型，必须可比较
 * @param <Value> 值类型
 */
public class BinarySearchST<Key extends Comparable<Key>, Value> {
    // 1. 定义Item内部类，封装键值对，实现Comparable
    private static class Item<Key extends Comparable<Key>,Value> implements Comparable<Item> {
        Key key;
        Value val;

        public Item(Key key, Value val) {
            this.key = key;
            this.val = val;
        }

        // 按key比较，用于排序和二分查找
        @Override
        public int compareTo(Item other) {
            return this.key.compareTo((Key) other.key);
        }
    }

    private Item[] items;  // 存储Item对象的有序数组
    private int n;         // 符号表中键值对的数量

    // -------------------------- 构造函数 --------------------------
    /**
     * 构造函数：初始化指定容量的空符号表
     */
    @SuppressWarnings("unchecked")
    public BinarySearchST(int capacity) {
        items = new Item[capacity];
        n = 0;
    }

    /**
     * 默认构造函数：初始容量设为8
     */
    public BinarySearchST() {
        this(8);
    }

    /**
     * 题目要求：接收Item数组，归并排序后去重，构造符号表
     */
    public BinarySearchST(Item[] inputItems) {
        if (inputItems == null) throw new IllegalArgumentException("inputItems is null");

        // 1. 复制输入数组，避免修改原数组
        items = inputItems.clone();
        n = items.length;

        // 2. 归并排序整个数组
        sort(items, 0, n - 1);

        // 3. 去重（相同key只保留最后一个，和put()逻辑一致）
        int j = 0;
        for (int i = 0; i < n; i++) {
            if (i == 0 || items[i].key.compareTo(items[j - 1].key) != 0) {
                items[j++] = items[i];
            } else {
                // 相同key，更新值（保留最后一个）
                items[j - 1].val = items[i].val;
            }
        }
        n = j;

        // 4. 调整数组大小，去除多余空间
        resize(n);
    }

    // -------------------------- 归并排序实现（用于构造函数） --------------------------
    /**
     * 归并排序入口
     */
    private void sort(Item[] a, int lo, int hi) {
        if (hi <= lo) return;
        int mid = lo + (hi - lo) / 2;
        sort(a, lo, mid);
        sort(a, mid + 1, hi);
        merge(a, lo, mid, hi);
    }

    /**
     * 归并两个有序子数组
     */
    private void merge(Item[] a, int lo, int mid, int hi) {
        Item[] aux = new Item[hi - lo + 1];
        int i = lo, j = mid + 1;
        for (int k = 0; k < aux.length; k++) {
            if (i > mid) aux[k] = a[j++];
            else if (j > hi) aux[k] = a[i++];
            else if (a[j].compareTo(a[i]) < 0) aux[k] = a[j++];
            else aux[k] = a[i++];
        }
        // 复制回原数组
        System.arraycopy(aux, 0, a, lo, aux.length);
    }

    // -------------------------- 基础工具方法 --------------------------
    /**
     * 返回符号表大小
     */
    public int size() {
        return n;
    }

    /**
     * 判断是否为空
     */
    public boolean isEmpty() {
        return n == 0;
    }

    /**
     * 调整数组大小（扩容/缩容）
     */
    @SuppressWarnings("unchecked")
    private void resize(int max) {
        if (max < n) throw new IllegalArgumentException("New size too small");
        Item[] temp = (Item[]) new Item[max];
        System.arraycopy(items, 0, temp, 0, n);
        items = temp;
    }

    // -------------------------- 核心二分查找rank方法 --------------------------
    /**
     * 返回key在有序数组中的排名（小于key的元素数量）
     */
    public int rank(Key key) {
        if (key == null) throw new IllegalArgumentException("key is null");

        int lo = 0, hi = n - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            int cmp = key.compareTo((Key) items[mid].key);
            if (cmp < 0) hi = mid - 1;
            else if (cmp > 0) lo = mid + 1;
            else return mid; // 找到，返回索引
        }
        return lo; // 未找到，返回插入点
    }

    // -------------------------- 符号表核心API --------------------------
    /**
     * 查找key对应的值
     */
    public Value get(Key key) {
        if (key == null) throw new IllegalArgumentException("key is null");
        if (isEmpty()) return null;

        int i = rank(key);
        if (i < n && items[i].key.compareTo(key) == 0) {
            return (Value) items[i].val;
        }
        return null;
    }

    /**
     * 插入/更新键值对
     */
    public void put(Key key, Value val) {
        if (key == null) throw new IllegalArgumentException("key is null");
        if (val == null) {
            delete(key);
            return;
        }

        int i = rank(key);
        // 1. 键已存在，直接更新
        if (i < n && items[i].key.compareTo(key) == 0) {
            items[i].val = val;
            return;
        }

        // 2. 数组满了，扩容2倍
        if (n == items.length) {
            resize(2 * items.length);
        }

        // 3. 元素后移，插入新Item
        for (int j = n; j > i; j--) {
            items[j] = items[j - 1];
        }
        items[i] = new Item(key, val);
        n++;

        // 4. 缩容优化（元素数<1/4容量时缩容1/2）
        if (n > 0 && n == items.length / 4) {
            resize(items.length / 2);
        }
    }

    /**
     * 删除指定key
     */
    public void delete(Key key) {
        if (key == null) throw new IllegalArgumentException("key is null");
        if (isEmpty()) return;

        int i = rank(key);
        if (i >= n || items[i].key.compareTo(key) != 0) return;

        // 元素前移，覆盖待删除项
        for (int j = i; j < n - 1; j++) {
            items[j] = items[j + 1];
        }
        n--;
        items[n] = null; // 清空引用，避免内存泄漏

        // 缩容
        if (n > 0 && n == items.length / 4) {
            resize(items.length / 2);
        }
    }

    /**
     * 判断是否包含key
     */
    public boolean contains(Key key) {
        return get(key) != null;
    }

    // -------------------------- 有序符号表扩展API --------------------------
    /**
     * 返回最小的key
     */
    public Key min() {
        if (isEmpty()) throw new NoSuchElementException("ST is empty");
        return (Key) items[0].key;
    }

    /**
     * 返回最大的key
     */
    public Key max() {
        if (isEmpty()) throw new NoSuchElementException("ST is empty");
        return (Key) items[n - 1].key;
    }

    /**
     * 返回排名为k的key
     */
    public Key select(int k) {
        if (k < 0 || k >= n) throw new IllegalArgumentException("invalid index");
        return (Key) items[k].key;
    }

    /**
     * 返回小于等于key的最大key
     */
    public Key floor(Key key) {
        if (key == null) throw new IllegalArgumentException("key is null");
        int i = rank(key);
        if (i < n && items[i].key.compareTo(key) == 0) return key;
        if (i == 0) return null;
        return (Key) items[i - 1].key;
    }

    /**
     * 返回大于等于key的最小key
     */
    public Key ceiling(Key key) {
        if (key == null) throw new IllegalArgumentException("key is null");
        int i = rank(key);
        if (i == n) return null;
        return (Key) items[i].key;
    }

    /**
     * 返回所有key的迭代器（升序）
     */
    public Iterable<Key> keys() {
        return () -> new Iterator<Key>() {
            private int current = 0;

            @Override
            public boolean hasNext() {
                return current < n;
            }

            @Override
            public Key next() {
                if (!hasNext()) throw new NoSuchElementException();
                return (Key) items[current++].key;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    // -------------------------- 测试用例 --------------------------
    public static void main(String[] args) {
        // 测试1：普通构造+put/get
        BinarySearchST<String, Integer> st1 = new BinarySearchST<>();
        st1.put("S", 0);
        st1.put("E", 1);
        st1.put("A", 2);
        st1.put("R", 3);
        st1.put("C", 4);
        st1.put("H", 5);
        st1.put("E", 6); // 更新
        System.out.println("=== 普通构造测试 ===");
        for (String key : st1.keys()) {
            System.out.println(key + " : " + st1.get(key));
        }

        // 测试2：Item数组构造+归并排序
        BinarySearchST.Item[] items =
                new Item[7];
        items[0] = new Item("S", 0);
        items[1] = new Item("E", 1);
        items[2] = new Item("A", 2);
        items[3] = new Item("R", 3);
        items[4] = new Item("C", 4);
        items[5] = new Item("H", 5);
        items[6] = new Item("E", 6); // 重复key

        BinarySearchST<String, Integer> st2 = new BinarySearchST<>(items);
        System.out.println("\n=== Item数组构造+归并排序测试 ===");
        for (String key : st2.keys()) {
            System.out.println(key + " : " + st2.get(key));
        }
    }
}