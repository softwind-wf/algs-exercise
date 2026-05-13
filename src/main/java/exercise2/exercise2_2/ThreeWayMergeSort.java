package exercise2.exercise2_2;

public class ThreeWayMergeSort {

    // 对外暴露的排序入口
    public static void sort(int[] arr) {
        if (arr == null || arr.length <= 1) {
            return;
        }
        sort(arr, 0, arr.length - 1);
    }

    // 递归排序：对 [left, right] 区间进行三路归并排序
    private static void sort(int[] arr, int left, int right) {
        // 递归终止条件：区间长度小于等于1时已有序
        if (right - left + 1 <= 1) {
            return;
        }

        // 计算两个分割点，将区间 [left, right] 三等分
        int mid1 = left + (right - left) / 3;
        int mid2 = right - (right - left) / 3;

        // 递归排序三个子区间
        sort(arr, left, mid1);
        sort(arr, mid1 + 1, mid2);
        sort(arr, mid2 + 1, right);

        // 三路合并：将三个有序子区间合并为一个有序区间
        merge(arr, left, mid1, mid2, right);
    }

    // 三路合并：合并 [left, mid1]、[mid1+1, mid2]、[mid2+1, right] 三个有序区间
    private static void merge(int[] arr, int left, int mid1, int mid2, int right) {
        int len1 = mid1 - left + 1;
        int len2 = mid2 - mid1;
        int len3 = right - mid2;

        // 复制三个子数组到临时数组
        int[] leftArr = new int[len1];
        int[] midArr = new int[len2];
        int[] rightArr = new int[len3];

        for (int i = 0; i < len1; i++) {
            leftArr[i] = arr[left + i];
        }
        for (int i = 0; i < len2; i++) {
            midArr[i] = arr[mid1 + 1 + i];
        }
        for (int i = 0; i < len3; i++) {
            rightArr[i] = arr[mid2 + 1 + i];
        }

        // 三个指针分别遍历三个临时数组
        int i = 0, j = 0, k = 0;
        // 指向原数组当前要填充的位置
        int idx = left;

        // 三路归并核心：每次取三个子数组当前最小元素
        while (i < len1 && j < len2 && k < len3) {
            if (leftArr[i] <= midArr[j] && leftArr[i] <= rightArr[k]) {
                arr[idx++] = leftArr[i++];
            } else if (midArr[j] <= leftArr[i] && midArr[j] <= rightArr[k]) {
                arr[idx++] = midArr[j++];
            } else {
                arr[idx++] = rightArr[k++];
            }
        }

        // 处理剩余的两个子数组
        while (i < len1 && j < len2) {
            if (leftArr[i] <= midArr[j]) {
                arr[idx++] = leftArr[i++];
            } else {
                arr[idx++] = midArr[j++];
            }
        }
        while (j < len2 && k < len3) {
            if (midArr[j] <= rightArr[k]) {
                arr[idx++] = midArr[j++];
            } else {
                arr[idx++] = rightArr[k++];
            }
        }
        while (i < len1 && k < len3) {
            if (leftArr[i] <= rightArr[k]) {
                arr[idx++] = leftArr[i++];
            } else {
                arr[idx++] = rightArr[k++];
            }
        }

        // 处理最后剩余的单个子数组
        while (i < len1) {
            arr[idx++] = leftArr[i++];
        }
        while (j < len2) {
            arr[idx++] = midArr[j++];
        }
        while (k < len3) {
            arr[idx++] = rightArr[k++];
        }
    }

    // 测试
    public static void main(String[] args) {
        int[] arr = {38, 27, 43, 3, 9, 82, 10, 19, 50, 22};
        System.out.println("排序前：");
        for (int num : arr) {
            System.out.print(num + " ");
        }

        sort(arr);

        System.out.println("\n排序后：");
        for (int num : arr) {
            System.out.print(num + " ");
        }
    }
}