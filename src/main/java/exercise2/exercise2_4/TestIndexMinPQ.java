package exercise2.exercise2_4;

public class TestIndexMinPQ {
    public static void main(String[] args) {
        // 创建最大容量为5的索引优先队列
        IndexMinPQ<String> pq = new IndexMinPQ<>(5);

        // 插入元素
        pq.insert(0, "A");
        pq.insert(1, "C");
        pq.insert(2, "B");
        pq.insert(3, "D");

        System.out.println("最小元素：" + pq.min()); // 输出：A
        System.out.println("最小元素索引：" + pq.minIndex()); // 输出：0

        // 修改索引2的元素为"Z"
        pq.change(2, "Z");
        System.out.println("修改后最小元素：" + pq.min()); // 输出：A

        // 删除最小元素
        int minIdx = pq.delMin();
        System.out.println("删除的最小元素索引：" + minIdx); // 输出：0
        System.out.println("删除后最小元素：" + pq.min()); // 输出：B

        // 判断索引0是否存在
        System.out.println("索引0是否存在：" + pq.contains(0)); // 输出：false
    }
}