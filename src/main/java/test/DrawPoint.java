package test;

import edu.princeton.cs.algs4.StdDraw;

public class DrawPoint {
    public static void main(String[] args) {
        StdDraw.setCanvasSize(400, 400); // 画布400x400
        StdDraw.setPenRadius(0.05);      // 粗点（默认0.002）
        StdDraw.setPenColor(StdDraw.BLUE);// 蓝色
        StdDraw.point(0.5, 0.5);         // 在画布中心画点
    }
}