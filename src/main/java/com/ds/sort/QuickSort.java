package com.ds.sort;

import java.util.Arrays;
import java.util.Random;

/**
 * 快速排序：分治思想，选取 pivot，将数组划分为小于和大于 pivot 的两部分，
 * 再递归排序左右两部分。
 * 不稳定排序，原地排序，平均时间复杂度 O(n log n)，最坏 O(n²)。
 */
public class QuickSort {

    private final Random rand = new Random();

    /**
     * 基础版快速排序
     * 固定选最右元素作为 pivot。
     */
    public void sort(int[] a) {
        shuffle(a);
        sort(a, 0, a.length - 1);
    }

    private void sort(int[] a, int lo, int hi) {
        while (lo < hi) {
            int p = partition(a, lo, hi);

            // 优先递归较小的一侧，另一侧用循环处理，保证栈深度 O(log n)
            if (p - lo < hi - p) {
                sort(a, lo, p - 1);
                lo = p + 1;
            } else {
                sort(a, p + 1, hi);
                hi = p - 1;
            }
        }
    }

    /**
     * 分区：以 a[hi] 为 pivot，返回 pivot 最终位置。
     * Lomuto 分区方案：简单直观。
     */
    private int partition(int[] a, int lo, int hi) {
        int pivot = a[hi];
        int i = lo - 1;  // i 指向小于 pivot 区域的最后一个元素
        for (int j = lo; j < hi; j++) {
            if (a[j] <= pivot) {
                i++;
                swap(a, i, j);
            }
        }
        // 将 pivot 放到正确位置
        swap(a, i + 1, hi);
        return i + 1;
    }

    /**
     * 随机化快速排序
     * 随机选择 pivot 并与最右元素交换，避免最坏情况。
     */
    public void sortRandom(int[] a) {
        sortRandom(a, 0, a.length - 1);
    }

    private void sortRandom(int[] a, int lo, int hi) {
        if (lo >= hi) {
            return;
        }
        // 随机选 pivot 并交换到末尾
        int pivotIdx = lo + rand.nextInt(hi - lo + 1);
        swap(a, pivotIdx, hi);

        int p = partition(a, lo, hi);
        sortRandom(a, lo, p - 1);
        sortRandom(a, p + 1, hi);
    }

    /**
     * 三向切分快速排序（Dijkstra 3-way partitioning）
     * 将数组分为 < pivot、== pivot、> pivot 三部分。
     * 对于含大量重复元素的数组，效率远高于普通快排（接近 O(n)）。
     */
    public void sort3Way(int[] a) {
        sort3Way(a, 0, a.length - 1);
    }

    private void sort3Way(int[] a, int lo, int hi) {
        if (lo >= hi) {
            return;
        }

        // 随机选 pivot 并放到 a[lo]，避免在有序/逆序输入上递归深度退化
        int pivotIdx = lo + rand.nextInt(hi - lo + 1);
        swap(a, lo, pivotIdx);

        int lt = lo;        // a[lo..lt-1] < pivot
        int gt = hi;        // a[gt+1..hi] > pivot
        int i = lo;         // a[lt..i-1] == pivot
        int pivot = a[lo];  // 选定第一个元素为 pivot

        while (i <= gt) {
            if (a[i] < pivot) {
                swap(a, lt, i);
                lt++;
                i++;
            } else if (a[i] > pivot) {
                swap(a, i, gt);
                gt--;
            } else {
                i++;
            }
        }
        // 此时 a[lo..lt-1] < pivot, a[lt..gt] == pivot, a[gt+1..hi] > pivot
        sort3Way(a, lo, lt - 1);
        sort3Way(a, gt + 1, hi);
    }

    /**
     * 优化版快速排序
     * 1. 小区间使用插入排序。
     * 2. 三数取中法选 pivot，减少最坏情况概率。
     * 3. 三向切分处理重复元素。
     */
    public void sortOptimized(int[] a) {
        sortOptimized(a, 0, a.length - 1);
    }

    private void sortOptimized(int[] a, int lo, int hi) {
        // 小区间改用插入排序
        if (hi - lo <= 15) {
            insertionSort(a, lo, hi);
            return;
        }
        // 三数取中，并将 pivot 放到末尾
        int mid = lo + (hi - lo) / 2;
        if (a[lo] > a[mid]) swap(a, lo, mid);
        if (a[lo] > a[hi]) swap(a, lo, hi);
        if (a[mid] > a[hi]) swap(a, mid, hi);
        swap(a, mid, hi);  // 中位数放到末尾作为 pivot

        int p = partition(a, lo, hi);
        sortOptimized(a, lo, p - 1);
        sortOptimized(a, p + 1, hi);
    }

    /**
     * Hoare 分区方案（经典双指针法）
     * 比 Lomuto 方案平均少 1/3 的交换次数。
     */
    public void sortHoare(int[] a) {
        sortHoare(a, 0, a.length - 1);
    }

    private void sortHoare(int[] a, int lo, int hi) {
        if (lo >= hi) {
            return;
        }
        int p = partitionHoare(a, lo, hi);
        // 注意：Hoare 分区返回的分界点，左半边 <= pivot，右半边 >= pivot
        sortHoare(a, lo, p);
        sortHoare(a, p + 1, hi);
    }

    private int partitionHoare(int[] a, int lo, int hi) {
        int pivot = a[lo];  // 选第一个元素作为 pivot
        int i = lo - 1;
        int j = hi + 1;
        while (true) {
            // 从左向右找第一个 >= pivot 的元素
            do {
                i++;
            } while (a[i] < pivot);
            // 从右向左找第一个 <= pivot 的元素
            do {
                j--;
            } while (a[j] > pivot);
            // 指针相遇
            if (i >= j) {
                return j;
            }
            swap(a, i, j);
        }
    }

    /**
     * 双轴快速排序（Dual-Pivot Quicksort / Yaroslavskiy 算法）
     * 选两个 pivot（pivot1 <= pivot2），将数组分成三个区域：
     *   [lo, lt-1]  < pivot1
     *   [lt, gt]    >= pivot1 且 <= pivot2
     *   [gt+1, hi]  > pivot2
     * 然后递归排序三个区域。Java 7+ Arrays.sort(int[]) 使用的就是此算法。
     */
    public void sortDualPivot(int[] a) {
        sortDualPivot(a, 0, a.length - 1);
    }

    private void sortDualPivot(int[] a, int lo, int hi) {
        if (lo >= hi) {
            return;
        }
        // 小区间改用插入排序
        if (hi - lo <= 27) {
            insertionSort(a, lo, hi);
            return;
        }

        // 确保 pivot1 <= pivot2
        if (a[lo] > a[hi]) {
            swap(a, lo, hi);
        }

        int pivot1 = a[lo];
        int pivot2 = a[hi];

        // 指针初始化
        int lt = lo + 1;   // a[lo..lt-1] < pivot1
        int gt = hi - 1;   // a[gt+1..hi] > pivot2
        int k = lt;        // 扫描指针

        // 主循环：扫描 [lt, gt] 区域
        while (k <= gt) {
            if (a[k] < pivot1) {
                // 小于 pivot1，交换到左区
                swap(a, k, lt);
                lt++;
            } else if (a[k] > pivot2) {
                // 大于 pivot2，收缩右边界，把大于 pivot2 的交换到右侧
                while (k < gt && a[gt] > pivot2) {
                    gt--;
                }
                swap(a, k, gt);
                gt--;
                // 交换后 a[k] 的新值可能 < pivot1，需要再判断一次
                if (a[k] < pivot1) {
                    swap(a, k, lt);
                    lt++;
                }
            }
            // 在 [pivot1, pivot2] 范围内，保持在中区
            k++;
        }

        // 把 pivot 放到正确位置
        lt--;
        gt++;
        swap(a, lo, lt);   // pivot1 归位
        swap(a, hi, gt);   // pivot2 归位

        // 三路递归
        sortDualPivot(a, lo, lt - 1);    // < pivot1
        sortDualPivot(a, lt + 1, gt - 1); // between
        sortDualPivot(a, gt + 1, hi);     // > pivot2
    }

    private void insertionSort(int[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            int key = a[i];
            int j = i - 1;
            while (j >= lo && a[j] > key) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = key;
        }
    }

    private void swap(int[] a, int i, int j) {
        int tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    private void shuffle(int[] a) {
        int n = a.length;
        for (int i = n - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            swap(a, i, j);
        }
    }

    public static void main(String[] args) {
        QuickSort qs = new QuickSort();

        int[] arr1 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        qs.sort(arr1);
        System.out.println("基础版:     " + Arrays.toString(arr1));

        int[] arr2 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        qs.sortRandom(arr2);
        System.out.println("随机化:     " + Arrays.toString(arr2));

        int[] arr3 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        qs.sort3Way(arr3);
        System.out.println("三向切分:   " + Arrays.toString(arr3));

        int[] arr4 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        qs.sortOptimized(arr4);
        System.out.println("优化版:     " + Arrays.toString(arr4));

        int[] arr5 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        qs.sortHoare(arr5);
        System.out.println("Hoare版:    " + Arrays.toString(arr5));

        int[] arr6 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        qs.sortDualPivot(arr6);
        System.out.println("双轴快排:   " + Arrays.toString(arr6));

        // 边界测试
        int[] empty = {};
        qs.sortDualPivot(empty);
        System.out.println("空数组:     " + Arrays.toString(empty));

        int[] single = {42};
        qs.sortDualPivot(single);
        System.out.println("单元素:     " + Arrays.toString(single));

        int[] sorted = {1, 2, 3, 4, 5};
        qs.sortDualPivot(sorted);
        System.out.println("已有序:     " + Arrays.toString(sorted));

        int[] reversed = {9, 8, 7, 6, 5, 4, 3, 2, 1};
        qs.sortDualPivot(reversed);
        System.out.println("逆序排序:   " + Arrays.toString(reversed));

        int[] dup = {4, 2, 4, 1, 2, 3, 1, 3};
        qs.sortDualPivot(dup);
        System.out.println("含重复元素: " + Arrays.toString(dup));

        // 大量重复元素
        int size = 50;
        int[] manyDup = new int[size];
        for (int i = 0; i < size; i++) {
            manyDup[i] = i % 5;
        }
        qs.sortDualPivot(manyDup);
        System.out.println("大量重复:   " + Arrays.toString(Arrays.copyOf(manyDup, 20)) + "...");

        // 随机大数据排序对比
        int n = 100000;
        int[] data1 = new int[n];
        int[] data2 = new int[n];
        Random r = new Random(42);
        for (int i = 0; i < n; i++) {
            data1[i] = r.nextInt(1000000);
        }
        System.arraycopy(data1, 0, data2, 0, n);

        long t1 = System.nanoTime();
        qs.sort(data1);
        long t2 = System.nanoTime();
        qs.sortDualPivot(data2);
        long t3 = System.nanoTime();

        System.out.printf("10万元素: 普通快排 %.1fms, 双轴快排 %.1fms%n",
                (t2 - t1) / 1e6, (t3 - t2) / 1e6);
    }

}
