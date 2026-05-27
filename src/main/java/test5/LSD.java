package test5;

public class LSD {
    /**
     * 将字符串数组按字典序升序排序（适应长度不同的字符串）
     * @param a 待排序的字符串数组
     * @param W 参数已不再使用（为保持接口兼容而保留）
     */
    public static void sort(String[] a, int W) {
        if (a == null || a.length == 0) return;
        int N = a.length;

        // 找出最长字符串的长度
        int maxLen = 0;
        for (String s : a) {
            if (s != null && s.length() > maxLen) {
                maxLen = s.length();
            }
        }

        int R = 256;               // 扩展 ASCII 字符集
        String[] aux = new String[N];

        // 从最低有效位（最右边字符）向最高有效位（最左边字符）排序
        for (int d = maxLen - 1; d >= 0; d--) {
            // 计数数组大小：R+2，索引 0..R+1
            // 其中 key = 0 表示“空字符”（字符串长度不足 d+1）
            // key = 1..256 表示实际字符（字符码 + 1）
            int[] count = new int[R + 2];

            // 1. 统计频率：注意使用 count[key+1] 进行累加
            for (int i = 0; i < N; i++) {
                int key = (d < a[i].length()) ? (a[i].charAt(d) + 1) : 0;
                count[key + 1]++;
            }

            // 2. 将频率转换为索引（前缀和）
            for (int r = 0; r < R + 1; r++) {
                count[r + 1] += count[r];
            }

            // 3. 将元素分类到 aux 中
            for (int i = 0; i < N; i++) {
                int key = (d < a[i].length()) ? (a[i].charAt(d) + 1) : 0;
                aux[count[key]++] = a[i];
            }

            // 4. 回写
            for (int i = 0; i < N; i++) {
                a[i] = aux[i];
            }
        }
    }

    public static void main(String[] args) {
        // 测试数据：包含不同长度的字符串
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
                "3ATW723",
                "abcde",
                "abyxz",
                "abcvcxz",
                "b",
                "ba",
                "c"
        };

        System.out.println("排序前：");
        printArray(a);

        // 调用 LSD 排序（第二个参数不再使用，可传任意值）
        LSD.sort(a, 7);

        System.out.println("\n排序后（字典序）：");
        printArray(a);
    }

    // 辅助方法：打印数组
    private static void printArray(String[] a) {
        for (String s : a) {
            System.out.println(s);
        }
    }
}