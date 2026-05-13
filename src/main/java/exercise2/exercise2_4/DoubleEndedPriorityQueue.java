package exercise2.exercise2_4;

import java.util.HashMap;
import java.util.Map;

public class DoubleEndedPriorityQueue<Key extends Comparable<Key>> {
    private Key[] minHeap;      // 最小堆数组（1-based 索引）
    private Key[] maxHeap;      // 最大堆数组（1-based 索引）
    private int minN = 0;       // 最小堆元素个数
    private int maxN = 0;       // 最大堆元素个数
    private final Map<Key, Integer> count; // 惰性删除计数

    // 构造函数：初始化堆容量
    @SuppressWarnings("unchecked")
    public DoubleEndedPriorityQueue(int capacity) {
        minHeap = (Key[]) new Comparable[capacity + 1];
        maxHeap = (Key[]) new Comparable[capacity + 1];
        count = new HashMap<>();
    }

    private int totalElements() {
        int total = 0;
        for (int c : count.values()) {
            if (c > 0) total += c;
        }
        return total;
    }

    // 插入元素：同步插入最小堆和最大堆
    public void insert(Key key) {
        if (minN >= minHeap.length - 1 || maxN >= maxHeap.length - 1) {
            throw new IllegalStateException("优先队列已满");
        }
        minHeap[++minN] = key;
        swimMin(minN);
        // 插入最大堆
        maxHeap[++maxN] = key;
        swimMax(maxN);
        // 更新计数
        count.put(key, count.getOrDefault(key, 0) + 1);
    }

    // 查找最小元素：O(1)
    public Key findMin() {
        cleanMinHeap();
        if (minN == 0) return null;
        return minHeap[1];
    }

    // 查找最大元素：O(1)
    public Key findMax() {
        cleanMaxHeap();
        if (maxN == 0) return null;
        return maxHeap[1];
    }

    // 删除最小元素：O(log n)
    public Key deleteMin() {
        cleanMinHeap();
        if (minN == 0) return null;
        Key min = minHeap[1];
        exch(minHeap, 1, minN--);
        sinkMin(1);
        if (count.get(min) > 0) {
            count.put(min, count.get(min) - 1);
        }
        return min;
    }

    // 删除最大元素：O(log n)
    public Key deleteMax() {
        cleanMaxHeap();
        if (maxN == 0) return null;
        Key max = maxHeap[1];
        exch(maxHeap, 1, maxN--);
        sinkMax(1);
        if (count.get(max) > 0) {
            count.put(max, count.get(max) - 1);
        }
        return max;
    }

    // 惰性清理最小堆：移除堆顶已被标记删除的元素
    private void cleanMinHeap() {
        while (minN > 0 && count.get(minHeap[1]) == 0) {
            exch(minHeap, 1, minN--);
            sinkMin(1);
        }
    }

    // 惰性清理最大堆：移除堆顶已被标记删除的元素
    private void cleanMaxHeap() {
        while (maxN > 0 && count.get(maxHeap[1]) == 0) {
            exch(maxHeap, 1, maxN--);
            sinkMax(1);
        }
    }

    // --- 最小堆工具方法（来自《算法》第四版 MinPQ）---
    private void swimMin(int k) {
        while (k > 1 && greater(minHeap[k/2], minHeap[k])) {
            exch(minHeap, k, k/2);
            k = k/2;
        }
    }

    private void sinkMin(int k) {
        while (2*k <= minN) {
            int j = 2*k;
            if (j < minN && greater(minHeap[j], minHeap[j+1])) j++;
            if (!greater(minHeap[k], minHeap[j])) break;
            exch(minHeap, k, j);
            k = j;
        }
    }

    // --- 最大堆工具方法（来自《算法》第四版 MaxPQ）---
    private void swimMax(int k) {
        while (k > 1 && less(maxHeap[k/2], maxHeap[k])) {
            exch(maxHeap, k, k/2);
            k = k/2;
        }
    }

    private void sinkMax(int k) {
        while (2*k <= maxN) {
            int j = 2*k;
            if (j < maxN && less(maxHeap[j], maxHeap[j+1])) j++;
            if (!less(maxHeap[k], maxHeap[j])) break;
            exch(maxHeap, k, j);
            k = j;
        }
    }

    // --- 通用辅助方法 ---
    private boolean greater(Key v, Key w) {
        return v.compareTo(w) > 0;
    }

    private boolean less(Key v, Key w) {
        return v.compareTo(w) < 0;
    }

    private void exch(Key[] a, int i, int j) {
        Key temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }

    public boolean isEmpty() {
        cleanMinHeap();
        return minN == 0;
    }

    // 测试用例
    public static void main(String[] args) {
        DoubleEndedPriorityQueue<Integer> pq = new DoubleEndedPriorityQueue<>(10);
        pq.insert(5);
        pq.insert(3);
        pq.insert(7);
        pq.insert(2);

        System.out.println("findMin: " + pq.findMin());  // 2
        System.out.println("findMax: " + pq.findMax());  // 7
        System.out.println("deleteMin: " + pq.deleteMin()); // 2
        System.out.println("deleteMax: " + pq.deleteMax()); // 7
        System.out.println("findMin: " + pq.findMin());  // 3
        System.out.println("findMax: " + pq.findMax());  // 5
    }

    public int size() {
        cleanMinHeap();
        return minN;
    }

}