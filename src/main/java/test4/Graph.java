package test4;

import edu.princeton.cs.algs4.Bag;
import edu.princeton.cs.algs4.In;

public class Graph {
    private final int V;           // 顶点数目
    private int E;                 // 边的数目
    private Bag<Integer>[] adj;    // 邻接表

    /**
     * 构造函数：创建一个含有V个顶点但不含边的图
     * @param V 顶点总数
     */
    public Graph(int V) {
        this.V = V;
        this.E = 0;
        adj = (Bag<Integer>[]) new Bag[V]; // 创建邻接表
        for (int v = 0; v < V; v++) {
            adj[v] = new Bag<Integer>();    // 将所有链表初始化为空
        }
    }

    /**
     * 构造函数：从文件中读取一幅图
     * 文件格式：
     * 第一行：V（顶点数）
     * 第二行：E（边数）
     * 接下来E行：每行两个整数，表示一条边
     */
    public Graph(In in) {
        // 读取V并初始化图
        this(in.readInt());


        // 读取E
        int E = in.readInt();
        // 读取E条边
        for (int i = 0; i < E; i++) {
            int v = in.readInt(); // 读取一个顶点
            int w = in.readInt(); // 读取另一个顶点
            addEdge(v, w);        // 添加一条连接它们的边
        }
        in.close();
    }

    /**
     * 返回顶点数
     * @return 顶点数V
     */
    public int V() {
        return V;
    }

    /**
     * 返回边数
     * @return 边数E
     */
    public int E() {
        return E;
    }

    /**
     * 向图中添加一条边v-w
     * @param v 顶点v
     * @param w 顶点w
     */
    public void addEdge(int v, int w) {
        adj[v].add(w);   // 将w添加到v的链表中
        adj[w].add(v);   // 将v添加到w的链表中
        E++;
    }

    /**
     * 返回和v相邻的所有顶点
     * @param v 顶点v
     * @return 可迭代的相邻顶点集合
     */
    public Iterable<Integer> adj(int v) {
        return adj[v];
    }

    // ------------------- 以下是书上的常用工具方法 -------------------

    /**
     * 计算顶点v的度数
     * @param G 图
     * @param v 顶点v
     * @return v的度数
     */
    public static int degree(Graph G, int v) {
        int degree = 0;
        for (int w : G.adj(v)) {
            degree++;
        }
        return degree;
    }

    /**
     * 计算所有顶点的最大度数
     * @param G 图
     * @return 最大度数
     */
    public static int maxDegree(Graph G) {
        int max = 0;
        for (int v = 0; v < G.V(); v++) {
            if (degree(G, v) > max) {
                max = degree(G, v);
            }
        }
        return max;
    }

    /**
     * 计算所有顶点的平均度数
     * @param G 图
     * @return 平均度数
     */
    public static double avgDegree(Graph G) {
        return 2.0 * G.E() / G.V();
    }

    /**
     * 计算自环的个数
     * @param G 图
     * @return 自环的数量
     */
    public static int numberOfSelfLoops(Graph G) {
        int count = 0;
        for (int v = 0; v < G.V(); v++) {
            for (int w : G.adj(v)) {
                if (v == w) count++;
            }
        }
        return count / 2; // 每条边都被记过两次
    }

    /**
     * 图的邻接表的字符串表示
     * @return 图的字符串格式
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(V + " vertices, " + E + " edges\n");
        for (int v = 0; v < V; v++) {
            s.append(v).append(": ");
            for (int w : this.adj(v)) {
                s.append(w).append(" ");
            }
            s.append("\n");
        }
        return s.toString();
    }

    // ------------------- 测试用例 -------------------
    public static void main(String[] args) {
        // 替换成你的文件路径，比如 "tinyG.txt"
        String filename = "tinyG.txt";
        Graph G = new Graph(new In(filename));

        System.out.println("=== 图的信息 ===");
        System.out.println(G);

        System.out.println("顶点数: " + G.V());
        System.out.println("边数: " + G.E());
        System.out.println("最大度数: " + Graph.maxDegree(G));
        System.out.println("平均度数: " + Graph.avgDegree(G));
        System.out.println("自环数量: " + Graph.numberOfSelfLoops(G));

    }
}