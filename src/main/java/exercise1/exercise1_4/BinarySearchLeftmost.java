package exercise1.exercise1_4;

import java.util.Arrays;

public class BinarySearchLeftmost {
    // 返回最小的索引 i 使得 a[i] == key，若不存在则返回 -1
    public static int rankLeftmost(int[] a, int key) {
        int lo = 0;
        int hi = a.length - 1;
        int result = -1;  // 记录最左边的匹配位置

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (key < a[mid]) {
                hi = mid - 1;
            } else if (key > a[mid]) {
                lo = mid + 1;
            } else {  // a[mid] == key
                result = mid;    // 记录当前匹配
                hi = mid - 1;    // 继续向左搜索
            }
        }
        return result;
    }

    public static void main(String[] args) {
        int[] a = {1, 2, 2, 2, 3, 4, 5, 5, 6};
        Arrays.sort(a);  // 确保有序
        System.out.println(rankLeftmost(a, 2)); // 输出 1
        System.out.println(rankLeftmost(a, 5)); // 输出 6
        System.out.println(rankLeftmost(a, 7)); // 输出 -1
    }
}