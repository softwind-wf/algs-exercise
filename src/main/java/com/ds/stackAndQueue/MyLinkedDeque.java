package com.ds.stackAndQueue;

import java.util.Objects;

/**
 * 双端队列（Deque — Double-Ended Queue）—— 基于双向链表
 *
 * 核心思想：
 * 双端队列是一种两端都可以进行插入和删除的线性表。
 * 它同时具备栈和队列的能力——可以在队首/队尾自由地入队和出队。
 *
 * 操作对称性：
 *   队首端: addFirst / removeFirst / peekFirst   (等价于栈的 push / pop / peek)
 *   队尾端: addLast  / removeLast  / peekLast    (等价于队列的 enqueue / — / peekLast)
 *
 * 使用头尾哨兵节点：
 *   head ↔ 数据节点1 ↔ 数据节点2 ↔ ... ↔ 数据节点N ↔ tail
 * 哨兵简化了边界处理，所有插入删除操作仅需修改相邻节点的指针。
 *
 * 生活中的类比：一列人可以同时从两端上下车的地铁车厢。
 *
 * @param <E> 元素类型
 */
public class MyLinkedDeque<E> {

    // ==================== 节点内部类 ====================

    /**
     * 双向链表的节点，持有数据域、前驱指针和后继指针
     */
    private static class Node<E> {
        E data;
        Node<E> prev;
        Node<E> next;

        Node(E data) {
            this.data = data;
        }

        Node() {
            this.data = null;
        }
    }

    // ==================== 成员变量 ====================

    private final Node<E> head;  // 头哨兵节点
    private final Node<E> tail;  // 尾哨兵节点
    private int size;            // 元素数量

    // ==================== 构造方法 ====================

    public MyLinkedDeque() {
        head = new Node<>();
        tail = new Node<>();
        head.next = tail;
        tail.prev = head;
        size = 0;
    }

    // ==================== 基础查询 ====================

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    // ==================== 队首操作 ====================

    /**
     * 在队首添加元素
     * 时间复杂度：O(1)
     */
    public void addFirst(E data) {
        Node<E> newNode = new Node<>(data);
        newNode.next = head.next;
        newNode.prev = head;
        head.next.prev = newNode;
        head.next = newNode;
        size++;
    }

    /**
     * 移除并返回队首元素
     * 时间复杂度：O(1)
     *
     * @return 队首元素，空时返回 null
     */
    public E removeFirst() {
        if (isEmpty()) {
            return null;
        }
        Node<E> first = head.next;
        E data = first.data;
        head.next = first.next;
        first.next.prev = head;
        // 断开引用，帮助 GC
        first.prev = null;
        first.next = null;
        size--;
        return data;
    }

    /**
     * 查看队首元素但不移除
     * 时间复杂度：O(1)
     */
    public E peekFirst() {
        if (isEmpty()) {
            return null;
        }
        return head.next.data;
    }

    // ==================== 队尾操作 ====================

    /**
     * 在队尾添加元素
     * 时间复杂度：O(1)
     */
    public void addLast(E data) {
        Node<E> newNode = new Node<>(data);
        newNode.prev = tail.prev;
        newNode.next = tail;
        tail.prev.next = newNode;
        tail.prev = newNode;
        size++;
    }

    /**
     * 移除并返回队尾元素
     * 时间复杂度：O(1)
     *
     * @return 队尾元素，空时返回 null
     */
    public E removeLast() {
        if (isEmpty()) {
            return null;
        }
        Node<E> last = tail.prev;
        E data = last.data;
        tail.prev = last.prev;
        last.prev.next = tail;
        // 断开引用，帮助 GC
        last.prev = null;
        last.next = null;
        size--;
        return data;
    }

    /**
     * 查看队尾元素但不移除
     * 时间复杂度：O(1)
     */
    public E peekLast() {
        if (isEmpty()) {
            return null;
        }
        return tail.prev.data;
    }

    // ==================== 别名方法（兼容栈/队列语义）====================

    /** 等价于 addFirst——入栈 */
    public void push(E data) {
        addFirst(data);
    }

    /** 等价于 removeFirst——出栈 */
    public E pop() {
        return removeFirst();
    }

    /** 等价于 peekFirst——查看栈顶 */
    public E peek() {
        return peekFirst();
    }

    /** 等价于 addLast——入队 */
    public void enqueue(E data) {
        addLast(data);
    }

    /** 等价于 removeFirst——出队 */
    public E dequeue() {
        return removeFirst();
    }

    // ==================== 按索引访问 ====================

    /**
     * 按照下标获取元素（0 为队首，size-1 为队尾）
     * 从距离更近的一端开始遍历
     */
    public E get(int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("下标越界: " + index);
        }

        Node<E> current;
        if (index < size / 2) {
            // 从队首向后遍历
            current = head.next;
            for (int i = 0; i < index; i++) {
                current = current.next;
            }
        } else {
            // 从队尾向前遍历
            current = tail.prev;
            for (int i = size - 1; i > index; i--) {
                current = current.prev;
            }
        }
        return current.data;
    }

    /**
     * 更新指定下标的元素
     */
    public void set(int index, E data) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("下标越界: " + index);
        }
        Node<E> current;
        if (index < size / 2) {
            current = head.next;
            for (int i = 0; i < index; i++) {
                current = current.next;
            }
        } else {
            current = tail.prev;
            for (int i = size - 1; i > index; i--) {
                current = current.prev;
            }
        }
        current.data = data;
    }

    // ==================== 批量操作 ====================

    @SuppressWarnings("unchecked")
    public void addAllFirst(E... items) {
        for (E item : items) {
            addFirst(item);
        }
    }

    @SuppressWarnings("unchecked")
    public void addAllLast(E... items) {
        for (E item : items) {
            addLast(item);
        }
    }

    /**
     * 清空双端队列
     */
    public void clear() {
        Node<E> current = head.next;
        while (current != tail) {
            Node<E> next = current.next;
            current.prev = null;
            current.next = null;
            current = next;
        }
        head.next = tail;
        tail.prev = head;
        size = 0;
    }

    // ==================== 查询操作 ====================

    /**
     * 判断是否包含指定元素
     */
    public boolean contains(E data) {
        Node<E> current = head.next;
        while (current != tail) {
            if (Objects.equals(data, current.data)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    /**
     * 查找指定元素首次出现的下标（队首为 0）
     * 不存在返回 -1
     */
    public int indexOf(E data) {
        Node<E> current = head.next;
        int index = 0;
        while (current != tail) {
            if (Objects.equals(data, current.data)) {
                return index;
            }
            current = current.next;
            index++;
        }
        return -1;
    }

    /**
     * 按值删除——删除第一个匹配的元素
     *
     * @return 删除成功返回 true
     */
    public boolean remove(E data) {
        Node<E> current = head.next;
        while (current != tail) {
            if (Objects.equals(data, current.data)) {
                current.prev.next = current.next;
                current.next.prev = current.prev;
                current.prev = null;
                current.next = null;
                size--;
                return true;
            }
            current = current.next;
        }
        return false;
    }

    // ==================== 转换操作 ====================

    /**
     * 转换为数组（队首在 index 0）
     */
    @SuppressWarnings("unchecked")
    public E[] toArray() {
        Object[] result = new Object[size];
        Node<E> current = head.next;
        for (int i = 0; i < size; i++) {
            result[i] = current.data;
            current = current.next;
        }
        return (E[]) result;
    }

    // ==================== 遍历操作 ====================

    /**
     * 正向遍历（队首 → 队尾）
     */
    public void traversal() {
        if (isEmpty()) {
            System.out.println("双端队列为空");
            return;
        }
        Node<E> current = head.next;
        while (current != tail) {
            System.out.print(current.data + " ");
            current = current.next;
        }
        System.out.println();
    }

    /**
     * 反向遍历（队尾 → 队首）
     */
    public void traversalReverse() {
        if (isEmpty()) {
            System.out.println("双端队列为空");
            return;
        }
        Node<E> current = tail.prev;
        while (current != head) {
            System.out.print(current.data + " ");
            current = current.prev;
        }
        System.out.println();
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[] (空双端队列)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("队首 ↔ ");
        Node<E> current = head.next;
        while (current != tail) {
            sb.append("[").append(current.data).append("]");
            current = current.next;
            if (current != tail) {
                sb.append(" ↔ ");
            }
        }
        sb.append(" ↔ 队尾");
        return sb.toString();
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        System.out.println("========== 双端队列测试 ==========\n");

        MyLinkedDeque<Integer> deque = new MyLinkedDeque<>();

        // 1. 两端添加
        System.out.println("--- 1. 两端添加 ---");
        deque.addFirst(20);
        deque.addFirst(10);
        deque.addLast(30);
        deque.addLast(40);
        System.out.println("addFirst(20), addFirst(10), addLast(30), addLast(40)");
        System.out.println("size: " + deque.size());
        System.out.println(deque);
        System.out.println();

        // 2. 两端查看
        System.out.println("--- 2. 两端查看 ---");
        System.out.println("peekFirst(): " + deque.peekFirst());
        System.out.println("peekLast(): " + deque.peekLast());
        System.out.println();

        // 3. 两端删除
        System.out.println("--- 3. 两端删除 ---");
        System.out.println("removeFirst(): " + deque.removeFirst());
        System.out.println("removeLast(): " + deque.removeLast());
        System.out.println("删除后: " + deque);
        System.out.println("size: " + deque.size());
        System.out.println();

        // 4. 栈语义（push/pop）
        System.out.println("--- 4. 栈语义 push/pop ---");
        deque.clear();
        deque.push(1);
        deque.push(2);
        deque.push(3);
        System.out.println("push(1,2,3) 后: " + deque);
        System.out.println("pop(): " + deque.pop());
        System.out.println("pop(): " + deque.pop());
        System.out.println("pop 后: " + deque);
        System.out.println();

        // 5. 队列语义（enqueue/dequeue）
        System.out.println("--- 5. 队列语义 enqueue/dequeue ---");
        deque.clear();
        deque.enqueue(10);
        deque.enqueue(20);
        deque.enqueue(30);
        System.out.println("enqueue(10,20,30) 后: " + deque);
        System.out.println("dequeue(): " + deque.dequeue());
        System.out.println("dequeue(): " + deque.dequeue());
        System.out.println("dequeue 后: " + deque);
        System.out.println();

        // 6. 按索引访问
        System.out.println("--- 6. 按索引访问 ---");
        deque.clear();
        for (int i = 1; i <= 5; i++) {
            deque.addLast(i * 100);
        }
        System.out.println("当前: " + deque);
        for (int i = 0; i < deque.size(); i++) {
            System.out.println("get(" + i + "): " + deque.get(i));
        }
        System.out.println();

        // 7. 更新元素
        System.out.println("--- 7. 更新元素 ---");
        deque.set(2, 999);
        System.out.println("set(2, 999) 后: " + deque);
        System.out.println();

        // 8. 查询
        System.out.println("--- 8. 查询测试 ---");
        System.out.println("contains(200): " + deque.contains(200));
        System.out.println("contains(999): " + deque.contains(999));
        System.out.println("indexOf(200): " + deque.indexOf(200));
        System.out.println("indexOf(999): " + deque.indexOf(999));
        System.out.println();

        // 9. 按值删除
        System.out.println("--- 9. 按值删除 ---");
        deque.remove(999);
        System.out.println("remove(999) 后: " + deque);
        deque.remove(100);
        System.out.println("remove(100) 后: " + deque);
        System.out.println();

        // 10. 正反向遍历
        System.out.println("--- 10. 正反向遍历 ---");
        System.out.print("正向 (队首→队尾): ");
        deque.traversal();
        System.out.print("反向 (队尾→队首): ");
        deque.traversalReverse();
        System.out.println();

        // 11. 批量添加 + 转数组
        System.out.println("--- 11. 批量添加 + 转数组 ---");
        deque.addAllFirst(-1, -2);
        deque.addAllLast(9999, 10000);
        System.out.println("批量添加后: " + deque);
        Object[] arr = deque.toArray();
        System.out.print("数组: ");
        for (Object o : arr) {
            System.out.print(o + " ");
        }
        System.out.println("\n");

        // 12. 清空
        System.out.println("--- 12. 清空 ---");
        deque.clear();
        System.out.println("clear 后 isEmpty: " + deque.isEmpty());
        System.out.println("clear 后: " + deque);
        System.out.println();

        // 13. 边界测试
        System.out.println("--- 13. 空队列边界测试 ---");
        MyLinkedDeque<String> empty = new MyLinkedDeque<>();
        System.out.println("空 peekFirst(): " + empty.peekFirst());
        System.out.println("空 peekLast(): " + empty.peekLast());
        System.out.println("空 removeFirst(): " + empty.removeFirst());
        System.out.println("空 removeLast(): " + empty.removeLast());
        System.out.println("空 pop(): " + empty.pop());
        System.out.println("空 dequeue(): " + empty.dequeue());
        System.out.println("空 contains(\"a\"): " + empty.contains("a"));
        System.out.println("空 remove(\"a\"): " + empty.remove("a"));
        try {
            empty.get(0);
        } catch (IllegalArgumentException e) {
            System.out.println("预期异常: " + e.getMessage());
        }
        System.out.println();

        System.out.println("========== 测试完成 ==========");
    }
}
