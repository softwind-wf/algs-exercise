package test6.test6_4;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

public class FlowNetwork {
    private final int V;                    // 顶点总数
    private int E;                          // 边总数
    private LinkedList<FlowEdge>[] adj;     // 邻接表 adj[v]：v出发的所有流边

    // 构造1：创建含V个顶点的空流量网络
    public FlowNetwork(int V) {
        this.V = V;
        this.E = 0;
        adj = new LinkedList[V];
        for (int v = 0; v < V; v++) {
            adj[v] = new LinkedList<>();
        }
    }

    // 构造2：从输入流文件构造流量网络（对应tinyFN.txt格式）
    public FlowNetwork(Scanner in) {
        this(in.nextInt());  // 第一行读取顶点数V
        int EInput = in.nextInt(); // 第二行读取边数E
        for (int i = 0; i < EInput; i++) {
            int v = in.nextInt();
            int w = in.nextInt();
            double cap = in.nextDouble();
            FlowEdge edge = new FlowEdge(v, w, cap);
            addEdge(edge);
        }
    }

    // 返回顶点总数V
    public int V() {
        return V;
    }

    // 返回边总数E
    public int E() {
        return E;
    }

    // 向网络添加一条流边e
    public void addEdge(FlowEdge e) {
        int v = e.from();
        int w = e.to();
        adj[v].add(e);
        adj[w].add(e);
        E++;
    }

    // 返回从顶点v指出的所有边（Iterable，可for-each遍历）
    public Iterable<FlowEdge> adj(int v) {
        return adj[v];
    }

    // 返回流量网络中全部边的集合
    public Iterable<FlowEdge> edges() {
        LinkedList<FlowEdge> allEdges = new LinkedList<>();
        for (int v = 0; v < V; v++) {
            for (FlowEdge e : adj[v]) {
                allEdges.add(e);
            }
        }
        return allEdges;
    }

    // 格式化打印整个流量网络
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(V).append(" 个顶点, ").append(E).append(" 条边\n");
        for (int v = 0; v < V; v++) {
            sb.append(v).append(": ");
            for (FlowEdge e : adj[v]) {
                sb.append(e).append("  ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // 测试主方法：读取tinyFN.txt演示
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(new File("tinyFN.txt"));
            FlowNetwork fn = new FlowNetwork(scanner);
            System.out.println(fn);
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("未找到 tinyFN.txt 文件");
        }
    }
}