package exercise1.exercise1_5;

import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;

public class RandomGrid {

    // 嵌套类：封装一个连接 p-q
    public static class Connection {
        public final int p;
        public final int q;

        public Connection(int p, int q) {
            this.p = p;
            this.q = q;
        }

        @Override
        public String toString() {
            return p + " " + q;
        }
    }

    // 简化版 RandomBag：基于 ArrayList + shuffle 实现随机遍历
    private static class RandomBag<T> {
        private ArrayList<T> list = new ArrayList<>();
        private Random random = new Random();

        public void add(T item) {
            list.add(item);
        }

        // 随机打乱后返回所有元素
        public Iterable<T> shuffle() {
            Collections.shuffle(list, random);
            return list;
        }
    }

    // 生成 N×N 网格的所有随机连接
    public static Connection[] generate(int N) {
        RandomBag<Connection> bag = new RandomBag<>();
        Random random = new Random();

        // 生成所有水平连接 (i,j) ↔ (i,j+1)
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N - 1; j++) {
                int p = i * N + j;
                int q = i * N + (j + 1);
                // 随机方向
                if (random.nextBoolean()) {
                    bag.add(new Connection(p, q));
                } else {
                    bag.add(new Connection(q, p));
                }
            }
        }

        // 生成所有垂直连接 (i,j) ↔ (i+1,j)
        for (int i = 0; i < N - 1; i++) {
            for (int j = 0; j < N; j++) {
                int p = i * N + j;
                int q = (i + 1) * N + j;
                // 随机方向
                if (random.nextBoolean()) {
                    bag.add(new Connection(p, q));
                } else {
                    bag.add(new Connection(q, p));
                }
            }
        }

        // 转换为数组并返回
        ArrayList<Connection> shuffled = new ArrayList<>();
        for (Connection c : bag.shuffle()) {
            shuffled.add(c);
        }
        return shuffled.toArray(new Connection[0]);
    }

    // 主方法：从命令行读取 N 并输出所有连接
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java RandomGrid <N>");
            System.exit(1);
        }
        int N = Integer.parseInt(args[0]);
        Connection[] connections = generate(N);
        for (Connection c : connections) {
            System.out.println(c);
        }
    }
}