package cn.exercise.algs4.datastructure.heap;

import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;

/**
 * 优先队列（Priority Queue）—— 基于二叉堆（Binary Heap）
 *
 * 核心思想：
 * 不同于普通队列的 FIFO 规则，优先队列每次出队的是优先级最高（或最低）的元素。
 * 使用二叉堆实现，保证插入和删除最值均为 O(log n)。
 *
 * 二叉堆是一个完全二叉树，用数组紧凑存储：
 *   - 节点 k 的父节点: (k - 1) / 2
 *   - 节点 k 的左子节点: 2 * k + 1
 *   - 节点 k 的右子节点: 2 * k + 2
 *
 * 默认小顶堆（min-heap），可通过 Comparator 改为大顶堆（max-heap）。
 * 支持动态扩容，以及 O(n) 的批量建堆（heapify）。
 *
 * 经典应用：任务调度（先处理优先级高的）、Dijkstra 最短路径、
 *         Huffman 编码、Top-K 问题、堆排序
 *
 * @param <E> 元素类型
 */
public class MyPriorityQueue<E> {

    // ==================== 常量与成员变量 ====================

    private static final int DEFAULT_CAPACITY = 11;

    private Object[] heap;         // 底层数组，heap[0] 是堆顶
    private int size;              // 当前元素个数
    private final Comparator<? super E> comparator;  // 比较器，null 表示使用自然顺序

    // ==================== 构造方法 ====================

    public MyPriorityQueue() {
        this(DEFAULT_CAPACITY, null);
    }

    public MyPriorityQueue(int initialCapacity) {
        this(initialCapacity, null);
    }

    /**
     * @param comparator 元素比较器；null 表示元素必须实现 Comparable
     */
    public MyPriorityQueue(int initialCapacity, Comparator<? super E> comparator) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("初始容量必须大于 0: " + initialCapacity);
        }
        this.heap = new Object[initialCapacity];
        this.size = 0;
        this.comparator = comparator;
    }

    /**
     * 从已有数组直接建堆 —— O(n) 的 heapify 操作
     */
    @SuppressWarnings("unchecked")
    public MyPriorityQueue(E[] items, Comparator<? super E> comparator) {
        this.comparator = comparator;
        this.size = items.length;
        this.heap = Arrays.copyOf(items, items.length);
        heapify();
    }

    // ==================== 基础查询 ====================

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    // ==================== 核心操作 ====================

    /**
     * 插入元素
     * 时间复杂度：O(log n)
     *
     * 算法：将新元素放在堆末尾，然后通过 swim（上浮）操作将其移动到正确位置
     */
    public void insert(E item) {
        if (item == null) {
            throw new IllegalArgumentException("不能插入 null");
        }
        ensureCapacity(size + 1);
        heap[size] = item;
        swim(size);
        size++;
    }

    /**
     * 移除并返回堆顶元素（优先级最高/最低的元素）
     * 时间复杂度：O(log n)
     *
     * 算法：将堆顶与堆末尾元素交换，移除原堆顶，然后对新堆顶执行 sink（下沉）
     *
     * @throws NoSuchElementException 如果堆为空
     */
    @SuppressWarnings("unchecked")
    public E delTop() {
        if (isEmpty()) {
            throw new NoSuchElementException("优先队列为空");
        }
        E result = (E) heap[0];
        size--;
        heap[0] = heap[size];   // 将末尾元素移到堆顶
        heap[size] = null;       // 清除引用，帮助 GC
        if (size > 0) {
            sink(0);             // 下沉调整
        }
        return result;
    }

    /**
     * 查看堆顶元素但不移除
     * 时间复杂度：O(1)
     */
    @SuppressWarnings("unchecked")
    public E peek() {
        if (isEmpty()) {
            return null;
        }
        return (E) heap[0];
    }

    // ==================== 堆的核心操作：上浮与下沉 ====================

    /**
     * 上浮（swim）—— 将位置 k 的元素向上移动直到堆序恢复
     *
     * 当子节点比父节点"更小"（高优先级）时，与父节点交换，
     * 持续上浮直到到达堆顶或堆序满足。
     */
    private void swim(int k) {
        while (k > 0) {
            int parent = (k - 1) / 2;
            if (compare(k, parent) >= 0) {
                break;  // 堆序已满足：k >= parent（小顶堆）
            }
            swap(k, parent);
            k = parent;
        }
    }

    /**
     * 下沉（sink）—— 将位置 k 的元素向下移动直到堆序恢复
     *
     * 与左右子节点中"更小"（高优先级）的那个比较，
     * 若当前节点更大则交换，持续下沉直到叶子节点或堆序满足。
     */
    private void sink(int k) {
        int half = size / 2;  // 第一个叶子节点的索引
        while (k < half) {
            int left = 2 * k + 1;
            int right = left + 1;
            int smaller = left;  // 默认左子更小

            // 选左右子中更小的那个
            if (right < size && compare(right, left) < 0) {
                smaller = right;
            }

            if (compare(k, smaller) <= 0) {
                break;  // 堆序已满足：k <= 最小子节点（小顶堆）
            }

            swap(k, smaller);
            k = smaller;
        }
    }

    // ==================== 批量建堆 ====================

    /**
     * 将任意数组调整为堆 —— Floyd 建堆算法
     * 时间复杂度：O(n)
     *
     * 原理：从最后一个非叶子节点开始，依次向前对所有非叶子节点执行 sink。
     * 证明：每个节点下沉的代价与其高度成正比，所有节点的高度之和为 O(n)。
     */
    private void heapify() {
        // 从最后一个非叶子节点开始向前下沉
        for (int i = (size / 2) - 1; i >= 0; i--) {
            sink(i);
        }
    }

    // ==================== 删除与更新 ====================

    /**
     * 删除指定元素（通过线性扫描定位，O(n)；然后 O(log n) 调整）
     *
     * @return 是否成功删除
     */
    public boolean remove(E item) {
        if (item == null) {
            return false;
        }
        int idx = indexOf(item);
        if (idx < 0) {
            return false;
        }
        removeAt(idx);
        return true;
    }

    /**
     * 删除指定位置的元素
     */
    @SuppressWarnings("unchecked")
    private E removeAt(int k) {
        E removed = (E) heap[k];
        size--;
        heap[k] = heap[size];
        heap[size] = null;

        if (k < size) {
            // 先尝试上浮，再尝试下沉（总有一个会生效）
            swim(k);
            sink(k);
        }
        return removed;
    }

    /**
     * 判断是否包含指定元素（O(n) 线性扫描）
     */
    public boolean contains(E item) {
        return indexOf(item) >= 0;
    }

    private int indexOf(E item) {
        for (int i = 0; i < size; i++) {
            if (item.equals(heap[i])) {
                return i;
            }
        }
        return -1;
    }

    // ==================== 容量管理 ====================

    private void ensureCapacity(int needed) {
        if (needed > heap.length) {
            int newCapacity = heap.length * 2;
            if (newCapacity < needed) {
                newCapacity = needed;
            }
            heap = Arrays.copyOf(heap, newCapacity);
        }
    }

    // ==================== 比较与交换 ====================

    @SuppressWarnings("unchecked")
    private int compare(int i, int j) {
        if (comparator != null) {
            return comparator.compare((E) heap[i], (E) heap[j]);
        }
        return ((Comparable<? super E>) heap[i]).compareTo((E) heap[j]);
    }

    private void swap(int i, int j) {
        Object tmp = heap[i];
        heap[i] = heap[j];
        heap[j] = tmp;
    }

    // ==================== 堆排序 ====================

    /**
     * 返回按优先级排序的数组（会清空当前队列）
     * 时间复杂度：O(n log n)
     *
     * 注意：此操作会破坏堆结构
     */
    @SuppressWarnings("unchecked")
    public E[] toSortedArray() {
        Object[] result = new Object[size];
        int originalSize = size;
        for (int i = 0; i < originalSize; i++) {
            result[i] = delTop();
        }
        return (E[]) result;
    }

    /**
     * 对任意数组进行堆排序（静态方法，不影响原数组的副本）
     * 时间复杂度：O(n log n)，空间：O(1) 原地排序
     */
    @SuppressWarnings("unchecked")
    public static <T> void heapSort(T[] arr, Comparator<? super T> comparator) {
        int n = arr.length;

        // 第一步：建堆（O(n)）
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapSortSink(arr, n, i, comparator);
        }

        // 第二步：逐个将堆顶（最大值）交换到数组末尾（O(n log n)）
        for (int i = n - 1; i > 0; i--) {
            swap(arr, 0, i);              // 堆顶移到 i 位置
            heapSortSink(arr, i, 0, comparator);  // 缩小堆并调整
        }
    }

    private static <T> void heapSortSink(T[] arr, int heapSize, int k, Comparator<? super T> cmp) {
        int half = heapSize / 2;
        while (k < half) {
            int left = 2 * k + 1;
            int right = left + 1;
            int larger = left;  // 堆排序用大顶堆
            if (right < heapSize && compare(arr, right, left, cmp) > 0) {
                larger = right;
            }
            if (compare(arr, k, larger, cmp) >= 0) {
                break;
            }
            swap(arr, k, larger);
            k = larger;
        }
    }

    private static <T> int compare(T[] arr, int i, int j, Comparator<? super T> cmp) {
        if (cmp != null) {
            return cmp.compare(arr[i], arr[j]);
        }
        return ((Comparable<? super T>) arr[i]).compareTo(arr[j]);
    }

    private static <T> void swap(T[] arr, int i, int j) {
        T tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    // ==================== 转换与调试 ====================

    @SuppressWarnings("unchecked")
    public E[] toArray() {
        return (E[]) Arrays.copyOf(heap, size);
    }

    /**
     * 打印堆的树形结构（调试用）
     */
    public void printTree() {
        if (isEmpty()) {
            System.out.println("(空堆)");
            return;
        }
        printTree(0, "", true);
    }

    private void printTree(int k, String prefix, boolean isTail) {
        System.out.println(prefix + (isTail ? "└── " : "├── ") + heap[k]);

        int left = 2 * k + 1;
        int right = 2 * k + 2;

        if (left >= size && right >= size) return;

        if (right < size) {
            printTree(left, prefix + (isTail ? "    " : "│   "), false);
            printTree(right, prefix + (isTail ? "    " : "│   "), true);
        } else if (left < size) {
            printTree(left, prefix + (isTail ? "    " : "│   "), true);
        }
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[] (空优先队列)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[堆顶] ");
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(", ");
            sb.append(heap[i]);
        }
        sb.append(" [堆尾]");
        return sb.toString();
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        System.out.println("========== 优先队列测试 ==========\n");

        // 1. 小顶堆基本操作
        System.out.println("--- 1. 小顶堆 insert + delTop ---");
        MyPriorityQueue<Integer> pq = new MyPriorityQueue<>();
        int[] data = {5, 3, 8, 1, 9, 2, 7, 4, 6};
        for (int d : data) {
            pq.insert(d);
        }
        System.out.println("插入: " + Arrays.toString(data));
        System.out.println("堆结构:");
        pq.printTree();
        System.out.println("size: " + pq.size() + ", peek(): " + pq.peek());
        System.out.println("逐一出队: ");
        while (!pq.isEmpty()) {
            System.out.print(pq.delTop() + " ");
        }
        System.out.println("\n");

        // 2. heapify 批量建堆 —— O(n)
        System.out.println("--- 2. Heapify 批量建堆 O(n) ---");
        Integer[] items = {9, 1, 6, 4, 3, 8, 5, 2, 7};
        MyPriorityQueue<Integer> pq2 = new MyPriorityQueue<>(items, null);
        System.out.println("原数组: " + Arrays.toString(items));
        System.out.println("建堆后数组: " + Arrays.toString(pq2.toArray()));
        pq2.printTree();
        System.out.print("依序出队: ");
        while (!pq2.isEmpty()) {
            System.out.print(pq2.delTop() + " ");
        }
        System.out.println("\n");

        // 3. 大顶堆（自定义 Comparator）
        System.out.println("--- 3. 大顶堆（max-heap）---");
        MyPriorityQueue<Integer> maxPq = new MyPriorityQueue<>(11,
                (a, b) -> b.compareTo(a));  // 逆序比较器
        for (int d : data) {
            maxPq.insert(d);
        }
        maxPq.printTree();
        System.out.print("大顶堆出队（从大到小）: ");
        while (!maxPq.isEmpty()) {
            System.out.print(maxPq.delTop() + " ");
        }
        System.out.println("\n");

        // 4. 删除指定元素
        System.out.println("--- 4. 删除指定元素 ---");
        MyPriorityQueue<Integer> pq3 = new MyPriorityQueue<>();
        for (int d : data) {
            pq3.insert(d);
        }
        System.out.println("原堆: " + pq3);
        System.out.println("remove(7): " + pq3.remove(7));
        System.out.println("remove(99): " + pq3.remove(99));
        System.out.println("contains(9): " + pq3.contains(9));
        System.out.println("contains(7): " + pq3.contains(7));
        System.out.println("删除后: " + pq3);
        System.out.print("出队: ");
        while (!pq3.isEmpty()) {
            System.out.print(pq3.delTop() + " ");
        }
        System.out.println("\n");

        // 5. 堆排序
        System.out.println("--- 5. 堆排序（静态方法）---");
        Integer[] toSort = {42, 13, 7, 99, 3, 55, 21, 87, 1};
        System.out.println("排序前: " + Arrays.toString(toSort));
        heapSort(toSort, null);  // 原地排序
        System.out.println("排序后: " + Arrays.toString(toSort));
        System.out.println();

        // 6. toSortedArray
        System.out.println("--- 6. toSortedArray ---");
        MyPriorityQueue<Integer> pq4 = new MyPriorityQueue<>();
        pq4.insert(30);
        pq4.insert(10);
        pq4.insert(50);
        pq4.insert(20);
        pq4.insert(40);
        System.out.println("堆内: " + pq4);
        Object[] sorted = pq4.toSortedArray();
        System.out.println("排序结果(队列已空): " + Arrays.toString(sorted));
        System.out.println("队列空: " + pq4.isEmpty());
        System.out.println();

        // 7. 字符串优先队列
        System.out.println("--- 7. 字符串优先队列（字典序）---");
        MyPriorityQueue<String> strPq = new MyPriorityQueue<>();
        strPq.insert("banana");
        strPq.insert("apple");
        strPq.insert("cherry");
        strPq.insert("date");
        System.out.print("按字典序出队: ");
        while (!strPq.isEmpty()) {
            System.out.print(strPq.delTop() + " ");
        }
        System.out.println("\n");

        // 8. 边界测试
        System.out.println("--- 8. 边界测试 ---");
        MyPriorityQueue<String> empty = new MyPriorityQueue<>();
        System.out.println("空 peek: " + empty.peek());
        try {
            empty.delTop();
        } catch (NoSuchElementException e) {
            System.out.println("空 delTop 预期异常: " + e.getMessage());
        }

        // 9. 性能测试
        System.out.println("\n--- 9. 性能测试 ---");
        MyPriorityQueue<Integer> big = new MyPriorityQueue<>();
        int N = 100000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
            big.insert((int) (Math.random() * N));
        }
        long end = System.currentTimeMillis();
        System.out.println("插入 " + N + " 个随机数: " + (end - start) + "ms");

        start = System.currentTimeMillis();
        while (!big.isEmpty()) {
            big.delTop();
        }
        end = System.currentTimeMillis();
        System.out.println("全部出队: " + (end - start) + "ms");

        System.out.println();
        System.out.println("========== 测试完成 ==========");
    }
}
