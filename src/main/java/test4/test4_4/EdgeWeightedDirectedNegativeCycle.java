package test4.test4_4;

import java.util.ArrayList;
import java.util.List;

// 改造后的类：检测【负权环】
public class EdgeWeightedDirectedNegativeCycle {
    private boolean[] marked;        // 已访问
    private DirectedEdge[] edgeTo;  // 路径记录
    private boolean[] onStack;      // 递归栈
    private List<DirectedEdge> cycle; // 负权环

    public EdgeWeightedDirectedNegativeCycle(EdgeWeightedDigraph G) {
        marked = new boolean[G.V()];
        edgeTo = new DirectedEdge[G.V()];
        onStack = new boolean[G.V()];

        // 对每个点 DFS，找环 + 判断是否负权
        for (int v = 0; v < G.V(); v++) {
            if (!marked[v]) dfs(G, v);
        }
    }

    // DFS 找环
    private void dfs(EdgeWeightedDigraph G, int v) {
        onStack[v] = true;
        marked[v] = true;

        for (DirectedEdge e : G.adj(v)) {
            int w = e.to();

            // 已经找到负权环，直接返回
            if (cycle != null) return;

            if (!marked[w]) {
                edgeTo[w] = e;
                dfs(G, w);
            }
            // 发现环：回溯路径构建环
            else if (onStack[w]) {
                List<DirectedEdge> candidateCycle = new ArrayList<>();
                DirectedEdge cur = e;
                while (cur.from() != w) {
                    candidateCycle.add(cur);
                    cur = edgeTo[cur.from()];
                }
                candidateCycle.add(cur);
                // 关键：检查这个环是不是【负权环】
                if (sumWeight(candidateCycle) < 0) {
                    cycle = candidateCycle;
                    return;
                }
            }
        }
        onStack[v] = false;
    }

    // 计算环的总权重
    private double sumWeight(List<DirectedEdge> cycle) {
        double sum = 0.0;
        for (DirectedEdge e : cycle) sum += e.weight();
        return sum;
    }

    // API：是否存在负权环
    public boolean hasNegativeCycle() {
        return cycle != null;
    }

    // API：返回负权环
    public Iterable<DirectedEdge> negativeCycle() {
        return cycle;
    }
}