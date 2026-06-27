package test6.test6_4;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class FordFulkerson {
    private boolean[] marked;       // BFS标记：能否从s到达v（剩余网络）
    private FlowEdge[] edgeTo;      // edgeTo[v]：s到v路径上最后一条边
    private double value;           // 当前最大流量

    public FordFulkerson(FlowNetwork G, int s, int t) {
        value = 0.0;
        // 循环：只要存在增广路就更新流量
        while (hasAugmentingPath(G, s, t)) {
            // 1. 找增广路上瓶颈容量
            double bottle = Double.POSITIVE_INFINITY;
            for (int v = t; v != s; v = edgeTo[v].other(v)) {
                bottle = Math.min(bottle, edgeTo[v].residualCapacityTo(v));
            }
            // 2. 沿增广路推送瓶颈流量
            for (int v = t; v != s; v = edgeTo[v].other(v)) {
                edgeTo[v].addResidualFlowTo(v, bottle);
            }
            value += bottle;
        }
    }

    // BFS 在剩余网络查找 s -> t 增广路（图片核心代码）
    private boolean hasAugmentingPath(FlowNetwork G, int s, int t) {
        marked = new boolean[G.V()];
        edgeTo = new FlowEdge[G.V()];
        Queue<Integer> q = new LinkedList<>();

        marked[s] = true;
        q.offer(s);

        while (!q.isEmpty()) {
            int v = q.poll();
            for (FlowEdge e : G.adj(v)) {
                int w = e.other(v);
                // 剩余容量>0 且未访问，说明存在可走的剩余边
                if (e.residualCapacityTo(w) > 0 && !marked[w]) {
                    edgeTo[w] = e;
                    marked[w] = true;
                    q.offer(w);
                }
            }
        }
        // t被标记 = 存在增广路
        return marked[t];
    }

    // 返回最大流总流量
    public double value() {
        return value;
    }

    // 判断顶点v是否在s的最小割中（剩余网络可达）
    public boolean inCut(int v) {
        return marked[v];
    }

    // 主函数：书本标准测试入口
    public static void main(String[] args) {
        Scanner sc = null;
        try {
            sc = new Scanner(new File("tinyFN.txt"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        FlowNetwork G = new FlowNetwork(sc);
        int s = 0;
        int t = G.V() - 1;
        FordFulkerson maxflow = new FordFulkerson(G, s, t);

        System.out.println("Max flow from " + s + " to " + t);
        for (int v = 0; v < G.V(); v++) {
            for (FlowEdge e : G.adj(v)) {
                if (e.from() == v && e.flow() > 0) {
                    System.out.println("  " + e);
                }
            }
        }
        System.out.println("Max flow value = " + maxflow.value());
        sc.close();
    }
}