package test3;

public class SparseMatrixMultiplication {
    public static void main(String[] args) {
        // 1. 定义向量 x
        double[] x = {0.05, 0.04, 0.36, 0.37, 0.19};

        // 2. 把矩阵 a 的每一行表示为 SparseVector
        SparseVector[] a = new SparseVector[5];

        // 第 0 行：[0, 0.90, 0, 0, 0]
        a[0] = new SparseVector();
        a[0].put(1, 0.90);

        // 第 1 行：[0, 0, 0.36, 0.36, 0.18]
        a[1] = new SparseVector();
        a[1].put(2, 0.36);
        a[1].put(3, 0.36);
        a[1].put(4, 0.18);

        // 第 2 行：[0, 0, 0, 0.90, 0]
        a[2] = new SparseVector();
        a[2].put(3, 0.90);

        // 第 3 行：[0.90, 0, 0, 0, 0]
        a[3] = new SparseVector();
        a[3].put(0, 0.90);

        // 第 4 行：[0.47, 0, 0.47, 0, 0]
        a[4] = new SparseVector();
        a[4].put(0, 0.47);
        a[4].put(2, 0.47);

        // 3. 计算矩阵向量乘法 a * x
        double[] b = new double[5];
        for (int i = 0; i < 5; i++) {
            b[i] = a[i].dot(x);
        }

        // 4. 输出结果
        System.out.println("结果向量 b：");
        for (double v : b) {
            System.out.printf("%.4f%n", v);
        }
    }
}
