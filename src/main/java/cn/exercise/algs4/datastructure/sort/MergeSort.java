package cn.exercise.algs4.datastructure.sort;

import java.util.Arrays;

/**
 * 归并排序：分治思想，将数组递归地分成两半，分别排序后再合并。
 * 稳定排序，非原地排序（需要额外空间），时间复杂度 O(n log n)。
 */
public class MergeSort {

    /**
     * 自顶向下归并排序（递归版）
     */
    public void sort(int[] a) {
        int[] aux = new int[a.length];
        sort(a, aux, 0, a.length - 1);
    }

    private void sort(int[] a, int[] aux, int lo, int hi) {
        if (lo >= hi) {
            return;
        }
        int mid = lo + (hi - lo) / 2;
        sort(a, aux, lo, mid);
        sort(a, aux, mid + 1, hi);
        merge(a, aux, lo, mid, hi);
    }

    /**
     * 自底向上归并排序（迭代版）
     * 从子数组长度 1 开始，两两合并，逐步扩大子数组长度。
     * 无需递归，适合链表排序。
     */
    public void sortBottomUp(int[] a) {
        int n = a.length;
        int[] aux = new int[n];
        // sz: 当前子数组长度，每次翻倍
        for (int sz = 1; sz < n; sz *= 2) {
            for (int lo = 0; lo < n - sz; lo += sz * 2) {
                int mid = lo + sz - 1;
                int hi = Math.min(lo + sz * 2 - 1, n - 1);
                merge(a, aux, lo, mid, hi);
            }
        }
    }

    /**
     * 优化版归并排序
     * 1. 小区间改用插入排序，减少递归开销。
     * 2. 如果左右两半已经有序（a[mid] <= a[mid+1]），跳过 merge。
     * 3. 交替使用原数组和辅助数组，减少拷贝。
     */
    public void sortOptimized(int[] a) {
        int[] aux = Arrays.copyOf(a, a.length);
        sortOptimized(aux, a, 0, a.length - 1);
    }

    private void sortOptimized(int[] src, int[] dst, int lo, int hi) {
        // 小区间使用插入排序
        if (hi - lo <= 15) {
            insertionSort(dst, lo, hi);
            return;
        }
        int mid = lo + (hi - lo) / 2;
        // 注意：递归时 src 和 dst 互换，减少数组拷贝
        sortOptimized(dst, src, lo, mid);
        sortOptimized(dst, src, mid + 1, hi);

        // 如果已经有序，直接拷贝，跳过 merge
        if (src[mid] <= src[mid + 1]) {
            System.arraycopy(src, lo, dst, lo, hi - lo + 1);
            return;
        }

        mergeOptimized(src, dst, lo, mid, hi);
    }

    private void mergeOptimized(int[] src, int[] dst, int lo, int mid, int hi) {
        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) {
                dst[k] = src[j++];
            } else if (j > hi) {
                dst[k] = src[i++];
            } else if (src[i] <= src[j]) {
                dst[k] = src[i++];
            } else {
                dst[k] = src[j++];
            }
        }
    }

    /**
     * 合并两个有序子数组 [lo, mid] 和 [mid+1, hi]。
     */
    private void merge(int[] a, int[] aux, int lo, int mid, int hi) {
        // 将原数组复制到辅助数组
        if (hi + 1 - lo >= 0) System.arraycopy(a, lo, aux, lo, hi + 1 - lo);

        int i = lo;       // 左半部分的指针
        int j = mid + 1;  // 右半部分的指针

        for (int k = lo; k <= hi; k++) {
            if (i > mid) {                      // 左半部分已取完
                a[k] = aux[j++];
            } else if (j > hi) {                // 右半部分已取完
                a[k] = aux[i++];
            } else if (aux[i] <= aux[j]) {      // 取较小者，等号保证稳定性
                a[k] = aux[i++];
            } else {
                a[k] = aux[j++];
            }
        }
    }

    /**
     * 在数组 dst 的 [lo, hi] 区间上做插入排序。
     */
    private void insertionSort(int[] dst, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            int key = dst[i];
            int j = i - 1;
            while (j >= lo && dst[j] > key) {
                dst[j + 1] = dst[j];
                j--;
            }
            dst[j + 1] = key;
        }
    }

    public static void main(String[] args) {
        MergeSort ms = new MergeSort();

        int[] arr1 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        ms.sort(arr1);
        System.out.println("递归版:     " + Arrays.toString(arr1));

        int[] arr2 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        ms.sortBottomUp(arr2);
        System.out.println("迭代版:     " + Arrays.toString(arr2));

        int[] arr3 = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        ms.sortOptimized(arr3);
        System.out.println("优化版:     " + Arrays.toString(arr3));

        // 边界测试
        int[] empty = {};
        ms.sort(empty);
        System.out.println("空数组:     " + Arrays.toString(empty));

        int[] single = {42};
        ms.sort(single);
        System.out.println("单元素:     " + Arrays.toString(single));

        int[] sorted = {1, 2, 3, 4, 5};
        ms.sortOptimized(sorted);
        System.out.println("已有序:     " + Arrays.toString(sorted));

        int[] reversed = {9, 8, 7, 6, 5, 4, 3, 2, 1};
        ms.sort(reversed);
        System.out.println("逆序排序:   " + Arrays.toString(reversed));

        int[] dup = {4, 2, 4, 1, 2, 3, 1, 3};
        ms.sort(dup);
        System.out.println("含重复元素: " + Arrays.toString(dup));

        // 稳定性验证
        MergeSortTest.verifyStability();
    }
}

/**
 * 归并排序稳定性验证
 */
class MergeSortTest {

    /**
     * 自定义包装类，验证排序稳定性。
     * 相同 value 的元素应保持原始顺序。
     */
    static class Item {
        int value;
        int index;

        Item(int value, int index) {
            this.value = value;
            this.index = index;
        }

        @Override
        public String toString() {
            return value + "(" + index + ")";
        }
    }

    static void verifyStability() {
        Item[] items = {
                new Item(3, 0), new Item(1, 1), new Item(3, 2),
                new Item(2, 3), new Item(1, 4), new Item(4, 5),
                new Item(2, 6), new Item(3, 7)
        };

        // 使用归并排序（稳定）
        mergeSortStable(items);

        // 检查相同值的元素是否保持原始顺序
        boolean stable = true;
        for (int i = 0; i < items.length - 1; i++) {
            if (items[i].value == items[i + 1].value
                    && items[i].index > items[i + 1].index) {
                stable = false;
                break;
            }
        }
        System.out.println("稳定性验证: " + (stable ? "稳定" : "不稳定"));
        System.out.println("排序结果:   " + Arrays.toString(items));
    }

    @SuppressWarnings("unchecked")
    static void mergeSortStable(Item[] items) {
        Item[] aux = new Item[items.length];
        mergeSort(items, aux, 0, items.length - 1);
    }

    static void mergeSort(Item[] items, Item[] aux, int lo, int hi) {
        if (lo >= hi) return;
        int mid = lo + (hi - lo) / 2;
        mergeSort(items, aux, lo, mid);
        mergeSort(items, aux, mid + 1, hi);
        merge(items, aux, lo, mid, hi);
    }

    static void merge(Item[] items, Item[] aux, int lo, int mid, int hi) {
        for (int k = lo; k <= hi; k++) {
            aux[k] = items[k];
        }
        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) {
                items[k] = aux[j++];
            } else if (j > hi) {
                items[k] = aux[i++];
            } else if (aux[i].value <= aux[j].value) {  // <= 保证稳定性
                items[k] = aux[i++];
            } else {
                items[k] = aux[j++];
            }
        }
    }
}
