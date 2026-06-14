package test4;

public class CC {
    private boolean[] marked; // 标记顶点是否已被访问
    private int[] id;        // 每个顶点所属连通分量的编号
    private int count;       // 连通分量的总数

    public CC(Graph G) {
        marked = new boolean[G.V()];
        id = new int[G.V()];
        count = 0;

        // 遍历所有顶点，未被访问过就作为新的连通分量起点
        for (int s = 0; s < G.V(); s++) {
            if (!marked[s]) {
                dfs(G, s); // 深度优先搜索，标记整个连通分量
                count++;   // 连通分量数+1
            }
        }
    }

    // 深度优先搜索，标记连通分量并分配id
    private void dfs(Graph G, int v) {
        marked[v] = true;
        id[v] = count; // 当前顶点属于第count个连通分量
        for (int w : G.adj(v)) {
            if (!marked[w]) {
                dfs(G, w);
            }
        }
    }

    // 判断v和w是否连通
    public boolean connected(int v, int w) {
        return id[v] == id[w];
    }

    // 返回连通分量的总数
    public int count() {
        return count;
    }

    // 返回顶点v所属连通分量的编号
    public int id(int v) {
        return id[v];
    }
}