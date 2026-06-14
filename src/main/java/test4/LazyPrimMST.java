package test4;

import edu.princeton.cs.algs4.*;

public class LazyPrimMST implements MST<Edge> {
    private boolean[] marked;        // 最小生成树的顶点
    private Queue<Edge> mst;         // 最小生成树的边
    private MinPQ<Edge> pq;          // 横切边（包括失效的边）
    private double weight;           // 最小生成树的总权重

    public LazyPrimMST(EdgeWeightedGraph G) {
        marked = new boolean[G.V()];
        mst = new Queue<>();
        pq = new MinPQ<>();
        weight = 0.0;

        visit(G, 0);
        while (!pq.isEmpty()) {
            Edge e = pq.delMin();
            int v = e.either();
            int w = e.other(v);

            // 跳过失效边（两个顶点都已经在树中）
            if (marked[v] && marked[w]) continue;

            // 将边加入最小生成树
            mst.enqueue(e);
            weight += e.weight();

            // 将未标记的顶点加入树，并更新横切边
            if (!marked[v]) visit(G, v);
            if (!marked[w]) visit(G, w);
        }
    }

    private void visit(EdgeWeightedGraph G, int v) {
        marked[v] = true;
        for (Edge e : G.adj(v)) {
            if (!marked[e.other(v)]) {
                pq.insert(e);
            }
        }
    }

    // 返回最小生成树的所有边
    public Iterable<Edge> edges() {
        return mst;
    }

    // 返回最小生成树的总权重
    public double weight() {
        return weight;
    }

    public static void main(String[] args) {
        // 从文件读取加权无向图
        In in = new In("tinyEWG.txt");
        EdgeWeightedGraph G = new EdgeWeightedGraph(in);

        // 运行LazyPrim算法
        LazyPrimMST mst = new LazyPrimMST(G);

        // 输出结果
        System.out.println("Lazy Prim MST edges:");
        for (Edge e : mst.edges()) {
            System.out.println(e);
        }
        System.out.printf("Total weight: %.2f\n", mst.weight());
    }
}