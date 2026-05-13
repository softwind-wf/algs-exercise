package exercise1.exercise1_4;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 单队列实现栈（每个栈操作的队列操作数为线性级别 O(n)）
 */
public class OneQueueStack<T> {
    private final Queue<T> queue;

    public OneQueueStack() {
        queue = new LinkedList<>();
    }

    /**
     * 入栈：直接加入队列尾部
     * 时间复杂度：O(1)
     */
    public void push(T item) {
        queue.offer(item);
    }

    /**
     * 出栈：将前 size-1 个元素出队再入队，使最后一个元素到队首，然后出队
     * 时间复杂度：O(n)
     */
    public T pop() {
        if (queue.isEmpty()) {
            throw new IllegalStateException("Stack is empty");
        }
        int size = queue.size();
        // 将前 size-1 个元素循环移动到队尾
        for (int i = 0; i < size - 1; i++) {
            queue.offer(queue.poll());
        }
        // 此时队首就是最后入栈的元素，出队返回
        return queue.poll();
    }

    /**
     * 取栈顶：与 pop 逻辑一致，只是最后不删除，只返回
     * 时间复杂度：O(n)
     */
    public T peek() {
        if (queue.isEmpty()) {
            throw new IllegalStateException("Stack is empty");
        }
        int size = queue.size();
        for (int i = 0; i < size - 1; i++) {
            queue.offer(queue.poll());
        }
        T top = queue.peek();
        // 把队首元素再移回队尾，保持队列结构不变
        queue.offer(queue.poll());
        return top;
    }

    /**
     * 判断栈是否为空
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * 栈大小
     */
    public int size() {
        return queue.size();
    }

    // 测试验证
    public static void main(String[] args) {
        OneQueueStack<Integer> stack = new OneQueueStack<>();

        // 入栈：1 -> 2 -> 3
        stack.push(1);
        stack.push(2);
        stack.push(3);

        // 出栈：应输出 3 2 1
        System.out.println("Pop: " + stack.pop()); // 3
        System.out.println("Peek: " + stack.peek()); // 2
        System.out.println("Pop: " + stack.pop()); // 2
        System.out.println("Pop: " + stack.pop()); // 1

        // 栈为空，抛出异常
        try {
            stack.pop();
        } catch (IllegalStateException e) {
            System.out.println("Exception caught: " + e.getMessage()); // Stack is empty
        }
    }
}