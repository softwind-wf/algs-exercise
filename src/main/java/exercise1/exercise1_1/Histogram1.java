package exercise1.exercise1_1;

public class Histogram1 {
    // 静态方法 histogram()
    public static int[] histogram(int[] a, int M) {
        // 创建大小为 M 的结果数组，初始值都是 0
        int[] result = new int[M];
        
        // 遍历输入数组 a
        for (int num : a) {
            // 只统计 0 到 M-1 范围内的数
            if (num >= 0 && num < M) {
                result[num]++;
            }
        }
        
        return result;
    }

    public static void main(String[] args) {
        // 测试用例
        int[] a = {1, 2, 2, 3, 0, 2, 1};
        int M = 4;
        int[] hist = histogram(a, M);
        
        // 打印直方图结果
        System.out.println("直方图结果：");
        for (int i = 0; i < M; i++) {
            System.out.println(i + " 出现的次数：" + hist[i]);
        }
        
        // 验证所有元素之和是否等于原数组长度
        int sum = 0;
        for (int count : hist) {
            sum += count;
        }
        System.out.println("\n直方图元素总和：" + sum);
        System.out.println("原数组长度：" + a.length);
    }
}