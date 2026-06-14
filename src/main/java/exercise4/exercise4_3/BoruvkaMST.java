package exercise4.exercise4_3;

import java.util.LinkedList;
import java.util.Queue;


// Borůvka 算法实现
public class BoruvkaMST {
    private Queue<Edge> mst;
    private double weight;

    public BoruvkaMST(EdgeWeightedGraph G) {
        mst = new LinkedList<>();
        weight = 0.0;
        UF uf = new UF(G.V());

        // 当 MST 边数不足 V-1 时继续迭代
        while (mst.size() < G.V() - 1) {
            // 维护每个连通分量的最小出边
            Edge[] minEdge = new Edge[G.V()];

            // 第一轮：遍历所有边，为每个分量找到最小边
            for (Edge e : G.edges()) {
                int v = e.either();
                int w = e.other(v);
                int rootV = uf.find(v);
                int rootW = uf.find(w);
                if (rootV == rootW) continue; // 同一分量，跳过

                // 更新 rootV 的最小边
                if (minEdge[rootV] == null || e.weight() < minEdge[rootV].weight()) {
                    minEdge[rootV] = e;
                }
                // 更新 rootW 的最小边
                if (minEdge[rootW] == null || e.weight() < minEdge[rootW].weight()) {
                    minEdge[rootW] = e;
                }
            }

            // 第二轮：把所有找到的边加入 MST
            for (int i = 0; i < G.V(); i++) {
                Edge e = minEdge[i];
                if (e == null) continue;
                int v = e.either();
                int w = e.other(v);
                if (!uf.connected(v, w)) {
                    uf.union(v, w);
                    mst.add(e);
                    weight += e.weight();
                }
            }
        }
    }

    public Iterable<Edge> edges() { return mst; }
    public double weight() { return weight; }

    // 测试用例（和之前的 tinyEWG.txt 一致）
    public static void main(String[] args) {
        EdgeWeightedGraph G = new EdgeWeightedGraph(8);
        G.addEdge(new Edge(0, 7, 0.16));
        G.addEdge(new Edge(2, 3, 0.17));
        G.addEdge(new Edge(1, 7, 0.19));
        G.addEdge(new Edge(0, 2, 0.26));
        G.addEdge(new Edge(5, 7, 0.28));
        G.addEdge(new Edge(4, 5, 0.35));
        G.addEdge(new Edge(6, 2, 0.40));

        BoruvkaMST mst = new BoruvkaMST(G);
        System.out.println("Borůvka 算法生成的 MST：");
        for (Edge e : mst.edges()) {
            System.out.println(e);
        }
        System.out.printf("总权重：%.2f\n", mst.weight());
    }
}