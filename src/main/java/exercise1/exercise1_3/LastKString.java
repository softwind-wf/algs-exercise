package exercise1.exercise1_3;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class LastKString {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("请提供一个整数参数 k。");
            return;
        }

        int k = Integer.parseInt(args[0]);
        Queue<String> queue = new LinkedList<>();
        int total = 0; // 记录输入的字符串总数

        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入字符串（每行一个，输入空行结束）：");
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                break;
            }
            queue.add(line);
            total++; // 每输入一个字符串，总数加1
            if (queue.size() > k) {
                queue.remove();
            }
        }
        scanner.close();

        // 关键修正：判断总数是否 >= k
        if (total >= k) {
            System.out.println("倒数第 " + k + " 个字符串是: " + queue.peek());
        } else {
            System.out.println("输入的字符串数量不足 " + k + " 个，无法找到倒数第 " + k + " 个字符串。");
        }
    }
}