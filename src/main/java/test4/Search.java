package test4;

import edu.princeton.cs.algs4.In;

public class Search {
    private boolean[] marked; // 标记顶点是否与起点连通
    private int count;       // 与起点连通的顶点总数

    /**
     * 找到图G中与起点s连通的所有顶点
     * @param G 无向图
     * @param s 起点
     */
    public Search(Graph G, int s) {
        marked = new boolean[G.V()];
        dfs(G, s); // 从s开始深度优先搜索
    }

    // 深度优先搜索：标记所有与v连通的顶点
    private void dfs(Graph G, int v) {
        marked[v] = true;
        count++;
        for (int w : G.adj(v)) {
            if (!marked[w]) {
                dfs(G, w);
            }
        }
    }

    /**
     * 判断顶点v是否和起点s连通
     * @param v 目标顶点
     * @return true：连通；false：不连通
     */
    public boolean marked(int v) {
        return marked[v];
    }

    /**
     * 获取与起点s连通的顶点总数
     * @return 连通的顶点数量
     */
    public int count() {
        return count;
    }

    // 测试用例
    public static void main(String[] args) {
        // 示例：从文件读取图（用你之前的Graph类）
        try {
            Graph G = new Graph(new In("tinyG.txt"));
            int s = 0; // 起点设为0
            Search search = new Search(G, s);

            System.out.println("与起点 " + s + " 连通的顶点：");
            for (int v = 0; v < G.V(); v++) {
                if (search.marked(v)) {
                    System.out.print(v + " ");
                }
            }
            System.out.println("\n连通的顶点总数：" + search.count());

            // 检查是否所有顶点都连通
            if (search.count() != G.V()) {
                System.out.println("图不是连通的。");
            } else {
                System.out.println("图是连通的。");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}