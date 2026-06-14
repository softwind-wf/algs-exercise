package exercise1.exercise1_4;

public class LocalMinimum2D {

    public static int[] localMin(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return new int[]{-1, -1};
        }
        int n = matrix.length;
        return findLocalMin(matrix, 0, n - 1, 0, n - 1);
    }

    private static int[] findLocalMin(int[][] a, int rowStart, int rowEnd, int colStart, int colEnd) {
        if (colStart > colEnd || rowStart > rowEnd) {
            return new int[]{-1, -1};
        }

        int midCol = colStart + (colEnd - colStart) / 2;

        // 在 midCol 列上找最小值所在行
        int minRow = rowStart;
        for (int r = rowStart; r <= rowEnd; r++) {
            if (a[r][midCol] < a[minRow][midCol]) {
                minRow = r;
            }
        }

        // 检查是否比左右邻居小
        boolean leftSmaller = midCol > colStart && a[minRow][midCol - 1] < a[minRow][midCol];
        boolean rightSmaller = midCol < colEnd && a[minRow][midCol + 1] < a[minRow][midCol];

        // 检查上下边界已经由列最小值保证，但是我们要确保不比左右小就是局部最小吗？
        // 还要确保不比上下大。由于在列内最小，所以上下都不比它大，因此如果左右也不比它小，就是局部最小。
        if (!leftSmaller && !rightSmaller) {
            return new int[]{minRow, midCol};
        }

        // 如果左边更小，去左半区
        if (leftSmaller) {
            return findLocalMin(a, rowStart, rowEnd, colStart, midCol - 1);
        }

        // 否则去右半区（右边更小）
        return findLocalMin(a, rowStart, rowEnd, midCol + 1, colEnd);
    }

    public static void main(String[] args) {
        int[][] matrix = {
            {10, 8, 10, 10},
            {9,  20,  6,  9},
            {10, 6, 5, 10},
            {10, 10, 10, 10}
        };
        int[] pos = localMin(matrix);
        System.out.println("局部最小位置: (" + pos[0] + ", " + pos[1] + "), 值: " + matrix[pos[0]][pos[1]]);
    }
}