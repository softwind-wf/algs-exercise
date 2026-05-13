package exercise1.exercise1_4;

import java.util.Stack;

/**
 * 双栈实现队列（支持 amortized O(1) 的入队、出队、取队首）
 */
public class TwoStacksQueue<T> {
    // 入队栈：所有新元素压入此处
    private final Stack<T> inStack;
    // 出队栈：元素弹出时，先倒转此栈，再弹出
    private final Stack<T> outStack;

    public TwoStacksQueue() {
        inStack = new Stack<>();
        outStack = new Stack<>();
    }

    /**
     * 入队：直接压入 inStack
     * 时间复杂度：O(1)
     */
    public void enqueue(T item) {
        inStack.push(item);
    }

    /**
     * 出队：若 outStack 空，先把 inStack 倒转至 outStack，再弹出
     * 时间复杂度：amortized O(1)（最坏 O(n)，均摊后常数）
     */
    public T dequeue() {
        // 关键：outStack 为空时才倒转
        if (outStack.isEmpty()) {
            transferInToOut();
        }
        if (outStack.isEmpty()) {
            throw new IllegalStateException("Queue is empty");
        }
        return outStack.pop();
    }

    /**
     * 取队首元素：逻辑同 dequeue，仅不弹出
     */
    public T peek() {
        if (outStack.isEmpty()) {
            transferInToOut();
        }
        if (outStack.isEmpty()) {
            throw new IllegalStateException("Queue is empty");
        }
        return outStack.peek();
    }

    /**
     * 判断队列是否为空
     */
    public boolean isEmpty() {
        return inStack.isEmpty() && outStack.isEmpty();
    }

    /**
     * 队列大小
     */
    public int size() {
        return inStack.size() + outStack.size();
    }

    /**
     * 核心：将 inStack 所有元素转移到 outStack（实现逆序复原）
     */
    private void transferInToOut() {
        while (!inStack.isEmpty()) {
            outStack.push(inStack.pop());
        }
    }

    // 测试验证
    public static void main(String[] args) {
        TwoStacksQueue<Integer> queue = new TwoStacksQueue<>();

        // 入队：1 -> 2 -> 3
        queue.enqueue(1);
        queue.enqueue(2);
        queue.enqueue(3);

        // 出队：应输出 1 2 3
        System.out.println("Dequeue: " + queue.dequeue()); // 1
        System.out.println("Dequeue: " + queue.dequeue()); // 2

        // 再入队 4
        queue.enqueue(4);

        // 继续出队：应输出 3 4
        System.out.println("Dequeue: " + queue.dequeue()); // 3
        System.out.println("Dequeue: " + queue.dequeue()); // 4

        // 队列为空，抛出异常
        try {
            queue.dequeue();
        } catch (IllegalStateException e) {
            System.out.println("Exception caught: " + e.getMessage()); // Queue is empty
        }
    }
}