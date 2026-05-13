package exercise1.exercise1_1;

import edu.princeton.cs.algs4.StdIn;
import edu.princeton.cs.algs4.StdOut;
import java.util.PriorityQueue;
import java.util.Collections;

public class KthSmallest {
    public static void main(String[] args) {
        // 从命令行读取 k
        int k = Integer.parseInt(args[0]);
        
        // 创建一个最大堆（默认是最小堆，所以用 Collections.reverseOrder() 反转）
        PriorityQueue<Double> maxHeap = new PriorityQueue<>(k, Collections.reverseOrder());

        // 读取输入并维护堆
        while (!StdIn.isEmpty()) {
            double num = StdIn.readDouble();
            if (maxHeap.size() < k) {
                maxHeap.offer(num);
            } else if (num < maxHeap.peek()) {
                maxHeap.poll();
                maxHeap.offer(num);
            }
        }

        // 输出第 k 小的数
        if (maxHeap.size() == k) {
            StdOut.println("第 " + k + " 小的数是：" + maxHeap.peek());
        } else {
            StdOut.println("输入的数不足 " + k + " 个");
        }
    }
}