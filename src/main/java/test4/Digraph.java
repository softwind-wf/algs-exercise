package test4;

import edu.princeton.cs.algs4.Bag;
import edu.princeton.cs.algs4.In;

import java.util.NoSuchElementException;
import java.util.Scanner;



public class Digraph {
    private final int V;          // 顶点总数
    private int E;                // 边的总数
    private Bag<Integer>[] adj;   // 邻接表 adj[v] = 所有 v→w 的 w

    // 构造一个含有 V 个顶点但没有边的有向图
    public Digraph(int V) {
        if (V < 0) throw new IllegalArgumentException("顶点数必须非负");
        this.V = V;
        this.E = 0;
        adj = (Bag<Integer>[]) new Bag[V];
        for (int v = 0; v < V; v++) {
            adj[v] = new Bag<Integer>();
        }
    }

    // 从输入流读取有向图（和教材一致）
    public Digraph(In in) {
        if (in == null) throw new IllegalArgumentException("输入流不能为空");
        try {

            this.V = in.readInt();
            if (V < 0) throw new IllegalArgumentException("顶点数必须非负");
            adj = (Bag<Integer>[]) new Bag[V];
            for (int v = 0; v < V; v++) {
                adj[v] = new Bag<Integer>();
            }
            int E = in.readInt();
            if (E < 0) throw new IllegalArgumentException("边数必须非负");
            for (int i = 0; i < E; i++) {
                int v = in.readInt();
                int w = in.readInt();
                addEdge(v, w);
            }
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("输入格式错误", e);
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

    // 向有向图添加一条边 v → w
    public void addEdge(int v, int w) {
        validateVertex(v);
        validateVertex(w);
        adj[v].add(w);
        E++;
    }

    // 返回由 v 指出的边所连接的所有顶点
    public Iterable<Integer> adj(int v) {
        validateVertex(v);
        return adj[v];
    }

    // 返回该图的反向图（所有边反转方向）
    public Digraph reverse() {
        Digraph R = new Digraph(V);
        for (int v = 0; v < V; v++) {
            for (int w : adj(v)) {
                R.addEdge(w, v);
            }
        }
        return R;
    }

    // 验证顶点是否合法
    private void validateVertex(int v) {
        if (v < 0 || v >= V) {
            throw new IllegalArgumentException("顶点 " + v + " 不在 0 和 " + (V - 1) + " 之间");
        }
    }

    // 字符串表示（可选，方便打印）
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(V).append(" 个顶点, ").append(E).append(" 条边\n");
        for (int v = 0; v < V; v++) {
            s.append(v).append(": ");
            for (int w : adj[v]) {
                s.append(w).append(" ");
            }
            s.append("\n");
        }
        return s.toString();
    }

    // 测试用例：用 tinyDG.txt 格式的输入来测试
    public static void main(String[] args) {

        Digraph G = new Digraph(new In("tinyDG.txt"));
        System.out.println("原始有向图：");
        System.out.println(G);

        System.out.println("反向图：");
        System.out.println(G.reverse());
    }
}