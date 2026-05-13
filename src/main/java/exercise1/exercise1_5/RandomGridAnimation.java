package exercise1.exercise1_5;

import edu.princeton.cs.algs4.StdDraw;
import edu.princeton.cs.algs4.StdRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class RandomGridAnimation {

    // 并查集（使用你之前的加权+路径压缩版本）
    private static class CompressionWeightedQuickUnionUF {
        private int[] id;
        private int[] sz;
        private int count;

        public CompressionWeightedQuickUnionUF(int N) {
            count = N;
            id = new int[N];
            sz = new int[N];
            for (int i = 0; i < N; i++) {
                id[i] = i;
                sz[i] = 1;
            }
        }

        public int count() { return count; }
        public boolean connected(int p, int q) { return find(p) == find(q); }

        public int find(int p) {
            int root = p;
            while (root != id[root]) root = id[root];
            while (p != root) {
                int next = id[p];
                id[p] = root;
                p = next;
            }
            return root;
        }

        public void union(int p, int q) {
            int i = find(p);
            int j = find(q);
            if (i == j) return;
            if (sz[i] < sz[j]) { id[i] = j; sz[j] += sz[i]; }
            else               { id[j] = i; sz[i] += sz[j]; }
            count--;
        }
    }

    // 连接嵌套类
    public static class Connection {
        public final int p;
        public final int q;

        public Connection(int p, int q) {
            this.p = p;
            this.q = q;
        }
    }

    // 随机 Bag：用于打乱连接顺序
    private static class RandomBag<T> {
        private ArrayList<T> list = new ArrayList<>();
        private Random random=new Random();

        public void add(T item) { list.add(item); }
        public Iterable<T> shuffle() {
            Collections.shuffle(list, random);
            return list;
        }
    }

    // 生成 N×N 随机连接
    public static Connection[] generate(int N) {
        RandomBag<Connection> bag = new RandomBag<>();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N-1; j++) {
                int p = i*N + j;
                int q = i*N + j+1;
                if (StdRandom.bernoulli()) bag.add(new Connection(p, q));
                else                       bag.add(new Connection(q, p));
            }
        }
        for (int i = 0; i < N-1; i++) {
            for (int j = 0; j < N; j++) {
                int p = i*N + j;
                int q = (i+1)*N + j;
                if (StdRandom.bernoulli()) bag.add(new Connection(p, q));
                else                       bag.add(new Connection(q, p));
            }
        }
        ArrayList<Connection> shuffled = new ArrayList<>();
        for (Connection c : bag.shuffle()) shuffled.add(c);
        return shuffled.toArray(new Connection[0]);
    }

    // 动画主方法
    public static void main(String[] args) {
        int N = Integer.parseInt(args[0]);
        int scale = 512;
        double radius = 0.4 / N;

        // 初始化绘图
        StdDraw.setCanvasSize(scale, scale);
        StdDraw.setXscale(-0.5, N-0.5);
        StdDraw.setYscale(-0.5, N-0.5);
        StdDraw.clear(StdDraw.WHITE);
        StdDraw.setPenColor(StdDraw.BLACK);
        StdDraw.setPenRadius(0.005);

        // 绘制所有节点
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                StdDraw.filledCircle(j, N-1 - i, radius);
            }
        }

        // 初始化并查集
        CompressionWeightedQuickUnionUF uf = new CompressionWeightedQuickUnionUF(N*N);
        Connection[] connections = generate(N);

        // 遍历连接并绘制动画
        StdDraw.setPenColor(StdDraw.RED);
        StdDraw.setPenRadius(0.008);
        for (Connection c : connections) {
            int p = c.p;
            int q = c.q;
            if (!uf.connected(p, q)) {
                uf.union(p, q);
                // 转换为坐标
                int i1 = p / N, j1 = p % N;
                int i2 = q / N, j2 = q % N;
                double x1 = j1, y1 = N-1 - i1;
                double x2 = j2, y2 = N-1 - i2;
                StdDraw.line(x1, y1, x2, y2);
                StdDraw.pause(100); // 控制动画速度
            }
        }
    }
}