package test;

import edu.princeton.cs.algs4.StdDraw;

public class DrawChessboard {
    public static void main(String[] args) {
        // 1. 基础设置
        StdDraw.setCanvasSize(600, 600);
        StdDraw.setScale(0, 8); // 8x8棋盘，坐标0~8
        StdDraw.setPenRadius(0.002);

        // 2. 双层循环绘制8x8格子
        for (int i = 0; i < 8; i++) { // 行
            for (int j = 0; j < 8; j++) { // 列
                // 计算每个正方形的中心坐标（i+0.5, j+0.5），半边长0.5
                double x = i + 0.5;
                double y = j + 0.5;
                // 交替颜色：行+列是偶数→白色，奇数→黑色
                if ((i + j) % 2 == 0) {
                    StdDraw.setPenColor(StdDraw.WHITE);
                } else {
                    StdDraw.setPenColor(StdDraw.BLACK);
                }
                StdDraw.filledSquare(x, y, 0.5); // 填充正方形
                // 绘制格子边框（黑色）
                StdDraw.setPenColor(StdDraw.BLACK);
                StdDraw.square(x, y, 0.5);
            }
        }
    }
}