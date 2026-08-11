package cn.exercise.algs4.datastructure.search;

/**
 * 插值查找：在有序且均匀分布的数组中快速定位目标值。
 * 根据目标值与边界值的比例估算位置，逼近速度比二分查找更快。
 */
public class InterpolationSearch {

    /**
     * 迭代版插值查找
     * @param a      有序数组（升序，均匀分布时性能最优）
     * @param target 要查找的目标值
     * @return 目标值的索引，若不存在返回 -1
     */
    public int search(int[] a, int target) {
        int lo = 0;
        int hi = a.length - 1;

        // target 必须在 [a[lo], a[hi]] 范围内，且 lo <= hi
        while (lo <= hi && target >= a[lo] && target <= a[hi]) {
            // 只有一个元素时直接比较，避免除以零
            if (lo == hi) {
                return a[lo] == target ? lo : -1;
            }

            // 插值公式：按值的相对位置估算下标
            // pos = lo + (target - a[lo]) / (a[hi] - a[lo]) * (hi - lo)
            int pos = lo + (int) ((long) (target - a[lo]) * (hi - lo) / (a[hi] - a[lo]));

            if (a[pos] == target) {
                return pos;
            }
            if (a[pos] < target) {
                lo = pos + 1;
            } else {
                hi = pos - 1;
            }
        }
        return -1;
    }

    /**
     * 递归版插值查找
     */
    public int searchRecursive(int[] a, int target, int lo, int hi) {
        // 越界或不在范围内
        if (lo > hi || target < a[lo] || target > a[hi]) {
            return -1;
        }

        if (lo == hi) {
            return a[lo] == target ? lo : -1;
        }

        int pos = lo + (int) ((long) (target - a[lo]) * (hi - lo) / (a[hi] - a[lo]));

        if (a[pos] == target) {
            return pos;
        }
        if (a[pos] < target) {
            return searchRecursive(a, target, pos + 1, hi);
        } else {
            return searchRecursive(a, target, lo, pos - 1);
        }
    }

    public int searchRecursive(int[] a, int target) {
        return searchRecursive(a, target, 0, a.length - 1);
    }

    public static void main(String[] args) {
        InterpolationSearch is = new InterpolationSearch();

        // 均匀分布：查找极快
        int[] arr = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};

        System.out.println("=== 迭代版 ===");
        System.out.println("找 70 的索引: " + is.search(arr, 70));   // 6
        System.out.println("找 10 的索引: " + is.search(arr, 10));   // 0
        System.out.println("找 100 的索引: " + is.search(arr, 100)); // 9
        System.out.println("找 55 的索引: " + is.search(arr, 55));   // -1

        System.out.println("\n=== 递归版 ===");
        System.out.println("找 70 的索引: " + is.searchRecursive(arr, 70));   // 6
        System.out.println("找 55 的索引: " + is.searchRecursive(arr, 55));   // -1

        // 不均匀分布也能工作，只是速度退化
        int[] arr2 = {1, 2, 3, 4, 100, 200, 300, 400};
        System.out.println("\n=== 不均匀分布 ===");
        System.out.println("找 100 的索引: " + is.search(arr2, 100)); // 4
    }
}
