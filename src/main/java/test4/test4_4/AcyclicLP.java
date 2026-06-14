package test4.test4_4;

import java.util.Stack;

// 修正后的 AcyclicLP（最长路径）
class AcyclicLP {
    private double[] distTo;          // 从源点到各顶点的最长路径长度
    private DirectedEdge[] edgeTo;    // 最长路径的最后一条边

    public AcyclicLP(EdgeWeightedDigraph G, int s) {
        distTo = new double[G.V()];
        edgeTo = new DirectedEdge[G.V()];

        // 初始化：所有距离设为 -Infinity，源点设为 0
        for (int v = 0; v < G.V(); v++)
            distTo[v] = Double.NEGATIVE_INFINITY;
        distTo[s] = 0.0;

        // 生成拓扑序（核心：确保按拓扑序松弛）
        Topological topological = new Topological(G);
        if (!topological.hasOrder())
            throw new IllegalArgumentException("Digraph is not acyclic");

        // 按拓扑序松弛边
        for (int v : topological.order()) {
            for (DirectedEdge e : G.adj(v))
                relax(e);
        }
    }

    private void relax(DirectedEdge e) {
        int v = e.from(), w = e.to();
        if (distTo[w] < distTo[v] + e.weight()) {
            distTo[w] = distTo[v] + e.weight();
            edgeTo[w] = e;
        }
    }

    public double distTo(int v) { return distTo[v]; }

    public boolean hasPathTo(int t) {
        return distTo[t]>Double.NEGATIVE_INFINITY;
    }

    public Iterable<DirectedEdge> pathTo(int t) {
        if (!hasPathTo(t)) return null;
        Stack<DirectedEdge> path = new Stack<DirectedEdge>();
        for (DirectedEdge e = edgeTo[t]; e != null; e = edgeTo[e.from()])
            path.push(e);
        return path;
    }
}