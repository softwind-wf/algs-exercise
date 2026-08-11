package cn.exercise.algs4.datastructure.sort;

import java.util.Arrays;

/**
 * 堆排序：利用堆数据结构，先建堆，再反复取出堆顶（最大值）放到数组末尾。
 * 不稳定排序，原地排序，时间复杂度 O(n log n)。
 *
 * 使用 0 索引的大顶堆：
 *   父节点: (i - 1) / 2
 *   左子:   2 * i + 1
 *   右子:   2 * i + 2
 */
public class HeapSort {

    /**
     * 基础版堆排序
     * 1. 建堆：从最后一个非叶节点开始自底向上 sink。
     * 2. 排序：反复取出堆顶（最大值）与堆末尾交换，缩小堆再 sink。
     */
    public void sort(int[] a) {
        int n = a.length;

        // 1. 建堆（自底向上 heapify）：O(n)
        // 从最后一个非叶节点开始，逐个 sink
        for (int i = n / 2 - 1; i >= 0; i--) {
            sink(a, i, n);
        }

        // 2. 排序：O(n log n)
        // 每次把堆顶（最大值）换到末尾，然后缩小堆范围，恢复堆性质
        for (int i = n - 1; i > 0; i--) {
            swap(a, 0, i);      // 最大值归位
            sink(a, 0, i);      // 修复剩余部分的堆
        }
    }

    /**
     * 下沉操作（大顶堆）
     * 将索引 k 的元素下沉，使其满足父节点 >= 子节点。
     *
     * @param a 数组
     * @param k 需要下沉的元素索引
     * @param n 堆的大小（有效范围 [0, n-1]）
     */
    private void sink(int[] a, int k, int n) {
        while (true) {
            int largest = k;
            int left = 2 * k + 1;
            int right = 2 * k + 2;

            // 找出父和两个子中的最大值索引
            if (left < n && a[left] > a[largest]) {
                largest = left;
            }
            if (right < n && a[right] > a[largest]) {
                largest = right;
            }

            // 如果父已经是最大，停止
            if (largest == k) {
                break;
            }

            // 与较大的子交换，继续下沉
            swap(a, k, largest);
            k = largest;
        }
    }

    // 等效的另一种 sink 写法（非 while(true) 风格）
    private void sink2(int[] a, int k, int n) {
        int half = n / 2;  // 非叶节点边界，>= half 都是叶子
        while (k < half) {
            int child = 2 * k + 1;
            // 选较大的子
            if (child + 1 < n && a[child + 1] > a[child]) {
                child++;
            }
            // 如果父 >= 较大的子，停止
            if (a[k] >= a[child]) {
                break;
            }
            swap(a, k, child);
            k = child;
        }
    }

    /**
     * 优化版：使用 Floyd 的"先下沉到底再上浮"策略，减少比较次数。
     * 建堆方式也有所不同。
     */
    public void sortOptimized(int[] a) {
        int n = a.length;

        // 建堆
        for (int i = n / 2 - 1; i >= 0; i--) {
            sinkFloyd(a, i, n);
        }

        for (int i = n - 1; i > 0; i--) {
            swap(a, 0, i);
            sinkFloyd(a, 0, i);
        }
    }

    /**
     * Floyd 优化版下沉：先一路沉到底，再上浮到正确位置。
     * 减少了一半的比较次数：每层只比子节点，不比父子；
     * 最后一步 swim 把值上浮到正确位置。
     */
    private void sinkFloyd(int[] a, int k, int n) {
        int val = a[k];          // 保存原始值
        int half = n / 2;

        // 一路沉到底：每层只选较大的子提上来，不比较父子
        while (k < half) {
            int child = 2 * k + 1;
            if (child + 1 < n && a[child + 1] > a[child]) {
                child++;
            }
            a[k] = a[child];     // 子节点直接上提
            k = child;
        }
        // k 是叶子位置（空洞），放入原始值
        a[k] = val;

        // 上浮到正确位置（因为提上来的值可能比原始值小，也可能比原始值大）
        while (k > 0) {
            int parent = (k - 1) / 2;
            if (a[parent] >= a[k]) {
                break;
            }
            swap(a, k, parent);
            k = parent;
        }
    }

    private void swap(int[] a, int i, int j) {
        int tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    public static void main(String[] args) {
        HeapSort hs = new HeapSort();

        int[] arr1 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        hs.sort(arr1);
        System.out.println("基础版:     " + Arrays.toString(arr1));

        int[] arr2 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        hs.sortOptimized(arr2);
        System.out.println("优化版:     " + Arrays.toString(arr2));

        // 边界测试
        int[] empty = {};
        hs.sort(empty);
        System.out.println("空数组:     " + Arrays.toString(empty));

        int[] single = {42};
        hs.sort(single);
        System.out.println("单元素:     " + Arrays.toString(single));

        int[] sorted = {1, 2, 3, 4, 5};
        hs.sort(sorted);
        System.out.println("已有序:     " + Arrays.toString(sorted));

        int[] reversed = {9, 8, 7, 6, 5, 4, 3, 2, 1};
        hs.sort(reversed);
        System.out.println("逆序排序:   " + Arrays.toString(reversed));

        int[] dup = {4, 2, 4, 1, 2, 3, 1, 3};
        hs.sort(dup);
        System.out.println("含重复元素: " + Arrays.toString(dup));

        // 建堆流程打印
        System.out.println("\n=== 建堆过程演示 ===");
        int[] demo = {4, 10, 3, 5, 1, 7, 2};
        System.out.println("原始数组: " + Arrays.toString(demo));
        int n = demo.length;
        for (int i = n / 2 - 1; i >= 0; i--) {
            hs.sink(demo, i, n);
            System.out.println("sink(" + i + ")后:  " + Arrays.toString(demo));
        }
        System.out.println("建堆完成: " + Arrays.toString(demo));

        // 排序过程
        for (int i = n - 1; i > 0; i--) {
            int tmp = demo[0];
            demo[0] = demo[i];
            demo[i] = tmp;
            hs.sink(demo, 0, i);
        }
        System.out.println("排序完成: " + Arrays.toString(demo));
    }
}
