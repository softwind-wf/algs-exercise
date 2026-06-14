package exercise1.exercise1_1;

public class CoprimeMatrix {
    public static void main(String[] args) {
        int N = 10; // 可以修改为任意正整数
        boolean[][] a = new boolean[N][N];

        // 填充布尔数组
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                a[i][j] = gcd(i, j) == 1;
            }
        }

        // 打印矩阵
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                System.out.print(a[i][j] ? "T " : "F ");
            }
            System.out.println();
        }
    }

    // 计算最大公约数（欧几里得算法）
    private static int gcd(int p, int q) {
        if (q == 0) return p;
        return gcd(q, p % q);
    }
}