package exercise1.exercise1_3;

public class LinkedSteque<T> implements Steque<T> {

    // 链表节点内部类
    private static class Node<T> {
        T item;
        Node<T> next;

        Node(T item) {
            this.item = item;
            this.next = null;
        }
    }

    private Node<T> head; // 栈顶（链表头部）
    private Node<T> tail; // 队列尾（链表尾部）
    private int size;     // 元素数量

    public LinkedSteque() {
        head = null;
        tail = null;
        size = 0;
    }

    // 在栈顶（头部）添加元素
    @Override
    public void push(T item) {
        Node<T> newNode = new Node<>(item);
        if (isEmpty()) {
            head = newNode;
            tail = newNode;
        } else {
            newNode.next = head;
            head = newNode;
        }
        size++;
    }

    // 从栈顶（头部）移除并返回元素
    @Override
    public T pop() {
        if (isEmpty()) {
            throw new IllegalStateException("Steque is empty");
        }
        T item = head.item;
        head = head.next;
        size--;
        // 如果弹出后栈为空，需要同时将 tail 置空
        if (isEmpty()) {
            tail = null;
        }
        return item;
    }

    // 在队列尾部添加元素
    @Override
    public void enqueue(T item) {
        Node<T> newNode = new Node<>(item);
        if (isEmpty()) {
            head = newNode;
            tail = newNode;
        } else {
            tail.next = newNode;
            tail = newNode;
        }
        size++;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void pushLeft(T item) {
        // pushLeft 等同于在栈顶添加元素，即调用 push
        push(item);
    }

    @Override
    public void pushRight(T item) {
        // pushRight 等同于在队列尾部添加元素，即调用 enqueue
        enqueue(item);
    }

    @Override
    public T popLeft() {
        // popLeft 等同于从栈顶移除并返回元素，即调用 pop
        return pop();
    }
}