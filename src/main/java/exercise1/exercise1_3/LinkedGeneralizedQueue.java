package exercise1.exercise1_3;

import java.util.NoSuchElementException;

public class LinkedGeneralizedQueue<Item> {
    private Node first; // 最早插入的元素（队头）
    private Node last;  // 最后插入的元素（队尾）
    private int n;      // 队列中的元素数量

    private class Node {
        Item item;
        Node next;
    }

    public LinkedGeneralizedQueue() {
        first = null;
        last = null;
        n = 0;
    }

    public boolean isEmpty() {
        return n == 0;
    }

    public void insert(Item x) {
        Node oldlast = last;
        last = new Node();
        last.item = x;
        last.next = null;
        if (isEmpty()) first = last;
        else oldlast.next = last;
        n++;
    }

    public Item delete(int k) {
        if (isEmpty()) throw new NoSuchElementException("Queue underflow");
        if (k < 1 || k > n) throw new IllegalArgumentException("Invalid k");

        Node current = first;
        Node prev = null;

        // 找到第k个结点
        for (int i = 1; i < k; i++) {
            prev = current;
            current = current.next;
        }

        Item item = current.item;

        // 删除结点
        if (prev == null) {
            // 删除的是第一个结点
            first = first.next;
        } else {
            prev.next = current.next;
        }

        // 如果删除的是最后一个结点，更新last
        if (current == last) {
            last = prev;
        }

        n--;
        return item;
    }
}