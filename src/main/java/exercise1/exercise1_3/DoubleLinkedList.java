package exercise1.exercise1_3;

public class DoubleLinkedList {

    // 双向链表结点类
    public static class DoubleNode<T> {
        public T item;
        public DoubleNode<T> prev; // 前驱结点
        public DoubleNode<T> next; // 后继结点

        public DoubleNode(T item) {
            this.item = item;
            this.prev = null;
            this.next = null;
        }
    }

    /**
     * 在表头插入结点
     * @param head 链表头结点
     * @param newNode 要插入的新结点
     * @return 插入后的新头结点
     */
    public static <T> DoubleNode<T> insertAtHead(DoubleNode<T> head, DoubleNode<T> newNode) {
        if (head == null) {
            return newNode;
        }
        newNode.next = head;
        head.prev = newNode;
        return newNode;
    }

    /**
     * 在表尾插入结点
     * @param head 链表头结点
     * @param newNode 要插入的新结点
     * @return 插入后的链表头结点
     */
    public static <T> DoubleNode<T> insertAtTail(DoubleNode<T> head, DoubleNode<T> newNode) {
        if (head == null) {
            return newNode;
        }
        DoubleNode<T> current = head;
        while (current.next != null) {
            current = current.next;
        }
        current.next = newNode;
        newNode.prev = current;
        return head;
    }

    /**
     * 从表头删除结点
     * @param head 链表头结点
     * @return 删除后的新头结点
     */
    public static <T> DoubleNode<T> deleteFromHead(DoubleNode<T> head) {
        if (head == null || head.next == null) {
            return null;
        }
        DoubleNode<T> newHead = head.next;
        newHead.prev = null;
        return newHead;
    }

    /**
     * 从表尾删除结点
     * @param head 链表头结点
     * @return 删除后的链表头结点
     */
    public static <T> DoubleNode<T> deleteFromTail(DoubleNode<T> head) {
        if (head == null || head.next == null) {
            return null;
        }
        DoubleNode<T> current = head;
        while (current.next != null) {
            current = current.next;
        }
        DoubleNode<T> prevNode = current.prev;
        prevNode.next = null;
        return head;
    }

    /**
     * 在指定结点之前插入新结点
     * @param head 链表头结点
     * @param target 指定结点
     * @param newNode 要插入的新结点
     * @return 插入后的链表头结点
     */
    public static <T> DoubleNode<T> insertBefore(DoubleNode<T> head, DoubleNode<T> target, DoubleNode<T> newNode) {
        if (target == null || newNode == null) {
            return head;
        }
        if (target.prev == null) { // target是头结点
            return insertAtHead(head, newNode);
        }
        DoubleNode<T> prevNode = target.prev;
        prevNode.next = newNode;
        newNode.prev = prevNode;
        newNode.next = target;
        target.prev = newNode;
        return head;
    }

    /**
     * 在指定结点之后插入新结点
     * @param head 链表头结点
     * @param target 指定结点
     * @param newNode 要插入的新结点
     * @return 插入后的链表头结点
     */
    public static <T> DoubleNode<T> insertAfter(DoubleNode<T> head, DoubleNode<T> target, DoubleNode<T> newNode) {
        if (target == null || newNode == null) {
            return head;
        }
        if (target.next == null) { // target是尾结点
            return insertAtTail(head, newNode);
        }
        DoubleNode<T> nextNode = target.next;
        target.next = newNode;
        newNode.prev = target;
        newNode.next = nextNode;
        nextNode.prev = newNode;
        return head;
    }

    /**
     * 删除指定结点
     * @param head 链表头结点
     * @param target 要删除的结点
     * @return 删除后的链表头结点
     */
    public static <T> DoubleNode<T> deleteNode(DoubleNode<T> head, DoubleNode<T> target) {
        if (head == null || target == null) {
            return head;
        }
        if (target.prev == null) { // target是头结点
            return deleteFromHead(head);
        }
        if (target.next == null) { // target是尾结点
            return deleteFromTail(head);
        }
        DoubleNode<T> prevNode = target.prev;
        DoubleNode<T> nextNode = target.next;
        prevNode.next = nextNode;
        nextNode.prev = prevNode;
        return head;
    }

    // 辅助方法：打印链表
    public static <T> void printList(DoubleNode<T> head) {
        DoubleNode<T> current = head;
        while (current != null) {
            System.out.print(current.item + " ");
            current = current.next;
        }
        System.out.println();
    }

    // 测试主方法
    public static void main(String[] args) {
        // 构建初始链表: 1 <-> 2 <-> 3
        DoubleNode<Integer> node1 = new DoubleNode<>(1);
        DoubleNode<Integer> node2 = new DoubleNode<>(2);
        DoubleNode<Integer> node3 = new DoubleNode<>(3);
        node1.next = node2;
        node2.prev = node1;
        node2.next = node3;
        node3.prev = node2;
        DoubleNode<Integer> head = node1;

        System.out.println("原始链表:");
        printList(head); // 输出: 1 2 3

        // 测试在表头插入 0
        head = insertAtHead(head, new DoubleNode<>(0));
        System.out.println("在表头插入 0 后:");
        printList(head); // 输出: 0 1 2 3

        // 测试在表尾插入 4
        head = insertAtTail(head, new DoubleNode<>(4));
        System.out.println("在表尾插入 4 后:");
        printList(head); // 输出: 0 1 2 3 4

        // 测试从表头删除
        head = deleteFromHead(head);
        System.out.println("从表头删除后:");
        printList(head); // 输出: 1 2 3 4

        // 测试从表尾删除
        head = deleteFromTail(head);
        System.out.println("从表尾删除后:");
        printList(head); // 输出: 1 2 3

        // 测试在结点2之前插入 1.5
        DoubleNode<Integer> node1_5 = new DoubleNode<Integer>((int) 1.5);
        head = insertBefore(head, node2, node1_5);
        System.out.println("在结点2之前插入 1.5 后:");
        printList(head); // 输出: 1 1.5 2 3

        // 测试在结点2之后插入 2.5
        DoubleNode<Integer> node2_5 = new DoubleNode<Integer>((int) 2.5);
        head = insertAfter(head, node2, node2_5);
        System.out.println("在结点2之后插入 2.5 后:");
        printList(head); // 输出: 1 1.5 2 2.5 3

        // 测试删除结点2
        head = deleteNode(head, node2);
        System.out.println("删除结点2后:");
        printList(head); // 输出: 1 1.5 2.5 3
    }
}