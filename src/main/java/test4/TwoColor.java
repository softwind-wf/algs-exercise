package test4;





// 二分图检测类
public class TwoColor {
    private boolean[] marked;
    private boolean[] color;
    private boolean isTwoColorable;

    public TwoColor(Graph G) {
        marked = new boolean[G.V()];
        color = new boolean[G.V()];
        isTwoColorable = true;
        for (int s = 0; s < G.V(); s++) {
            if (!marked[s]) {
                dfs(G, s);
            }
        }
    }

    private void dfs(Graph G, int v) {
        marked[v] = true;
        for (int w : G.adj(v)) {
            if (!marked[w]) {
                // 相邻节点染相反颜色
                color[w] = !color[v];
                dfs(G, w);
            }
            // 相邻节点颜色相同，不是二分图
            else if (color[w] == color[v]) {
                isTwoColorable = false;
            }
        }
    }

    public boolean isBipartite() {
        return isTwoColorable;
    }

    // 测试
    public static void main(String[] args) {
        // 测试二分图（树）
        Graph bipartiteTree = new Graph(4);
        bipartiteTree.addEdge(0, 1);
        bipartiteTree.addEdge(1, 2);
        bipartiteTree.addEdge(2, 3);
        TwoColor color1 = new TwoColor(bipartiteTree);
        System.out.println("树是否为二分图？" + color1.isBipartite()); // true

        // 测试非二分图（三角形）
        Graph triangle = new Graph(3);
        triangle.addEdge(0, 1);
        triangle.addEdge(1, 2);
        triangle.addEdge(2, 0);
        TwoColor color2 = new TwoColor(triangle);
        System.out.println("三角形是否为二分图？" + color2.isBipartite()); // false
    }
}