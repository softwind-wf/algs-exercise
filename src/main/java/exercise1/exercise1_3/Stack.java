package exercise1.exercise1_3;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Stack<Item> implements Iterable<Item> {
    private Node<Item> first;     // 栈顶
    private int n;                // 元素数量
    private int modCount = 0;     // 修改计数器

    // 链表节点
    private static class Node<Item> {
        private Item item;
        private Node<Item> next;
    }

    // 初始化空栈
    public Stack() {
        first = null;
        n = 0;
    }

    // 判断栈是否为空
    public boolean isEmpty() {
        return first == null;
    }

    // 返回栈大小
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
        modCount++; // 修改计数器自增
    }

    // 出栈操作
    public Item pop() {
        if (isEmpty()) throw new NoSuchElementException("Stack underflow");
        Item item = first.item;        // 保存栈顶元素
        first = first.next;            // 删除栈顶节点
        n--;
        modCount++; // 修改计数器自增
        return item;                   // 返回保存的元素
    }

    // 返回栈顶元素（不删除）
    public Item peek() {
        if (isEmpty()) throw new NoSuchElementException("Stack underflow");
        return first.item;
    }

    // 返回迭代器
    public Iterator<Item> iterator() {
        return new ListIterator<Item>(first, modCount);
    }

    // 实现迭代器
    private class ListIterator<Item> implements Iterator<Item> {
        private Node<Item> current;
        private final int expectedModCount;

        public ListIterator(Node<Item> first, int modCount) {
            current = first;
            this.expectedModCount = modCount; // 保存创建迭代器时的修改次数
        }

        public boolean hasNext() {
            // 检查是否被并发修改
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            return current != null;
        }

        public Item next() {
            // 检查是否被并发修改
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (!hasNext()) throw new NoSuchElementException();
            Item item = current.item;
            current = current.next;
            return item;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    // 测试用例
    public static void main(String[] args) {
        Stack<String> s = new Stack<String>();
        s.push("a");
        s.push("b");
        s.push("c");

        // 正常迭代
        for (String item : s) {
            System.out.println(item);
        }

        // 测试并发修改异常
        Iterator<String> it = s.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
            s.push("d"); // 在迭代过程中修改栈，会抛出异常
        }
    }
}