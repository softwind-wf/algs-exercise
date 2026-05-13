package exercise1.exercise1_3;

public class Main {
    public static void main(String[] args) {
        // 测试数组实现
        ArrayGeneralizedQueue<String> aq = new ArrayGeneralizedQueue<>();
        aq.insert("A");
        aq.insert("B");
        aq.insert("C");
        System.out.println(aq.delete(2)); // 输出 B

        // 测试链表实现
        LinkedGeneralizedQueue<Integer> lq = new LinkedGeneralizedQueue<>();
        lq.insert(1);
        lq.insert(2);
        lq.insert(3);
        System.out.println(lq.delete(1)); // 输出 1
    }
}