package exercise2.exercise2_2;

import java.util.Arrays;
import java.util.List;

public class ThreeListCommonName {

    // 二分查找：判断target是否在已排序的数组sortedList中
    private static boolean binarySearch(String[] sortedList, String target) {
        int left = 0;
        int right = sortedList.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            int cmp = sortedList[mid].compareTo(target);
            if (cmp == 0) {
                return true;
            } else if (cmp < 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return false;
    }

    public static String findCommonName(List<String> list1, List<String> list2, List<String> list3) {
        // 1. 将列表2和3转为数组并排序（O(N log N)）
        String[] sortedList2 = list2.toArray(new String[0]);
        Arrays.sort(sortedList2);
        String[] sortedList3 = list3.toArray(new String[0]);
        Arrays.sort(sortedList3);

        // 2. 遍历列表1，逐个检查是否在列表2和3中存在
        for (String name : list1) {
            // 先在列表2中二分查找
            if (binarySearch(sortedList2, name)) {
                // 再在列表3中二分查找
                if (binarySearch(sortedList3, name)) {
                    return name; // 找到第一个公共名字，直接返回
                }
            }
        }
        return null; // 无公共名字
    }

    // 测试
    public static void main(String[] args) {
        List<String> list1 = Arrays.asList("Alice", "Bob", "Charlie", "David");
        List<String> list2 = Arrays.asList("Bob", "David", "Eve", "Frank");
        List<String> list3 = Arrays.asList("Charlie", "Bob", "Grace", "David");

        String common = findCommonName(list1, list2, list3);
        System.out.println("第一个公共名字：" + common); // 输出 Bob
    }
}