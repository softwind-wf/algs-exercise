package exercise2.exercise2_4;

public class OrderedArrayMaxPQ<Key extends Comparable<Key>> {
    private Key[] pq;       // 存储元素的数组（保持有序）
    private int n;          // 元素数量

    // 初始化容量
    @SuppressWarnings("unchecked")
    public OrderedArrayMaxPQ(int capacity) {
        pq = (Key[]) new Comparable[capacity];
        n = 0;
    }

    // 默认初始容量为 2
    public OrderedArrayMaxPQ() {
        this(2);
    }

    // 插入元素：插入排序方式保持数组升序，最大元素在末尾
    public void insert(Key key) {
        // 扩容：当数组满时，容量翻倍
        if (n == pq.length) resize(2 * pq.length);

        int i = n - 1;
        // 从后往前遍历，将比 key 大的元素右移一格
        while (i >= 0 && less(key, pq[i])) {
            pq[i + 1] = pq[i];
            i--;
        }
        // 插入 key 到正确位置
        pq[i + 1] = key;
        n++;
    }

    // 删除并返回最大元素（数组最后一个元素）
    public Key delMax() {
        if (isEmpty()) throw new IllegalStateException("Priority queue underflow");

        // 最大元素在数组末尾
        Key max = pq[n - 1];
        pq[--n] = null; // 置空并减少元素数量

        // 缩容：当元素数量 ≤ 容量的 1/4 时，容量减半
        if (n > 0 && n == pq.length / 4) resize(pq.length / 2);

        return max;
    }

    // 查看最大元素
    public Key max() {
        if (isEmpty()) throw new IllegalStateException("Priority queue underflow");
        return pq[n - 1];
    }

    // 辅助方法：判断 a < b
    private boolean less(Key a, Key b) {
        return a.compareTo(b) < 0;
    }

    // 辅助方法：调整数组大小
    @SuppressWarnings("unchecked")
    private void resize(int newSize) {
        Key[] newPq = (Key[]) new Comparable[newSize];
        for (int i = 0; i < n; i++) {
            newPq[i] = pq[i];
        }
        pq = newPq;
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
        OrderedArrayMaxPQ<Integer> pq = new OrderedArrayMaxPQ<>();
        pq.insert(5);
        pq.insert(2);
        pq.insert(8);
        pq.insert(1);

        System.out.println("当前最大元素: " + pq.max()); // 8
        System.out.println("删除最大元素: " + pq.delMax()); // 8
        System.out.println("删除后最大元素: " + pq.max()); // 5
        System.out.println("剩余元素数量: " + pq.size()); // 3
    }
}