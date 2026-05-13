package exercise1.exercise1_3;

public class CircularLinkedListQueue<T> {

    private static class Node<T> {
        T item;
        Node<T> next;

        Node(T item) {
            this.item = item;
            this.next = null;
        }
    }

    // 仅使用一个 Node 实例变量，指向链表的最后一个结点
    private Node<T> last;
    private int n; // 队列中元素的个数

    public CircularLinkedListQueue() {
        this.last = null;
        this.n = 0;
    }

    /**
     * 入队操作：在队列尾部添加元素
     * @param item 要添加的元素
     */
    public void enqueue(T item) {
        Node<T> newNode = new Node<>(item);
        if (isEmpty()) {
            // 队列为空，新结点既是第一个也是最后一个，形成自环
            newNode.next = newNode;
            last = newNode;
        } else {
            // 新结点的 next 指向队首 (last.next)
            newNode.next = last.next;
            // 原队尾的 next 指向新结点
            last.next = newNode;
            // 更新 last 指针，使其指向新的队尾
            last = newNode;
        }
        n++;
    }

    /**
     * 出队操作：从队列头部移除并返回元素
     * @return 队首元素，如果队列为空则返回 null
     */
    public T dequeue() {
        if (isEmpty()) {
            return null;
        }

        Node<T> first = last.next; // 队首结点是 last.next
        T item = first.item;

        if (n == 1) {
            // 队列只有一个元素，出队后变为空
            last = null;
        } else {
            // 将 last.next 指向原队首的下一个结点（新的队首）
            last.next = first.next;
        }

        n--;
        return item;
    }

    /**
     * 检查队列是否为空
     * @return 如果队列为空返回 true，否则返回 false
     */
    public boolean isEmpty() {
        return n == 0;
    }

    /**
     * 获取队列中元素的个数
     * @return 队列大小
     */
    public int size() {
        return n;
    }

    /**
     * 打印队列中的所有元素（用于测试）
     */
    public void printQueue() {
        if (isEmpty()) {
            System.out.println("队列为空");
            return;
        }
        Node<T> current = last.next; // 从队首开始遍历
        do {
            System.out.print(current.item + " ");
            current = current.next;
        } while (current != last.next); // 遍历到再次回到队首时结束
        System.out.println();
    }

    public static void main(String[] args) {
        CircularLinkedListQueue<String> queue = new CircularLinkedListQueue<>();

        // 测试入队
        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");
        System.out.println("队列元素: ");
        queue.printQueue(); // 输出: A B C
        System.out.println("队列大小: " + queue.size()); // 输出: 3

        // 测试出队
        String item = queue.dequeue();
        System.out.println("出队元素: " + item); // 输出: A
        System.out.println("出队后队列元素: ");
        queue.printQueue(); // 输出: B C
        System.out.println("队列大小: " + queue.size()); // 输出: 2

        // 继续入队
        queue.enqueue("D");
        System.out.println("入队 D 后队列元素: ");
        queue.printQueue(); // 输出: B C D
        System.out.println("队列大小: " + queue.size()); // 输出: 3

        // 清空队列
        while (!queue.isEmpty()) {
            System.out.println("出队: " + queue.dequeue());
        }
        System.out.println("队列是否为空: " + queue.isEmpty()); // 输出: true
    }
}