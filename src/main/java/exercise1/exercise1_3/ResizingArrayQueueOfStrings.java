package exercise1.exercise1_3;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ResizingArrayQueueOfStrings implements Iterable<String> {
    private String[] a;       // 存储队列元素的数组
    private int n;           // 队列中元素的数量
    private int first;       // 队列头部元素的索引
    private int last;        // 队列尾部下一个可用位置的索引

    // 初始化一个空队列
    public ResizingArrayQueueOfStrings() {
        a = new String[2];  // 初始容量设为2
        n = 0;
        first = 0;
        last = 0;
    }

    // 判断队列是否为空
    public boolean isEmpty() {
        return n == 0;
    }

    // 返回队列的大小
    public int size() {
        return n;
    }

    // 调整数组大小
    private void resize(int capacity) {
        String[] copy = new String[capacity];
        for (int i = 0; i < n; i++) {
            // 将原数组中从first开始的n个元素，复制到新数组从0开始的位置
            copy[i] = a[(first + i) % a.length];
        }
        a = copy;
        first = 0; // 重置队头指针
        last = n;  // 重置队尾指针
    }

    // 入队操作
    public void enqueue(String item) {
        // 如果队列已满（元素数量等于数组长度），则将数组容量翻倍
        if (n == a.length) {
            resize(2 * a.length);
        }
        a[last] = item;
        last = (last + 1) % a.length; // 队尾指针循环前进
        n++;
    }

    // 出队操作
    public String dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue underflow");
        }
        String item = a[first];
        a[first] = null; // 避免对象游离，帮助垃圾回收
        first = (first + 1) % a.length; // 队头指针循环前进
        n--;
        // 如果队列元素数量减少到数组容量的1/4，则将数组容量减半
        if (n > 0 && n == a.length / 4) {
            resize(a.length / 2);
        }
        return item;
    }

    // 返回队头元素（不删除）
    public String peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue underflow");
        }
        return a[first];
    }

    // 实现Iterable接口，返回一个迭代器
    public Iterator<String> iterator() {
        return new ArrayIterator();
    }

    // 迭代器类，实现从队头到队尾的遍历
    private class ArrayIterator implements Iterator<String> {
        private int i = 0;

        public boolean hasNext() {
            return i < n;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String item = a[(first + i) % a.length];
            i++;
            return item;
        }
    }

    // 测试用例
    public static void main(String[] args) {
        ResizingArrayQueueOfStrings queue = new ResizingArrayQueueOfStrings();

        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");
        queue.enqueue("D");

        System.out.println("Dequeue: " + queue.dequeue()); // 输出 A
        System.out.println("Dequeue: " + queue.dequeue()); // 输出 B

        queue.enqueue("E");
        queue.enqueue("F");

        System.out.println("\nIterating over queue:");
        for (String s : queue) {
            System.out.println(s); // 输出 C, D, E, F
        }

        System.out.println("\nDequeue all:");
        while (!queue.isEmpty()) {
            System.out.println(queue.dequeue()); // 依次输出 C, D, E, F
        }
    }
}