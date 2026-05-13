package exercise1.exercise1_3;

public class Queue<Item> {
    private Node first;
    private Node last;
    private int N;

    private class Node {
        Item item;
        Node next;
    }

    public boolean isEmpty() { return first == null; }
    public int size() { return N; }

    public void enqueue(Item item) {
        Node oldlast = last;
        last = new Node();
        last.item = item;
        last.next = null;
        if (isEmpty()) first = last;
        else oldlast.next = last;
        N++;
    }

    public Item dequeue() {
        Item item = first.item;
        first = first.next;
        if (isEmpty()) last = null;
        N--;
        return item;
    }

    // catenation 操作：破坏性地将 q2 连接到 this 之后
    public void catenation(Queue<Item> q2) {
        if (q2.isEmpty()) return;
        if (this.isEmpty()) {
            this.first = q2.first;
            this.last = q2.last;
        } else {
            this.last.next = q2.first;
            this.last = q2.last;
        }
        this.N += q2.N;
        // 清空 q2，实现破坏性
        q2.first = null;
        q2.last = null;
        q2.N = 0;
    }
}