package exercise2.exercise2_4;

public class UnorderedArrayMaxPQ<Key extends Comparable<Key>> {
    private Key[] pq;       // 存储元素的数组
    private int n;          // 元素数量

    // 初始化容量
    public UnorderedArrayMaxPQ(int capacity) {
        pq = (Key[]) new Comparable[capacity];
        n = 0;
    }

    // 默认初始容量为 2
    public UnorderedArrayMaxPQ() {
        this(2);
    }

    // 插入元素（和栈 push 一致，直接追加到末尾）
    public void insert(Key key) {
        // 扩容：当数组满时，容量翻倍
        if (n == pq.length) resize(2 * pq.length);
        pq[n++] = key;
    }

    // 删除并返回最大元素
    public Key delMax() {
        if (isEmpty()) throw new IllegalStateException("Priority queue underflow");

        // 1. 找到最大值的索引
        int maxIdx = 0;
        for (int i = 1; i < n; i++) {
            if (less(maxIdx, i)) {
                maxIdx = i;
            }
        }

        // 2. 交换最大值与最后一个元素
        exch(maxIdx, n - 1);

        // 3. 删除最大值（置空）
        Key max = pq[n - 1];
        pq[--n] = null;

        // 缩容：当元素数量 ≤ 容量的 1/4 时，容量减半
        if (n > 0 && n == pq.length / 4) resize(pq.length / 2);

        return max;
    }

    // 查看最大元素
    public Key max() {
        if (isEmpty()) throw new IllegalStateException("Priority queue underflow");
        int maxIdx = 0;
        for (int i = 1; i < n; i++) {
            if (less(maxIdx, i)) maxIdx = i;
        }
        return pq[maxIdx];
    }

    // 辅助方法：比较 pq[i] < pq[j]
    private boolean less(int i, int j) {
        return pq[i].compareTo(pq[j]) < 0;
    }

    // 辅助方法：交换 pq[i] 和 pq[j]
    private void exch(int i, int j) {
        Key temp = pq[i];
        pq[i] = pq[j];
        pq[j] = temp;
    }

    // 辅助方法：调整数组大小
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
        UnorderedArrayMaxPQ<Integer> pq = new UnorderedArrayMaxPQ<>();
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