package test;

import edu.princeton.cs.algs4.StdDraw;

public class DrawCircle {
    public static void main(String[] args) {
        StdDraw.setCanvasSize(400, 400);
        StdDraw.setScale(0, 10); // 坐标范围0~10，更直观
        StdDraw.setPenColor(StdDraw.GREEN);
        StdDraw.filledCircle(5, 5, 3); // 圆心(5,5)，半径2
    }
}