package exercise4.exercise4_3;

import java.util.LinkedList;
import java.util.Queue;



// Prim 算法实现「最小生成森林」
public class PrimMSTForest {
    private Queue<Edge> forest;
    private double totalWeight;

    public PrimMSTForest(EdgeWeightedGraph G) {
        forest = new LinkedList<>();
        totalWeight = 0.0;

        // 1. 先找所有连通分量
        CC cc = new CC(G);
        boolean[] processed = new boolean[cc.count()];

        // 2. 对每个连通分量跑 Prim 算法
        for (int v = 0; v < G.V(); v++) {
            int componentId = cc.id(v);
            if (!processed[componentId]) {
                // 对这个分量跑 Prim，收集 MST 边
                prim(G, v);
                processed[componentId] = true;
            }
        }
    }

    // 对以 s 为起点的连通分量跑 Prim 算法
    private void prim(EdgeWeightedGraph G, int s) {
        boolean[] marked = new boolean[G.V()];
        Edge[] edgeTo = new Edge[G.V()];
        double[] distTo = new double[G.V()];
        for (int i = 0; i < G.V(); i++) distTo[i] = Double.POSITIVE_INFINITY;
        distTo[s] = 0.0;

        MinPQ<Edge> pq = new MinPQ<>();
        pq.insert(new Edge(s, s, 0.0)); // 虚拟边，启动算法

        while (!pq.isEmpty()) {
            Edge e = pq.delMin();
            int v = e.either();
            int w = e.other(v);

            // 跳过已处理的顶点
            if (marked[v] && marked[w]) continue;

            // 找到未标记的顶点
            int unmarked = marked[v] ? w : v;
            marked[unmarked] = true;
            if (edgeTo[unmarked] != null) {
                forest.add(edgeTo[unmarked]);
                totalWeight += edgeTo[unmarked].weight();
            }

            // 松弛所有邻边
            for (Edge adjEdge : G.adj(unmarked)) {
                int to = adjEdge.other(unmarked);
                if (!marked[to] && adjEdge.weight() < distTo[to]) {
                    distTo[to] = adjEdge.weight();
                    edgeTo[to] = adjEdge;
                    pq.insert(adjEdge);
                }
            }
        }
    }

    public Iterable<Edge> edges() { return forest; }
    public double totalWeight() { return totalWeight; }

    // 测试用例
    public static void main(String[] args) {
        EdgeWeightedGraph G = new EdgeWeightedGraph(6);
        // 分量1：0-1-2
        G.addEdge(new Edge(0, 1, 1.0));
        G.addEdge(new Edge(1, 2, 2.0));
        G.addEdge(new Edge(0, 2, 3.0));
        // 分量2：3-4-5
        G.addEdge(new Edge(3, 4, 1.0));
        G.addEdge(new Edge(4, 5, 2.0));
        G.addEdge(new Edge(3, 5, 3.0));

        PrimMSTForest forest = new PrimMSTForest(G);
        System.out.println("最小生成森林的边：");
        for (Edge e : forest.edges()) {
            System.out.println(e);
        }
        System.out.printf("森林总权重：%.2f\n", forest.totalWeight());
    }
}