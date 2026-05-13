package test4;

import edu.princeton.cs.algs4.In;

import java.util.LinkedList;
import java.util.Queue;

public class SearchBFS {
    private boolean[] marked;
    private int count;

    public SearchBFS(Graph G, int s) {
        marked = new boolean[G.V()];
        bfs(G, s);
    }

    private void bfs(Graph G, int v) {
        Queue<Integer> queue = new LinkedList<>();
        marked[v] = true;
        count++;
        queue.add(v);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            for (int w : G.adj(current)) {
                if (!marked[w]) {
                    marked[w] = true;
                    count++;
                    queue.add(w);
                }
            }
        }
    }

    public boolean marked(int v) {
        return marked[v];
    }

    public int count() {
        return count;
    }

    public static void main(String[] args) {
        // 示例：从文件读取图（用你之前的Graph类）
        try {
            Graph G = new Graph(new In("tinyG.txt"));
            int s = 0; // 起点设为0
            SearchBFS search = new SearchBFS(G, s);

            System.out.println("与起点 " + s + " 连通的顶点：");
            for (int v = 0; v < G.V(); v++) {
                if (search.marked(v)) {
                    System.out.print(v + " ");
                }
            }
            System.out.println("\n连通的顶点总数：" + search.count());

            // 检查是否所有顶点都连通
            if (search.count() != G.V()) {
                System.out.println("图不是连通的。");
            } else {
                System.out.println("图是连通的。");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}