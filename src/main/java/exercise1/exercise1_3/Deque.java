package exercise1.exercise1_3;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Deque<Item> implements Iterable<Item> {
    private Node first; // 队列头
    private Node last;  // 队列尾
    private int N;      // 元素数量

    // 双向链表节点
    private class Node {
        Item item;
        Node next;
        Node prev;
    }

    // 创建空双向队列
    public Deque() {
        first = null;
        last = null;
        N = 0;
    }

    // 判断队列是否为空
    public boolean isEmpty() {
        return N == 0;
    }

    // 返回队列中的元素数量
    public int size() {
        return N;
    }

    // 向左端添加一个新元素
    public void pushLeft(Item item) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null");
        Node oldFirst = first;
        first = new Node();
        first.item = item;
        first.next = oldFirst;
        if (isEmpty()) {
            last = first;
        } else {
            oldFirst.prev = first;
        }
        N++;
    }

    // 向右端添加一个新元素
    public void pushRight(Item item) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null");
        Node oldLast = last;
        last = new Node();
        last.item = item;
        last.prev = oldLast;
        if (isEmpty()) {
            first = last;
        } else {
            oldLast.next = last;
        }
        N++;
    }

    // 从左端删除一个元素
    public Item popLeft() {
        if (isEmpty()) throw new NoSuchElementException("Deque underflow");
        Item item = first.item;
        first = first.next;
        N--;
        if (isEmpty()) {
            last = null;
        } else {
            first.prev = null;
        }
        return item;
    }

    // 从右端删除一个元素
    public Item popRight() {
        if (isEmpty()) throw new NoSuchElementException("Deque underflow");
        Item item = last.item;
        last = last.prev;
        N--;
        if (isEmpty()) {
            first = null;
        } else {
            last.next = null;
        }
        return item;
    }

    // 实现迭代器
    public Iterator<Item> iterator() {
        return new DequeIterator();
    }

    private class DequeIterator implements Iterator<Item> {
        private Node current = first;

        public boolean hasNext() {
            return current != null;
        }

        public Item next() {
            if (!hasNext()) throw new NoSuchElementException();
            Item item = current.item;
            current = current.next;
            return item;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}