package test;

import edu.princeton.cs.algs4.StdDraw;

public class DrawPolygon {
    public static void main(String[] args) {
        StdDraw.setCanvasSize(400, 400);
        // 菱形顶点坐标
        double[] x = {0.1, 0.2, 0.7, 0.2};
        double[] y = {0.2, 0.5, 0.2, 0.1};
        StdDraw.setPenColor(StdDraw.ORANGE);
        StdDraw.filledPolygon(x, y); // 填充菱形
    }
}