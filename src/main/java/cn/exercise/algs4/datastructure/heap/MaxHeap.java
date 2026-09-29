package cn.exercise.algs4.datastructure.heap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.List;

/**
 * 大顶堆（Max-Heap）—— 基于二叉堆（Binary Heap）
 *
 * 核心性质：任意父节点的值 >= 其左右子节点的值，
 * 因此堆顶 heap[0] 始终是整个堆中的最大元素。
 *
 * 二叉堆是一个完全二叉树，用数组紧凑存储：
 *   - 节点 k 的父节点: (k - 1) / 2
 *   - 节点 k 的左子节点: 2 * k + 1
 *   - 节点 k 的右子节点: 2 * k + 2
 *
 * 支持动态扩容，以及 O(n) 的批量建堆（heapify）。
 *
 * 经典应用：求 Top-K 最大元素、优先级调度、堆排序（升序）
 *
 * @param <E> 元素类型，必须实现 Comparable 接口
 */
public class MaxHeap<E extends Comparable<? super E>> {

    // ==================== 常量与成员变量 ====================

    private static final int DEFAULT_CAPACITY = 11;

    private Object[] heap;         // 底层数组，heap[0] 是堆顶（最大值）
    private int size;              // 当前元素个数

    // ==================== 构造方法 ====================

    public MaxHeap() {
        this(DEFAULT_CAPACITY);
    }

    public MaxHeap(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("初始容量必须大于 0: " + initialCapacity);
        }
        this.heap = new Object[initialCapacity];
        this.size = 0;
    }

    /**
     * 从已有数组直接建堆 —— O(n) 的 heapify 操作
     *
     * 算法：从最后一个非叶子节点 (size/2 - 1) 开始，依次对每个节点执行 sink 下沉
     */
    public MaxHeap(E[] items) {
        this.size = items.length;
        this.heap = Arrays.copyOf(items, items.length);
        for (int i = size / 2 - 1; i >= 0; i--) {
            sink(i);
        }
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
    @SuppressWarnings("unchecked")
    public void insert(E item) {
        if (item == null) {
            throw new IllegalArgumentException("不能插入 null");
        }
        if (size == heap.length) {
            grow();
        }
        heap[size++] = item;
        swim(size - 1);
    }

    /**
     * 移除并返回堆顶元素（最大值）
     * 时间复杂度：O(log n)
     *
     * 算法：将堆顶与堆末尾元素交换，移除原堆顶，然后对新堆顶执行 sink（下沉）
     *
     * @throws NoSuchElementException 如果堆为空
     */
    @SuppressWarnings("unchecked")
    public E delMax() {
        if (isEmpty()) {
            throw new NoSuchElementException("堆为空");
        }
        E max = (E) heap[0];
        size--;
        heap[0] = heap[size];   // 将末尾元素移到堆顶
        heap[size] = null;      // 清除引用，帮助 GC
        if (size > 0) {
            sink(0);            // 下沉调整
        }
        return max;
    }

    /**
     * 查看堆顶元素（最大值）但不移除
     * 时间复杂度：O(1)
     *
     * @throws NoSuchElementException 如果堆为空
     */
    @SuppressWarnings("unchecked")
    public E peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("堆为空");
        }
        return (E) heap[0];
    }

    /**
     * Top-K 问题：返回堆中最大的 k 个元素（降序排列），不修改原堆
     *
     * 时间复杂度：O(k log n) —— 在堆副本上弹出 k 次堆顶
     * 空间复杂度：O(n) —— 需要复制一份堆结构
     *
     * 算法：复制当前堆，然后连续执行 k 次 delMax，
     *       每次弹出的就是当前剩余元素中的最大值
     *
     * @param k 需要的最大元素个数
     * @return 前 k 大元素组成的列表（降序）；若 k >= size 则返回全部元素
     * @throws IllegalArgumentException 如果 k 为负数
     */
    public List<E> topK(int k) {
        if (k < 0) {
            throw new IllegalArgumentException("k 不能为负数: " + k);
        }
        List<E> result = new ArrayList<>(Math.min(k, size));

        // 复制一份堆结构，避免破坏原堆
        MaxHeap<E> copy = new MaxHeap<>();
        copy.heap = Arrays.copyOf(heap, Math.max(size, 1));
        copy.size = size;

        int count = Math.min(k, size);
        for (int i = 0; i < count; i++) {
            result.add(copy.delMax());
        }
        return result;
    }

    /**
     * Top-K 问题（静态便捷方法）：返回数组中最大的 k 个元素（降序排列）
     *
     * 时间复杂度：O(n + k log n) —— O(n) heapify 建堆 + O(k log n) 弹出 k 次
     * 对比完全排序 O(n log n)：当 k << n 时本方法更优
     *
     * @param items 元素数组
     * @param k     需要的最大元素个数
     * @param <E>   元素类型，必须实现 Comparable 接口
     * @return 前 k 大元素组成的列表（降序）
     * @throws IllegalArgumentException 如果 items 为 null 或 k 为负数
     */
    public static <E extends Comparable<? super E>> List<E> topK(E[] items, int k) {
        if (items == null) {
            throw new IllegalArgumentException("输入数组不能为 null");
        }
        if (k < 0) {
            throw new IllegalArgumentException("k 不能为负数: " + k);
        }
        if (k == 0 || items.length == 0) {
            return new ArrayList<>();
        }
        return new MaxHeap<>(items).topK(k);
    }

    // ==================== 堆的核心操作：上浮与下沉 ====================

    /**
     * 上浮（swim）—— 将位置 k 的元素向上移动直到堆序恢复
     *
     * 当子节点比父节点"更大"时，与父节点交换，
     * 持续上浮直到到达堆顶或父节点不小于它。
     */
    @SuppressWarnings("unchecked")
    private void swim(int k) {
        E item = (E) heap[k];
        while (k > 0) {
            int parent = (k - 1) / 2;
            if (((E) heap[parent]).compareTo(item) >= 0) {
                break;  // 父节点 >= 当前元素，堆序已满足
            }
            heap[k] = heap[parent];  // 父节点下移
            k = parent;
        }
        heap[k] = item;  // 放到最终位置
    }

    /**
     * 下沉（sink）—— 将位置 k 的元素向下移动直到堆序恢复
     *
     * 找出左右子节点中较大者，如果比当前元素大则交换，持续下沉。
     */
    @SuppressWarnings("unchecked")
    private void sink(int k) {
        E item = (E) heap[k];
        int half = size / 2;  // half 之后都是叶子节点
        while (k < half) {
            int child = 2 * k + 1;          // 左子节点
            if (child + 1 < size
                    && ((E) heap[child]).compareTo((E) heap[child + 1]) < 0) {
                child++;                     // 选出较大的子节点
            }
            if (((E) heap[child]).compareTo(item) <= 0) {
                break;  // 最大子节点 <= 当前元素，堆序已满足
            }
            heap[k] = heap[child];  // 较大子节点上移
            k = child;
        }
        heap[k] = item;  // 放到最终位置
    }

    // ==================== 内部工具 ====================

    /**
     * 扩容为原来的 2 倍（容量较小时翻倍增长，保证均摊 O(1) 扩容成本）
     */
    private void grow() {
        int newCapacity = heap.length + (heap.length >> 1) + 1;
        heap = Arrays.copyOf(heap, newCapacity);
    }

    @Override
    public String toString() {
        Object[] elements = Arrays.copyOf(heap, size);
        return Arrays.toString(elements);
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        // 1. 逐个插入
        System.out.println("--- 1. 逐个插入 ---");
        int[] data = {4, 1, 3, 2, 16, 9, 10, 14, 8, 7};
        MaxHeap<Integer> maxHeap = new MaxHeap<>();
        for (int d : data) {
            maxHeap.insert(d);
            System.out.println("插入 " + d + " 后: " + maxHeap);
        }
        System.out.println("size: " + maxHeap.size() + ", peek(): " + maxHeap.peek());

        // 2. 逐一出堆（从大到小）
        System.out.println("\n--- 2. 逐一出堆（从大到小） ---");
        while (!maxHeap.isEmpty()) {
            System.out.print(maxHeap.delMax() + " ");
        }
        System.out.println("\n");

        // 3. heapify 批量建堆 —— O(n)
        System.out.println("--- 3. Heapify 批量建堆 O(n) ---");
        Integer[] items = {9, 1, 6, 4, 3, 8, 5, 2, 7};
        MaxHeap<Integer> heap2 = new MaxHeap<>(items);
        System.out.println("原数组: " + Arrays.toString(items));
        System.out.println("建堆后: " + heap2);
        System.out.print("出堆: ");
        while (!heap2.isEmpty()) {
            System.out.print(heap2.delMax() + " ");
        }
        System.out.println("\n");

        // 4. 边界测试
        System.out.println("--- 4. 边界测试 ---");
        MaxHeap<String> empty = new MaxHeap<>();
        try {
            empty.peek();
        } catch (NoSuchElementException e) {
            System.out.println("空堆 peek 预期异常: " + e.getMessage());
        }
        try {
            empty.delMax();
        } catch (NoSuchElementException e) {
            System.out.println("空堆 delMax 预期异常: " + e.getMessage());
        }

        // 5. Top-K 问题
        System.out.println("\n--- 5. Top-K 问题 ---");
        Integer[] nums = {3, 8, 1, 9, 2, 7, 4, 6, 5, 0};
        System.out.println("原数组: " + Arrays.toString(nums));

        // 5.1 静态方法：直接从数组取 Top-3
        System.out.println("数组 Top-3: " + MaxHeap.topK(nums, 3));
        System.out.println("数组 Top-5: " + MaxHeap.topK(nums, 5));

        // 5.2 实例方法：从已有堆中取 Top-3，且不破坏原堆
        MaxHeap<Integer> heap3 = new MaxHeap<>(nums);
        System.out.println("建堆后: " + heap3);
        System.out.println("堆 Top-3: " + heap3.topK(3));
        System.out.println("取 Top-3 后原堆不变: " + heap3);

        // 5.3 边界：k > size 返回全部；k = 0 返回空
        System.out.println("Top-20（超过堆大小，返回全部）: " + heap3.topK(20));
        System.out.println("Top-0: " + heap3.topK(0));

        System.out.println("\n========== 测试完成 ==========");
    }
}
