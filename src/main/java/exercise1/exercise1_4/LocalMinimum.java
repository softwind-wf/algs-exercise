package exercise1.exercise1_4;

public class LocalMinimum {

    // 返回任意一个局部最小值的索引
    public static int localMin(int[] a) {
        if (a == null || a.length == 0) return -1;
        if (a.length == 1) return 0; // 只有一个元素，它自己是局部最小（边界情况）
        if (a[0] < a[1]) return 0;   // 左边界情况
        if (a[a.length - 1] < a[a.length - 2]) return a.length - 1; // 右边界情况

        return localMinBinary(a, 0, a.length - 1);
    }

    // 二分递归查找局部最小
    private static int localMinBinary(int[] a, int lo, int hi) {
        if (lo > hi) return -1;

        int mid = lo + (hi - lo) / 2;

        // 检查 mid 是否为局部最小
        boolean leftSmaller = (mid == 0) || (a[mid] < a[mid - 1]);
        boolean rightSmaller = (mid == a.length - 1) || (a[mid] < a[mid + 1]);

        if (leftSmaller && rightSmaller) {
            return mid;
        }

        // 如果左边邻居更小，去左边找
        if (mid > 0 && a[mid - 1] < a[mid]) {
            return localMinBinary(a, lo, mid - 1);
        }

        // 否则去右边找（右边邻居更小）
        return localMinBinary(a, mid + 1, hi);
    }

    public static void main(String[] args) {
        int[] arr = {9, 7, 5, 6, 3, 2, 4, 8, 10};
        int idx = localMin(arr);
        if (idx != -1) {
            System.out.println("局部最小索引: " + idx + ", 值: " + arr[idx]);
        } else {
            System.out.println("没有局部最小？");
        }
    }
}