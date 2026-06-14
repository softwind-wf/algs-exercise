package exercise1.exercise1_1;

public class BadShuffleTest {

    // 错误的洗牌算法
    public static void badShuffle(double[] a) {
        int N = a.length;
        for (int i = 0; i < N; i++) {
            // 从整个数组中随机选一个元素交换（错误）
            int r = (int) (Math.random() * N);
            double temp = a[i];
            a[i] = a[r];
            a[r] = temp;
        }
    }

    public static void main(String[] args) {
        int M = Integer.parseInt(args[0]);
        int N = Integer.parseInt(args[1]);

        int[][] count = new int[M][M];

        for (int experiment = 0; experiment < N; experiment++) {
            double[] a = new double[M];
            for (int i = 0; i < M; i++) {
                a[i] = i;
            }

            badShuffle(a);

            for (int j = 0; j < M; j++) {
                int element = (int) a[j];
                count[element][j]++;
            }
        }

        // 打印计数表格
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < M; j++) {
                System.out.printf("%6d", count[i][j]);
            }
            System.out.println();
        }

        System.out.println("\n理论期望值: " + (double) N / M);
    }
}