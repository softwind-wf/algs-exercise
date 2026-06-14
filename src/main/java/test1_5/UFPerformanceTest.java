package test1_5;

public class UFPerformanceTest {
    public static void main(String[] args) {
        int size = 1_000_000;       // 100万节点
        int operations = 10_000_000; // 1000万次操作

        // 测试你的算法
        CompressionWeightedQuickUnionUF uf = new CompressionWeightedQuickUnionUF(size);

        long start = System.nanoTime();

        // 执行大量随机 union + connected 操作
        for (int i = 0; i < operations; i++) {
            int p = (int) (Math.random() * size);
            int q = (int) (Math.random() * size);
            if (uf.connected(p, q)) {
                // 啥也不做
            } else {
                uf.union(p, q);
            }
        }

        long end = System.nanoTime();
        double ms = (end - start) / 1e6;
        double opsPerMs = operations / ms;

        System.out.println("节点数量：" + size);
        System.out.println("总操作数：" + operations);
        System.out.printf("总耗时：%.2f 毫秒\n", ms);
        System.out.printf("每秒可处理：%.0f 万次操作\n", opsPerMs / 10000);
    }
}

