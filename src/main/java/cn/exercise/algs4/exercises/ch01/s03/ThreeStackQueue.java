package cn.exercise.algs4.exercises.ch01.s03;

import java.util.Stack;

public class ThreeStackQueue<Item> {
    private Stack<Item> inStack = new Stack<>();
    private Stack<Item> outStack = new Stack<>();
    private Stack<Item> bufStack = new Stack<>();
    private Stack<Item> tempStack = new Stack<>(); // 用于存储入队的新元素

    // 队列状态：是否正在进行元素迁移
    private boolean isShuffling = false;

    // 迁移剩余步数
    private int shuffleSteps = 0;

    // 入队操作
    public synchronized void enqueue(Item item) {
        if (isShuffling) {
            // 在 shuffling 期间，将新元素暂时存入 tempStack
            tempStack.push(item);
        } else {
            inStack.push(item);
        }
    }

    // 出队操作
    public synchronized Item dequeue() {
        if (isEmpty()) {
            throw new RuntimeException("Queue underflow");
        }

        handleShuffling();

        // 如果 outStack 为空，启动迁移流程
        if (outStack.isEmpty()) {
            startShuffling();
        }

        // 确保 outStack 不为空
        if (!outStack.isEmpty()) {
            return outStack.pop();
        } else {
            throw new RuntimeException("Unexpected empty outStack");
        }
    }

    // 处理 shuffling 逻辑
    private void handleShuffling() {
        if (isShuffling) {
            if (!bufStack.isEmpty()) {
                inStack.push(bufStack.pop());
                shuffleSteps--;
            }
            if (shuffleSteps == 0) {
                isShuffling = false;
                mergeTempStack();
            }
        }
    }

    // 将迁移期间入队的元素按原顺序并入 inStack（保持 FIFO 顺序）
    private void mergeTempStack() {
        Stack<Item> reversed = new Stack<>();
        while (!tempStack.isEmpty()) {
            reversed.push(tempStack.pop());
        }
        while (!reversed.isEmpty()) {
            inStack.push(reversed.pop());
        }
    }

    // 启动迁移流程：将 inStack 中的元素准备好，供 outStack 使用
    private void startShuffling() {
        if (inStack.isEmpty()) {
            return;
        }

        // 1. 将 inStack 中除最底部元素外的所有元素移到 bufStack
        Item bottom = null;
        while (!inStack.isEmpty()) {
            Item current = inStack.pop();
            if (inStack.isEmpty()) {
                bottom = current;
            } else {
                bufStack.push(current);
            }
        }

        // 2. 将最底部元素（队首）移到 outStack
        outStack.push(bottom);

        // 3. 设置迁移状态：剩余需要从 buf 移回 in 的元素数量
        isShuffling = true;
        shuffleSteps = bufStack.size();
    }

    public boolean isEmpty() {
        return outStack.isEmpty() && inStack.isEmpty() && bufStack.isEmpty() && tempStack.isEmpty();
    }
}
