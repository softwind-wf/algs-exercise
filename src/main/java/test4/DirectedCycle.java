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
        for (int w : G.adj(v)) {
            if (hasCycle()) return; // 已经找到环，直接返回

            if (!marked[w]) {
                edgeTo[w] = v;
                dfs(G, w);
            } else if (onStack[w]) {
                // 找到了环：从v出发，回到了栈中的w
                cycle = new Stack<>();
                for (int x = v; x != w; x = edgeTo[x]) {
                    cycle.push(x);
                }
                cycle.push(w);
                cycle.push(v);
            }
        }
        onStack[v] = false; // 回溯，移出递归栈
    }

    // 是否存在有向环
    public boolean hasCycle() {
        return cycle != null;
    }

    // 返回有向环上的所有顶点（如果不存在则返回null）
    public Iterable<Integer> cycle() {
        return cycle;
    }

    // 测试
    public static void main(String[] args) {
        // 示例1：有环图（和书上的例子类似：0→1→2→0）
        Digraph cyclicG = new Digraph(new In("tinyDG.txt"));
        DirectedCycle cycle1 = new DirectedCycle(cyclicG);
        System.out.println("图1是否有环？" + cycle1.hasCycle());
        if (cycle1.hasCycle()) {
            System.out.print("环上的顶点：");
            for (int v : cycle1.cycle()) {
                System.out.print(v + " ");
            }
            System.out.println();
        }

        // 示例2：无环有向图（DAG）
        Digraph acyclicG = new Digraph(4);
        acyclicG.addEdge(0,1);
        acyclicG.addEdge(1,2);
        acyclicG.addEdge(2,3);
        DirectedCycle cycle2 = new DirectedCycle(acyclicG);
        System.out.println("图2是否有环？" + cycle2.hasCycle());
    }
}