package exercise2.exercise2_4;

public class LinkedListMaxPQ<Key extends Comparable<Key>> {
    // 链表节点
    private class Node {
        Key item;
        Node next;
        Node(Key item, Node next) {
            this.item = item;
            this.next = next;
        }
    }

    private Node first; // 链表表头（最大元素）
    private int n;      // 元素数量

    // 插入元素，保持链表从大到小逆序
    public void push(Key key) {
        // 情况1：链表为空，或新元素比表头更大，直接插在表头
        if (first == null || less(first.item, key)) {
            first = new Node(key, first);
        } else {
            // 情况2：遍历找到合适位置插入
            Node current = first;
            while (current.next != null && !less(current.next.item, key)) {
                current = current.next;
            }
            current.next = new Node(key, current.next);
        }
        n++;
    }

    // 删除并返回最大元素（表头元素）
    public Key pop() {
        if (isEmpty()) throw new IllegalStateException("Priority queue underflow");
        Key max = first.item;
        first = first.next;
        n--;
        return max;
    }

    // 查看最大元素
    public Key max() {
        if (isEmpty()) throw new IllegalStateException("Priority queue underflow");
        return first.item;
    }

    // 辅助方法：判断 a < b
    private boolean less(Key a, Key b) {
        return a.compareTo(b) < 0;
    }

    // 判空
    public boolean isEmpty() {
        return n == 0;
    }

    // 元素数量
    public int size() {
        return n;
    }

    // 测试
    public static void main(String[] args) {
        LinkedListMaxPQ<Integer> pq = new LinkedListMaxPQ<>();
        pq.push(5);
        pq.push(2);
        pq.push(8);
        pq.push(1);

        System.out.println("当前最大元素: " + pq.max()); // 8
        System.out.println("删除最大元素: " + pq.pop()); // 8
        System.out.println("删除后最大元素: " + pq.max()); // 5
        System.out.println("剩余元素数量: " + pq.size()); // 3
    }
}