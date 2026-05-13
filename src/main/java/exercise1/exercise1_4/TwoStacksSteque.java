package exercise1.exercise1_4;

import java.util.Stack;

/**
 * 双栈实现 steque，所有操作均摊 O(1)
 */
public class TwoStacksSteque<T> {
    private final Stack<T> leftStack;  // 队首侧（enqueue 用）
    private final Stack<T> rightStack; // 栈顶侧（push/pop 用）

    public TwoStacksSteque() {
        leftStack = new Stack<>();
        rightStack = new Stack<>();
    }

    /**
     * 栈顶（队尾）入栈：O(1)
     */
    public void push(T item) {
        rightStack.push(item);
    }

    /**
     * 队首入队：O(1)
     */
    public void enqueue(T item) {
        leftStack.push(item);
    }

    /**
     * 栈顶（队尾）出栈：均摊 O(1)
     */
    public T pop() {
        if (isEmpty()) {
            throw new IllegalStateException("Steque is empty");
        }
        // 右栈空时，将左栈全部转移到右栈（逆序）
        if (rightStack.isEmpty()) {
            transferLeftToRight();
        }
        return rightStack.pop();
    }

    /**
     * 查看栈顶元素：均摊 O(1)
     */
    public T peek() {
        if (isEmpty()) {
            throw new IllegalStateException("Steque is empty");
        }
        if (rightStack.isEmpty()) {
            transferLeftToRight();
        }
        return rightStack.peek();
    }

    public boolean isEmpty() {
        return leftStack.isEmpty() && rightStack.isEmpty();
    }

    public int size() {
        return leftStack.size() + rightStack.size();
    }

    /**
     * 核心：将 leftStack 所有元素转移到 rightStack，实现顺序逆序
     * 每个元素最多被转移一次，因此 pop 操作均摊为 O(1)
     */
    private void transferLeftToRight() {
        while (!leftStack.isEmpty()) {
            rightStack.push(leftStack.pop());
        }
    }

    // 测试
    public static void main(String[] args) {
        TwoStacksSteque<Integer> steque = new TwoStacksSteque<>();

        steque.enqueue(1);  // 队首: 1
        steque.push(2);     // 队尾: 2 → 顺序: 1, 2
        steque.enqueue(0);  // 队首: 0 → 顺序: 0, 1, 2
        steque.push(3);     // 队尾: 3 → 顺序: 0, 1, 2, 3

        System.out.println("Pop: " + steque.pop());  // 3
        System.out.println("Pop: " + steque.pop());  // 2
        System.out.println("Pop: " + steque.pop());  // 1
        System.out.println("Pop: " + steque.pop());  // 0

        try {
            steque.pop();
        } catch (IllegalStateException e) {
            System.out.println("Exception: " + e.getMessage()); // Steque is empty
        }
    }
}