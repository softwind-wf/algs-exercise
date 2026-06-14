package test;

import edu.princeton.cs.algs4.StdDraw;

public class DrawAllShapes {
    public static void main(String[] args) {
        // 1. 基础设置
        StdDraw.setCanvasSize(800, 800); // 大画布避免重叠
        StdDraw.setScale(0, 10);         // 坐标0~10，方便定位
        StdDraw.setPenRadius(0.01);      // 统一画笔粗细

        // 2. 绘制各图形
        // 点
        StdDraw.setPenColor(StdDraw.BLUE);
        StdDraw.point(1, 1);

        // 直线
        StdDraw.setPenColor(StdDraw.RED);
        StdDraw.line(1, 2, 3, 4);

        // 填充圆
        StdDraw.setPenColor(StdDraw.GREEN);
        StdDraw.filledCircle(5, 8, 1);

        // 描边正方形
        StdDraw.setPenColor(StdDraw.YELLOW);
        StdDraw.square(8, 8, 0.8);

        // 填充矩形
        StdDraw.setPenColor(StdDraw.PURPLE);
        StdDraw.filledRectangle(8, 2, 1.5, 0.8);

        // 圆弧
        StdDraw.setPenColor(StdDraw.BLACK);
        StdDraw.arc(5, 4, 1.2, 0, 180);

        // 填充多边形（三角形）
        StdDraw.setPenColor(StdDraw.ORANGE);
        double[] xTri = {2, 4, 3};
        double[] yTri = {7, 7, 9};
        StdDraw.filledPolygon(xTri, yTri);
    }
}