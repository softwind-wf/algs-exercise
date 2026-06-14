package test;

import edu.princeton.cs.algs4.StdDraw;

public class DrawStar {
    public static void main(String[] args) {
        // 1. 基础设置
        StdDraw.setCanvasSize(600, 600);
        StdDraw.setScale(-1, 1);
        StdDraw.setPenColor(StdDraw.RED);

        // 2. 定义五角星参数
        double centerX = 0;    // 中心x
        double centerY = 0;    // 中心y
        double radius = 0.8;   // 外接圆半径
        int numPoints = 5;     // 顶点数
        double[] xPoints = new double[numPoints]; // 存储顶点x坐标
        double[] yPoints = new double[numPoints]; // 存储顶点y坐标

        // 3. 计算每个顶点的坐标（从90度开始，逆时针绘制）
        for (int i = 0; i < numPoints; i++) {
            // 每个顶点的角度：90° - 72°×i（72是360/5）
            double angle = 90 - 72 * i;
            // 转换为弧度（Math.sin/cos的参数要求）
            double radian = Math.toRadians(angle);
            // 计算坐标
            xPoints[i] = centerX + radius * Math.cos(radian);
            yPoints[i] = centerY + radius * Math.sin(radian);
        }

        // 4. 绘制填充五角星
        StdDraw.filledPolygon(xPoints, yPoints);
        // 绘制五角星轮廓（黑色，加粗）
        StdDraw.setPenColor(StdDraw.BLACK);
        StdDraw.setPenRadius(0.01);
        StdDraw.polygon(xPoints, yPoints);
    }
}