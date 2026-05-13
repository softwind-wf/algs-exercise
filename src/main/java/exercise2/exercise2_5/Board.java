package exercise2.exercise2_5;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 8 数码棋盘状态封装
 * 存储当前棋盘、计算启发函数、生成邻接状态
 */
public class Board {
    private final int[][] tiles; // 3x3 棋盘，0 代表空格
    private final int n;         // 棋盘大小（固定为3）
    private int hamming;         // 错位格子数（h1）
    private int manhattan;       // 曼哈顿距离之和（h2）
    private int manhattanSq;     // 曼哈顿距离平方和（h3）

    // 构造函数：初始化棋盘
    public Board(int[][] tiles) {
        this.n = tiles.length;
        this.tiles = new int[n][n];
        // 防御性拷贝，防止外部修改
        for (int i = 0; i < n; i++) {
            System.arraycopy(tiles[i], 0, this.tiles[i], 0, n);
        }
        // 预计算3种启发函数
        computeHeuristics();
    }

    // 预计算所有启发函数
    private void computeHeuristics() {
        hamming = 0;
        manhattan = 0;
        manhattanSq = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int val = tiles[i][j];
                if (val == 0) continue; // 空格不计算
                // 目标位置：数字 val 应该在 ( (val-1)/3, (val-1)%3 )
                int targetI = (val - 1) / n;
                int targetJ = (val - 1) % n;
                // 1. 错位格子数
                if (i != targetI || j != targetJ) {
                    hamming++;
                }
                // 2. 曼哈顿距离
                int dist = Math.abs(i - targetI) + Math.abs(j - targetJ);
                manhattan += dist;
                // 3. 曼哈顿距离平方和
                manhattanSq += dist * dist;
            }
        }
    }

    // 获取3种启发函数
    public int hamming() { return hamming; }
    public int manhattan() { return manhattan; }
    public int manhattanSquared() { return manhattanSq; }

    // 判断是否是目标状态
    public boolean isGoal() {
        return hamming == 0; // 错位格子数为0即目标
    }

    // 判断两个棋盘是否相等
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Board board = (Board) o;
        return Arrays.deepEquals(tiles, board.tiles);
    }

    // 生成哈希（用于A*的closed集合去重）
    @Override
    public int hashCode() {
        return Arrays.deepHashCode(tiles);
    }

    // 生成所有邻接状态（空格上下左右移动后的棋盘）
    public Iterable<Board> neighbors() {
        List<Board> neighbors = new ArrayList<>();
        // 找到空格位置
        int zeroI = -1, zeroJ = -1;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (tiles[i][j] == 0) {
                    zeroI = i;
                    zeroJ = j;
                    break;
                }
            }
            if (zeroI != -1) break;
        }

        // 尝试4个方向移动：上、下、左、右
        // 上：空格和上面的格子交换（zeroI > 0）
        if (zeroI > 0) {
            int[][] newTiles = copyTiles();
            swap(newTiles, zeroI, zeroJ, zeroI - 1, zeroJ);
            neighbors.add(new Board(newTiles));
        }
        // 下：空格和下面的格子交换（zeroI < n-1）
        if (zeroI < n - 1) {
            int[][] newTiles = copyTiles();
            swap(newTiles, zeroI, zeroJ, zeroI + 1, zeroJ);
            neighbors.add(new Board(newTiles));
        }
        // 左：空格和左边的格子交换（zeroJ > 0）
        if (zeroJ > 0) {
            int[][] newTiles = copyTiles();
            swap(newTiles, zeroI, zeroJ, zeroI, zeroJ - 1);
            neighbors.add(new Board(newTiles));
        }
        // 右：空格和右边的格子交换（zeroJ < n-1）
        if (zeroJ < n - 1) {
            int[][] newTiles = copyTiles();
            swap(newTiles, zeroI, zeroJ, zeroI, zeroJ + 1);
            neighbors.add(new Board(newTiles));
        }
        return neighbors;
    }

    // 拷贝棋盘
    private int[][] copyTiles() {
        int[][] copy = new int[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(tiles[i], 0, copy[i], 0, n);
        }
        return copy;
    }

    // 交换两个格子
    private void swap(int[][] arr, int i1, int j1, int i2, int j2) {
        int temp = arr[i1][j1];
        arr[i1][j1] = arr[i2][j2];
        arr[i2][j2] = temp;
    }

    // 打印棋盘
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                sb.append(String.format("%2d ", tiles[i][j]));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}