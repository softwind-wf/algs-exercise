package exercise1.exercise1_3;

public class RingBuffer<Item> {
    private final Item[] buffer;
    private int head = 0;
    private int tail = 0;
    private int count = 0;
    private final int capacity;

    @SuppressWarnings("unchecked")
    public RingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.buffer = (Item[]) new Object[capacity];
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean isFull() {
        return count == capacity;
    }

    public int size() {
        return count;
    }

    public synchronized void enqueue(Item item) throws InterruptedException {
        while (isFull()) {
            wait(); // 缓冲区满，生产者等待
        }
        buffer[tail] = item;
        tail = (tail + 1) % capacity;
        count++;
        notifyAll(); // 唤醒等待的消费者
    }

    public synchronized Item dequeue() throws InterruptedException {
        while (isEmpty()) {
            wait(); // 缓冲区空，消费者等待
        }
        Item item = buffer[head];
        buffer[head] = null; // 避免游离对象
        head = (head + 1) % capacity;
        count--;
        notifyAll(); // 唤醒等待的生产者
        return item;
    }


        public static void main(String[] args) {
            RingBuffer<Integer> buffer = new RingBuffer<>(3);

            // 生产者线程
            new Thread(() -> {
                try {
                    for (int i = 1; i <= 5; i++) {
                        buffer.enqueue(i);
                        System.out.println("Produced: " + i);
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

            // 消费者线程
            new Thread(() -> {
                try {
                    for (int i = 1; i <= 5; i++) {
                        int item = buffer.dequeue();
                        System.out.println("Consumed: " + item);
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

}