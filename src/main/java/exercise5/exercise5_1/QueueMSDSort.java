package exercise5.exercise5_1;

import java.util.LinkedList;
import java.util.Queue;

public class QueueMSDSort {
    // 字符集大小：这里假设是小写字母，26个；如果包含其他字符可调整
    private static final int R = 256; // 扩展为ASCII字符集更通用

    public static void sort(String[] a) {
        // 为每个字符创建一个队列（盒子）
        Queue<String>[] queues = new Queue[R];
        for (int i = 0; i < R; i++) {
            queues[i] = new LinkedList<>();
        }

        // 第一次遍历：按首字符分发到对应队列
        for (String s : a) {
            if (s.isEmpty()) {
                // 空字符串排在最前面
                queues[0].offer(s);
            } else {
                char c = s.charAt(0);
                queues[c].offer(s);
            }
        }

        // 对每个队列递归排序（去掉首字符，对剩余部分排序）
        for (int r = 0; r < R; r++) {
            if (!queues[r].isEmpty()) {
                // 递归处理当前队列里的字符串，从第2位字符开始排序
                sortQueue(queues[r], 1);
            }
        }

        // 合并所有队列，得到最终结果
        int index = 0;
        for (int r = 0; r < R; r++) {
            while (!queues[r].isEmpty()) {
                a[index++] = queues[r].poll();
            }
        }
    }

    /**
     * 递归排序单个队列里的字符串，从第d位字符开始
     * @param queue 待排序的字符串队列
     * @param d 当前处理的字符位置（从0开始）
     */
    private static void sortQueue(Queue<String> queue, int d) {
        if (queue.isEmpty()) return;

        // 为当前字符位置创建队列（盒子）
        Queue<String>[] queues = new Queue[R + 1];
        for (int i = 0; i <= R; i++) {
            queues[i] = new LinkedList<>();
        }

        // 遍历队列，按第d位字符分发
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            String s = queue.poll();
            if (s.length() <= d) {
                // 字符串已经处理完，放到最前面的队列
                queues[0].offer(s);
            } else {
                char c = s.charAt(d);
                queues[c + 1].offer(s);
            }
        }

        boolean allInZero = true;
        for (int r = 1; r <= R; r++) {
            if (!queues[r].isEmpty()) {
                allInZero = false;
                break;
            }
        }

        if (allInZero) {
            while (!queues[0].isEmpty()) {
                queue.offer(queues[0].poll());
            }
            return;
        }

        for (int r = 0; r <= R; r++) {
            if (!queues[r].isEmpty()) {
                sortQueue(queues[r], d + 1);
            }
        }

        // 合并回原队列
        for (int r = 0; r <= R; r++) {
            while (!queues[r].isEmpty()) {
                queue.offer(queues[r].poll());
            }
        }
    }

    // 测试
    public static void main(String[] args) {
        String[] test = {"she", "sells", "seashells", "by", "the", "seashore",
                         "the", "shells", "she", "sells", "are", "surely", "seashells"};
        sort(test);
        for (String s : test) {
            System.out.println(s);
        }
    }
}