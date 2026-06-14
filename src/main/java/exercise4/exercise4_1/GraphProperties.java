package exercise4.exercise4_1;

import edu.princeton.cs.algs4.Graph;
import edu.princeton.cs.algs4.BreadthFirstPaths;
import edu.princeton.cs.algs4.In;

public class GraphProperties {
    private int[] eccentricities; // 每个顶点的离心率
    private int diameter;         // 直径
    private int radius;           // 半径
    private int center;           // 中点

    // 构造函数：预处理图G，计算所有属性
    public GraphProperties(Graph G) {
        // 第一步：检查图是否连通
        if (!isConnected(G)) {
            throw new IllegalArgumentException("图必须是连通的！");
        }

        int V = G.V();
        eccentricities = new int[V];
        diameter = 0;
        radius = Integer.MAX_VALUE;
        center = -1;

        // 第二步：对每个顶点，用BFS计算它的离心率
        for (int v = 0; v < V; v++) {
            BreadthFirstPaths bfs = new BreadthFirstPaths(G, v);
            int maxDist = 0;

            // 找到v到其他所有顶点的最短路径的最大值
            for (int w = 0; w < V; w++) {
                int dist = bfs.distTo(w);
                if (dist > maxDist) {
                    maxDist = dist;
                }
            }

            eccentricities[v] = maxDist;

            // 更新直径（所有离心率的最大值）
            if (maxDist > diameter) {
                diameter = maxDist;
            }

            // 更新半径和中点（所有离心率的最小值）
            if (maxDist < radius) {
                radius = maxDist;
                center = v;
            }
        }
    }

    // 辅助方法：检查图是否连通
    private boolean isConnected(Graph G) {
        BreadthFirstPaths bfs = new BreadthFirstPaths(G, 0);
        for (int v = 0; v < G.V(); v++) {
            if (!bfs.marked(v)) {
                return false;
            }
        }
        return true;
    }

    // 返回顶点v的离心率
    public int eccentricity(int v) {
        return eccentricities[v];
    }

    // 返回图的直径
    public int diameter() {
        return diameter;
    }

    // 返回图的半径
    public int radius() {
        return radius;
    }

    // 返回图的一个中点
    public int center() {
        return center;
    }

    // 测试用例
    public static void main(String[] args) {
        // 示例：读取图（可替换为你自己的图数据）
         Graph G = new Graph(new In("tinyG.txt"));
         GraphProperties gp = new GraphProperties(G);

         System.out.println("直径: " + gp.diameter());
         System.out.println("半径: " + gp.radius());
         System.out.println("中点: " + gp.center());
         System.out.println("顶点0的离心率: " + gp.eccentricity(0));
    }
}