package test4;

import edu.princeton.cs.algs4.Bag;
import edu.princeton.cs.algs4.In;

public class EdgeWeightedGraph {
    private final int V;                // 顶点总数
    private int E;                      // 边的总数
    private Bag<Edge>[] adj;           // 邻接表

    // 创建一个含有V个顶点的空图
    public EdgeWeightedGraph(int V) {
        this.V = V;
        this.E = 0;
        adj = (Bag<Edge>[]) new Bag[V];
        for (int v = 0; v < V; v++)
            adj[v] = new Bag<Edge>();
    }

    // 从输入流读取图
    public EdgeWeightedGraph(In in) {
        this(in.readInt());      // 读取顶点数V
        int E = in.readInt();    // 读取边数E
        for (int i = 0; i < E; i++) {
            int v = in.readInt();
            int w = in.readInt();
            double weight = in.readDouble();
            Edge e = new Edge(v, w, weight);
            addEdge(e);
        }
    }

    // 返回顶点数
    public int V() {
        return V;
    }

    // 返回边数
    public int E() {
        return E;
    }

    // 向图中添加一条边
    public void addEdge(Edge e) {
        int v = e.either();
        int w = e.other(v);
        adj[v].add(e);
        adj[w].add(e);
        E++;
    }

    // 返回顶点v的邻接边
    public Iterable<Edge> adj(int v) {
        return adj[v];
    }

    // 返回图中所有边（不重复）
    public Iterable<Edge> edges() {
        Bag<Edge> b = new Bag<>();
        for (int v = 0; v < V; v++) {
            for (Edge e : adj[v]) {
                if (e.other(v) > v) { // 只存一次，避免重复边
                    b.add(e);
                }
            }
        }
        return b;
    }

    // 图的字符串表示
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(V).append(" vertices, ").append(E).append(" edges\n");
        for (int v = 0; v < V; v++) {
            s.append(v).append(": ");
            for (Edge e : adj[v]) {
                s.append(e).append("  ");
            }
            s.append("\n");
        }
        return s.toString();
    }

    public static void main(String[] args) {
        In in = new In("tinyEWG.txt");
        EdgeWeightedGraph G = new EdgeWeightedGraph(in);
        System.out.println(G);
        System.out.println("All edges:");
        for (Edge e : G.edges()) {
            System.out.println(e);
        }
    }
}