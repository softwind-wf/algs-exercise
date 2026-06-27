package test6.test6_4;

import java.util.*;

public class MaxCapacityFordFulkerson {

    // 边的内部类
    static class Edge {
        int to;      // 目标顶点
        int rev;     // 反向边在邻接表中的索引
        long cap;    // 剩余容量

        Edge(int to, int rev, long cap) {
            this.to = to;
            this.rev = rev;
            this.cap = cap;
        }
    }

    // 添加一条有向边及其反向边
    private static void addEdge(List<Edge>[] graph, int from, int to, long cap) {
        Edge fwd = new Edge(to, graph[to].size(), cap);
        Edge rev = new Edge(from, graph[from].size(), 0);
        graph[from].add(fwd);
        graph[to].add(rev);
    }

    /**
     * 最大容量 Ford-Fulkerson 算法
     * @param n     顶点数 (0 ~ n-1)
     * @param edges 边列表，每条边为 [u, v, capacity]
     * @param s     源点
     * @param t     汇点
     * @return 最大流量
     */
    public static long maxFlow(int n, int[][] edges, int s, int t) {
        // 构建邻接表
        List<Edge>[] graph = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            graph[i] = new ArrayList<>();
        }
        for (int[] e : edges) {
            addEdge(graph, e[0], e[1], e[2]);
        }

        long flow = 0;
        final long INF = Long.MAX_VALUE / 4;

        while (true) {
            // 最大瓶颈值
            long[] maxBottleneck = new long[n];
            int[] prevNode = new int[n];
            int[] prevEdge = new int[n];
            Arrays.fill(prevNode, -1);
            Arrays.fill(prevEdge, -1);

            // 最大堆：存储 (瓶颈值, 节点)，使用逆序实现最大堆
            PriorityQueue<long[]> pq = new PriorityQueue<>(
                (a, b) -> Long.compare(b[0], a[0])  // 按瓶颈值降序
            );
            maxBottleneck[s] = INF;
            pq.offer(new long[]{INF, s});

            while (!pq.isEmpty()) {
                long[] cur = pq.poll();
                long bottleneck = cur[0];
                int v = (int) cur[1];

                // 如果当前值小于已记录的最大瓶颈，说明是过期数据，跳过
                if (bottleneck < maxBottleneck[v]) continue;
                if (v == t) break;  // 已经到达汇点，可以提前结束搜索

                for (int i = 0; i < graph[v].size(); i++) {
                    Edge e = graph[v].get(i);
                    if (e.cap > 0) {
                        long nb = Math.min(bottleneck, e.cap);
                        if (nb > maxBottleneck[e.to]) {
                            maxBottleneck[e.to] = nb;
                            prevNode[e.to] = v;
                            prevEdge[e.to] = i;
                            pq.offer(new long[]{nb, e.to});
                        }
                    }
                }
            }

            long bottleneck = maxBottleneck[t];
            if (bottleneck == 0) break;  // 没有增广路径

            // 沿前驱链增广
            int v = t;
            while (v != s) {
                int u = prevNode[v];
                int ei = prevEdge[v];
                Edge e = graph[u].get(ei);
                e.cap -= bottleneck;
                graph[v].get(e.rev).cap += bottleneck;
                v = u;
            }

            flow += bottleneck;
        }

        return flow;
    }

    // 测试示例
    public static void main(String[] args) {
        // 构造与之前相同的图：4个顶点，边：0->1容量10，0->2容量5，1->2容量15，1->3容量20，2->3容量10
        int[][] edges = {
            {0, 1, 10},
            {0, 2, 5},
            {1, 2, 15},
            {1, 3, 20},
            {2, 3, 10}
        };
        long maxFlow = maxFlow(4, edges, 0, 3);
        System.out.println("最大流: " + maxFlow);  // 输出应为 15
    }
}