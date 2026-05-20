package test4.test4_4;

public class TestShortestPath {
    public static void main(String[] args) {
        int V = 8;
        EdgeWeightedDigraph G = new EdgeWeightedDigraph(V);

        // 构造图（和之前例子一样）
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

        int s = 0;
        int t = 6;

        // 使用两点版 Dijkstra
        DijkstraSP sp = new DijkstraSP(G, s, t);

        if (sp.hasPathTo(t)) {
            System.out.printf("从 %d 到 %d 的最短路径长度：%.2f\n", s, t, sp.distTo(t));
            System.out.print("路径：");
            for (DirectedEdge e : sp.pathTo(t)) {
                System.out.print(e + "  ");
            }
        } else {
            System.out.printf("从 %d 到 %d 没有路径\n", s, t);
        }
    }
}