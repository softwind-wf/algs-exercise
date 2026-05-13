package test4;

import edu.princeton.cs.algs4.Bag;
import edu.princeton.cs.algs4.In;

public class CCTest {
    public static void main(String[] args) {
        // 构造一个和 tinyG.txt 类似的无向图
        Graph G = new Graph(new In("tinyG.txt"));


        CC cc = new CC(G);
        int M = cc.count();
        System.out.println(M + " components");

        // 按连通分量分组输出顶点
        Bag<Integer>[] components = (Bag<Integer>[]) new Bag[M];
        for (int i = 0; i < M; i++) {
            components[i] = new Bag<>();
        }
        for (int v = 0; v < G.V(); v++) {
            components[cc.id(v)].add(v);
        }

        // 打印每个连通分量
        for (int i = 0; i < M; i++) {
            for (int v : components[i]) {
                System.out.print(v + " ");
            }
            System.out.println();
        }

        // 测试connected方法
        System.out.println("\n0和4是否连通？" + cc.connected(0, 4));
        System.out.println("0和7是否连通？" + cc.connected(0, 7));
    }
}