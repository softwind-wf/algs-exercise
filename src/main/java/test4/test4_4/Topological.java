package test4.test4_4;

import java.util.Stack;

public class Topological {
    private boolean[] marked;
    private Stack<Integer> order;

    public Topological(EdgeWeightedDigraph G) {
        marked = new boolean[G.V()];
        order = new Stack<>();
        for (int v = 0; v < G.V(); v++) {
            if (!marked[v]) {
                dfs(G, v);
            }
        }
    }

    private void dfs(EdgeWeightedDigraph G, int v) {
        marked[v] = true;
        for (DirectedEdge e : G.adj(v)) {
            int w = e.to();
            if (!marked[w]) {
                dfs(G, w);
            }
        }
        order.push(v);
    }

    public Iterable<Integer> order() {
        return order;
    }
}