package exercise1.exercise1_1;

public class ShuffleTest {

    // 洗牌算法（Knuth 洗牌）
    public static void shuffle(double[] a) {
        int N = a.length;
        for (int i = 0; i < N; i++) {
            // 从 a[i..N-1] 中随机选一个元素与 a[i] 交换
            int r = i + (int) (Math.random() * (N - i));
            double temp = a[i];
            a[i] = a[r];
            a[r] = temp;
        }
    }

    public static void main(String[] args) {
        // 读取命令行参数
        int M = Integer.parseInt(args[0]);
        int N = Integer.parseInt(args[1]);

        // 创建 M×M 的计数表格
        int[][] count = new int[M][M];

        for (int experiment = 0; experiment < N; experiment++) {
            // 初始化数组：a[i] = i
            double[] a = new double[M];
            for (int i = 0; i < M; i++) {
                a[i] = i;
            }

            // 洗牌
            shuffle(a);

            // 记录每个元素的最终位置
            for (int j = 0; j < M; j++) {
                int element = (int) a[j];
                count[element][j]++;
            }
        }

        // 打印 M×M 的计数表格
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < M; j++) {
                System.out.printf("%6d", count[i][j]);
            }
            System.out.println();
        }

        // 打印理论期望值
        System.out.println("\n理论期望值: " + (double) N / M);
    }
}