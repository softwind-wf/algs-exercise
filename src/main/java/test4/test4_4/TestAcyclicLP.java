package test4.test4_4;

public class TestAcyclicLP {
    public static void main(String[] args) {
        // 构建和教材里一样的无环加权有向图
        int V = 8;
        EdgeWeightedDigraph G = new EdgeWeightedDigraph(V);

        G.addEdge(new DirectedEdge(5, 4, 0.35));
        G.addEdge(new DirectedEdge(4, 7, 0.37));
        G.addEdge(new DirectedEdge(5, 7, 0.28));
        G.addEdge(new DirectedEdge(5, 1, 0.32));
        G.addEdge(new DirectedEdge(4, 0, 0.38));
        G.addEdge(new DirectedEdge(0, 2, 0.26));
        G.addEdge(new DirectedEdge(7, 3, 0.39));
        G.addEdge(new DirectedEdge(1, 3, 0.29));
        G.addEdge(new DirectedEdge(2, 7, 0.34));
        G.addEdge(new DirectedEdge(3, 6, 0.52));
        G.addEdge(new DirectedEdge(1, 2, 0.36));

        int s = 5;
        AcyclicLP lp = new AcyclicLP(G, s);

        // 输出每个顶点的最长路径
        for (int t = 0; t < G.V(); t++) {
            System.out.printf("%d to %d", s, t);
            if (lp.hasPathTo(t)) {
                System.out.printf(" (%.2f): ", lp.distTo(t));
                for (DirectedEdge e : lp.pathTo(t)) {
                    System.out.print(e + "  ");
                }
            } else {
                System.out.print(" (no path)");
            }
            System.out.println();
        }
    }
}