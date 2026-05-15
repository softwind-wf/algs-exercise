package exercise4.exercise4_1;

import edu.princeton.cs.algs4.Graph;
import edu.princeton.cs.algs4.StdDraw;

public class EuclideanGraph {
    private final Graph graph;          // 底层图结构
    private final double[] x;           // 每个顶点的 x 坐标
    private final double[] y;           // 每个顶点的 y 坐标
    private final int V;                // 顶点总数

    /**
     * 构造一个空的欧几里得图
     * @param V 顶点总数
     */
    public EuclideanGraph(int V) {
        this.V = V;
        this.graph = new Graph(V);
        this.x = new double[V];
        this.y = new double[V];
    }

    /**
     * 为指定顶点设置坐标
     * @param v 顶点编号
     * @param xCoord x 坐标
     * @param yCoord y 坐标
     */
    public void setCoordinate(int v, double xCoord, double yCoord) {
        validateVertex(v);
        x[v] = xCoord;
        y[v] = yCoord;
    }

    /**
     * 获取顶点的 x 坐标
     * @param v 顶点编号
     * @return x 坐标
     */
    public double x(int v) {
        validateVertex(v);
        return x[v];
    }

    /**
     * 获取顶点的 y 坐标
     * @param v 顶点编号
     * @return y 坐标
     */
    public double y(int v) {
        validateVertex(v);
        return y[v];
    }

    /**
     * 添加一条边
     * @param v 顶点1
     * @param w 顶点2
     */
    public void addEdge(int v, int w) {
        graph.addEdge(v, w);
    }

    /**
     * 获取图的顶点数
     * @return 顶点数
     */
    public int V() {
        return V;
    }

    /**
     * 获取图的边数
     * @return 边数
     */
    public int E() {
        return graph.E();
    }

    /**
     * 获取顶点的邻接表
     * @param v 顶点编号
     * @return 邻接顶点的可迭代对象
     */
    public Iterable<Integer> adj(int v) {
        return graph.adj(v);
    }

    /**
     * 用 StdDraw 绘制整个图
     * 1. 先画所有边
     * 2. 再画所有顶点（圆+数字标签）
     */
    public void show() {
        // 初始化画布，根据坐标范围自动设置坐标系
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (int v = 0; v < V; v++) {
            if (x[v] < minX) minX = x[v];
            if (x[v] > maxX) maxX = x[v];
            if (y[v] < minY) minY = y[v];
            if (y[v] > maxY) maxY = y[v];
        }

        // 给边界留一点空白
        double padding = 0.1;
        double width = maxX - minX;
        double height = maxY - minY;
        StdDraw.setXscale(minX - padding * width, maxX + padding * width);
        StdDraw.setYscale(minY - padding * height, maxY + padding * height);

        // 画边：为了避免重复画无向边，我们只在 v < w 时画一次
        StdDraw.setPenColor(StdDraw.GRAY);
        StdDraw.setPenRadius(0.005);
        for (int v = 0; v < V; v++) {
            for (int w : graph.adj(v)) {
                if (v < w) {
                    StdDraw.line(x[v], y[v], x[w], y[w]);
                }
            }
        }

        // 画顶点
        StdDraw.setPenColor(StdDraw.BLACK);
        StdDraw.setPenRadius(0.025);
        for (int v = 0; v < V; v++) {
            StdDraw.point(x[v], y[v]);
            // 在顶点旁边标上数字
            StdDraw.setPenColor(StdDraw.RED);
            StdDraw.text(x[v], y[v] + 0.03 * height, Integer.toString(v));
            StdDraw.setPenColor(StdDraw.BLACK);
        }
    }

    // 验证顶点编号是否合法
    private void validateVertex(int v) {
        if (v < 0 || v >= V) {
            throw new IllegalArgumentException("顶点编号 " + v + " 不在 0 和 " + (V-1) + " 之间");
        }
    }

    // 测试用例
    public static void main(String[] args) {
        // 构造一个简单的 5 顶点欧几里得图
        EuclideanGraph eg = new EuclideanGraph(5);

        // 设置每个顶点的坐标
        eg.setCoordinate(0, 0.1, 0.1);
        eg.setCoordinate(1, 0.9, 0.1);
        eg.setCoordinate(2, 0.9, 0.9);
        eg.setCoordinate(3, 0.1, 0.9);
        eg.setCoordinate(4, 0.5, 0.5);

        // 添加边（构成一个正方形加中心）
        eg.addEdge(0, 1);
        eg.addEdge(1, 2);
        eg.addEdge(2, 3);
        eg.addEdge(3, 0);
        eg.addEdge(4, 0);
        eg.addEdge(4, 1);
        eg.addEdge(4, 2);
        eg.addEdge(4, 3);

        // 绘制图
        eg.show();
    }
}