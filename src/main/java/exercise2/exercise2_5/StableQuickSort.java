package exercise2.exercise2_5;

public class StableQuickSort {

    public static void stableQuickSort(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        sort(arr, 0, arr.length - 1);
    }

    private static void sort(int[] arr, int lo, int hi) {
        if (lo >= hi) return;

        int pivot = arr[lo];
        int[] temp = new int[hi - lo + 1];
        int left = 0;
        int right = temp.length - 1;
        int leftCount = 0;

        for (int i = lo; i <= hi; i++) {
            if (arr[i] < pivot) {
                temp[left++] = arr[i];
                leftCount++;
            }
        }

        for (int i = lo; i <= hi; i++) {
            if (arr[i] == pivot) {
                temp[left++] = arr[i];
            }
        }

        for (int i = lo; i <= hi; i++) {
            if (arr[i] > pivot) {
                temp[right--] = arr[i];
            }
        }

        System.arraycopy(temp, 0, arr, lo, temp.length);

        int lessEnd = lo + leftCount - 1;
        int greaterStart = lo + left;
        
        if (lessEnd > lo) {
            sort(arr, lo, lessEnd);
        }
        if (greaterStart < hi) {
            sort(arr, greaterStart, hi);
        }
    }

    // 测试
    public static void main(String[] args) {
        int[] arr = {5, 3, 2, 5, 1, 3};
        stableQuickSort(arr);
        for (int num : arr) {
            System.out.print(num + " ");
        }
    }
}