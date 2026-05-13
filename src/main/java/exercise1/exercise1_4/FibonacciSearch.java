package exercise1.exercise1_4;

public class FibonacciSearch {

    public static int fibonacciSearch(int[] a, int key) {
        int n = a.length;
        if (n == 0) return -1;

        // 找到大于等于 n 的最小斐波那契数
        int f2 = 0;  // F_{k-2}
        int f1 = 1;  // F_{k-1}
        int f = 1;   // F_k

        while (f < n) {
            f2 = f1;
            f1 = f;
            f = f1 + f2;  // 只用加法
        }

        int offset = -1;  // 当前查找范围的起始偏移

        // 开始查找
        while (f > 1) {
            // 计算检查点的索引
            int i = Math.min(offset + f2, n - 1);

            if (a[i] < key) {
                // 目标在右侧
                f = f1;        // F_k = F_{k-1}
                f1 = f2;       // F_{k-1} = F_{k-2}
                f2 = f - f1;   // F_{k-2} = F_k - F_{k-1}（减法）
                offset = i;     // 更新偏移
            } else if (a[i] > key) {
                // 目标在左侧
                f = f2;        // F_k = F_{k-2}
                f1 = f1 - f2;  // F_{k-1} = F_{k-1} - F_{k-2}（减法）
                f2 = f - f1;   // F_{k-2} = F_k - F_{k-1}（减法）
                // offset 保持不变
            } else {
                // 找到目标
                return i;
            }
        }

        // 检查最后一个元素
        if (f1 == 1 && offset + 1 < n && a[offset + 1] == key) {
            return offset + 1;
        }

        return -1;
    }

    public static void main(String[] args) {
        // 正确的测试数组 - 8个元素
        int[] a = {1, 3, 5, 7, 9, 11, 13, 15};

        System.out.println("测试数组: " + java.util.Arrays.toString(a));
        System.out.println("数组长度: " + a.length);
        System.out.println();

        // 测试所有存在的元素
        int[] testKeys = {1, 3, 5, 7, 9, 11, 13, 15};

        System.out.println("测试存在的元素：");
        for (int key : testKeys) {
            int index = fibonacciSearch(a, key);
            if (index != -1) {
                System.out.println("找到 " + key + " 在索引 " + index);
            } else {
                System.out.println("错误：未找到 " + key);
            }
        }

        System.out.println("\n测试不存在的元素：");
        // 测试不存在的元素
        int[] notExist = {0, 2, 4, 6, 8, 10, 12, 14, 16};
        for (int key : notExist) {
            int index = fibonacciSearch(a, key);
            if (index == -1) {
                System.out.println("正确：未找到 " + key);
            } else {
                System.out.println("错误：找到不存在的 " + key + " 在索引 " + index);
            }
        }
    }
}