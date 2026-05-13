package test4;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class BreadthFirstPaths {
    private boolean[] marked; // 顶点是否被访问过（是否连通）
    private int[] edgeTo;     // 记录最短路径的前驱节点
    private final int s;      // 起点

    public BreadthFirstPaths(Graph G, int s) {
        marked = new boolean[G.V()];
        edgeTo = new int[G.V()];
        this.s = s;
        bfs(G, s); // 从起点开始BFS
    }

    // 广度优先搜索核心方法
    private void bfs(Graph G, int s) {
        Queue<Integer> queue = new LinkedList<>();
        marked[s] = true;    // 标记起点
        queue.add(s);       // 起点入队

        while (!queue.isEmpty()) {
            int v = queue.poll(); // 队首元素出队
            // 遍历所有邻接点
            for (int w : G.adj(v)) {
                if (!marked[w]) {
                    edgeTo[w] = v;       // 记录w的前驱是v（保证最短路径）
                    marked[w] = true;    // 标记已访问
                    queue.add(w);        // 邻接点入队
                }
            }
        }
    }

    // 判断是否存在从s到v的路径
    public boolean hasPathTo(int v) {
        return marked[v];
    }

    // 返回从s到v的路径（和DFS版本的pathTo代码一样）
    public Iterable<Integer> pathTo(int v) {
        if (!hasPathTo(v)) return null;
        Stack<Integer> path = new Stack<>();
        for (int x = v; x != s; x = edgeTo[x]) {
            path.push(x);
        }
        path.push(s);
        return path;
    }
}