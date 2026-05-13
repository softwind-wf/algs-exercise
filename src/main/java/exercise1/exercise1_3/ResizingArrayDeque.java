package exercise1.exercise1_3;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ResizingArrayDeque<Item> implements Iterable<Item> {
    private Item[] a;
    private int left;   // 队列头索引
    private int right;  // 队列尾索引
    private int N;      // 元素数量
    private int capacity;

    // 创建空双向队列
    public ResizingArrayDeque() {
        capacity = 2;
        a = (Item[]) new Object[capacity];
        left = 0;
        right = 0;
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

    // 调整数组大小
    private void resize(int newCapacity) {
        Item[] newA = (Item[]) new Object[newCapacity];
        for (int i = 0; i < N; i++) {
            newA[i] = a[(left + i) % capacity];
        }
        a = newA;
        left = 0;
        right = N;
        capacity = newCapacity;
    }

    // 向左端添加一个新元素
    public void pushLeft(Item item) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null");
        if (N == capacity) resize(2 * capacity);
        left = (left - 1 + capacity) % capacity;
        a[left] = item;
        N++;
    }

    // 向右端添加一个新元素
    public void pushRight(Item item) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null");
        if (N == capacity) resize(2 * capacity);
        a[right] = item;
        right = (right + 1) % capacity;
        N++;
    }

    // 从左端删除一个元素
    public Item popLeft() {
        if (isEmpty()) throw new NoSuchElementException("Deque underflow");
        Item item = a[left];
        a[left] = null; // 避免对象游离
        left = (left + 1) % capacity;
        N--;
        if (N > 0 && N == capacity / 4) resize(capacity / 2);
        return item;
    }

    // 从右端删除一个元素
    public Item popRight() {
        if (isEmpty()) throw new NoSuchElementException("Deque underflow");
        right = (right - 1 + capacity) % capacity;
        Item item = a[right];
        a[right] = null; // 避免对象游离
        N--;
        if (N > 0 && N == capacity / 4) resize(capacity / 2);
        return item;
    }

    // 实现迭代器
    public Iterator<Item> iterator() {
        return new ArrayDequeIterator();
    }

    private class ArrayDequeIterator implements Iterator<Item> {
        private int i = 0;

        public boolean hasNext() {
            return i < N;
        }

        public Item next() {
            if (!hasNext()) throw new NoSuchElementException();
            Item item = a[(left + i) % capacity];
            i++;
            return item;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}