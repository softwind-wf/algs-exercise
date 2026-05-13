package exercise1.exercise1_4;

import java.util.Stack;
import java.util.EmptyStackException;

public class DequeWithThreeStacks1<T> {
    private final Stack<T> leftStack;  // 存储左端元素
    private final Stack<T> rightStack; // 存储右端元素
    private final Stack<T> bufferStack; // 迁移缓冲区

    public DequeWithThreeStacks1() {
        leftStack = new Stack<>();
        rightStack = new Stack<>();
        bufferStack = new Stack<>();
    }

    // 将 R 的一半元素迁移到 L
    private void moveHalfFromRToL() {
        int size = rightStack.size();
        int half = size / 2;

        // 1. 把 R 的后半部分倒入 Buffer
        for (int i = 0; i < half; i++) {
            bufferStack.push(rightStack.pop());
        }
        // 2. 把 Buffer 倒给 L
        while (!bufferStack.isEmpty()) {
            leftStack.push(bufferStack.pop());
        }
    }

    // 将 L 的一半元素迁移到 R
    private void moveHalfFromLToR() {
        int size = leftStack.size();
        int half = size / 2;

        // 1. 把 L 的后半部分倒入 Buffer
        for (int i = 0; i < half; i++) {
            bufferStack.push(leftStack.pop());
        }
        // 2. 把 Buffer 倒给 R
        while (!bufferStack.isEmpty()) {
            rightStack.push(bufferStack.pop());
        }
    }

    // 平衡操作：维护左右栈大小差距 <= 1
    private void balanceLeftRight() {
        if (leftStack.size() - rightStack.size() > 1) {
            moveHalfFromLToR();
        } else if (rightStack.size() - leftStack.size() > 1) {
            moveHalfFromRToL();
        }
    }

    // --- 公共 API ---
    public void addFirst(T item) {
        leftStack.push(item);
        balanceLeftRight();
    }

    public void addLast(T item) {
        rightStack.push(item);
        balanceLeftRight();
    }

    public T removeFirst() {
        if (isEmpty()) throw new EmptyStackException();
        
        // 如果左边空了，从右边迁移一半过来
        if (leftStack.isEmpty()) {
            moveHalfFromRToL();
        }
        return leftStack.pop();
    }

    public T removeLast() {
        if (isEmpty()) throw new EmptyStackException();
        
        // 如果右边空了，从左边迁移一半过来
        if (rightStack.isEmpty()) {
            moveHalfFromLToR();
        }
        return rightStack.pop();
    }
    
    public T peekFirst() {
        if (isEmpty()) throw new EmptyStackException();
        if (leftStack.isEmpty()) moveHalfFromRToL();
        return leftStack.peek();
    }
    
    public T peekLast() {
        if (isEmpty()) throw new EmptyStackException();
        if (rightStack.isEmpty()) moveHalfFromLToR();
        return rightStack.peek();
    }

    public boolean isEmpty() {
        return leftStack.isEmpty() && rightStack.isEmpty();
    }

    public int size() {
        return leftStack.size() + rightStack.size();
    }
}