package test;

import edu.princeton.cs.algs4.StdDraw;

public class DrawSquare {
    public static void main(String[] args) {
        StdDraw.setCanvasSize(400, 400);
        StdDraw.setPenRadius(0.01);
        StdDraw.setPenColor(StdDraw.YELLOW);
        StdDraw.square(0.5, 0.5, 0.2); // 中心(0.5,0.5)，半边长0.2 → 边长0.4
    }
}