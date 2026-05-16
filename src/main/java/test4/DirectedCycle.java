package test4;


// 有向图环检测
public class DirectedCycle {
    private boolean[] marked;   // 记录是否已访问（状态2）
    private boolean[] onStack;  // 记录是否在当前递归栈（状态1）
    private boolean hasCycle;   // 是否存在环

    public DirectedCycle(Digraph G) {
        marked = new boolean[G.V()];
        onStack = new boolean[G.V()];
        hasCycle = false;

        // 对每个未访问的节点执行DFS
        for (int v = 0; v < G.V(); v++) {
            if (!marked[v]) {
                dfs(G, v);
            }
        }
    }

    private void dfs(Digraph G, int v) {
        // 标记为正在访问
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