package test4;

import edu.princeton.cs.algs4.MinPQ;
import edu.princeton.cs.algs4.UF;

import java.util.LinkedList;
import java.util.Queue;

// Kruskal 算法主类
public class KruskalMST {
    private Queue<Edge> mst;
    private double weight; // 最小生成树的总权重

    public KruskalMST(EdgeWeightedGraph G) {
        mst = new LinkedList<>();
        MinPQ<Edge> pq = new MinPQ<>();
        for (Edge e : G.edges()) {
            pq.insert(e);
        }
        UF uf = new UF(G.V());

        while (!pq.isEmpty() && mst.size() < G.V() - 1) {
            Edge e = pq.delMin();
            int v = e.either();
            int w = e.other(v);
            if (uf.connected(v, w)) {
                continue;
            }
            uf.union(v, w);
            mst.add(e); // LinkedList 实现了 Queue 接口
            weight += e.weight();
        }
    }

    public Iterable<Edge> edges() {
        return mst;
    }

    public double weight() {
        return weight;
    }

    // 测试用例（对应 tinyEWG.txt）
    public static void main(String[] args) {
        EdgeWeightedGraph G = new EdgeWeightedGraph(8);
        G.addEdge(new Edge(0, 7, 0.16));
        G.addEdge(new Edge(2, 3, 0.17));
        G.addEdge(new Edge(1, 7, 0.19));
        G.addEdge(new Edge(0, 2, 0.26));
        G.addEdge(new Edge(5, 7, 0.28));
        G.addEdge(new Edge(4, 5, 0.35));
        G.addEdge(new Edge(6, 2, 0.40));

        KruskalMST mst = new KruskalMST(G);
        System.out.println("最小生成树的边：");
        for (Edge e : mst.edges()) {
            System.out.println(e);
        }
        System.out.printf("总权重：%.2f\n", mst.weight());
    }
}