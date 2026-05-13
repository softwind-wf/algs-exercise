package exercise1.exercise1_1;

import edu.princeton.cs.algs4.StdOut;

public class Matrix {

    // 向量点乘
    public static double dot(double[] x, double[] y) {
        if (x.length != y.length)
            throw new IllegalArgumentException("向量长度必须相等");
        double sum = 0.0;
        for (int i = 0; i < x.length; i++)
            sum += x[i] * y[i];
        return sum;
    }

    // 矩阵和矩阵之积
    public static double[][] mult(double[][] a, double[][] b) {
        int m = a.length;
        int n = a[0].length;
        int p = b[0].length;
        if (n != b.length)
            throw new IllegalArgumentException("矩阵维度不匹配");
        double[][] c = new double[m][p];
        for (int i = 0; i < m; i++)
            for (int j = 0; j < p; j++)
                for (int k = 0; k < n; k++)
                    c[i][j] += a[i][k] * b[k][j];
        return c;
    }

    // 转置矩阵
    public static double[][] transpose(double[][] a) {
        int m = a.length;
        int n = a[0].length;
        double[][] b = new double[n][m];
        for (int i = 0; i < m; i++)
            for (int j = 0; j < n; j++)
                b[j][i] = a[i][j];
        return b;
    }

    // 矩阵和向量之积
    public static double[] mult(double[][] a, double[] x) {
        int m = a.length;
        int n = a[0].length;
        if (n != x.length)
            throw new IllegalArgumentException("矩阵列数必须等于向量长度");
        double[] y = new double[m];
        for (int i = 0; i < m; i++)
            y[i] = dot(a[i], x);
        return y;
    }

    // 向量和矩阵之积
    public static double[] mult(double[] y, double[][] a) {
        int m = a.length;
        int n = a[0].length;
        if (y.length != m)
            throw new IllegalArgumentException("向量长度必须等于矩阵行数");
        double[] x = new double[n];
        for (int j = 0; j < n; j++)
            for (int i = 0; i < m; i++)
                x[j] += y[i] * a[i][j];
        return x;
    }

    // 测试用例
    public static void main(String[] args) {
        StdOut.println("=== 测试向量点乘 ===");
        double[] x = {1, 2, 3};
        double[] y = {4, 5, 6};
        StdOut.println("dot(x, y) = " + dot(x, y));

        StdOut.println("\n=== 测试矩阵和矩阵之积 ===");
        double[][] a = {{1, 2}, {3, 4}};
        double[][] b = {{5, 6}, {7, 8}};
        double[][] c = mult(a, b);
        printMatrix(c);

        StdOut.println("\n=== 测试转置矩阵 ===");
        double[][] d = transpose(a);
        printMatrix(d);

        StdOut.println("\n=== 测试矩阵和向量之积 ===");
        double[] v = {1, 2};
        double[] w = mult(a, v);
        printVector(w);

        StdOut.println("\n=== 测试向量和矩阵之积 ===");
        double[] z = mult(v, a);
        printVector(z);
    }

    // 辅助方法：打印矩阵
    private static void printMatrix(double[][] matrix) {
        for (double[] row : matrix) {
            for (double val : row) {
                StdOut.printf("%6.1f ", val);
            }
            StdOut.println();
        }
    }

    // 辅助方法：打印向量
    private static void printVector(double[] vector) {
        for (double val : vector) {
            StdOut.printf("%6.1f ", val);
        }
        StdOut.println();
    }
}