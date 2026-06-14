package exercise2.exercise2_4;

public class IndexMinPQ<Item extends Comparable<Item>> {
    private final int maxN;       // 最大索引数
    private int size;            // 当前堆中元素个数
    private final int[] pq;      // 堆数组，pq[i] = 索引k（1-based）
    private final int[] qp;      // 反向映射，qp[k] = 索引k在堆中的位置（不存在则为-1）
    private final Item[] keys;   // 存储元素，keys[k] = 索引k对应的元素

    // 构造函数：初始化最大容量为 maxN 的索引优先队列
    @SuppressWarnings("unchecked")
    public IndexMinPQ(int maxN) {
        if (maxN < 0) throw new IllegalArgumentException();
        this.maxN = maxN;
        this.size = 0;
        this.pq = new int[maxN + 1];    // 堆从1开始
        this.qp = new int[maxN];         // 索引范围 0~maxN-1
        this.keys = (Item[]) new Comparable[maxN];

        // 初始化：所有索引k都不在堆中，qp[k] = -1
        for (int k = 0; k < maxN; k++) {
            qp[k] = -1;
        }
    }

    // ==================== 核心 API ====================

    // 插入元素：将索引k与item关联
    public void insert(int k, Item item) {
        if (k < 0 || k >= maxN) throw new IllegalArgumentException("索引越界");
        if (contains(k)) throw new IllegalArgumentException("索引已存在");

        size++;
        qp[k] = size;
        pq[size] = k;
        keys[k] = item;
        swim(size); // 上浮维护最小堆性质
    }

    // 修改元素：将索引k的元素更新为item
    public void change(int k, Item item) {
        if (k < 0 || k >= maxN) throw new IllegalArgumentException("索引越界");
        if (!contains(k)) throw new IllegalArgumentException("索引不存在");

        int pos = qp[k];
        Item oldItem = keys[k];
        keys[k] = item;
        
        // 根据新旧值的比较决定上浮还是下沉
        if (item.compareTo(oldItem) < 0) {
            swim(pos);  // 新值更小，上浮
        } else if (item.compareTo(oldItem) > 0) {
            sink(pos);  // 新值更大，下沉
        }
        // 如果相等，不需要调整
    }

    // 判断索引k是否存在
    public boolean contains(int k) {
        if (k < 0 || k >= maxN) throw new IllegalArgumentException("索引越界");
        return qp[k] != -1;
    }

    // 删除索引k及其关联元素
    public void delete(int k) {
        if (k < 0 || k >= maxN) throw new IllegalArgumentException("索引越界");
        if (!contains(k)) throw new IllegalArgumentException("索引不存在");

        int idx = qp[k];
        swap(idx, size--);
        swim(idx);
        sink(idx);
        qp[k] = -1;
        keys[k] = null;
    }

    // 返回最小元素
    public Item min() {
        if (isEmpty()) throw new IllegalStateException("队列为空");
        return keys[pq[1]];
    }

    // 返回最小元素的索引
    public int minIndex() {
        if (isEmpty()) throw new IllegalStateException("队列为空");
        return pq[1];
    }

    // 删除最小元素并返回其索引
    public int delMin() {
        if (isEmpty()) throw new IllegalStateException("队列为空");

        int minIdx = pq[1];
        swap(1, size--);
        sink(1);

        qp[minIdx] = -1;
        keys[minIdx] = null;
        return minIdx;
    }

    // 判断队列是否为空
    public boolean isEmpty() {
        return size == 0;
    }

    // 返回队列中元素个数
    public int size() {
        return size;
    }

    // ==================== 辅助方法：堆操作 ====================

    // 上浮：维护最小堆性质
    private void swim(int i) {
        while (i > 1 && greater(i / 2, i)) {
            swap(i, i / 2);
            i = i / 2;
        }
    }

    // 下沉：维护最小堆性质
    private void sink(int i) {
        while (2 * i <= size) {
            int j = 2 * i;
            // 找到左右子节点中较小的那个
            if (j < size && greater(j, j + 1)) j++;
            // 如果当前节点 <= 子节点，停止下沉
            if (!greater(i, j)) break;
            swap(i, j);
            i = j;
        }
    }

    // 比较：keys[pq[i]] > keys[pq[j]]
    private boolean greater(int i, int j) {
        return keys[pq[i]].compareTo(keys[pq[j]]) > 0;
    }

    // 交换堆中两个位置的元素
    private void swap(int i, int j) {
        int temp = pq[i];
        pq[i] = pq[j];
        pq[j] = temp;
        // 更新反向映射
        qp[pq[i]] = i;
        qp[pq[j]] = j;
    }
}