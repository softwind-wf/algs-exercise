package exercise1.exercise1_1;

import edu.princeton.cs.algs4.StdDraw;

import java.util.Random;

public class RandomConnections {
    public static void main(String[] args) {
        // 从命令行读取参数
        int N = Integer.parseInt(args[0]);
        double p = Double.parseDouble(args[1]);

        // 设置画布大小与坐标范围
        StdDraw.setCanvasSize(800, 800);
        StdDraw.setXscale(-1.2, 1.2);
        StdDraw.setYscale(-1.2, 1.2);
        StdDraw.setPenColor(StdDraw.GRAY);

        // 存储圆上点的坐标
        double[][] points = new double[N][2];
        for (int i = 0; i < N; i++) {
            double angle = 2 * Math.PI * i / N;
            points[i][0] = Math.cos(angle);
            points[i][1] = Math.sin(angle);
        }

        // 绘制所有点
        StdDraw.setPenColor(StdDraw.BLACK);
        for (int i = 0; i < N; i++) {
            StdDraw.filledCircle(points[i][0], points[i][1], 0.05);
        }

        // 随机连接点对
        StdDraw.setPenColor(StdDraw.GRAY);
        Random random = new Random();
        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
                if (random.nextDouble() < p) {
                    StdDraw.line(points[i][0], points[i][1], points[j][0], points[j][1]);
                }
            }
        }
    }
}