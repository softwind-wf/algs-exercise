package exercise1.exercise1_4;

public class BitonicSearch {

    // 返回目标值的索引，没找到返回 -1
    public static int search(int[] a, int target) {
        if (a == null || a.length == 0) return -1;

        // 1. 找峰值
        int peak = findPeak(a);
        if (peak == -1) return -1; // 不应该发生，但以防万一

        // 2. 在左侧递增部分二分查找
        int idx = binarySearchAsc(a, 0, peak, target);
        if (idx != -1) return idx;

        // 3. 在右侧递减部分二分查找
        return binarySearchDesc(a, peak + 1, a.length - 1, target);
    }

    // 找峰值索引
    private static int findPeak(int[] a) {
        int lo = 0;
        int hi = a.length - 1;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;

            // 检查 mid 是否为峰值
            boolean leftOk = (mid == 0) || (a[mid] > a[mid - 1]);
            boolean rightOk = (mid == a.length - 1) || (a[mid] > a[mid + 1]);

            if (leftOk && rightOk) {
                return mid;
            }

            if (mid < a.length - 1 && a[mid] < a[mid + 1]) {
                // 峰值在右边
                lo = mid + 1;
            } else {
                // 峰值在左边（包括 mid）
                hi = mid - 1;
            }
        }
        return -1;
    }

    // 递增区间二分查找
    private static int binarySearchAsc(int[] a, int lo, int hi, int target) {
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] == target) return mid;
            if (a[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return -1;
    }

    // 递减区间二分查找
    private static int binarySearchDesc(int[] a, int lo, int hi, int target) {
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] == target) return mid;
            if (a[mid] > target) { // 递减区间，比目标大往右
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        int[] arr = {1, 3, 5, 7, 6, 4, 2};
        int target = 4;
        int idx = search(arr, target);
        System.out.println("目标 " + target + " 的索引: " + idx);
    }
}