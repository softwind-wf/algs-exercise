package test;

import edu.princeton.cs.algs4.StdDraw;

public class DrawFace {
    public static void main(String[] args) {
        // 1. 基础设置
        StdDraw.setCanvasSize(600, 600);
        StdDraw.setScale(-1, 1); // 坐标范围-1~1，中心(0,0)
        StdDraw.setPenRadius(0.005);

        // 2. 绘制脸（填充圆，中心0,0，半径0.8）
        StdDraw.setPenColor(StdDraw.PINK);
        StdDraw.filledCircle(0, 0, 0.8);
        StdDraw.setPenColor(StdDraw.BLACK);
        StdDraw.circle(0, 0, 0.8); // 脸的轮廓线

        // 3. 绘制眼睛（两个小圆，对称分布）
        double eyeRadius = 0.1;
        StdDraw.filledCircle(-0.3, 0.2, eyeRadius); // 左眼
        StdDraw.filledCircle(0.3, 0.2, eyeRadius);  // 右眼

        // 4. 绘制鼻子（直线+小三角形）
        StdDraw.line(0, 0.1, 0, -0.2); // 鼻梁
        double[] noseX = {0, -0.1, 0.1};
        double[] noseY = {-0.2, -0.3, -0.3};
        StdDraw.filledPolygon(noseX, noseY); // 鼻头

        // 5. 绘制嘴巴（圆弧，从-60度到60度，向下弯曲）
        StdDraw.setPenRadius(0.01);
        StdDraw.arc(0, -0.4, 0.3, -60, 60);
    }
}