package exercise1.exercise1_3;

public class Steque1<Item> implements Steque<Item>{

        private Node first;
        private Node last;
        private int N;

        private class Node {
            Item item;
            Node next;
        }

        public boolean isEmpty() { return first == null; }
        public int size() { return N; }

    @Override
    public void pushLeft(Item item) {
        Node oldfirst = first;
        first = new Node();
        first.item = item;
        first.next = oldfirst;
        if (oldfirst == null) last = first;
        N++;
    }

    @Override
    public void pushRight(Item item) {
        Node newnode = new Node();
        newnode.item = item;
        newnode.next = null;
        if (isEmpty()) {
            first = newnode;
            last = newnode;
        } else {
            last.next = newnode;
            last = newnode;
        }
        N++;
    }

    @Override
    public Item popLeft() {
        if (isEmpty()) return null;
        Item item = first.item;
        first = first.next;
        if (isEmpty()) last = null;
        N--;
        return item;
    }

    // 头部入栈
        public void push(Item item) {
            Node oldfirst = first;
            first = new Node();
            first.item = item;
            first.next = oldfirst;
            if (oldfirst == null) last = first;
            N++;
        }

        // 尾部入队
        public void enqueue(Item item) {
            Node oldlast = last;
            last = new Node();
            last.item = item;
            last.next = null;
            if (isEmpty()) first = last;
            else oldlast.next = last;
            N++;
        }

        public Item pop() {
            Item item = first.item;
            first = first.next;
            if (isEmpty()) last = null;
            N--;
            return item;
        }

        // catenation 操作：破坏性地将 st2 连接到 this 之后
        public void catenation(Steque1<Item> st2) {
            if (st2.isEmpty()) return;
            if (this.isEmpty()) {
                this.first = st2.first;
                this.last = st2.last;
            } else {
                this.last.next = st2.first;
                this.last = st2.last;
            }
            this.N += st2.N;
            // 清空 st2，实现破坏性
            st2.first = null;
            st2.last = null;
            st2.N = 0;
        }




}
