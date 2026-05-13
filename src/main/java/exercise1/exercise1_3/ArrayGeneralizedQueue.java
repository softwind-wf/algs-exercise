package exercise1.exercise1_3;

import java.util.NoSuchElementException;

public class ArrayGeneralizedQueue<Item> {
    private Item[] a;
    private int n; // 队列中的元素数量

    public ArrayGeneralizedQueue() {
        a = (Item[]) new Object[2];
        n = 0;
    }

    public boolean isEmpty() {
        return n == 0;
    }

    private void resize(int capacity) {
        Item[] temp = (Item[]) new Object[capacity];
        for (int i = 0; i < n; i++) {
            temp[i] = a[i];
        }
        a = temp;
    }

    public void insert(Item x) {
        if (n == a.length) resize(2 * a.length);
        a[n++] = x;
    }

    public Item delete(int k) {
        if (isEmpty()) throw new NoSuchElementException("Queue underflow");
        if (k < 1 || k > n) throw new IllegalArgumentException("Invalid k");

        Item item = a[k-1];
        // 将k之后的元素向前移动一位
        for (int i = k-1; i < n-1; i++) {
            a[i] = a[i+1];
        }
        a[n-1] = null; // 避免游离对象
        n--;
        // 缩容
        if (n > 0 && n == a.length/4) resize(a.length/2);
        return item;
    }
}