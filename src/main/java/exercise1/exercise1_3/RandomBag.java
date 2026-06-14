package exercise1.exercise1_3;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

public class RandomBag<Item> implements Iterable<Item> {
    private Item[] a;         // 数组存储元素
    private int N;            // 元素数量
    private static final int INIT_CAPACITY = 2;

    // 创建一个空随机背包
    public RandomBag() {
        a = (Item[]) new Object[INIT_CAPACITY];
        N = 0;
    }

    // 背包是否为空
    public boolean isEmpty() {
        return N == 0;
    }

    // 背包中的元素数量
    public int size() {
        return N;
    }

    // 调整数组大小
    private void resize(int capacity) {
        Item[] copy = (Item[]) new Object[capacity];
        for (int i = 0; i < N; i++) {
            copy[i] = a[i];
        }
        a = copy;
    }

    // 添加一个元素
    public void add(Item item) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null");
        if (N == a.length) resize(2 * a.length);
        a[N++] = item;
    }

    // 实现迭代器
    public Iterator<Item> iterator() {
        return new RandomBagIterator();
    }

    private class RandomBagIterator implements Iterator<Item> {
        private int current;
        private Item[] shuffled;

        public RandomBagIterator() {
            // 复制数组并随机打乱顺序
            shuffled = (Item[]) new Object[N];
            for (int i = 0; i < N; i++) {
                shuffled[i] = a[i];
            }
            shuffle(shuffled);
            current = 0;
        }

        // Fisher-Yates 洗牌算法，随机打乱数组
        private void shuffle(Item[] array) {
            Random rnd = new Random();
            for (int i = array.length - 1; i > 0; i--) {
                int index = rnd.nextInt(i + 1);
                Item temp = array[index];
                array[index] = array[i];
                array[i] = temp;
            }
        }

        public boolean hasNext() {
            return current < N;
        }

        public Item next() {
            if (!hasNext()) throw new NoSuchElementException();
            return shuffled[current++];
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}