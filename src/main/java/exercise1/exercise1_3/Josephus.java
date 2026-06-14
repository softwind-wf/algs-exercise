package exercise1.exercise1_3;

import java.util.LinkedList;
import java.util.Queue;

public class Josephus {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Josephus N M");
            return;
        }
        int N = Integer.parseInt(args[0]);
        int M = Integer.parseInt(args[1]);

        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < N; i++) {
            queue.add(i);
        }

        while (!queue.isEmpty()) {
            // 把前 M-1 个人移到队尾
            for (int i = 0; i < M - 1; i++) {
                queue.add(queue.remove());
            }
            // 第 M 个人出队并打印
            System.out.print(queue.remove() + " ");
        }
    }
}