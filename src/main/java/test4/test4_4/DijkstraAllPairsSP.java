package test4.test4_4;

import java.util.Stack;

public class DijkstraAllPairsSP {
    // 每个顶点对应一个单源最短路径对象
    private DijkstraSP[] all;

    // 构造方法：预处理所有顶点对的最短路径
    public DijkstraAllPairsSP(EdgeWeightedDigraph G) {
        all = new DijkstraSP[G.V()];
        for (int v = 0; v < G.V(); v++) {
            // 对每个顶点v，计算以v为起点的所有点的最短路径
            all[v] = new DijkstraSP(G, v);
        }
    }

    // 查询从s到t的最短路径
    public Iterable<DirectedEdge> path(int s, int t) {
        return all[s].pathTo(t);
    }

    // 查询从s到t的最短路径长度
    public double dist(int s, int t) {
        return all[s].distTo(t);
    }

    // 测试主函数
    public static void main(String[] args) {
        int V = 8;
        EdgeWeightedDigraph G = new EdgeWeightedDigraph(V);

        // 构建和之前一样的测试图
        G.addEdge(new DirectedEdge(4, 5, 0.35));
        G.addEdge(new DirectedEdge(5, 4, 0.35));
        G.addEdge(new DirectedEdge(4, 7, 0.37));
        G.addEdge(new DirectedEdge(5, 7, 0.28));
        G.addEdge(new DirectedEdge(7, 5, 0.28));
        G.addEdge(new DirectedEdge(5, 1, 0.32));
        G.addEdge(new DirectedEdge(0, 4, 0.38));
        G.addEdge(new DirectedEdge(0, 2, 0.26));
        G.addEdge(new DirectedEdge(7, 3, 0.39));
        G.addEdge(new DirectedEdge(1, 3, 0.29));
        G.addEdge(new DirectedEdge(2, 7, 0.34));
        G.addEdge(new DirectedEdge(6, 2, 0.40));
        G.addEdge(new DirectedEdge(6, 0, 0.58));
        G.addEdge(new DirectedEdge(3, 6, 0.52));
        G.addEdge(new DirectedEdge(1, 2, 0.36));
        G.addEdge(new DirectedEdge(6, 4, 0.93));

        // 预处理所有点对
        DijkstraAllPairsSP apsp = new DijkstraAllPairsSP(G);

        // 测试多个点对
        int[][] testPairs = {{0, 6}, {1, 4}, {2, 5}};
        for (int[] pair : testPairs) {
            int s = pair[0], t = pair[1];
            System.out.printf("从 %d 到 %d 的最短路径长度：%.2f\n", s, t, apsp.dist(s, t));
            System.out.print("路径：");
            if (apsp.path(s, t) != null) {
                for (DirectedEdge e : apsp.path(s, t)) {
                    System.out.print(e + "  ");
                }
            } else {
                System.out.print("不存在路径");
            }
            System.out.println("\n------------------------");
        }
    }
}