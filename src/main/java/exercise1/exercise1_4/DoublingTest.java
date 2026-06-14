package exercise1.exercise1_4;

import java.util.ArrayList;
import edu.princeton.cs.algs4.StdDraw;
import edu.princeton.cs.algs4.StdOut;
import edu.princeton.cs.algs4.StdRandom;
import edu.princeton.cs.algs4.Stopwatch;

public class DoublingTest {
    
    public static double timeTrial(int N) {
        int MAX = 1000000;
        int[] a = new int[N];
        for (int i = 0; i < N; i++) {
            a[i] = StdRandom.uniformInt(-MAX, MAX);
        }
        Stopwatch timer = new Stopwatch();
        ThreeSum.count(a);
        return timer.elapsedTime();
    }

    public static void main(String[] args) {
        ArrayList<Integer> sizes = new ArrayList<>();
        ArrayList<Double> times = new ArrayList<>();

        StdDraw.setCanvasSize(800, 400);
        StdDraw.enableDoubleBuffering();

        // 收集数据
        for (int N = 250; N <= 8000; N += N) {
            double time = timeTrial(N);
            sizes.add(N);
            times.add(time);
            StdOut.printf("%7d %5.3f\n", N, time);
        }

        // 画标准图像
        drawStandardPlot(sizes, times);
        StdDraw.show();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 清空画布画对数图像
        StdDraw.clear();
        drawLogLogPlot(sizes, times);
        StdDraw.show();
    }

    private static void drawStandardPlot(ArrayList<Integer> sizes, ArrayList<Double> times) {
        int n = sizes.size();
        if (n == 0) return;

        double maxX = sizes.get(n-1) * 1.1;
        double maxY = 0;
        for (double t : times) {
            if (t > maxY) maxY = t;
        }
        maxY = maxY * 1.1;

        StdDraw.setXscale(0, maxX);
        StdDraw.setYscale(0, maxY);

        // 画坐标轴
        StdDraw.line(0, 0, maxX, 0);
        StdDraw.line(0, 0, 0, maxY);

        // 画点
        StdDraw.setPenRadius(0.01);
        for (int i = 0; i < n; i++) {
            double x = sizes.get(i);
            double y = times.get(i);
            StdDraw.point(x, y);
        }

        // 画线
        StdDraw.setPenRadius();
        for (int i = 1; i < n; i++) {
            double x1 = sizes.get(i-1);
            double y1 = times.get(i-1);
            double x2 = sizes.get(i);
            double y2 = times.get(i);
            StdDraw.line(x1, y1, x2, y2);
        }
    }

    private static void drawLogLogPlot(ArrayList<Integer> sizes, ArrayList<Double> times) {
        int n = sizes.size();
        if (n < 2) return;

        double[] lgX = new double[n];
        double[] lgY = new double[n];

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (int i = 0; i < n; i++) {
            lgX[i] = Math.log(sizes.get(i)) / Math.log(2);
            lgY[i] = Math.log(times.get(i)) / Math.log(2);

            if (lgX[i] < minX) minX = lgX[i];
            if (lgX[i] > maxX) maxX = lgX[i];
            if (lgY[i] < minY) minY = lgY[i];
            if (lgY[i] > maxY) maxY = lgY[i];
        }

        // 留边
        double xPad = (maxX - minX) * 0.1;
        double yPad = (maxY - minY) * 0.1;
        minX -= xPad;
        maxX += xPad;
        minY -= yPad;
        maxY += yPad;

        StdDraw.setXscale(minX, maxX);
        StdDraw.setYscale(minY, maxY);

        // 画坐标轴
        StdDraw.line(minX, minY, maxX, minY);
        StdDraw.line(minX, minY, minX, maxY);

        // 画点
        StdDraw.setPenRadius(0.01);
        for (int i = 0; i < n; i++) {
            StdDraw.point(lgX[i], lgY[i]);
        }

        // 画线
        StdDraw.setPenRadius();
        for (int i = 1; i < n; i++) {
            StdDraw.line(lgX[i-1], lgY[i-1], lgX[i], lgY[i]);
        }
    }
}