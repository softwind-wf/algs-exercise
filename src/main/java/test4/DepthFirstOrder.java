package test4;

import java.util.*;

public class DepthFirstOrder {
    private boolean[] marked;
    private Queue<Integer> pre;         // 所有顶点的前序排列
    private Queue<Integer> post;        // 所有顶点的后序排列
    private Stack<Integer> reversePost; // 所有顶点的逆后序排列

    public DepthFirstOrder(Digraph G) {
        pre = new LinkedList<>();
        post = new LinkedList<>();
        reversePost = new Stack<>();
        marked = new boolean[G.V()];

        for (int v = 0; v < G.V(); v++) {
            if (!marked[v]) {
                dfs(G, v);
            }
        }
    }

    private void dfs(Digraph G, int v) {
        // 前序：访问顶点时加入队列
        pre.add(v);
        marked[v] = true;

        for (int w : G.adj(v)) {
            if (!marked[w]) {
                dfs(G, w);
            }
        }

        // 后序：访问完所有邻接点后加入队列
        post.add(v);
        // 逆后序：访问完所有邻接点后压入栈（出栈时为逆序）
        reversePost.push(v);
    }

    public Iterable<Integer> pre() {
        return pre;
    }

    public Iterable<Integer> post() {
        return post;
    }

    // ✅ 这里是关键修改：返回正确的逆后序（栈顶到栈底）
    public Iterable<Integer> reversePost() {
        List<Integer> list = new ArrayList<>();
        while (!reversePost.isEmpty()) {
            list.add(reversePost.pop());
        }
        return list;
    }


    public static void main(String[] args) {
        // 构建和教材示例中结构一致的有向图
        Digraph G = new Digraph(13); // 顶点 0~12

        // 添加边（根据教材示例的结构还原）
        G.addEdge(0, 1);
        G.addEdge(0, 5);
        G.addEdge(0, 6);
        G.addEdge(2, 0);
        G.addEdge(2, 3);
        G.addEdge(3, 5);
        G.addEdge(5, 4);
        G.addEdge(6, 4);
        G.addEdge(6, 9);
        G.addEdge(7, 6);
        G.addEdge(7, 8);
        G.addEdge(8, 7);
        G.addEdge(8, 9);
        G.addEdge(9, 10);
        G.addEdge(9, 11);
        G.addEdge(9, 12);
        G.addEdge(10, 12);
        G.addEdge(11, 12);
        G.addEdge(12, 9);

        // 计算深度优先排序
        DepthFirstOrder dfo = new DepthFirstOrder(G);

        // 打印三种排序结果
        System.out.println("前序 (pre):");
        for (int v : dfo.pre()) {
            System.out.print(v + " ");
        }
        System.out.println("\n");

        System.out.println("后序 (post):");
        for (int v : dfo.post()) {
            System.out.print(v + " ");
        }
        System.out.println("\n");

        System.out.println("逆后序 (reversePost):");
        for (int v : dfo.reversePost()) {
            System.out.print(v + " ");
        }
        System.out.println();
    }
}