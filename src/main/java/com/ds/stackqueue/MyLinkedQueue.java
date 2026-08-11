package com.ds.stackqueue;

import java.util.Objects;

/**
 * 基于链表的队列（Queue）实现 —— 先进先出（FIFO）
 *
 * 核心思想：
 * 队列是一种受限的线性表，只允许在队尾进行插入（入队），在队首进行删除（出队）。
 * 最先进入队列的元素最先被取出，即"先进先出"（First In First Out）。
 *
 * 使用头尾两个指针：
 *   - head（队首）：出队操作的端点
 *   - tail（队尾）：入队操作的端点
 * 两个指针使得入队和出队均为 O(1)。
 *
 * 生活中的类比：排队买票——先到的人先买到票离开。
 *
 * @param <E> 元素类型
 */
public class MyLinkedQueue<E> {

    // ==================== 节点内部类 ====================

    /**
     * 单向链表的节点
     */
    private static class Node<E> {
        E data;
        Node<E> next;

        Node(E data, Node<E> next) {
            this.data = data;
            this.next = next;
        }
    }

    // ==================== 成员变量 ====================

    private Node<E> head;  // 队首指针（最早入队的元素）
    private Node<E> tail;  // 队尾指针（最晚入队的元素）
    private int size;      // 队列中元素数量

    // ==================== 构造方法 ====================

    public MyLinkedQueue() {
        head = null;
        tail = null;
        size = 0;
    }

    // ==================== 基础查询 ====================

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    // ==================== 核心操作 ====================

    /**
     * 入队 —— 将元素添加到队尾
     *
     * 时间复杂度：O(1)
     *
     * @param data 要入队的元素
     */
    public void enqueue(E data) {
        Node<E> newNode = new Node<>(data, null);
        if (isEmpty()) {
            // 空队列：头尾指向同一个节点
            head = newNode;
            tail = newNode;
        } else {
            // 接到队尾并更新 tail
            tail.next = newNode;
            tail = newNode;
        }
        size++;
    }

    /**
     * 出队 —— 移除并返回队首元素
     *
     * 时间复杂度：O(1)
     *
     * @return 队首元素，队列为空时返回 null
     */
    public E dequeue() {
        if (isEmpty()) {
            return null;
        }
        E data = head.data;
        head = head.next;
        size--;
        if (isEmpty()) {
            // 队列变空，tail 也需要置 null
            tail = null;
        }
        return data;
    }

    /**
     * 查看队首元素但不移除
     *
     * 时间复杂度：O(1)
     *
     * @return 队首元素，队列为空时返回 null
     */
    public E peek() {
        if (isEmpty()) {
            return null;
        }
        return head.data;
    }

    /**
     * 查看队尾元素
     *
     * 时间复杂度：O(1)
     *
     * @return 队尾元素，队列为空时返回 null
     */
    public E peekLast() {
        if (isEmpty()) {
            return null;
        }
        return tail.data;
    }

    // ==================== 批量操作 ====================

    /**
     * 批量入队
     */
    @SuppressWarnings("unchecked")
    public void enqueueAll(E... items) {
        for (E item : items) {
            enqueue(item);
        }
    }

    /**
     * 清空队列
     */
    public void clear() {
        Node<E> current = head;
        while (current != null) {
            Node<E> next = current.next;
            current.next = null;
            current = next;
        }
        head = null;
        tail = null;
        size = 0;
    }

    // ==================== 查询操作 ====================

    /**
     * 判断队列中是否包含指定元素
     */
    public boolean contains(E data) {
        Node<E> current = head;
        while (current != null) {
            if (Objects.equals(data, current.data)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    /**
     * 查找元素在队列中的位置（队首位置为 0，队尾为 size-1）
     * 不存在返回 -1
     */
    public int indexOf(E data) {
        Node<E> current = head;
        int index = 0;
        while (current != null) {
            if (Objects.equals(data, current.data)) {
                return index;
            }
            current = current.next;
            index++;
        }
        return -1;
    }

    // ==================== 转换操作 ====================

    /**
     * 将队列内容转换为数组（队首在 index 0，队尾在 index size-1）
     */
    @SuppressWarnings("unchecked")
    public E[] toArray() {
        Object[] result = new Object[size];
        Node<E> current = head;
        for (int i = 0; i < size; i++) {
            result[i] = current.data;
            current = current.next;
        }
        return (E[]) result;
    }

    // ==================== 遍历操作 ====================

    /**
     * 从队首到队尾遍历并打印元素
     */
    public void traversal() {
        if (isEmpty()) {
            System.out.println("队列为空");
            return;
        }
        Node<E> current = head;
        int index = 0;
        while (current != null) {
            System.out.print("[" + index + "]=" + current.data + " ");
            current = current.next;
            index++;
        }
        System.out.println();
    }

    // ==================== 实用工具 ====================

    /**
     * 安全的出队 —— 如果队列为空，返回指定的默认值
     */
    public E dequeueOrDefault(E defaultValue) {
        if (isEmpty()) {
            return defaultValue;
        }
        return dequeue();
    }

    /**
     * 安全的查看队首 —— 如果队列为空，返回指定的默认值
     */
    public E peekOrDefault(E defaultValue) {
        if (isEmpty()) {
            return defaultValue;
        }
        return head.data;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[] (空队列)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("队首 → ");
        Node<E> current = head;
        while (current != null) {
            sb.append("[").append(current.data).append("]");
            current = current.next;
            if (current != null) {
                sb.append(" → ");
            }
        }
        sb.append(" → 队尾");
        return sb.toString();
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        System.out.println("========== 队列测试 ==========\n");

        MyLinkedQueue<Integer> queue = new MyLinkedQueue<>();

        // 1. 入队测试
        System.out.println("--- 1. 入队测试 ---");
        queue.enqueue(10);
        queue.enqueue(20);
        queue.enqueue(30);
        queue.enqueue(40);
        queue.enqueue(50);
        System.out.println("入队 10, 20, 30, 40, 50");
        System.out.println("size: " + queue.size());
        System.out.println(queue);
        System.out.println();

        // 2. 查看队首/队尾
        System.out.println("--- 2. 查看队首/队尾 ---");
        System.out.println("peek(): " + queue.peek());
        System.out.println("peekLast(): " + queue.peekLast());
        System.out.println("peek 后 size(应不变): " + queue.size());
        System.out.println();

        // 3. 出队测试
        System.out.println("--- 3. 出队测试 ---");
        System.out.println("dequeue(): " + queue.dequeue());
        System.out.println("dequeue(): " + queue.dequeue());
        System.out.println("出队 2 次后 size: " + queue.size());
        System.out.println("当前队首: " + queue.peek());
        System.out.println("当前队尾: " + queue.peekLast());
        System.out.println(queue);
        System.out.println();

        // 4. 遍历
        System.out.println("--- 4. 遍历 ---");
        queue.traversal();
        System.out.println();

        // 5. 查找
        System.out.println("--- 5. 查找测试 ---");
        System.out.println("indexOf(30): " + queue.indexOf(30));
        System.out.println("indexOf(50): " + queue.indexOf(50));
        System.out.println("indexOf(99): " + queue.indexOf(99));
        System.out.println("contains(40): " + queue.contains(40));
        System.out.println("contains(99): " + queue.contains(99));
        System.out.println();

        // 6. 批量入队
        System.out.println("--- 6. 批量入队 ---");
        queue.enqueueAll(100, 200, 300);
        System.out.println("enqueueAll(100, 200, 300) 后: " + queue);
        System.out.println("size: " + queue.size());
        System.out.println();

        // 7. 转换为数组
        System.out.println("--- 7. 转换为数组 ---");
        Object[] arr = queue.toArray();
        System.out.print("数组 (队首→队尾): ");
        for (Object o : arr) {
            System.out.print(o + " ");
        }
        System.out.println("\n");

        // 8. 逐个出队到空
        System.out.println("--- 8. 逐个出队到空 ---");
        while (!queue.isEmpty()) {
            System.out.print(queue.dequeue() + " ");
        }
        System.out.println();
        System.out.println("全部出队后 isEmpty: " + queue.isEmpty());
        System.out.println("全部出队后 size: " + queue.size());
        System.out.println(queue);
        System.out.println();

        // 9. 清空
        System.out.println("--- 9. 重新入队后清空 ---");
        queue.enqueueAll(1, 2, 3);
        System.out.println("入队后: " + queue);
        queue.clear();
        System.out.println("clear 后 isEmpty: " + queue.isEmpty());
        System.out.println();

        // 10. 边界测试
        System.out.println("--- 10. 空队列边界测试 ---");
        MyLinkedQueue<String> empty = new MyLinkedQueue<>();
        System.out.println("空队列 peek(): " + empty.peek());
        System.out.println("空队列 peekLast(): " + empty.peekLast());
        System.out.println("空队列 dequeue(): " + empty.dequeue());
        System.out.println("空队列 contains(\"a\"): " + empty.contains("a"));
        System.out.println("空队列 indexOf(\"a\"): " + empty.indexOf("a"));
        System.out.println("dequeueOrDefault(\"默认\"): " + empty.dequeueOrDefault("默认"));
        System.out.println("peekOrDefault(\"默认\"): " + empty.peekOrDefault("默认"));
        empty.traversal();
        System.out.println();

        // 11. 单元素测试
        System.out.println("--- 11. 单元素测试 ---");
        queue.enqueue(42);
        System.out.println("入队一个元素: " + queue);
        System.out.println("peek: " + queue.peek());
        System.out.println("peekLast: " + queue.peekLast());
        System.out.println("dequeue: " + queue.dequeue());
        System.out.println("出队后: " + queue + ", isEmpty: " + queue.isEmpty());

        System.out.println();
        System.out.println("========== 测试完成 ==========");
    }
}
