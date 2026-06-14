package test4;

import java.util.*;

public class TransitiveClosureSCC {
    private final Digraph G;          // 原图
    private final int[] sccId;        // 每个顶点的 SCC 编号
    private final int sccCount;       // SCC 个数
    private final List<Integer>[] dag; // 压缩后的 DAG，邻接表

    // 两种策略
    private BitSet[] reachableFrom;   // 预计算的可达矩阵（仅小 SCC 数量时使用）
    private boolean useMatrix;        // 是否使用矩阵查询

    // 双向 BFS 用的反向图
    private List<Integer>[] dagRev;

    // 阈值，超出则改用在线搜索
    private static final int MATRIX_THRESHOLD = 10000;

    // BFS 的成员变量，复用（延迟初始化）
    private int bfsQueryId = 0;
    private int[] fMark;
    private int[] bMark;
    private Queue<Integer> bfsQueueF;
    private Queue<Integer> bfsQueueB;

    @SuppressWarnings("unchecked")
    public TransitiveClosureSCC(Digraph G) {
        this.G = G;
        int V = G.V();

        // ---------- 1. 求 SCC ----------
        sccId = new int[V];
        sccCount = tarjanSCC(G, sccId);

        // ---------- 2. 构建压缩 DAG ----------
        dag = new List[sccCount];
        dagRev = new List[sccCount];
        for (int i = 0; i < sccCount; i++) {
            dag[i] = new ArrayList<>();
            dagRev[i] = new ArrayList<>();
        }

        // 避免重复边，用 Set
        Set<Long> edgeSet = new HashSet<>();
        for (int u = 0; u < V; u++) {
            int su = sccId[u];
            for (int v : G.adj(u)) {
                int sv = sccId[v];
                if (su != sv) {
                    long key = (long) su << 32 | sv;
                    if (!edgeSet.contains(key)) {
                        edgeSet.add(key);
                        dag[su].add(sv);
                        dagRev[sv].add(su);
                    }
                }
            }
        }

        // ---------- 3. 决定策略 ----------
        if (sccCount <= MATRIX_THRESHOLD) {
            useMatrix = true;
            buildReachabilityMatrix();
        } else {
            useMatrix = false;
            reachableFrom = null; // 不占用额外大空间
        }
    }

    // ============= Tarjan SCC 算法 =============
    private int tarjanSCC(Digraph G, int[] sccId) {
        int V = G.V();
        int[] low = new int[V];
        int[] disc = new int[V];
        boolean[] onStack = new boolean[V];
        Deque<Integer> stack = new ArrayDeque<>();
        int[] time = {0};
        int[] comp = {0};
        Arrays.fill(disc, -1);

        for (int v = 0; v < V; v++) {
            if (disc[v] == -1) {
                dfsTarjan(v, G, low, disc, onStack, stack, time, comp, sccId);
            }
        }
        return comp[0];
    }

    private void dfsTarjan(int u, Digraph G, int[] low, int[] disc,
                           boolean[] onStack, Deque<Integer> stack,
                           int[] time, int[] comp, int[] sccId) {
        disc[u] = low[u] = time[0]++;
        stack.push(u);
        onStack[u] = true;

        for (int v : G.adj(u)) {
            if (disc[v] == -1) {
                dfsTarjan(v, G, low, disc, onStack, stack, time, comp, sccId);
                low[u] = Math.min(low[u], low[v]);
            } else if (onStack[v]) {
                low[u] = Math.min(low[u], disc[v]);
            }
        }

        if (low[u] == disc[u]) {
            int cur;
            do {
                cur = stack.pop();
                onStack[cur] = false;
                sccId[cur] = comp[0];
            } while (cur != u);
            comp[0]++;
        }
    }

    // ============= 构建可达矩阵（拓扑序 DP） =============
    private void buildReachabilityMatrix() {
        reachableFrom = new BitSet[sccCount];
        for (int i = 0; i < sccCount; i++) {
            reachableFrom[i] = new BitSet(sccCount);
            reachableFrom[i].set(i); // 自己可达自己
        }

        // 对 DAG 做拓扑排序
        int[] topo = topologicalSort();
        // 逆拓扑序计算可达集
        for (int i = sccCount - 1; i >= 0; i--) {
            int u = topo[i];
            for (int v : dag[u]) {
                reachableFrom[u].or(reachableFrom[v]);
            }
        }
    }

    private int[] topologicalSort() {
        int[] indeg = new int[sccCount];
        for (int u = 0; u < sccCount; u++) {
            for (int v : dag[u]) indeg[v]++;
        }
        Queue<Integer> q = new LinkedList<>();
        for (int i = 0; i < sccCount; i++) if (indeg[i] == 0) q.add(i);

        int[] order = new int[sccCount];
        int idx = 0;
        while (!q.isEmpty()) {
            int u = q.poll();
            order[idx++] = u;
            for (int v : dag[u]) {
                if (--indeg[v] == 0) q.add(v);
            }
        }
        return order;
    }

    // ============= 对外查询接口 =============
    public boolean reachable(int v, int w) {
        if (v == w) return true;
        int sv = sccId[v];
        int sw = sccId[w];
        if (sv == sw) return true;                // 同一 SCC 内直接可达

        if (useMatrix) {
            return reachableFrom[sv].get(sw);      // O(1) 位运算
        } else {
            return bfsReachable(sv, sw);           // 在线双向 BFS
        }
    }

    // ============= 双向 BFS（针对压缩 DAG） =============
    private boolean bfsReachable(int from, int to) {
        if (from == to) return true;

        bfsQueryId++;   // 时间戳，避免每轮清零
        Queue<Integer> fq = bfsQueueF;
        Queue<Integer> bq = bfsQueueB;
        fq.clear(); bq.clear();
        fq.add(from); bq.add(to);
        fMark[from] = bfsQueryId;
        bMark[to] = bfsQueryId;

        while (!fq.isEmpty() && !bq.isEmpty()) {
            if (fq.size() <= bq.size()) {
                int sz = fq.size();
                for (int i = 0; i < sz; i++) {
                    int cur = fq.poll();
                    for (int nxt : dag[cur]) {
                        if (fMark[nxt] == bfsQueryId) continue;
                        if (bMark[nxt] == bfsQueryId) return true;
                        fMark[nxt] = bfsQueryId;
                        fq.add(nxt);
                    }
                }
            } else {
                int sz = bq.size();
                for (int i = 0; i < sz; i++) {
                    int cur = bq.poll();
                    for (int pre : dagRev[cur]) {
                        if (bMark[pre] == bfsQueryId) continue;
                        if (fMark[pre] == bfsQueryId) return true;
                        bMark[pre] = bfsQueryId;
                        bq.add(pre);
                    }
                }
            }
        }
        return false;
    }
}