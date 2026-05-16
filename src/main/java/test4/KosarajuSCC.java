package test4;

// 算法4.6 计算强连通分量的 Kosaraju 算法
public class KosarajuSCC {
    private boolean[] marked;   // 已访问过的顶点
    private int[] id;          // 强连通分量的标识符
    private int count;         // 强连通分量的数量

    public KosarajuSCC(Digraph G) {
        marked = new boolean[G.V()];
        id = new int[G.V()];
        // 第一步：对原图的反向图 G^R 进行 DFS，获取逆后序
        DepthFirstOrder order = new DepthFirstOrder(G.reverse());
        // 第二步：按照逆后序的顺序，对原图 G 进行 DFS
        for (int s : order.reversePost()) {
            if (!marked[s]) {
                dfs(G, s);
                count++;
            }
        }
    }

    private void dfs(Digraph G, int v) {
        marked[v] = true;
        id[v] = count;
        for (int w : G.adj(v)) {
            if (!marked[w]) {
                dfs(G, w);
            }
        }
    }

    // 判断两个顶点是否强连通
    public boolean stronglyConnected(int v, int w) {
        return id[v] == id[w];
    }

    // 返回顶点 v 所在的强连通分量标识符
    public int id(int v) {
        return id[v];
    }

    // 返回强连通分量的总数
    public int count() {
        return count;
    }
}