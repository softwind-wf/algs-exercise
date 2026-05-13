package exercise2.exercise2_4;

public class TernaryMaxHeap<T extends Comparable<T>> {
    private T[] heap;
    private int size;
    private static final int DEFAULT_CAPACITY = 10;

    // 空堆构造
    @SuppressWarnings("unchecked")
    public TernaryMaxHeap() {
        heap = (T[]) new Comparable[DEFAULT_CAPACITY + 1];
        size = 0;
    }

    // 从数组构造并堆化
    @SuppressWarnings("unchecked")
    public TernaryMaxHeap(T[] array) {
        if (array == null) throw new IllegalArgumentException("Array cannot be null");
        heap = (T[]) new Comparable[array.length + 1];
        size = array.length;
        System.arraycopy(array, 0, heap, 1, array.length);
        // 三叉堆化：从最后一个非叶子节点开始下沉
        for (int k = size / 3; k >= 1; k--) {
            sink(k);
        }
    }

    // ==================== 核心 API ====================

    // 插入元素
    public void insert(T key) {
        if (key == null) throw new IllegalArgumentException("Key cannot be null");
        if (size == heap.length - 1) resize(2 * heap.length);
        heap[++size] = key;
        swim(size);
    }

    // 删除并返回最大元素
    public T delMax() {
        if (isEmpty()) throw new IllegalStateException("Heap is empty");
        T max = heap[1];
        swap(1, size--);
        sink(1);
        heap[size + 1] = null; // 避免内存泄漏
        if (size > 0 && size == (heap.length - 1) / 4) resize(heap.length / 2);
        return max;
    }

    // 判空
    public boolean isEmpty() {
        return size == 0;
    }

    // 获取堆大小
    public int size() {
        return size;
    }

    // 查看堆顶
    public T max() {
        if (isEmpty()) throw new IllegalStateException("Heap is empty");
        return heap[1];
    }

    // ==================== 辅助方法 ====================

    // 上浮：维护三叉堆性质
    private void swim(int k) {
        while (k > 1) {
            int parent = (k + 1) / 3; // 三叉父节点公式
            if (!less(parent, k)) break;
            swap(parent, k);
            k = parent;
        }
    }

    // 下沉：维护三叉堆性质
    private void sink(int k) {
        while (3 * k - 1 <= size) { // 第一个子节点存在
            int firstChild = 3 * k - 1;
            int maxChild = firstChild;

            // 比较三个子节点，找到最大的那个
            if (firstChild + 1 <= size && less(maxChild, firstChild + 1)) {
                maxChild = firstChild + 1;
            }
            if (firstChild + 2 <= size && less(maxChild, firstChild + 2)) {
                maxChild = firstChild + 2;
            }

            // 如果当前节点 >= 最大子节点，无需下沉
            if (!less(k, maxChild)) break;

            swap(k, maxChild);
            k = maxChild;
        }
    }

    // 比较：heap[i] < heap[j]
    private boolean less(int i, int j) {
        return heap[i].compareTo(heap[j]) < 0;
    }

    // 交换
    private void swap(int i, int j) {
        T temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }

    // 扩容
    @SuppressWarnings("unchecked")
    private void resize(int capacity) {
        T[] newHeap = (T[]) new Comparable[capacity];
        System.arraycopy(heap, 1, newHeap, 1, size);
        heap = newHeap;
    }

    // 打印堆（调试用）
    public void printHeap() {
        for (int i = 1; i <= size; i++) {
            System.out.print(heap[i] + " ");
        }
        System.out.println();
    }
}