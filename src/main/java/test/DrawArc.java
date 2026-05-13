package test;

import edu.princeton.cs.algs4.StdDraw;

public class DrawArc {
    public static void main(String[] args) {
        StdDraw.setCanvasSize(400, 400);
        StdDraw.setPenRadius(0.01);
        // 圆心(0.5,0.5)，半径0.3，从0度到90度（右→上）
        StdDraw.arc(0.5, 0.5, 0.3, 0, 90);
    }
}