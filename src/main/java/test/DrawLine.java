package test;

import edu.princeton.cs.algs4.StdDraw;

public class DrawLine {
    public static void main(String[] args) {
        StdDraw.setCanvasSize(400, 400);
        StdDraw.setPenRadius(0.01);
        StdDraw.setPenColor(StdDraw.RED);
        StdDraw.line(0.2, 0.2, 0.8, 0.8); // 从(0.2,0.2)到(0.8,0.8)
    }
}