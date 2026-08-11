package cn.exercise.algs4.datastructure.stackqueue;

import java.util.Arrays;
import java.util.Objects;

/**
 * 循环队列（Circular Queue）—— 基于数组的环形缓冲区
 *
 * 核心思想：
 * 普通队列用数组实现时，队首出队后前面的空间会被浪费，需要不断移动元素。
 * 循环队列让数组的尾部和头部"连接"起来，形成一个逻辑上的环：
 * 当 rear 或 front 到达数组末尾时，通过取模运算回到数组开头。
 *
 * 关键设计 —— 空/满判断：
 * 如果不加区分，front == rear 既可以表示"空"也可以表示"满"。
 * 本实现采用"浪费一个位置"的策略：预留一个空位，使得
 *   - 队空: front == rear
 *   - 队满: (rear + 1) % capacity == front
 *
 * 支持动态扩容/缩容，保持 O(1) 均摊时间复杂度。
 *
 * 生活中的类比：旋转餐桌——菜转一圈又回到起点。
 *
 * @param <E> 元素类型
 */
public class MyCircularQueue<E> {

    // ==================== 常量与成员变量 ====================

    private static final int DEFAULT_CAPACITY = 8;   // 默认初始容量
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private E[] elements;    // 底层数组（实际容量 = elements.length）
    private int front;       // 队首索引（指向第一个有效元素）
    private int rear;        // 队尾索引（指向下一个可插入的位置）
    private int size;        // 当前元素个数

    // ==================== 构造方法 ====================

    @SuppressWarnings("unchecked")
    public MyCircularQueue(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("初始容量必须大于 0: " + initialCapacity);
        }
        // 多分配一个空间用于区分空/满
        elements = (E[]) new Object[initialCapacity + 1];
        front = 0;
        rear = 0;
        size = 0;
    }

    public MyCircularQueue() {
        this(DEFAULT_CAPACITY);
    }

    // ==================== 基础查询 ====================

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 获取当前底层数组的容量（含预留空位）
     */
    public int capacity() {
        return elements.length - 1;
    }

    // ==================== 核心操作 ====================

    /**
     * 入队 —— 将元素添加到队尾
     * 均摊时间复杂度：O(1)
     *
     * @param data 要入队的元素
     */
    public void enqueue(E data) {
        ensureCapacity(size + 1);

        elements[rear] = data;
        rear = (rear + 1) % elements.length;
        size++;
    }

    /**
     * 出队 —— 移除并返回队首元素
     * 均摊时间复杂度：O(1)
     *
     * @return 队首元素，队列为空时返回 null
     */
    public E dequeue() {
        if (isEmpty()) {
            return null;
        }

        E data = elements[front];
        elements[front] = null;  // 帮助 GC
        front = (front + 1) % elements.length;
        size--;

        // 空间利用率过低时缩容
        if (size > 0 && size <= capacity() / 4 && capacity() > DEFAULT_CAPACITY) {
            shrink();
        }

        return data;
    }

    /**
     * 查看队首元素但不移除
     * 时间复杂度：O(1)
     */
    public E peek() {
        if (isEmpty()) {
            return null;
        }
        return elements[front];
    }

    /**
     * 查看队尾元素
     * 时间复杂度：O(1)
     */
    public E peekLast() {
        if (isEmpty()) {
            return null;
        }
        // rear 指向下一个可插入位置，队尾在 rear-1（取模处理）
        int lastIdx = (rear - 1 + elements.length) % elements.length;
        return elements[lastIdx];
    }

    // ==================== 查询操作 ====================

    /**
     * 按索引获取元素（队首为 0，队尾为 size-1）
     * O(1) —— 直接通过 front + index 计算物理位置
     */
    public E get(int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("下标越界: " + index);
        }
        int physicalIdx = (front + index) % elements.length;
        return elements[physicalIdx];
    }

    /**
     * 判断是否包含指定元素
     */
    public boolean contains(E data) {
        for (int i = 0; i < size; i++) {
            int idx = (front + i) % elements.length;
            if (Objects.equals(data, elements[idx])) {
                return true;
            }
        }
        return false;
    }

    // ==================== 容量管理 ====================

    /**
     * 扩容 —— 当队列满时将数组容量翻倍
     */
    @SuppressWarnings("unchecked")
    private void ensureCapacity(int needed) {
        if (needed > capacity()) {
            int newCapacity = Math.min(
                elements.length * 2,
                MAX_ARRAY_SIZE
            );
            if (newCapacity <= capacity()) {
                throw new OutOfMemoryError("队列已达到最大容量");
            }
            E[] newElements = (E[]) new Object[newCapacity + 1];
            copyTo(newElements);
            elements = newElements;
            front = 0;
            rear = size;
        }
    }

    /**
     * 缩容 —— 当元素过少时释放多余空间
     */
    @SuppressWarnings("unchecked")
    private void shrink() {
        int newCapacity = Math.max(DEFAULT_CAPACITY, capacity() / 2);
        if (newCapacity < size) {
            return;
        }
        E[] newElements = (E[]) new Object[newCapacity + 1];
        copyTo(newElements);
        elements = newElements;
        front = 0;
        rear = size;
    }

    /**
     * 将当前元素按顺序复制到新数组（从 index 0 开始存储）
     * 这样复制后 front=0, rear=size，数据结构线性化
     */
    private void copyTo(E[] dest) {
        for (int i = 0; i < size; i++) {
            dest[i] = elements[(front + i) % elements.length];
        }
    }

    // ==================== 批量操作 ====================

    @SuppressWarnings("unchecked")
    public void enqueueAll(E... items) {
        for (E item : items) {
            enqueue(item);
        }
    }

    /**
     * 清空队列
     */
    @SuppressWarnings("unchecked")
    public void clear() {
        // 清空所有引用帮助 GC
        for (int i = 0; i < size; i++) {
            elements[(front + i) % elements.length] = null;
        }
        elements = (E[]) new Object[DEFAULT_CAPACITY + 1];
        front = 0;
        rear = 0;
        size = 0;
    }

    // ==================== 转换操作 ====================

    /**
     * 转换为数组（队首在 index 0）
     */
    @SuppressWarnings("unchecked")
    public E[] toArray() {
        Object[] result = new Object[size];
        for (int i = 0; i < size; i++) {
            result[i] = elements[(front + i) % elements.length];
        }
        return (E[]) result;
    }

    // ==================== 遍历操作 ====================

    /**
     * 从队首到队尾遍历打印
     */
    public void traversal() {
        if (isEmpty()) {
            System.out.println("循环队列为空");
            return;
        }
        for (int i = 0; i < size; i++) {
            int idx = (front + i) % elements.length;
            System.out.print("[" + i + "]=" + elements[idx] + " ");
        }
        System.out.println();
    }

    /**
     * 打印底层数组的全部内容（调试用，展示环形布局）
     */
    public void debug() {
        System.out.println("========== 内部数组状态 ==========");
        System.out.println("capacity=" + capacity() + ", size=" + size
                + ", front=" + front + ", rear=" + rear);
        for (int i = 0; i < elements.length; i++) {
            String marker = "";
            if (i == front && i == rear) marker = " ← front/rear (空)";
            else if (i == front) marker = " ← front";
            else if (i == rear) marker = " ← rear";
            System.out.println("  [" + i + "] = " + elements[i] + marker);
        }
        System.out.println("==================================");
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[] (空循环队列)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("队首 → ");
        for (int i = 0; i < size; i++) {
            int idx = (front + i) % elements.length;
            sb.append("[").append(elements[idx]).append("]");
            if (i < size - 1) sb.append(" → ");
        }
        sb.append(" → 队尾");
        return sb.toString();
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        System.out.println("========== 循环队列测试 ==========\n");

        MyCircularQueue<Integer> queue = new MyCircularQueue<>(4); // 有效容量=4

        // 1. 基本入队出队
        System.out.println("--- 1. 基本入队 ---");
        queue.enqueue(10);
        queue.enqueue(20);
        queue.enqueue(30);
        queue.enqueue(40);
        System.out.println("入队 10,20,30,40");
        System.out.println("size: " + queue.size() + ", capacity: " + queue.capacity());
        System.out.println(queue);
        queue.debug();
        System.out.println();

        // 2. 环形特性验证 —— 出队后再入队
        System.out.println("--- 2. 环形特性验证 ---");
        System.out.println("dequeue(): " + queue.dequeue());
        System.out.println("dequeue(): " + queue.dequeue());
        System.out.println("dequeue 2 次后: " + queue);
        queue.debug();

        queue.enqueue(50);
        queue.enqueue(60);
        System.out.println("enqueue(50, 60) 后: " + queue);
        System.out.println("注意：新元素从数组开头位置填充（环形写回）");
        queue.debug();
        System.out.println();

        // 3. 扩容测试
        System.out.println("--- 3. 扩容测试 ---");
        queue.enqueue(70);
        queue.enqueue(80);
        System.out.println("enqueue(70, 80) 触发扩容后:");
        System.out.println("size: " + queue.size() + ", capacity: " + queue.capacity());
        System.out.println(queue);
        queue.debug();
        System.out.println();

        // 4. 查看队首/队尾
        System.out.println("--- 4. 查看队首/队尾 ---");
        System.out.println("peek(): " + queue.peek());
        System.out.println("peekLast(): " + queue.peekLast());
        System.out.println();

        // 5. 按索引获取
        System.out.println("--- 5. 按索引获取 ---");
        System.out.println("当前队列: " + queue);
        for (int i = 0; i < queue.size(); i++) {
            System.out.println("get(" + i + "): " + queue.get(i));
        }
        System.out.println();

        // 6. 查询
        System.out.println("--- 6. 查询 ---");
        System.out.println("contains(50): " + queue.contains(50));
        System.out.println("contains(99): " + queue.contains(99));
        System.out.println();

        // 7. 缩容测试
        System.out.println("--- 7. 缩容测试 ---");
        while (queue.size() > 2) {
            System.out.println("dequeue: " + queue.dequeue() + " → size=" + queue.size()
                    + ", capacity=" + queue.capacity());
        }
        System.out.println("缩容后队列: " + queue);
        queue.debug();
        System.out.println();

        // 8. 批量入队
        System.out.println("--- 8. 批量入队 + 转数组 ---");
        queue.enqueueAll(100, 200, 300, 400, 500);
        System.out.println("批量入队后: " + queue);
        Object[] arr = queue.toArray();
        System.out.print("数组: " + Arrays.toString(arr));
        System.out.println("\n");

        // 9. 清空
        System.out.println("--- 9. 清空 ---");
        queue.clear();
        System.out.println("clear 后 isEmpty: " + queue.isEmpty());
        System.out.println("clear 后 size: " + queue.size());
        System.out.println(queue);
        System.out.println();

        // 10. 边界测试
        System.out.println("--- 10. 空队列边界测试 ---");
        MyCircularQueue<String> empty = new MyCircularQueue<>();
        System.out.println("空 peek(): " + empty.peek());
        System.out.println("空 peekLast(): " + empty.peekLast());
        System.out.println("空 dequeue(): " + empty.dequeue());
        System.out.println("空 contains(\"a\"): " + empty.contains("a"));
        try {
            empty.get(0);
        } catch (IllegalArgumentException e) {
            System.out.println("预期异常: " + e.getMessage());
        }
        System.out.println();

        // 11. 满容量弹性和遍历
        System.out.println("--- 11. 满容量遍历 ---");
        MyCircularQueue<Character> cq = new MyCircularQueue<>(3);
        cq.enqueueAll('A', 'B', 'C');
        System.out.println("容量 3，入队 A,B,C: " + cq);
        cq.dequeue();         // front 移到 1
        cq.enqueue('D');      // rear 绕回 0
        cq.dequeue();         // front 移到 2
        cq.enqueue('E');      // rear 到 1
        System.out.println("经过一系列操作后: " + cq);
        cq.debug();
        cq.traversal();
        System.out.println();

        System.out.println("========== 测试完成 ==========");
    }
}
