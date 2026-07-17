package com.ds.linked;

import java.util.Objects;

/**
 * 自定义单向循环链表
 * 循环链表的特点：最后一个节点的 next 指向第一个节点，形成一个环
 *
 * @param <E> 元素类型
 */
public class MyCircularLinkedList<E> {

    // 单向循环链表节点内部类
    private static class Node<E> {
        E data;
        Node<E> next;

        Node(E data) {
            this.data = data;
            this.next = null;
        }

        Node() {
            this.data = null;
            this.next = null;
        }
    }

    // 指向链表中最后一个节点的引用
    // 若链表为空，last = null
    // last.next 始终指向第一个节点（头节点）
    private Node<E> last;

    // 链表中保存数据节点的数量
    private int size;

    public MyCircularLinkedList() {
        last = null;
        size = 0;
    }

    // ==================== 基础查询 ====================

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 获取第一个节点的引用（内部使用）
     * 空链表返回 null
     */
    private Node<E> first() {
        if (isEmpty()) {
            return null;
        }
        return last.next;
    }

    // ==================== 添加操作 ====================

    /**
     * 在链表头部添加元素
     */
    public void addFirst(E data) {
        Node<E> newNode = new Node<>(data);
        if (isEmpty()) {
            // 链表为空时，新节点自己指向自己形成环
            newNode.next = newNode;
            last = newNode;
        } else {
            // 新节点指向原头节点，尾节点指向新节点
            newNode.next = last.next;   // 原头节点
            last.next = newNode;        // 更新头节点
        }
        size++;
    }

    /**
     * 在链表尾部添加元素
     */
    public void addLast(E data) {
        // 尾部添加等同于先 addFirst 再把 last 指针后移
        addFirst(data);
        last = last.next;
    }

    /**
     * 在链表尾部添加元素（默认 add）
     */
    public void add(E data) {
        addLast(data);
    }

    /**
     * 在指定下标位置插入元素
     *
     * @param index 插入位置的下标（0 到 size）
     * @param data  要插入的元素
     */
    public void insert(int index, E data) {
        if (index < 0 || index > size) {
            throw new IllegalArgumentException("链表下标越界: " + index);
        }

        if (index == 0) {
            addFirst(data);
            return;
        }
        if (index == size) {
            addLast(data);
            return;
        }

        // 找到下标 index-1 的前驱节点
        Node<E> prev = last; // 从最后一个节点开始
        for (int i = 0; i < index; i++) {
            prev = prev.next;
        }
        // 循环结束后 prev 指向 index-1 的节点

        Node<E> newNode = new Node<>(data);
        newNode.next = prev.next;
        prev.next = newNode;
        size++;
    }

    // ==================== 删除操作 ====================

    /**
     * 删除并返回链表第一个元素，空链表返回 null
     */
    public E removeFirst() {
        if (isEmpty()) {
            return null;
        }

        Node<E> first = last.next;
        E data = first.data;

        if (size == 1) {
            // 只有一个节点，删除后链表为空
            last = null;
        } else {
            // 将尾节点指向新的头节点（原头节点的下一个）
            last.next = first.next;
        }

        // 断开被删除节点的引用，帮助 GC
        first.next = null;
        size--;
        return data;
    }

    /**
     * 删除并返回链表最后一个元素，空链表返回 null
     */
    public E removeLast() {
        if (isEmpty()) {
            return null;
        }

        if (size == 1) {
            return removeFirst();
        }

        // 找到倒数第二个节点（新尾节点）
        Node<E> prev = last;
        for (int i = 0; i < size - 1; i++) {
            prev = prev.next;
        }
        // 循环结束后 prev 指向倒数第二个节点

        E data = last.data;
        prev.next = last.next;      // 新尾节点指向头节点
        last.next = null;           // 断开旧尾节点
        last = prev;                // 更新尾节点引用
        size--;
        return data;
    }

    /**
     * 删除指定下标的元素并返回
     */
    public E remove(int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("链表下标越界: " + index);
        }

        if (index == 0) {
            return removeFirst();
        }
        if (index == size - 1) {
            return removeLast();
        }

        // 找到下标 index-1 的前驱节点
        Node<E> prev = last;
        for (int i = 0; i < index; i++) {
            prev = prev.next;
        }
        // 循环结束后 prev 指向 index-1 的节点

        Node<E> target = prev.next;
        E data = target.data;
        prev.next = target.next;
        target.next = null; // 断开引用，帮助 GC
        size--;
        return data;
    }

    /**
     * 按值删除元素，删除第一个匹配的元素
     *
     * @return 删除成功返回 true，否则返回 false
     */
    public boolean remove(E data) {
        if (isEmpty()) {
            return false;
        }

        // 特殊情况：删除的是第一个节点
        Node<E> first = last.next;
        if (Objects.equals(data, first.data)) {
            removeFirst();
            return true;
        }

        // 遍历查找匹配节点
        Node<E> prev = first;
        Node<E> current = first.next;
        int count = 1;  // 已检查的节点数，防止无限循环
        while (current != first && count < size) {
            if (Objects.equals(data, current.data)) {
                prev.next = current.next;
                current.next = null;
                if (current == last) {
                    last = prev;
                }
                size--;
                return true;
            }
            prev = current;
            current = current.next;
            count++;
        }

        return false;
    }

    // ==================== 查询操作 ====================

    /**
     * 按照下标获取链表中的元素
     */
    public E get(int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("链表下标越界: " + index);
        }
        Node<E> current = last.next; // 从头节点开始
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current.data;
    }

    /**
     * 获取链表第一个元素，空链表返回 null
     */
    public E getFirst() {
        if (isEmpty()) {
            return null;
        }
        return last.next.data;
    }

    /**
     * 获取链表最后一个元素，空链表返回 null
     */
    public E getLast() {
        if (isEmpty()) {
            return null;
        }
        return last.data;
    }

    /**
     * 更新指定下标的元素
     */
    public void update(int index, E data) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("链表下标越界: " + index);
        }
        Node<E> current = last.next;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        current.data = data;
    }

    /**
     * 判断链表是否包含指定元素
     */
    public boolean contains(E data) {
        return indexOf(data) != -1;
    }

    /**
     * 查找指定元素第一次出现的下标，不存在返回 -1
     */
    public int indexOf(E data) {
        if (isEmpty()) {
            return -1;
        }

        Node<E> first = last.next;
        Node<E> current = first;
        int index = 0;
        do {
            if (Objects.equals(data, current.data)) {
                return index;
            }
            current = current.next;
            index++;
        } while (current != first);

        return -1;
    }

    // ==================== 工具操作 ====================

    /**
     * 清空链表
     */
    public void clear() {
        // 断开所有节点之间的引用，帮助 GC
        if (!isEmpty()) {
            Node<E> current = last.next;
            last.next = null; // 先断开环
            while (current != null) {
                Node<E> next = current.next;
                current.next = null;
                current = next;
            }
        }
        last = null;
        size = 0;
    }

    /**
     * 正向遍历链表并打印每个元素
     */
    public void traversal() {
        if (isEmpty()) {
            System.out.println("链表为空");
            return;
        }
        Node<E> first = last.next;
        Node<E> current = first;
        do {
            System.out.println(current.data);
            current = current.next;
        } while (current != first);
    }

    /**
     * 从链表头部开始循环遍历 n 步（演示循环特性）
     * 即使 n 大于 size，遍历也不会越界，会继续循环
     *
     * @param n 遍历的步数
     */
    public void traversalCircular(int n) {
        if (isEmpty()) {
            System.out.println("链表为空");
            return;
        }
        Node<E> current = last.next;
        for (int i = 0; i < n; i++) {
            System.out.print(current.data + " ");
            current = current.next;
        }
        System.out.println();
    }

    /**
     * 将循环链表转换为数组（从头节点开始）
     */
    @SuppressWarnings("unchecked")
    public E[] toArray() {
        if (isEmpty()) {
            return (E[]) new Object[0];
        }
        Object[] result = new Object[size];
        Node<E> first = last.next;
        Node<E> current = first;
        int i = 0;
        do {
            result[i++] = current.data;
            current = current.next;
        } while (current != first);
        return (E[]) result;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        Node<E> first = last.next;
        Node<E> current = first;
        do {
            sb.append(current.data);
            current = current.next;
            if (current != first) {
                sb.append(" -> ");
            }
        } while (current != first);
        // 显示循环特性
        sb.append(" -> (回到头部)");
        return sb.toString();
    }

    // ==================== 弗洛伊德判环算法 ====================

    /**
     * 弗洛伊德判环算法（龟兔赛跑算法）—— 静态工具方法
     * 给定任意链表的头节点，判断是否存在环
     *
     * 算法原理：
     * 使用两个指针：慢指针（乌龟）每次移动 1 步，快指针（兔子）每次移动 2 步。
     * - 如果链表中不存在环，快指针会先到达 null，返回 false。
     * - 如果链表中存在环，快慢指针最终会在环中的某个节点相遇，返回 true。
     *
     * 时间复杂度：O(n)，空间复杂度：O(1)
     *
     * @param head 链表的头节点
     * @return 存在环返回 true，否则返回 false
     */
    public static <T> boolean hasCycle(Node<T> head) {
        if (head == null || head.next == null) {
            return false;
        }

        Node<T> slow = head;  // 慢指针（乌龟），每次走 1 步
        Node<T> fast = head;  // 快指针（兔子），每次走 2 步

        while (fast != null && fast.next != null) {
            slow = slow.next;           // 慢指针走 1 步
            fast = fast.next.next;      // 快指针走 2 步

            if (slow == fast) {
                return true;  // 相遇 → 存在环
            }
        }

        return false;  // 快指针到达 null → 无环
    }

    /**
     * 弗洛伊德判环算法 —— 找到环的入口节点（静态工具方法）
     *
     * 算法原理（数学证明）：
     * 设链表头到环入口的距离为 a，环入口到快慢指针相遇点的距离为 b，
     * 相遇点到环入口（沿环方向）的距离为 c，环的长度 L = b + c。
     *
     * 当快慢指针相遇时：
     * - 慢指针走过的距离：a + b
     * - 快指针走过的距离：a + b + k*L（k 为快指针在环中多走的圈数）
     *
     * 因为快指针速度是慢指针的 2 倍：2(a + b) = a + b + k*L
     * 化简得：a + b = k*L
     * 进一步：a = k*L - b = (k-1)*L + (L - b) = (k-1)*L + c
     *
     * 所以：a = c + (k-1)*L
     * 即：从链表头到环入口的距离 a，等于从相遇点沿环走到环入口的距离 c
     *     （再加若干整圈）
     *
     * 因此：将慢指针重置到链表头，快指针保持在相遇点，
     *       两者都以每次 1 步的速度前进，它们必定在环入口相遇。
     *
     * 时间复杂度：O(n)，空间复杂度：O(1)
     *
     * @param head 链表的头节点
     * @return 环的入口节点，若不存在环则返回 null
     */
    public static <T> Node<T> detectCycleEntry(Node<T> head) {
        if (head == null || head.next == null) {
            return null;
        }

        // 第一阶段：快慢指针找相遇点
        Node<T> slow = head;
        Node<T> fast = head;

        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;

            if (slow == fast) {
                // 第二阶段：一个指针回到头，两个指针同速前进，相遇点即环入口
                slow = head;
                while (slow != fast) {
                    slow = slow.next;
                    fast = fast.next;
                }
                return slow;  // 环的入口节点
            }
        }

        return null;  // 无环
    }

    /**
     * 计算环的长度（静态工具方法）
     *
     * 算法原理：
     * 先找到环中的任意一个节点（快慢指针相遇点），
     * 然后从该节点出发绕环一圈，计数经过的节点数。
     *
     * 时间复杂度：O(n)，空间复杂度：O(1)
     *
     * @param head 链表的头节点
     * @return 环的长度，若不存在环则返回 0
     */
    public static <T> int cycleLength(Node<T> head) {
        if (head == null || head.next == null) {
            return 0;
        }

        // 第一阶段：快慢指针找相遇点
        Node<T> slow = head;
        Node<T> fast = head;

        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;

            if (slow == fast) {
                // 第二阶段：从相遇点开始绕环一圈计数
                int length = 1;
                Node<T> current = slow.next;
                while (current != slow) {
                    length++;
                    current = current.next;
                }
                return length;
            }
        }

        return 0;  // 无环
    }

    // ========== 实例方法（操作当前链表） ==========

    /**
     * 判断当前循环链表是否存在环
     * 对于正确维护的循环链表，非空时始终返回 true
     *
     * @return 存在环返回 true
     */
    public boolean hasCycle() {
        if (isEmpty()) {
            return false;
        }
        return hasCycle(last.next);
    }

    /**
     * 获取当前循环链表环的入口节点
     * 对于正确维护的循环链表，入口节点即头节点（第一个节点）
     *
     * @return 环入口节点，空链表返回 null
     */
    public Node<E> detectCycleEntry() {
        if (isEmpty()) {
            return null;
        }
        return detectCycleEntry(last.next);
    }

    /**
     * 获取当前循环链表环的长度
     * 对于正确维护的循环链表，环的长度应当等于 size
     *
     * @return 环的长度
     */
    public int cycleLength() {
        if (isEmpty()) {
            return 0;
        }
        return cycleLength(last.next);
    }

    /**
     * 验证循环链表的完整性
     * 检查：是否存在环、环入口是否为头节点、环长度是否等于 size
     *
     * @return 完整性校验报告
     */
    public String verifyIntegrity() {
        if (isEmpty()) {
            return "链表为空，无环";
        }

        boolean hasCycle = hasCycle();
        Node<E> entry = detectCycleEntry();
        int detectedLength = cycleLength();

        Node<E> first = last.next;
        boolean entryIsFirst = (entry == first);
        boolean lengthMatches = (detectedLength == size);

        StringBuilder report = new StringBuilder();
        report.append("========== 循环链表完整性校验 ==========\n");
        report.append("是否存在环: ").append(hasCycle).append("\n");
        report.append("环的入口节点数据: ").append(entry != null ? entry.data : "null").append("\n");
        report.append("头节点数据: ").append(first != null ? first.data : "null").append("\n");
        report.append("环入口 == 头节点: ").append(entryIsFirst).append("\n");
        report.append("检测到的环长度: ").append(detectedLength).append("\n");
        report.append("链表记录 size: ").append(size).append("\n");
        report.append("环长度 == size: ").append(lengthMatches).append("\n");
        report.append("整体状态: ").append(hasCycle && entryIsFirst && lengthMatches
                ? "✓ 正常" : "✗ 异常").append("\n");
        report.append("==========================================");

        return report.toString();
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        System.out.println("========== 测试循环链表 ==========\n");

        MyCircularLinkedList<Integer> list = new MyCircularLinkedList<>();

        // 1. 测试添加
        System.out.println("--- 添加元素 ---");
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        System.out.println("链表内容: " + list);
        System.out.println("size: " + list.size());
        System.out.println();

        // 2. 测试查询
        System.out.println("--- 查询操作 ---");
        System.out.println("get(0): " + list.get(0));
        System.out.println("get(2): " + list.get(2));
        System.out.println("get(4): " + list.get(4));
        System.out.println("getFirst(): " + list.getFirst());
        System.out.println("getLast(): " + list.getLast());
        System.out.println("contains(3): " + list.contains(3));
        System.out.println("contains(10): " + list.contains(10));
        System.out.println("indexOf(3): " + list.indexOf(3));
        System.out.println();

        // 3. 测试头部和尾部插入
        System.out.println("--- 头尾插入 ---");
        list.addFirst(0);
        list.addLast(6);
        System.out.println("addFirst(0) + addLast(6): " + list);
        System.out.println("size: " + list.size());
        System.out.println();

        // 4. 测试指定位置插入
        System.out.println("--- 指定位置插入 ---");
        list.insert(3, 99);
        System.out.println("insert(3, 99): " + list);
        System.out.println();

        // 5. 测试循环遍历（遍历 12 步，超出 size）
        System.out.println("--- 循环遍历 12 步（演示循环特性）---");
        list.traversalCircular(12);
        System.out.println();

        // 6. 测试删除
        System.out.println("--- 删除操作 ---");
        System.out.println("removeFirst(): " + list.removeFirst());
        System.out.println("removeLast(): " + list.removeLast());
        System.out.println("remove(2): " + list.remove(2));
        System.out.println("删除后: " + list);
        System.out.println("size: " + list.size());
        System.out.println();

        // 7. 测试按值删除
        System.out.println("--- 按值删除 ---");
        System.out.println("remove(Integer.valueOf(99)): " + list.remove(Integer.valueOf(99)));
        System.out.println("remove(Integer.valueOf(999)): " + list.remove(Integer.valueOf(999)));
        System.out.println("按值删除后: " + list);
        System.out.println();

        // 8. 测试更新
        System.out.println("--- 更新操作 ---");
        list.update(1, 100);
        System.out.println("update(1, 100): " + list);
        System.out.println();

        // 9. 测试清空
        System.out.println("--- 清空链表 ---");
        list.clear();
        System.out.println("clear 后 isEmpty: " + list.isEmpty());
        System.out.println("clear 后链表: " + list);
        System.out.println();

        // 10. 测试空链表边界条件
        System.out.println("--- 空链表边界测试 ---");
        System.out.println("getFirst(): " + list.getFirst());
        System.out.println("getLast(): " + list.getLast());
        System.out.println("removeFirst(): " + list.removeFirst());
        System.out.println("removeLast(): " + list.removeLast());
        System.out.println("indexOf(1): " + list.indexOf(1));
        System.out.println("contains(1): " + list.contains(1));
        System.out.println();

        // 11. 测试单节点操作
        System.out.println("--- 单节点测试 ---");
        list.add(42);
        System.out.println("add(42) 后: " + list);
        System.out.println("getFirst(): " + list.getFirst());
        System.out.println("getLast(): " + list.getLast());
        list.traversalCircular(5);
        System.out.println("removeFirst(): " + list.removeFirst());
        System.out.println("删除后 isEmpty: " + list.isEmpty());
        System.out.println();

        // 12. 弗洛伊德判环算法测试
        System.out.println("========== 弗洛伊德判环算法 ==========\n");

        // 12.1 正常循环链表测试
        MyCircularLinkedList<String> circleList = new MyCircularLinkedList<>();
        circleList.add("A");
        circleList.add("B");
        circleList.add("C");
        circleList.add("D");
        circleList.add("E");
        System.out.println("--- 正常循环链表 ---");
        System.out.println("链表: " + circleList);
        System.out.println("size: " + circleList.size());
        System.out.println();
        System.out.println(circleList.verifyIntegrity());
        System.out.println();

        // 12.2 演示：构造一个带环的非循环链表
        // 手动构造: 1 -> 2 -> 3 -> 4 -> 5 -> 3 (环入口在节点 3)
        System.out.println("--- 手动构造带环链表 (1→2→3→4→5→3) ---");
        Node<Integer> n1 = new Node<>(1);
        Node<Integer> n2 = new Node<>(2);
        Node<Integer> n3 = new Node<>(3);
        Node<Integer> n4 = new Node<>(4);
        Node<Integer> n5 = new Node<>(5);

        n1.next = n2;
        n2.next = n3;
        n3.next = n4;
        n4.next = n5;
        n5.next = n3;  // 尾节点指向 n3，形成环，环入口为 n3

        System.out.println("hasCycle(n1): " + MyCircularLinkedList.hasCycle(n1));
        Node<Integer> entry = MyCircularLinkedList.detectCycleEntry(n1);
        System.out.println("环入口节点数据: " + (entry != null ? entry.data : "null"));
        System.out.println("环长度: " + MyCircularLinkedList.cycleLength(n1));
        System.out.println();

        // 12.3 无环链表测试
        System.out.println("--- 手动构造无环链表 (1→2→3→null) ---");
        Node<Integer> a1 = new Node<>(1);
        Node<Integer> a2 = new Node<>(2);
        Node<Integer> a3 = new Node<>(3);
        a1.next = a2;
        a2.next = a3;  // a3.next = null

        System.out.println("hasCycle(a1): " + MyCircularLinkedList.hasCycle(a1));
        System.out.println("环入口: " + MyCircularLinkedList.detectCycleEntry(a1));
        System.out.println("环长度: " + MyCircularLinkedList.cycleLength(a1));
        System.out.println();

        // 12.4 单节点自环
        System.out.println("--- 单节点自环 ---");
        Node<Integer> self = new Node<>(42);
        self.next = self;
        System.out.println("hasCycle(self): " + MyCircularLinkedList.hasCycle(self));
        Node<Integer> selfEntry = MyCircularLinkedList.detectCycleEntry(self);
        System.out.println("环入口节点数据: " + (selfEntry != null ? selfEntry.data : "null"));
        System.out.println("环长度: " + MyCircularLinkedList.cycleLength(self));
        System.out.println();

        // 12.5 两个节点形成的环
        System.out.println("--- 两节点环 (1↔2) ---");
        Node<Integer> b1 = new Node<>(1);
        Node<Integer> b2 = new Node<>(2);
        b1.next = b2;
        b2.next = b1;
        System.out.println("hasCycle(b1): " + MyCircularLinkedList.hasCycle(b1));
        Node<Integer> bEntry = MyCircularLinkedList.detectCycleEntry(b1);
        System.out.println("环入口节点数据: " + (bEntry != null ? bEntry.data : "null"));
        System.out.println("环长度: " + MyCircularLinkedList.cycleLength(b1));
        System.out.println();

        // 12.6 单节点空链表边界
        System.out.println("--- 边界测试 ---");
        System.out.println("hasCycle(null): " + MyCircularLinkedList.hasCycle(null));
        System.out.println("detectCycleEntry(null): " + MyCircularLinkedList.detectCycleEntry(null));
        System.out.println("cycleLength(null): " + MyCircularLinkedList.cycleLength(null));
        System.out.println();

        System.out.println("========== 测试完成 ==========");
    }
}
