package test4.test4_4;

import edu.princeton.cs.algs4.Bag;
import edu.princeton.cs.algs4.In;

public class EdgeWeightedDigraph {
    private final int V;                // 顶点总数
    private int E;                      // 边的总数
    private Bag<DirectedEdge>[] adj;    // 邻接表

    // 构造一个含有V个顶点的空图
    public EdgeWeightedDigraph(int V) {
        this.V = V;
        this.E = 0;
        adj = (Bag<DirectedEdge>[]) new Bag[V];
        for (int v = 0; v < V; v++) {
            adj[v] = new Bag<DirectedEdge>();
        }
    }

    // 从输入流读取图数据（对应练习4.4.2的实现）
    public EdgeWeightedDigraph(In in) {
        this(in.readInt()); // 读取顶点数V
        int E = in.readInt(); // 读取边数E
        for (int i = 0; i < E; i++) {
            int v = in.readInt();
            int w = in.readInt();
            double weight = in.readDouble();
            addEdge(new DirectedEdge(v, w, weight));
        }
    }

    // 返回顶点总数
    public int V() {
        return V;
    }

    // 返回边的总数
    public int E() {
        return E;
    }

    // 向图中添加一条边e
    public void addEdge(DirectedEdge e) {
        adj[e.from()].add(e);
        E++;
    }

    // 返回从顶点v出发的所有边
    public Iterable<DirectedEdge> adj(int v) {
        return adj[v];
    }

    // 返回图中所有边
    public Iterable<DirectedEdge> edges() {
        Bag<DirectedEdge> bag = new Bag<DirectedEdge>();
        for (int v = 0; v < V; v++) {
            for (DirectedEdge e : adj[v]) {
                bag.add(e);
            }
        }
        return bag;
    }

    // 输出图的字符串表示
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(V).append(" vertices, ").append(E).append(" edges\n");
        for (int v = 0; v < V; v++) {
            s.append(v).append(": ");
            for (DirectedEdge e : adj[v]) {
                s.append(e).append("  ");
            }
            s.append("\n");
        }
        return s.toString();
    }

    public static void main(String[] args) {
        EdgeWeightedDigraph G = new EdgeWeightedDigraph(new In("tinyEWD.txt"));
        System.out.println(G);
    }
}