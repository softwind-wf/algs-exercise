package exercise4.exercise4_3;

import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;

// 边
class Edge implements Comparable<Edge> {
    private final int v, w;
    private final double weight;

    public Edge(int v, int w, double weight) {
        this.v = v;
        this.w = w;
        this.weight = weight;
    }

    public double weight() { return weight; }
    public int either() { return v; }
    public int other(int vertex) {
        if (vertex == v) return w;
        else if (vertex == w) return v;
        else throw new IllegalArgumentException();
    }

    public int compareTo(Edge that) {
        return Double.compare(this.weight, that.weight);
    }

    public String toString() {
        return String.format("%d-%d %.2f", v, w, weight);
    }
}

// 带权无向图
class EdgeWeightedGraph {
    private final int V;
    private int E;
    private LinkedList<Edge>[] adj;

    public EdgeWeightedGraph(int V) {
        this.V = V;
        adj = (LinkedList<Edge>[]) new LinkedList[V];
        for (int i = 0; i < V; i++) adj[i] = new LinkedList<>();
    }

    public int V() { return V; }
    public void addEdge(Edge e) {
        int v = e.either(), w = e.other(v);
        adj[v].add(e);
        adj[w].add(e);
        E++;
    }

    public Iterable<Edge> adj(int v) { return adj[v]; }

    public Edge[] edges() {
        int m = E;
        Edge[] edges = new Edge[m];
        int index = 0;
        for (int v = 0; v < V; v++) {
            for (Edge e : adj[v]) {
                // 只添加一次每条边，避免重复（因为是无向图，每条边在两个顶点的邻接表中都存在）
                if (e.either() == v) { 
                    edges[index++] = e;
                }
            }
        }
        return edges;
    }

}

// ====================== 这里修复了！======================
class CC {
    private boolean[] marked;
    private int[] id;
    private int count;

    public CC(EdgeWeightedGraph G) {
        marked = new boolean[G.V()];
        id = new int[G.V()];
        count = 0;
        for (int v = 0; v < G.V(); v++) {
            if (!marked[v]) {
                id[v] = count;  // 先赋值
                dfs(G, v);      // 再搜索
                count++;        // 最后+1
            }
        }
    }

    private void dfs(EdgeWeightedGraph G, int v) {
        marked[v] = true;
        for (Edge e : G.adj(v)) {
            int w = e.other(v);
            if (!marked[w]) {
                id[w] = id[v];  // 子节点继承父节点分量号
                dfs(G, w);
            }
        }
    }

    public int count() { return count; }
    public int id(int v) { return id[v]; }
}
// =========================================================

// 最小优先队列
class MinPQ<Key extends Comparable<Key>> {
    private Key[] pq;
    private int N;

    public MinPQ() { pq = (Key[]) new Comparable[1024]; N = 0; }

    public boolean isEmpty() { return N == 0; }

    public void insert(Key key) {
        pq[++N] = key;
        swim(N);
    }

    public Key delMin() {
        Key min = pq[1];
        exch(1, N--);
        sink(1);
        return min;
    }

    private void swim(int k) {
        while (k > 1 && greater(k/2, k)) {
            exch(k, k/2);
            k = k/2;
        }
    }

    private void sink(int k) {
        while (2*k <= N) {
            int j = 2*k;
            if (j < N && greater(j, j+1)) j++;
            if (!greater(k, j)) break;
            exch(k, j);
            k = j;
        }
    }

    private boolean greater(int i, int j) {
        return pq[i].compareTo(pq[j]) > 0;
    }

    private void exch(int i, int j) {
        Key t = pq[i]; pq[i] = pq[j]; pq[j] = t;
    }
}

// 并查集
class UF {
    private int[] parent;
    private int[] rank;

    public UF(int n) {
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            rank[i] = 0;
        }
    }

    public int find(int p) {
        if (parent[p] != p) parent[p] = find(parent[p]);
        return parent[p];
    }

    public boolean connected(int p, int q) {
        return find(p) == find(q);
    }

    public void union(int p, int q) {
        int rootP = find(p);
        int rootQ = find(q);
        if (rootP == rootQ) return;
        if (rank[rootP] < rank[rootQ]) parent[rootP] = rootQ;
        else { parent[rootQ] = rootP; if (rank[rootP] == rank[rootQ]) rank[rootP]++; }
    }
}

// 最小生成森林
public class KruskalMSTForest {
    private List<Queue<Edge>> mstList;
    private List<Double> weightList;
    private CC cc;

    public KruskalMSTForest(EdgeWeightedGraph G) {
        cc = new CC(G);
        mstList = new ArrayList<>();
        weightList = new ArrayList<>();

        for (int i = 0; i < cc.count(); i++) {
            mstList.add(new LinkedList<>());
            weightList.add(0.0);
        }

        MinPQ<Edge> pq = new MinPQ<>();
        for (int v = 0; v < G.V(); v++) {
            for (Edge e : G.adj(v)) {
                if (e.either() > e.other(e.either())) continue;
                pq.insert(e);
            }
        }

        UF uf = new UF(G.V());

        while (!pq.isEmpty()) {
            Edge e = pq.delMin();
            int v = e.either();
            int w = e.other(v);

            if (uf.connected(v, w)) continue;
            uf.union(v, w);

            int c = cc.id(v);
            mstList.get(c).add(e);
            weightList.set(c, weightList.get(c) + e.weight());
        }
    }

    public List<Queue<Edge>> mstList() { return mstList; }
    public List<Double> weightList() { return weightList; }
    public CC cc() { return cc; }

    public static void main(String[] args) {
        EdgeWeightedGraph G = new EdgeWeightedGraph(6);

        // 分量 0：0,1,2
        G.addEdge(new Edge(0, 1, 1.0));
        G.addEdge(new Edge(1, 2, 2.0));
        G.addEdge(new Edge(0, 2, 3.0));

        // 分量 1：3,4,5
        G.addEdge(new Edge(3, 4, 1.0));
        G.addEdge(new Edge(4, 5, 2.0));
        G.addEdge(new Edge(3, 5, 3.0));

        KruskalMSTForest forest = new KruskalMSTForest(G);

        System.out.println("==== 最小生成森林（按连通分量分组）====\n");
        List<Queue<Edge>> mstList = forest.mstList();
        List<Double> weightList = forest.weightList();
        CC cc = forest.cc();

        for (int c = 0; c < cc.count(); c++) {
            System.out.println("连通分量 " + (c+1) + " 的最小生成树：");
            for (Edge e : mstList.get(c)) {
                System.out.println("  " + e);
            }
            System.out.printf("  分量总权重：%.2f\n\n", weightList.get(c));
        }
    }
}