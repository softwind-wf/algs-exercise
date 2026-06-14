package exercise1.exercise1_3;

public class Stack1<Item> {
    private Node first;
    private int N;

    private class Node {
        Item item;
        Node next;
    }

    public boolean isEmpty() { return first == null; }
    public int size() { return N; }

    public void push(Item item) {
        Node oldfirst = first;
        first = new Node();
        first.item = item;
        first.next = oldfirst;
        N++;
    }

    public Item pop() {
        Item item = first.item;
        first = first.next;
        N--;
        return item;
    }

    // catenation 操作：破坏性地将 s2 连接到 this 之后
    public void catenation(Stack1<Item> s2) {
        if (s2.isEmpty()) return;
        // 为了保持 s2 的弹出顺序，需要先反转 s2
        Stack1<Item> temp = new Stack1<>();
        while (!s2.isEmpty()) {
            temp.push(s2.pop());
        }
        while (!temp.isEmpty()) {
            this.push(temp.pop());
        }
    }
}