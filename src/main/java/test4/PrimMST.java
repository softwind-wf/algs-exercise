package test4;

import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.IndexMinPQ;
import edu.princeton.cs.algs4.Queue;

public class PrimMST implements MST<Edge> {
    private Edge[] edgeTo;                // 距离树最近的边
    private double[] distTo;              // distTo[w] = edgeTo[w].weight()
    private boolean[] marked;             // 如果v在树中则为true
    private IndexMinPQ<Double> pq;        // 有效的横切边

    public PrimMST(EdgeWeightedGraph G) {
        edgeTo = new Edge[G.V()];
        distTo = new double[G.V()];
        marked = new boolean[G.V()];
        for (int v = 0; v < G.V(); v++) {
            distTo[v] = Double.POSITIVE_INFINITY;
        }
        pq = new IndexMinPQ<>(G.V());

        distTo[0] = 0.0;
        pq.insert(0, 0.0);
        while (!pq.isEmpty()) {
            visit(G, pq.delMin());
        }
    }

    private void visit(EdgeWeightedGraph G, int v) {
        marked[v] = true;
        for (Edge e : G.adj(v)) {
            int w = e.other(v);
            if (marked[w]) continue; // 跳过已在树中的顶点

            // 如果这条边比当前到w的边更短，就更新
            if (e.weight() < distTo[w]) {
                edgeTo[w] = e;
                distTo[w] = e.weight();

                if (pq.contains(w)) {
                    pq.changeKey(w, distTo[w]);
                } else {
                    pq.insert(w, distTo[w]);
                }
            }
        }
    }

    // 返回最小生成树的所有边
    public Iterable<Edge> edges() {
        Queue<Edge> mst = new Queue<>();
        for (int v = 1; v < edgeTo.length; v++) {
            mst.enqueue(edgeTo[v]);
        }
        return mst;
    }

    // 返回最小生成树的总权重
    public double weight() {
        double total = 0.0;
        for (Edge e : edges()) {
            total += e.weight();
        }
        return total;
    }

    public static void main(String[] args) {
        In in = new In("tinyEWG.txt");
        EdgeWeightedGraph G = new EdgeWeightedGraph(in);

        MST<Edge> mst = new PrimMST(G);

        System.out.println("Prim MST (即时版) edges:");
        for (Edge e : mst.edges()) {
            System.out.println(e);
        }
        System.out.printf("Total weight: %.2f\n", mst.weight());
    }
}