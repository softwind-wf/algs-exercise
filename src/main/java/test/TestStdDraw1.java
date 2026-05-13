package test;

import edu.princeton.cs.algs4.StdDraw;

public class TestStdDraw1 {
    public static void main(String[] args) {
        // 1. 设置画布大小（像素）
        StdDraw.setCanvasSize(600, 600);
        
        // 2. 设置画笔样式
        StdDraw.setPenRadius(0.05); // 粗画笔
        StdDraw.setPenColor(StdDraw.BLUE); // 蓝色
        
        // 3. 画点（画布中心）
        StdDraw.point(0.5, 0.5);
        
        // 4. 改画笔样式，画直线
        StdDraw.setPenRadius(0.01);
        StdDraw.setPenColor(StdDraw.RED);
        StdDraw.line(0.2, 0.2, 0.8, 0.8); // 从(0.2,0.2)到(0.8,0.8)
    }
}