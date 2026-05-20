package test4.test4_4;

import java.util.Stack;

public class AcyclicLP {
    private DirectedEdge[] edgeTo;   // 记录最长路径上的边
    private double[] distTo;         // 记录从起点到每个顶点的最长路径长度

    // 构造方法：计算从s出发的所有点的最长路径
    public AcyclicLP(EdgeWeightedDigraph G, int s) {
        edgeTo = new DirectedEdge[G.V()];
        distTo = new double[G.V()];

        // 初始化：距离设为负无穷（和最短路径相反）
        for (int v = 0; v < G.V(); v++) {
            distTo[v] = Double.NEGATIVE_INFINITY;
        }
        distTo[s] = 0.0;  // 起点到自己的距离为0

        // 按拓扑顺序处理顶点
        Topological top = new Topological(G);
        for (int v : top.order()) {
            relax(G, v);
        }
    }

    // 松弛操作（和最短路径方向相反，求更大值）
    private void relax(EdgeWeightedDigraph G, int v) {
        for (DirectedEdge e : G.adj(v)) {
            int w = e.to();
            // 如果经过v到w的路径比当前已知的更长，就更新
            if (distTo[w] < distTo[v] + e.weight()) {
                distTo[w] = distTo[v] + e.weight();
                edgeTo[w] = e;
            }
        }
    }

    // 查询API
    public double distTo(int v) {
        return distTo[v];
    }

    public boolean hasPathTo(int v) {
        return distTo[v] > Double.NEGATIVE_INFINITY;
    }

    public Iterable<DirectedEdge> pathTo(int v) {
        if (!hasPathTo(v)) return null;
        Stack<DirectedEdge> path = new Stack<>();
        for (DirectedEdge e = edgeTo[v]; e != null; e = edgeTo[e.from()]) {
            path.push(e);
        }
        return path;
    }
}