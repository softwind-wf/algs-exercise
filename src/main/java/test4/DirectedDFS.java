package test4;

import edu.princeton.cs.algs4.Bag;
import edu.princeton.cs.algs4.In;

import java.util.Iterator;

public class DirectedDFS {
    // 标记数组：marked[v] = true 表示从起点集合可达
    private boolean[] marked;

    /**
     * 单点可达性构造函数：从起点 s 出发，标记所有可达的顶点
     * @param G 有向图
     * @param s 起点
     */
    public DirectedDFS(Digraph G, int s) {
        marked = new boolean[G.V()];
        dfs(G, s);
    }

    /**
     * 多点可达性构造函数：从 sources 中的所有顶点出发，标记所有可达的顶点
     * @param G 有向图
     * @param sources 起点集合
     */
    public DirectedDFS(Digraph G, Iterable<Integer> sources) {
        marked = new boolean[G.V()];
        for (int s : sources) {
            if (!marked[s]) {
                dfs(G, s);
            }
        }
    }

    /**
     * 深度优先搜索：递归标记从 v 出发可达的所有顶点
     * @param G 有向图
     * @param v 当前顶点
     */
    private void dfs(Digraph G, int v) {
        // 标记当前顶点为可达
        marked[v] = true;
        // 遍历 v 的所有出边
        for (int w : G.adj(v)) {
            // 如果 w 还没被标记，递归访问
            if (!marked[w]) {
                dfs(G, w);
            }
        }
    }

    /**
     * 判断顶点 v 是否可达
     * @param v 目标顶点
     * @return true 表示可达，false 表示不可达
     */
    public boolean marked(int v) {
        return marked[v];
    }

    /**
     * 测试用例：和教材 main 方法逻辑一致
     * 命令行用法：java DirectedDFS tinyDG.txt 1 （单点）
     *            java DirectedDFS tinyDG.txt 1 2 6 （多点）
     */
    public static void main(String[] args) {
        // 1. 读取有向图（这里用之前的 Digraph 实现）
        Digraph G = new Digraph(new In("tinyDG.txt"));

        // 2. 收集所有起点（从 args[1...] 读取）
        Bag<Integer> sources = new Bag<>();
        for (int i = 0; i < args.length; i++) {
            sources.add(Integer.parseInt(args[i]));
        }

        // 3. 执行可达性分析
        DirectedDFS reachable = new DirectedDFS(G, sources);

        // 4. 打印所有可达的顶点
        for (int v = 0; v < G.V(); v++) {
            if (reachable.marked(v)) {
                System.out.print(v + " ");
            }
        }
        System.out.println();
    }
}
