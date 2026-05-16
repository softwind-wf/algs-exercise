package test4;

import edu.princeton.cs.algs4.In;

import java.util.Stack;



// 有向环检测类（和书上API完全一致）
public class DirectedCycle {
    private boolean[] marked;        // 标记已访问的顶点
    private int[] edgeTo;            // 记录路径，edgeTo[w] = v 表示 v->w
    private Stack<Integer> cycle;    // 有向环上的所有顶点（如果存在）
    private boolean[] onStack;       // 标记当前递归栈中的顶点

    public DirectedCycle(Digraph G) {
        onStack = new boolean[G.V()];
        edgeTo = new int[G.V()];
        marked = new boolean[G.V()];
        for (int v = 0; v < G.V(); v++) {
            if (!marked[v]) {
                dfs(G, v);
            }
        }
    }

    private void dfs(Digraph G, int v) {
        onStack[v] = true;
        marked[v] = true;

        // 遍历所有邻接节点
        for (int w : G.adj(v)) {
            if (hasCycle) return; // 已经找到环，直接返回

            if (!marked[w]) {
                dfs(G, w);
            } else if (onStack[w]) {
                // 邻接节点在当前栈中，说明有环
                hasCycle = true;
            }
        }

        // 回溯，将当前节点移出递归栈
        onStack[v] = false;
    }

    public boolean hasCycle() {
        return hasCycle;
    }

    // 测试
    public static void main(String[] args) {
        // 测试1：有环图（0->1, 1->2, 2->0）
        Digraph cyclicGraph = new Digraph(3);
        cyclicGraph.addEdge(0, 1);
        cyclicGraph.addEdge(1, 2);
        cyclicGraph.addEdge(2, 0);
        DirectedCycle cycle1 = new DirectedCycle(cyclicGraph);
        System.out.println("有环图是否存在环？" + cycle1.hasCycle()); // true

        // 测试2：无环有向图（DAG）
        Digraph acyclicGraph = new Digraph(4);
        acyclicGraph.addEdge(0, 1);
        acyclicGraph.addEdge(1, 2);
        acyclicGraph.addEdge(2, 3);
        DirectedCycle cycle2 = new DirectedCycle(acyclicGraph);
        System.out.println("无环图是否存在环？" + cycle2.hasCycle()); // false
    }
}