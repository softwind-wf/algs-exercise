package test4;

import java.util.*;

/**
 * 使用双向BFS实现高效的有向图可达性查询
 * 支持多次查询，使用时间戳优化避免重复初始化
 *
 * 时间复杂度：
 * - 预处理：O(V + E)
 * - 单次查询：平均 O(√(V+E))，最坏 O(V+E)
 *
 * 空间复杂度：O(V + E)
 */

public class FastReachability {
    private final int V;
    private final List<Integer>[] adj;   // 正向图
    private final List<Integer>[] radj;  // 反向图

    // 时间戳标记，避免每轮重置数组
    private int[] forwardMark;   // 被哪次查询标记（存查询ID）
    private int[] backwardMark;
    private int queryID = 0;     // 每次查询递增，用作时间戳

    // 双向 BFS 的队列可以复用（也可以每次 new）
    private Queue<Integer> fq = new LinkedList<>();
    private Queue<Integer> bq = new LinkedList<>();

    @SuppressWarnings("unchecked")
    public FastReachability(Digraph G) {
        this.V = G.V();
        adj = (List<Integer>[]) new List[V];
        radj = (List<Integer>[]) new List[V];
        for (int v = 0; v < V; v++) {
            adj[v] = new ArrayList<>();
            radj[v] = new ArrayList<>();
        }
        // 拷贝图的边
        for (int v = 0; v < V; v++) {
            for (int w : G.adj(v)) {
                adj[v].add(w);
                radj[w].add(v);
            }
        }
        forwardMark = new int[V];
        backwardMark = new int[V];
    }

    // 查询 v 是否能到达 w
    public boolean reachable(int v, int w) {
        if (v < 0 || v >= V || w < 0 || w >= V) {
            throw new IllegalArgumentException("顶点必须在 0 和 " + (V-1) + " 之间");
        }
        if (v == w) return true;

        if (queryID == Integer.MAX_VALUE) {
            // 重置所有标记
            Arrays.fill(forwardMark, 0);
            Arrays.fill(backwardMark, 0);
            queryID = 0;
        }

        queryID++;  // 新一次查询，时间戳+1，等同于清空标记

        fq.clear(); bq.clear();
        fq.add(v);  bq.add(w);
        forwardMark[v] = queryID;
        backwardMark[w] = queryID;

        while (!fq.isEmpty() && !bq.isEmpty()) {
            // 总是扩展较小的队列（优化）
            if (fq.size() <= bq.size()) {
                if (expandForward()) return true;
            } else {
                if (expandBackward()) return true;
            }
        }
        return false;
    }

    private boolean expandForward() {
        int sz = fq.size();
        for (int i = 0; i < sz; i++) {
            int cur = fq.poll();
            for (int next : adj[cur]) {
                if (forwardMark[next] == queryID) continue;     // 本趟已访问
                if (backwardMark[next] == queryID) return true; // 相遇！
                forwardMark[next] = queryID;
                fq.add(next);
            }
        }
        return false;
    }

    private boolean expandBackward() {
        int sz = bq.size();
        for (int i = 0; i < sz; i++) {
            int cur = bq.poll();
            for (int prev : radj[cur]) {
                if (backwardMark[prev] == queryID) continue;
                if (forwardMark[prev] == queryID) return true;  // 相遇！
                backwardMark[prev] = queryID;
                bq.add(prev);
            }
        }
        return false;
    }
}