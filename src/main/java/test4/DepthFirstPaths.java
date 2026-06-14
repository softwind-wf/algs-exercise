package test4;

import java.util.Stack;

public class DepthFirstPaths {
    private boolean[] marked; // 标记顶点是否被访问过
    private int[] edgeTo;     // 记录到达顶点的前一个顶点（树边）
    private final int s;      // 起点

    public DepthFirstPaths(Graph G, int s) {
        marked = new boolean[G.V()];
        edgeTo = new int[G.V()];
        this.s = s;
        dfs(G, s); // 从起点开始DFS
    }

    // 深度优先搜索
    private void dfs(Graph G, int v) {
        marked[v] = true;
        for (int w : G.adj(v)) {
            if (!marked[w]) {
                edgeTo[w] = v; // 记录w的前驱是v
                dfs(G, w);
            }
        }
    }

    // 判断是否存在从s到v的路径
    public boolean hasPathTo(int v) {
        return marked[v];
    }

    // 返回从s到v的路径（Iterable形式）
    public Iterable<Integer> pathTo(int v) {
        if (!hasPathTo(v)) return null;
        Stack<Integer> path = new Stack<>();
        // 从v回溯到s
        for (int x = v; x != s; x = edgeTo[x]) {
            path.push(x);
        }
        path.push(s); // 最后加上起点
        return path;
    }
}