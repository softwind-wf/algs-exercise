package exercise2.exercise2_5;

import edu.princeton.cs.algs4.StdOut;
import java.util.Comparator;
import java.util.NoSuchElementException;

/**
 * 2.5.24 稳定的最大优先队列
 * 保证：优先级高的先出队；优先级相同的，按插入顺序先入先出（FIFO）
 * @param <Key> 存储的元素类型
 */
public class StableMaxPQ<Key> {
    // 内部包装类：给元素绑定优先级和时间戳
    private static class StableItem<Key> implements Comparable<StableItem<Key>> {
        Key item;       // 实际存储的元素
        double priority;// 优先级（越大越优先）
        long timestamp; // 时间戳：插入顺序，越小插入越早

        public StableItem(Key item, double priority, long timestamp) {
            this.item = item;
            this.priority = priority;
            this.timestamp = timestamp;
        }

        // 核心比较规则：先比优先级，再比时间戳
        @Override
        public int compareTo(StableItem<Key> that) {
            // 1. 先比较优先级：优先级高的应该"更大"（能浮到堆顶）
            int cmp = Double.compare(this.priority, that.priority);
            if (cmp != 0) {
                return cmp; // 优先级高的返回正值，被认为更大
            }
            // 2. 优先级相同：时间戳小的应该"更大"（先插入的先出队）
            // 注意：要反过来比，这样时间戳小的返回正值
            return Long.compare(that.timestamp, this.timestamp);
        }
    }

    // 优先队列底层：用堆存储StableItem
    private StableItem<Key>[] pq;
    private int n;                  // 当前元素数量
    private long timestampCounter;  // 全局时间戳计数器，每次插入自增
    private Comparator<Key> comparator; // 自定义比较器（可选）

    // 构造函数1：默认容量
    public StableMaxPQ() {
        this(1);
    }

    // 构造函数2：指定初始容量
    @SuppressWarnings("unchecked")
    public StableMaxPQ(int capacity) {
        pq = (StableItem<Key>[]) new StableItem[capacity + 1]; // 堆从索引1开始
        n = 0;
        timestampCounter = 0;
        comparator = null;
    }

    // 构造函数3：带自定义比较器
    @SuppressWarnings("unchecked")
    public StableMaxPQ(Comparator<Key> comparator) {
        this(1);
        this.comparator = comparator;
    }

    // 判断队列是否为空
    public boolean isEmpty() {
        return n == 0;
    }

    // 返回队列大小
    public int size() {
        return n;
    }

    // 插入元素（带优先级）
    public void insert(Key x, double priority) {
        // 扩容：数组满了就翻倍
        if (n == pq.length - 1) {
            resize(2 * pq.length);
        }
        // 插入元素：给元素绑定当前时间戳，然后计数器自增
        pq[++n] = new StableItem<>(x, priority, timestampCounter++);
        swim(n); // 上浮维护堆序
    }

    // 删除并返回优先级最高的元素（稳定保证：同优先级按插入顺序）
    public Key delMax() {
        if (isEmpty()) {
            throw new NoSuchElementException("优先队列为空");
        }
        StableItem<Key> max = pq[1];
        exch(1, n--);    // 把最后一个元素换到堆顶
        sink(1);        // 下沉维护堆序
        pq[n + 1] = null;// 防止内存泄漏
        // 缩容：元素数量小于容量1/4时，缩容到一半
        if (n > 0 && n == (pq.length - 1) / 4) {
            resize(pq.length / 2);
        }
        return max.item;
    }

    // 返回优先级最高的元素（不删除）
    public Key max() {
        if (isEmpty()) {
            throw new NoSuchElementException("优先队列为空");
        }
        return pq[1].item;
    }

    // 上浮操作：维护堆序
    private void swim(int k) {
        while (k > 1 && less(k / 2, k)) {
            exch(k / 2, k);
            k = k / 2;
        }
    }

    // 下沉操作：维护堆序
    private void sink(int k) {
        while (2 * k <= n) {
            int j = 2 * k;
            if (j < n && less(j, j + 1)) {
                j++;
            }
            if (!less(k, j)) {
                break;
            }
            exch(k, j);
            k = j;
        }
    }

    // 比较：i位置的元素是否小于j位置的元素
    private boolean less(int i, int j) {
        if (comparator == null) {
            return pq[i].compareTo(pq[j]) < 0;
        } else {
            // 自定义比较器：先按比较器比，再比时间戳
            int cmp = comparator.compare(pq[i].item, pq[j].item);
            if (cmp != 0) {
                return cmp < 0;
            }
            return Long.compare(pq[i].timestamp, pq[j].timestamp) > 0;
        }
    }

    // 交换两个位置的元素
    private void exch(int i, int j) {
        StableItem<Key> temp = pq[i];
        pq[i] = pq[j];
        pq[j] = temp;
    }

    // 数组扩容/缩容
    @SuppressWarnings("unchecked")
    private void resize(int capacity) {
        StableItem<Key>[] temp = (StableItem<Key>[]) new StableItem[capacity];
        for (int i = 1; i <= n; i++) {
            temp[i] = pq[i];
        }
        pq = temp;
    }

    // 测试主类
    public static void main(String[] args) {
        // 测试1：验证稳定性（同优先级按插入顺序出队）
        StdOut.println("=== 测试1：同优先级元素按插入顺序出队 ===");
        StableMaxPQ<String> pq = new StableMaxPQ<>();
        // 插入3个优先级相同的元素：A(优先级5)、B(优先级5)、C(优先级5)
        pq.insert("A", 5);
        pq.insert("B", 5);
        pq.insert("C", 5);
        // 出队顺序应该是 A → B → C（严格按插入顺序）
        while (!pq.isEmpty()) {
            StdOut.print(pq.delMax() + " ");
        }
        StdOut.println("\n");

        // 测试2：混合优先级+同优先级
        StdOut.println("=== 测试2：混合优先级+同优先级 ===");
        StableMaxPQ<String> pq2 = new StableMaxPQ<>();
        // 插入元素：
        // 优先级10：X
        // 优先级5：A、B、C
        // 优先级8：Y
        pq2.insert("A", 5);
        pq2.insert("X", 10);
        pq2.insert("B", 5);
        pq2.insert("Y", 8);
        pq2.insert("C", 5);
        // 出队顺序应该是：X(10) → Y(8) → A(5) → B(5) → C(5)
        while (!pq2.isEmpty()) {
            StdOut.print(pq2.delMax() + " ");
        }
        StdOut.println();
    }
}