package exercise1.exercise1_5;

import java.util.ArrayList;

public class DynamicWeightedQuickUnionUF {
    private ArrayList<Integer> parent;   // 父结点列表
    private ArrayList<Integer> size;     // 每个根节点的分量大小
    private int count;                  // 连通分量数量
    private int nextId;                 // 下一个新节点的标识符

    // 初始化为空集合
    public DynamicWeightedQuickUnionUF() {
        parent = new ArrayList<>();
        size = new ArrayList<>();
        count = 0;
        nextId = 0;
    }

    // 新增节点：返回新节点的标识符
    public int newSite() {
        int id = nextId++;
        parent.add(id);    // 新节点父结点是自己
        size.add(1);       // 初始分量大小为 1
        count++;           // 连通分量数 +1
        return id;
    }

    // 查找根节点（带路径压缩）
    public int find(int p) {
        // 先找到根
        int root = p;
        while (root != parent.get(root)) {
            root = parent.get(root);
        }
        // 路径压缩：将路径上所有节点直接指向根
        while (p != root) {
            int next = parent.get(p);
            parent.set(p, root);
            p = next;
        }
        return root;
    }

    // 检查连通性
    public boolean connected(int p, int q) {
        return find(p) == find(q);
    }

    // 加权合并
    public void union(int p, int q) {
        int rootP = find(p);
        int rootQ = find(q);
        if (rootP == rootQ) return;

        // 小树挂到大树下
        if (size.get(rootP) < size.get(rootQ)) {
            parent.set(rootP, rootQ);
            size.set(rootQ, size.get(rootQ) + size.get(rootP));
        } else {
            parent.set(rootQ, rootP);
            size.set(rootP, size.get(rootP) + size.get(rootQ));
        }
        count--;
    }

    // 返回连通分量数量
    public int count() {
        return count;
    }

    // 测试用例
    public static void main(String[] args) {
        DynamicWeightedQuickUnionUF uf = new DynamicWeightedQuickUnionUF();

        // 动态创建节点
        int a = uf.newSite(); // 0
        int b = uf.newSite(); // 1
        int c = uf.newSite(); // 2
        int d = uf.newSite(); // 3

        System.out.println("初始连通分量数: " + uf.count()); // 4

        // 合并节点
        uf.union(a, b);
        uf.union(c, d);
        System.out.println("合并后连通分量数: " + uf.count()); // 2

        uf.union(b, c);
        System.out.println("最终连通分量数: " + uf.count()); // 1

        // 再新增节点
        int e = uf.newSite(); // 4
        System.out.println("新增节点后连通分量数: " + uf.count()); // 2
    }
}