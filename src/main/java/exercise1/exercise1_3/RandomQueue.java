package exercise1.exercise1_3;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

public class RandomQueue<Item> implements Iterable<Item> {
    private Item[] a;
    private int n;
    private Random random;

    public RandomQueue() {
        a = (Item[]) new Object[2];
        n = 0;
        random = new Random();
    }

    public boolean isEmpty() {
        return n == 0;
    }

    private void resize(int capacity) {
        assert capacity >= n;
        Item[] temp = (Item[]) new Object[capacity];
        for (int i = 0; i < n; i++) {
            temp[i] = a[i];
        }
        a = temp;
    }

    public void enqueue(Item item) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null");
        if (n == a.length) resize(2 * a.length);
        a[n++] = item;
    }

    public Item dequeue() {
        if (isEmpty()) throw new NoSuchElementException("Queue underflow");
        int r = random.nextInt(n);
        // 交换随机位置和最后一个位置的元素
        Item temp = a[r];
        a[r] = a[n-1];
        a[n-1] = temp;
        // 删除并返回最后一个元素
        Item item = a[n-1];
        a[n-1] = null;
        n--;
        if (n > 0 && n == a.length/4) resize(a.length/2);
        return item;
    }

    public Item sample() {
        if (isEmpty()) throw new NoSuchElementException("Queue underflow");
        int r = random.nextInt(n);
        return a[r];
    }

    @Override
    public Iterator<Item> iterator() {
        return new RandomQueueIterator();
    }

    private class RandomQueueIterator implements Iterator<Item> {
        private int i;
        private final Item[] shuffled;

        public RandomQueueIterator() {
            // 复制数组并洗牌
            shuffled = (Item[]) new Object[n];
            for (int k = 0; k < n; k++) {
                shuffled[k] = a[k];
            }
            // Fisher-Yates 洗牌
            for (int k = shuffled.length - 1; k > 0; k--) {
                int j = random.nextInt(k + 1);
                Item temp = shuffled[k];
                shuffled[k] = shuffled[j];
                shuffled[j] = temp;
            }
            i = 0;
        }

        @Override
        public boolean hasNext() {
            return i < shuffled.length;
        }

        @Override
        public Item next() {
            if (!hasNext()) throw new NoSuchElementException();
            return shuffled[i++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    // 测试用例：桥牌发牌
    public static void main(String[] args) {
        class Card {
            private final String suit;
            private final String rank;

            public Card(String suit, String rank) {
                this.suit = suit;
                this.rank = rank;
            }

            @Override
            public String toString() {
                return rank + " of " + suit;
            }
        }

        RandomQueue<Card> deck = new RandomQueue<>();
        String[] suits = {"♠", "♥", "♦", "♣"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};

        // 初始化一副牌
        for (String suit : suits) {
            for (String rank : ranks) {
                deck.enqueue(new Card(suit, rank));
            }
        }

        // 发牌：4个人，每人13张
        for (int player = 1; player <= 4; player++) {
            System.out.println("Player " + player + ":");
            for (int i = 0; i < 13; i++) {
                System.out.println("  " + deck.dequeue());
            }
            System.out.println();
        }
    }
}