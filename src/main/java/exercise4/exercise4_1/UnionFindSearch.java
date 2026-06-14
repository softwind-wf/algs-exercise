package exercise4.exercise4_1;

import edu.princeton.cs.algs4.Graph;
import edu.princeton.cs.algs4.In;

public class UnionFindSearch {
    private int[] parent;   // 并查集父节点数组
    private int[] size;    // 每个树的大小（加权优化）
    private int[] componentSize; // 每个连通分量的大小
    private int s;          // 起点
    private Graph G;

    // 构造函数：预处理图G，建立并查集
    public UnionFindSearch(Graph G, int s) {
        this.G = G;
        this.s = s;
        int V = G.V();
        parent = new int[V];
        size = new int[V];
        componentSize = new int[V];

        // 初始化并查集
        for (int i = 0; i < V; i++) {
            parent[i] = i;
            size[i] = 1;
            componentSize[i] = 1;
        }

        // 遍历所有边，执行 union 操作
        for (int v = 0; v < V; v++) {
            for (int w : G.adj(v)) {
                if (find(v) != find(w)) {
                    union(v, w);
                }
            }
        }
    }

    // 带路径压缩的 find 操作
    private int find(int p) {
        while (p != parent[p]) {
            parent[p] = parent[parent[p]]; // 路径压缩（隔代压缩）
            p = parent[p];
        }
        return p;
    }

    // 加权 union 操作（小树挂到大树上）
    private void union(int p, int q) {
        int rootP = find(p);
        int rootQ = find(q);
        if (rootP == rootQ) return;

        if (size[rootP] < size[rootQ]) {
            parent[rootP] = rootQ;
            componentSize[rootQ] += componentSize[rootP];
            size[rootQ] += size[rootP];
        } else {
            parent[rootQ] = rootP;
            componentSize[rootP] += componentSize[rootQ];
            size[rootP] += size[rootQ];
        }
    }

    // 判断顶点v是否和起点s连通
    public boolean marked(int v) {
        return find(s) == find(v);
    }

    // 返回和起点s连通的顶点总数
    public int count() {
        return componentSize[find(s)];
    }

    // 测试用例
    public static void main(String[] args) {
        // 示例：读取图（可替换为你自己的图数据）
         Graph G = new Graph(new In("tinyG.txt"));
         int s = 0;
         UnionFindSearch search = new UnionFindSearch(G, s);

        // 示例输出
         System.out.println("和起点连通的顶点数：" + search.count());
         System.out.println("顶点是否连通：" + search.marked(9));
    }
}