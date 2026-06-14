package exercise2.exercise2_4;

public class TestMaxHeap {
    public static void main(String[] args) {
        // 1. 通过数组构造堆
        Comparable[] arr = {3, 1, 4, 1, 5, 9};
        MaxHeap heap = new MaxHeap(arr);
        System.out.print("堆化后：");
        heap.printHeap(); // 输出：9 5 4 1 1 3 

        // 2. 手动创建堆后调用heapify方法
        MaxHeap heap2 = new MaxHeap(10);
        heap2.heapify(new Comparable[]{2, 7, 1, 8, 2});
        System.out.print("heapify后：");
        heap2.printHeap(); // 输出：8 7 1 2 2 
    }
}