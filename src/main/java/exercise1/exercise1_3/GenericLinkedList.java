package exercise1.exercise1_3;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

// 核心优化1：给泛型T增加Comparable约束，确保存储的元素可比较
public class GenericLinkedList<T extends Comparable<T>> implements Iterable<T> {

    private static class Node<T> {
        T item;
        Node<T> next;

        Node(T item) {
            this.item = item;
        }
    }

    private Node<T> first;
    private int n;

    public GenericLinkedList() {
        this.first = null;
        this.n = 0;
    }

    // 尾部添加元素（原有方法，无修改）
    public void add(T item) {
        Node<T> newNode = new Node<>(item);
        if (first == null) {
            first = newNode;
        } else {
            Node<T> current = first;
            while (current.next != null) {
                current = current.next;
            }
            current.next = newNode;
        }
        n++;
    }

    // 原有辅助方法：获取第k个结点（无修改）
    public Node<T> getNode(int k) {
        if (k < 1 || k > n) return null;
        Node<T> current = first;
        for (int i = 1; i < k; i++) current = current.next;
        return current;
    }

    // ------------------- 修复后的max方法（非递归版） -------------------
    public T max(Node<T> firstNode) {
        // 空链表直接返回null
        if (firstNode == null) return null;

        T maxValue = firstNode.item;
        Node<T> current = firstNode.next;

        // 核心优化2：无需强制转换，直接调用compareTo（编译期安全）
        while (current != null) {
            if (current.item.compareTo(maxValue) > 0) {
                maxValue = current.item;
            }
            current = current.next;
        }
        return maxValue;
    }

    // ------------------- 修复后的maxRecursive方法（递归版） -------------------
    public T maxRecursive(Node<T> node) {
        // 基准条件：递归到链表末尾，返回null
        if (node == null) return null;

        // 递归获取后续结点的最大值
        T restMax = maxRecursive(node.next);

        // 核心优化3：简化逻辑，无需冗余判断，直接比较
        if (restMax == null) {
            // 后续无结点，当前结点就是最大值
            return node.item;
        }
        // 直接比较当前结点和后续最大值，返回较大者
        return node.item.compareTo(restMax) > 0 ? node.item : restMax;
    }

    // 原有基础方法（无修改）
    public boolean isEmpty() { return n == 0; }
    public int size() { return n; }

    // 迭代器实现（无修改）
    @Override
    public Iterator<T> iterator() {
        return new LinkedListIterator();
    }

    private class LinkedListIterator implements Iterator<T> {
        private Node<T> current = first;

        @Override
        public boolean hasNext() { return current != null; }

        @Override
        public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            T data = current.item;
            current = current.next;
            return data;
        }
    }

    /**
     * 迭代方式反转链表
     * @param x 链表首结点
     * @return 反转后链表的首结点
     */
    public static <T> Node<T> reverse(Node<T> x) {
        Node<T> first = x;
        Node<T> reverse = null;

        while (first != null) {
            Node<T> second = first.next; // 保存下一个结点
            first.next = reverse;       // 将当前结点插入到逆链表的开头
            reverse = first;            // 更新逆链表的首结点
            first = second;             // 移动到原链表的下一个结点
        }
        return reverse;
    }

    /**
     * 递归方式反转链表
     * @param first 链表首结点
     * @return 反转后链表的首结点
     */
    public static <T> Node<T> reverseRecursive(Node<T> first) {
        if (first == null) {
            return null;
        }
        if (first.next == null) {
            return first;
        }

        Node<T> second = first.next;
        Node<T> rest = reverseRecursive(second); // 递归反转后面的链表

        second.next = first; // 将第二个结点指向第一个结点
        first.next = null;   // 断开第一个结点与原链表的连接

        return rest; // 返回反转后链表的首结点
    }






    // 测试主方法
    public static void main(String[] args) {
        // 构建链表 1 -> 2 -> 3 -> 4 -> 5
        Node<Integer> head = new Node<>(1);
        head.next = new Node<>(2);
        head.next.next = new Node<>(3);
        head.next.next.next = new Node<>(4);
        head.next.next.next.next = new Node<>(5);

        System.out.println("原始链表:");
        printList(head); // 输出: 1 2 3 4 5

        // 测试迭代反转
        Node<Integer> reversedIterative = reverse(head);
        System.out.println("迭代反转后:");
        printList(reversedIterative); // 输出: 5 4 3 2 1

        // 重新构建链表，测试递归反转
        head = new Node<>(1);
        head.next = new Node<>(2);
        head.next.next = new Node<>(3);
        head.next.next.next = new Node<>(4);
        head.next.next.next.next = new Node<>(5);

        Node<Integer> reversedRecursive = reverseRecursive(head);
        System.out.println("递归反转后:");
        printList(reversedRecursive); // 输出: 5 4 3 2 1
    }

    /**
     * 辅助函数：打印链表
     */
    public static <T> void printList(Node<T> head) {
        Node<T> current = head;
        while (current != null) {
            System.out.print(current.item + " ");
            current = current.next;
        }
        System.out.println();
    }
}