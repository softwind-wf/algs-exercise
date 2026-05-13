package exercise1.exercise1_4;

public class CommonElements {
    public static void printCommon(int[] a, int[] b) {
        int i = 0, j = 0;
        int N = a.length;  // 假设两个数组长度相等
        
        while (i < N && j < N) {
            if (a[i] < b[j]) {
                i++;
            } else if (a[i] > b[j]) {
                j++;
            } else {
                System.out.println(a[i]);
                i++;
                j++;
            }
        }
    }

    public static void main(String[] args) {
        int[] a = {1, 3, 4, 6, 7, 9};
        int[] b = {2, 3, 5, 6, 8, 9};
        
        printCommon(a, b);
        // 输出：3, 6, 9
    }
}