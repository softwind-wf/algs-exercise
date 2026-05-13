package test;

import edu.princeton.cs.algs4.StdDraw;

public class DrawRectangle {
    public static void main(String[] args) {
        StdDraw.setCanvasSize(400, 400);
        StdDraw.setScale(0, 10);
        StdDraw.setPenColor(StdDraw.PURPLE);
        // 中心(5,5)，半宽3，半高1 → 宽6，高2
        StdDraw.filledRectangle(5, 5, 3, 1);
    }
}