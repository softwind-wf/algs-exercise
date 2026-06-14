package test1_5;

public class CompressionWeightedQuickUnionUF {
    private int[] id;    // 父结点数组
    private int[] sz;    // 每个根节点对应分量的大小
    private int count;   // 连通分量数量

    public CompressionWeightedQuickUnionUF(int N) {
        count = N;
        id = new int[N];
        for (int i = 0; i < N; i++) id[i] = i;
        sz = new int[N];
        for (int i = 0; i < N; i++) sz[i] = 1;
    }

    public int count() { return count; }
    public boolean connected(int p, int q) { return find(p) == find(q); }

    public int find(int p) {
        // 1. 先找到根节点
        int root = p;
        while (root != id[root]) {
            root = id[root];
        }
        // 2. 路径压缩：将查找路径上的所有节点直接指向根节点
        while (p != root) {
            int next = id[p]; // 记录当前节点的父节点
            id[p] = root;      // 将当前节点直接指向根
            p = next;          // 继续处理下一个节点
        }
        return root;
    }

    public void union(int p, int q) {
        int i = find(p);  // 注意：这里的 find 已带压缩功能
        int j = find(q);
        if (i == j) return;
        // 将小树的根节点连接到大树的根节点
        if (sz[i] < sz[j]) { id[i] = j; sz[j] += sz[i]; }
        else               { id[j] = i; sz[i] += sz[j]; }
        count--;
    }
}