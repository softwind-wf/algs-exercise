package exercise1.exercise1_1;

public class BinarySearch {

    // 查找小于key的元素数量
    public static int rank(int key, int[] a) {
        int lo = 0;
        int hi = a.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (key > a[mid]) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return lo;
    }

    // 查找等于key的元素数量
    public static int count(int key, int[] a) {
        int left = rank(key, a);
        int right = rank(key + 1, a);
        return right - left;
    }

    public static void main(String[] args) {
        int[] a = {1, 3, 5, 7, 7, 7, 9, 11};
        int key = 7;

        int r = rank(key, a);
        int c = count(key, a);

        System.out.println("小于 " + key + " 的元素数量：" + r);
        System.out.println("等于 " + key + " 的元素数量：" + c);
        System.out.println("等于 " + key + " 的元素下标范围：[" + r + ", " + (r + c - 1) + "]");
    }
}