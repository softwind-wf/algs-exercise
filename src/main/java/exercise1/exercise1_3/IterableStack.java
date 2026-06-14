package exercise1.exercise1_3;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class IterableStack<Item> implements Iterable<Item> {

    private Node<Item> first;    // 栈顶元素
    private int n;               // 栈中元素数量

    // 辅助类 Node
    private static class Node<Item> {
        private Item item;
        private Node<Item> next;
    }

    // 初始化空栈
    public IterableStack() {
        first = null;
        n = 0;
    }

    // 判断栈是否为空
    public boolean isEmpty() {
        return first == null;
    }

    // 返回栈的大小
    public int size() {
        return n;
    }

    // 入栈操作
    public void push(Item item) {
        Node<Item> oldfirst = first;
        first = new Node<Item>();
        first.item = item;
        first.next = oldfirst;
        n++;
    }

    // 出栈操作
    public Item pop() {
        if (isEmpty()) throw new NoSuchElementException("Stack underflow");
        Item item = first.item;        // 保存栈顶元素
        first = first.next;            // 删除栈顶元素
        n--;
        return item;                   // 返回保存的元素
    }

    // 返回栈顶元素（不删除）
    public Item peek() {
        if (isEmpty()) throw new NoSuchElementException("Stack underflow");
        return first.item;
    }

    // 实现 Iterable 接口，返回一个迭代器
    public Iterator<Item> iterator() {
        return new ListIterator<Item>(first);
    }

    // 迭代器类，实现从栈顶到栈底的遍历
    private class ListIterator<Item> implements Iterator<Item> {
        private Node<Item> current;

        public ListIterator(Node<Item> first) {
            current = first;
        }

        public boolean hasNext() {
            return current != null;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public Item next() {
            if (!hasNext()) throw new NoSuchElementException();
            Item item = current.item;
            current = current.next;
            return item;
        }
    }

    // 静态方法：复制一个字符串栈
    public static IterableStack<String> copy(IterableStack<String> stack) {
        IterableStack<String> temp = new IterableStack<>();
        IterableStack<String> copyStack = new IterableStack<>();

        // 第一步：遍历原栈，将元素压入临时栈，此时顺序反转
        for (String s : stack) {
            temp.push(s);
        }

        // 第二步：遍历临时栈，将元素压入目标栈，恢复原顺序
        for (String s : temp) {
            copyStack.push(s);
        }

        return copyStack;
    }

    // 测试用例
    public static void main(String[] args) {
        IterableStack<String> originalStack = new IterableStack<>();
        originalStack.push("A");
        originalStack.push("B");
        originalStack.push("C");
        originalStack.push("D");

        System.out.println("Original Stack:");
        for (String s : originalStack) {
            System.out.println(s); // 输出: D, C, B, A
        }

        IterableStack<String> copiedStack = IterableStack.copy(originalStack);

        System.out.println("\nCopied Stack:");
        for (String s : copiedStack) {
            System.out.println(s); // 输出: D, C, B, A
        }

        // 验证是独立的副本
        originalStack.pop();
        System.out.println("\nAfter popping from original:");
        System.out.println("Original Stack size: " + originalStack.size()); // 输出: 3
        System.out.println("Copied Stack size: " + copiedStack.size());     // 输出: 4
    }
}