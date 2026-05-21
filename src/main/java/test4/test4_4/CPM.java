package test4.test4_4;

import edu.princeton.cs.algs4.StdIn;
import edu.princeton.cs.algs4.StdOut;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

// 主类（修正图构建逻辑）
public class CPM {
    public static void main(String[] args) {
        // 模拟输入数据
//        String data = "10\n" +
//                "41.0  1 7 9\n" +
//                "51.0  2\n" +
//                "50.0\n" +
//                "36.0\n" +
//                "38.0\n" +
//                "45.0\n" +
//                "21.0  3 8\n" +
//                "32.0  3 8\n" +
//                "32.0  2\n" +
//                "29.0  4 6";
//        System.setIn(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));

        int N = StdIn.readInt();
        StdIn.readLine(); // 读取换行符

        // 构建图：2*N+2 个节点（0~N-1：任务开始，N~2N-1：任务结束，2N：源点，2N+1：汇点）
        EdgeWeightedDigraph G = new EdgeWeightedDigraph(2 * N + 2);
        int s = 2 * N;     // 源点（所有任务的起点）
        int t = 2 * N + 1; // 汇点（所有任务的终点）

        for (int i = 0; i < N; i++) {
            String[] a = StdIn.readLine().trim().split("\\s+");
            double duration = Double.parseDouble(a[0]);

            // 1. 任务 i 的开始节点 -> 任务 i 的结束节点（权重=任务耗时）
            int startNode = i;
            int endNode = i + N;
            G.addEdge(new DirectedEdge(startNode, endNode, duration));

            // 2. 源点 -> 所有任务的开始节点（权重=0，无前置依赖）
            G.addEdge(new DirectedEdge(s, startNode, 0.0));

            // 3. 所有任务的结束节点 -> 汇点（权重=0，完成后指向终点）
            G.addEdge(new DirectedEdge(endNode, t, 0.0));

            // 4. 处理依赖：当前任务的结束节点 -> 后继任务的开始节点（权重=0）
            for (int j = 1; j < a.length; j++) {
                int successor = Integer.parseInt(a[j]); // 后继任务编号（0~N-1）
                G.addEdge(new DirectedEdge(endNode, successor, 0.0));
            }
        }

        // 计算从源点 s 出发的最长路径（CPM 核心：最长路径对应最晚完成时间）
        AcyclicLP lp = new AcyclicLP(G, s);

        // 输出结果
        StdOut.println("Start times:");
        for (int i = 0; i < N; i++)
            StdOut.printf("%4d: %5.1f\n", i, lp.distTo(i));
        StdOut.printf("Finish time: %5.1f\n", lp.distTo(t));
    }
}