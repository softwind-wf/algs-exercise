package test4.test4_4;

import java.util.Stack;

// 拓扑排序实现（核心：Kahn 算法，避免递归栈溢出）
class Topological {
    private Iterable<Integer> order;  // 拓扑序
    private int[] inDegree;           // 入度

    public Topological(EdgeWeightedDigraph G) {
        inDegree = new int[G.V()];
        // 初始化入度
        for (int v = 0; v < G.V(); v++) {
            for (DirectedEdge e : G.adj(v)) {
                inDegree[e.to()]++;
            }
        }

        // Kahn 算法：用队列处理入度为 0 的节点
        java.util.Queue<Integer> queue = new java.util.LinkedList<>();
        for (int v = 0; v < G.V(); v++) {
            if (inDegree[v] == 0)
                queue.add(v);
        }

        java.util.List<Integer> orderList = new java.util.ArrayList<>();
        while (!queue.isEmpty()) {
            int v = queue.poll();
            orderList.add(v);
            for (DirectedEdge e : G.adj(v)) {
                int w = e.to();
                inDegree[w]--;
                if (inDegree[w] == 0)
                    queue.add(w);
            }
        }

        // 检查是否有环（拓扑序长度 != 顶点数）
        if (orderList.size() != G.V())
            order = null;
        else
            order = orderList;
    }

    public boolean hasOrder() { return order != null; }
    public Iterable<Integer> order() { return order; }
}