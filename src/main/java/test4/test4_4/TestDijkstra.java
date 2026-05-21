package test4.test4_4;

import java.util.Stack;

public class TestDijkstra {
    public static void main(String[] args) {
        // 构建一个有向加权图
        int V = 4;
        EdgeWeightedDigraph G = new EdgeWeightedDigraph(V);

        // 添加边
        G.addEdge(new DirectedEdge(0, 1, 2));
        G.addEdge(new DirectedEdge(0, 2, 5));
        G.addEdge(new DirectedEdge(2, 1, -4));
        G.addEdge(new DirectedEdge(1, 3, 1));


        // 从起点0开始计算最短路径
        int s = 0;
        DijkstraSP sp = new DijkstraSP(G, s);

        // 输出每个顶点的最短路径
        for (int t = 0; t < G.V(); t++) {
            System.out.print(s + " to " + t);
            System.out.printf(" (%.2f): ", sp.distTo(t));
            if (sp.hasPathTo(t)) {
                for (DirectedEdge e : sp.pathTo(t)) {
                    System.out.print(e + "  ");
                }
            }
            System.out.println();
        }
    }
}