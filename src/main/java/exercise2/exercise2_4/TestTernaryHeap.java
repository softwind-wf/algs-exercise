package exercise2.exercise2_4;

public class TestTernaryHeap {
    public static void main(String[] args) {
        // 1. 从数组构造三叉堆
        Integer[] arr = {3, 1, 4, 1, 5, 9, 2, 6};
        TernaryMaxHeap<Integer> heap = new TernaryMaxHeap<>(arr);
        System.out.print("堆化后：");
        heap.printHeap(); // 输出：9 5 4 1 1 3 2 6

        // 2. 插入元素
        heap.insert(8);
        System.out.print("插入8后：");
        heap.printHeap(); // 输出：9 8 4 5 1 3 2 6 1

        // 3. 删除最大元素
        System.out.println("删除最大元素：" + heap.delMax());
        System.out.print("删除后堆：");
        heap.printHeap(); // 输出：8 6 4 5 1 3 2 1
    }
}