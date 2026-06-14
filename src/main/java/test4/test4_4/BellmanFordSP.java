package test4.test4_4;

import edu.princeton.cs.algs4.In;

import java.util.*;





// 加权有向图的环检测
class EdgeWeightedDirectedCycle {
    private boolean[] marked;
    private DirectedEdge[] edgeTo;
    private Stack<DirectedEdge> cycle;
    private boolean[] onStack;

    public EdgeWeightedDirectedCycle(EdgeWeightedDigraph G) {
        marked = new boolean[G.V()];
        onStack = new boolean[G.V()];
        edgeTo = new DirectedEdge[G.V()];
        for (int v = 0; v < G.V(); v++)
            if (!marked[v]) dfs(G, v);
    }

    private void dfs(EdgeWeightedDigraph G, int v) {
        onStack[v] = true;
        marked[v] = true;
        for (DirectedEdge e : G.adj(v)) {
            int w = e.to();
            if (hasCycle()) return;
            else if (!marked[w]) {
                edgeTo[w] = e;
                dfs(G, w);
            } else if (onStack[w]) {
                cycle = new Stack<>();
                DirectedEdge f = e;
                while (f.from() != w) {
                    cycle.push(f);
                    f = edgeTo[f.from()];
                }
                cycle.push(f);
                return;
            }
        }
        onStack[v] = false;
    }

    public boolean hasCycle() { return cycle != null; }
    public Iterable<DirectedEdge> cycle() { return cycle; }
}

// Bellman-Ford 算法主类
public class BellmanFordSP {
    private double[] distTo;               // 从起点到某个顶点的路径长度
    private DirectedEdge[] edgeTo;          // 从起点到某个顶点的最后一条边
    private boolean[] onQ;                  // 该顶点是否存在于队列中
    private Queue<Integer> queue;           // 正在被放松的顶点
    private int cost;                       // relax()的调用次数
    private Iterable<DirectedEdge> cycle;   // edgeTo[]中是否有负权重环

    public BellmanFordSP(EdgeWeightedDigraph G, int s) {
        distTo = new double[G.V()];
        edgeTo = new DirectedEdge[G.V()];
        onQ = new boolean[G.V()];
        queue = new LinkedList<>();
        for (int v = 0; v < G.V(); v++)
            distTo[v] = Double.POSITIVE_INFINITY;
        distTo[s] = 0.0;
        queue.add(s);
        onQ[s] = true;
        while (!queue.isEmpty() && !hasNegativeCycle()) {
            int v = queue.poll();
            onQ[v] = false;
            relax(G, v);
        }
    }

    private void relax(EdgeWeightedDigraph G, int v) {
        for (DirectedEdge e : G.adj(v)) {
            int w = e.to();
            if (distTo[w] > distTo[v] + e.weight()) {
                distTo[w] = distTo[v] + e.weight();
                edgeTo[w] = e;
                if (!onQ[w]) {
                    queue.add(w);
                    onQ[w] = true;
                }
            }
        }
        if (cost++ % G.V() == 0)
            findNegativeCycle();
    }

    private void findNegativeCycle() {
        int V = edgeTo.length;
        EdgeWeightedDigraph spt = new EdgeWeightedDigraph(V);
        for (int v = 0; v < V; v++)
            if (edgeTo[v] != null)
                spt.addEdge(edgeTo[v]);

        EdgeWeightedDirectedCycle cf = new EdgeWeightedDirectedCycle(spt);
        cycle = cf.cycle();
    }

    public boolean hasNegativeCycle() {
        return cycle != null;
    }

    public Iterable<DirectedEdge> negativeCycle() {
        return cycle;
    }

    public double distTo(int v) {
        if (hasNegativeCycle())
            throw new UnsupportedOperationException("存在负权重环，无法计算最短路径");
        return distTo[v];
    }

    public boolean hasPathTo(int v) {
        return distTo[v] < Double.POSITIVE_INFINITY;
    }

    public Iterable<DirectedEdge> pathTo(int v) {
        if (hasNegativeCycle())
            throw new UnsupportedOperationException("存在负权重环，无法计算最短路径");
        if (!hasPathTo(v)) return null;
        Stack<DirectedEdge> path = new Stack<>();
        for (DirectedEdge e = edgeTo[v]; e != null; e = edgeTo[e.from()]) {
            path.push(e);
        }
        return path;
    }

    // 测试用例
    public static void main(String[] args) {
        //int V = 5;
        EdgeWeightedDigraph G = new EdgeWeightedDigraph(new In("tinyEWD.txt"));
//        G.addEdge(new DirectedEdge(0, 1, -1));
//        G.addEdge(new DirectedEdge(0, 2, 4));
//        G.addEdge(new DirectedEdge(1, 2, 3));
//        G.addEdge(new DirectedEdge(1, 3, 2));
//        G.addEdge(new DirectedEdge(1, 4, 2));
//        G.addEdge(new DirectedEdge(3, 1, 1));
//        G.addEdge(new DirectedEdge(3, 2, 5));
//        G.addEdge(new DirectedEdge(4, 3, -3));

        BellmanFordSP sp = new BellmanFordSP(G, 0);

        if (sp.hasNegativeCycle()) {
            System.out.println("图中存在负权重环：");
            for (DirectedEdge e : sp.negativeCycle()) {
                System.out.println(e);
            }
        } else {
            for (int v = 0; v < G.V(); v++) {
                System.out.printf("%d to %d (%.2f): ", 0, v, sp.distTo(v));
                if (sp.hasPathTo(v)) {
                    for (DirectedEdge e : sp.pathTo(v)) {
                        System.out.print(e + "  ");
                    }
                }
                System.out.println();
            }
        }
    }
}