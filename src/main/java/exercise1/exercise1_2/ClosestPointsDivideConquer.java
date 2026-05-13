package exercise1.exercise1_2;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class ClosestPointsDivideConquer {

    // 教材版 Point2D 类
    public static class Point2D implements Comparable<Point2D> {
        private final double x, y;

        public Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double x() { return x; }
        public double y() { return y; }

        public double distanceTo(Point2D that) {
            double dx = this.x - that.x;
            double dy = this.y - that.y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        public int compareTo(Point2D that) {
            if (this.x < that.x) return -1;
            if (this.x > that.x) return 1;
            if (this.y < that.y) return -1;
            if (this.y > that.y) return 1;
            return 0;
        }

        public static final Comparator<Point2D> Y_ORDER = Comparator.comparingDouble(Point2D::y).thenComparingDouble(Point2D::x);
    }

    // 主方法：计算最近点对距离
    public static double closest(Point2D[] points) {
        int n = points.length;
        if (n <= 1) return Double.POSITIVE_INFINITY;

        // 按 x 坐标排序
        Arrays.sort(points);
        Point2D[] aux = new Point2D[n];
        return closest(points, aux, 0, n - 1);
    }

    // 递归分治
    private static double closest(Point2D[] points, Point2D[] aux, int lo, int hi) {
        if (hi <= lo + 3) {
            return bruteForce(points, lo, hi);
        }

        int mid = lo + (hi - lo) / 2;
        Point2D midPoint = points[mid];
        double leftDist = closest(points, aux, lo, mid);
        double rightDist = closest(points, aux, mid + 1, hi);
        double d = Math.min(leftDist, rightDist);

        // 合并：检查中间带内的点
        int m = 0;
        for (int i = lo; i <= hi; i++) {
            if (Math.abs(points[i].x() - midPoint.x()) < d) {
                aux[m++] = points[i];
            }
        }

        // 按 y 坐标排序中间带内的点
        Arrays.sort(aux, 0, m, Point2D.Y_ORDER);

        // 检查中间带内的点对
        for (int i = 0; i < m; i++) {
            for (int j = i + 1; j < m && (aux[j].y() - aux[i].y()) < d; j++) {
                double dist = aux[i].distanceTo(aux[j]);
                if (dist < d) {
                    d = dist;
                }
            }
        }

        return d;
    }

    // 暴力计算小规模点集的最近距离
    private static double bruteForce(Point2D[] points, int lo, int hi) {
        double min = Double.POSITIVE_INFINITY;
        for (int i = lo; i <= hi; i++) {
            for (int j = i + 1; j <= hi; j++) {
                double dist = points[i].distanceTo(points[j]);
                if (dist < min) {
                    min = dist;
                }
            }
        }
        return min;
    }

    // 主程序入口
    public static void main(String[] args) {
        int N = Integer.parseInt(args[0]);
        Point2D[] points = new Point2D[N];
        Random random = new Random();

        // 生成单位正方形内的随机点
        for (int i = 0; i < N; i++) {
            double x = random.nextDouble();
            double y = random.nextDouble();
            points[i] = new Point2D(x, y);
        }

        double minDistance = closest(points);
        System.out.println("最近两点距离为：" + minDistance);
    }
}