package test4;



// 无环检测类
public class Cycle {
    private boolean[] marked;
    private boolean hasCycle;

    public Cycle(Graph G) {
        marked = new boolean[G.V()];
        hasCycle = false; // 初始化标记
        for (int s = 0; s < G.V(); s++) {
            if (!marked[s]) {
                dfs(G, s, s);
            }
        }
    }

    // 深度优先搜索，v为当前节点，u为父节点
    private void dfs(Graph G, int v, int u) {
        marked[v] = true;
        for (int w : G.adj(v)) {
            if (!marked[w]) {
                dfs(G, w, v);
            }
            // 遇到已访问且不是父节点的节点，说明有环
            else if (w != u) {
                hasCycle = true;
            }
        }
    }

    public boolean hasCycle() {
        return hasCycle;
    }

    // 测试
    public static void main(String[] args) {
        // 测试无环图（树）
        Graph tree = new Graph(4);
        tree.addEdge(0, 1);
        tree.addEdge(1, 2);
        tree.addEdge(2, 3);
        Cycle cycle1 = new Cycle(tree);
        System.out.println("无环树是否有环？" + cycle1.hasCycle()); // false

        // 测试有环图
        Graph cyclicGraph = new Graph(4);
        cyclicGraph.addEdge(0, 1);
        cyclicGraph.addEdge(1, 2);
        cyclicGraph.addEdge(2, 3);
        cyclicGraph.addEdge(3, 0);
        Cycle cycle2 = new Cycle(cyclicGraph);
        System.out.println("有环图是否有环？" + cycle2.hasCycle()); // true
    }
}