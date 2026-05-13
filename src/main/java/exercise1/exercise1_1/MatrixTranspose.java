package exercise1.exercise1_1;

public class MatrixTranspose {
    public static void main(String[] args) {
        // 定义一个 M 行 N 列的二维数组（示例：3行4列）
        int[][] matrix = {
            {1, 2, 3, 4},
            {5, 6, 7, 8},
            {9, 10, 11, 12}
        };

        // 获取原数组的行数和列数
        int M = matrix.length;
        int N = matrix[0].length;

        // 创建转置数组（N 行 M 列）
        int[][] transposed = new int[N][M];

        // 填充转置数组
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                transposed[j][i] = matrix[i][j];
            }
        }

        // 打印原数组
        System.out.println("原数组：");
        printMatrix(matrix);

        // 打印转置数组
        System.out.println("\n转置数组：");
        printMatrix(transposed);
    }

    // 辅助方法：打印二维数组
    public static void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            for (int num : row) {
                System.out.print(num + "\t");
            }
            System.out.println();
        }
    }
}