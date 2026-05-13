package exercise1.exercise1_3;

import java.util.Scanner;

public class MoveToFront {
    private Node first; // 链表首结点

    // 链表结点内部类
    private class Node {
        char item;
        Node next;
    }

    // 检查字符是否存在于链表中，如果存在则删除并返回true
    private boolean findAndDelete(char c) {
        if (first == null) return false;

        // 检查首结点
        if (first.item == c) {
            first = first.next;
            return true;
        }

        // 遍历查找并删除
        Node current = first;
        while (current.next != null) {
            if (current.next.item == c) {
                current.next = current.next.next; // 删除找到的结点
                return true;
            }
            current = current.next;
        }
        return false;
    }

    // 将字符插入到链表头部
    private void insertAtFront(char c) {
        Node oldFirst = first;
        first = new Node();
        first.item = c;
        first.next = oldFirst;
    }

    // 处理输入字符的主方法
    public void processInput() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入一串字符（输入结束后按回车）：");
        String input = scanner.nextLine();
        scanner.close();

        for (char c : input.toCharArray()) {
            // 如果字符已存在，先删除
            findAndDelete(c);
            // 再将字符插入到表头
            insertAtFront(c);
        }

        // 输出最终的链表
        System.out.println("处理后的链表（从表头到表尾）：");
        Node current = first;
        while (current != null) {
            System.out.print(current.item + " ");
            current = current.next;
        }
    }

    public static void main(String[] args) {
        MoveToFront mtf = new MoveToFront();
        mtf.processInput();
    }
}