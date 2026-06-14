package exercise2.exercise2_4;

public class MaxHeap {
    private final Object[] heap;
    private int size;

    // 原构造方法：指定容量初始化空堆
    public MaxHeap(int capacity) {
        heap = new Object[capacity + 1]; // 堆从索引1开始，容量+1
        size = 0;
    }

    // 新增构造方法：传入数组直接堆化
    public MaxHeap(Comparable[] array) {
        if (array == null) throw new IllegalArgumentException("Array cannot be null");
        // 堆数组长度 = 原数组长度 + 1（堆从索引1开始）
        heap = new Object[array.length + 1];
        size = array.length;
        // 把原数组元素复制到堆数组（从索引1开始）
        System.arraycopy(array, 0, heap, 1, array.length);
        // 堆化：从最后一个非叶子节点向前遍历，执行下沉操作
        for (int k = size / 2; k >= 1; k--) {
            sink(k);
        }
    }

    // ==================== 核心 API ====================
    public void insert(Comparable key) {
        if (key == null) throw new IllegalArgumentException("Key cannot be null");
        heap[++size] = key;
        swim(size);
    }

    public Comparable delMax() {
        if (isEmpty()) throw new NullPointerException("Heap underflow");
        Comparable max = (Comparable) heap[1];
        exch(1, size--);
        sink(1);
        heap[size + 1] = null; // 避免内存泄漏
        return max;
    }

    // 新增：堆化方法（支持外部传入数组堆化，覆盖当前堆）
    public void heapify(Comparable[] array) {
        if (array == null) throw new IllegalArgumentException("Array cannot be null");
        if (array.length > heap.length - 1) {
            throw new IllegalArgumentException("Array size exceeds heap capacity");
        }
        // 重置堆大小
        size = array.length;
        // 复制数组到堆（从索引1开始）
        System.arraycopy(array, 0, heap, 1, array.length);
        // 堆化核心逻辑
        for (int k = size / 2; k >= 1; k--) {
            sink(k);
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    // ==================== 上浮 ====================
    private void swim(int k) {
        while (k > 1 && less(k / 2, k)) {
            exch(k / 2, k);
            k = k / 2;
        }
    }

    // ==================== 下沉 ====================
    private void sink(int k) {
        while (2 * k <= size) {
            int child = 2 * k;
            // 找到子节点中较大的那个
            if (child < size && less(child, child + 1)) {
                child++;
            }
            // 如果当前节点 >= 子节点，无需下沉
            if (!less(k, child)) {
                break;
            }
            // 交换当前节点和子节点
            exch(k, child);
            k = child;
        }
    }

    // ==================== 工具方法 ====================
    private boolean less(int i, int j) {
        Comparable a = (Comparable) heap[i];
        Comparable b = (Comparable) heap[j];
        return a.compareTo(b) < 0;
    }

    private void exch(int i, int j) {
        Object temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }

    // 测试用：打印堆内容（从索引1开始）
    public void printHeap() {
        for (int i = 1; i <= size; i++) {
            System.out.print(heap[i] + " ");
        }
        System.out.println();
    }
}