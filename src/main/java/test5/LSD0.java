package test5;

public class LSD0 {
    // 通过前W个字符将a[]排序
    public static void sort(String[] a, int W) {
        int N = a.length;
        int R = 256;
        String[] aux = new String[N];

        for (int d = W - 1; d >= 0; d--) {
            // 根据第d个字符用键索引计数法排序
            int[] count = new int[R + 1];

            // 计算出现频率
            for (int i = 0; i < N; i++) {
                count[a[i].charAt(d) + 1]++;
            }

            // 将频率转换为索引
            for (int r = 0; r < R; r++) {
                count[r + 1] += count[r];
            }

            // 将元素分类
            for (int i = 0; i < N; i++) {
                aux[count[a[i].charAt(d)]++] = a[i];
            }

            // 回写
            for (int i = 0; i < N; i++) {
                a[i] = aux[i];
            }
        }
    }

    public static void main(String[] args) {
        // 测试数据（和书中例子一致，所有字符串长度都是7）
        String[] a = {
                "4PGC938",
                "2IYE230",
                "3CIO720",
                "1ICK750",
                "10HV845",
                "4JZY524",
                "1ICK750",
                "3CIO720",
                "10HV845",
                "10HV845",
                "2RLA629",
                "2RLA629",
                "3ATW723"
        };

        int W = 7; // 每个字符串的长度

        System.out.println("排序前：");
        printArray(a);

        // 调用LSD排序
        LSD0.sort(a, W);

        System.out.println("\n排序后：");
        printArray(a);
    }

    // 辅助方法：打印数组
    private static void printArray(String[] a) {
        for (String s : a) {
            System.out.println(s);
        }
    }
}