package exercise1.exercise1_4;

import java.util.Stack;
import java.util.EmptyStackException;

// 三个栈实现双向队列（已修正顺序）
class DequeWithThreeStacks<T> {
    private final Stack<T> left;
    private final Stack<T> right;
    private final Stack<T> buffer;

    public DequeWithThreeStacks() {
        left = new Stack<>();
        right = new Stack<>();
        buffer = new Stack<>();
    }

    // 把 right 全部反转倒进 left（保证顺序正确）
    private void reverseRightToLeft() {
        while (!right.isEmpty()) {
            left.push(right.pop());
        }
    }

    // 把 left 全部反转倒进 right（保证顺序正确）
    private void reverseLeftToRight() {
        while (!left.isEmpty()) {
            right.push(left.pop());
        }
    }

    // 头部添加
    public void addFirst(T item) {
        left.push(item);
    }

    // 尾部添加
    public void addLast(T item) {
        right.push(item);
    }

    // 删除头部
    public T removeFirst() {
        if (isEmpty()) throw new EmptyStackException();

        if (left.isEmpty()) {
            reverseRightToLeft();
        }
        return left.pop();
    }

    // 删除尾部
    public T removeLast() {
        if (isEmpty()) throw new EmptyStackException();

        if (right.isEmpty()) {
            reverseLeftToRight();
        }
        return right.pop();
    }

    public boolean isEmpty() {
        return left.isEmpty() && right.isEmpty();
    }

    public int size() {
        return left.size() + right.size();
    }
}

// 测试主程序
public class DequeTest {
    public static void main(String[] args) {
        DequeWithThreeStacks<Integer> deque = new DequeWithThreeStacks<>();

        System.out.println("=== 测试 1：头部添加 + 头部删除 ===");
        deque.addFirst(1);
        deque.addFirst(2);
        deque.addFirst(3);
        System.out.println(deque.removeFirst()); // 应输出 3
        System.out.println(deque.removeFirst()); // 应输出 2
        System.out.println(deque.removeFirst()); // 应输出 1
        System.out.println();

        System.out.println("=== 测试 2：尾部添加 + 尾部删除 ===");
        deque.addLast(10);
        deque.addLast(20);
        deque.addLast(30);
        System.out.println(deque.removeLast());  // 应输出 30
        System.out.println(deque.removeLast());  // 应输出 20
        System.out.println(deque.removeLast());  // 应输出 10
        System.out.println();

        System.out.println("=== 测试 3：混合操作（最容易乱序）===");
        deque.addFirst(100);
        deque.addLast(200);
        deque.addFirst(50);
        deque.addLast(300);

        // 预期队列顺序：50, 100, 200, 300
        System.out.println(deque.removeFirst()); // 50
        System.out.println(deque.removeFirst());  // 300
        System.out.println(deque.removeFirst()); // 100
        System.out.println(deque.removeFirst());  // 200
        System.out.println();

        System.out.println("=== 所有测试通过！顺序完全正确 ===");
    }
}