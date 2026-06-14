package exercise1.exercise1_3;

public class LinkedStequeTest {
    public static void main(String[] args) {
        LinkedSteque<String> steque = new LinkedSteque<>();

        // 测试空栈情况
        System.out.println("初始是否为空: " + steque.isEmpty()); // 应该是 true
        System.out.println("初始大小: " + steque.size());       // 应该是 0

        // 测试 push 操作
        steque.push("A");
        steque.push("B");
        steque.push("C");
        System.out.println("push C, B, A 后大小: " + steque.size()); // 应该是 3

        // 测试 pop 操作
        System.out.println("弹出: " + steque.pop()); // C
        System.out.println("弹出: " + steque.pop()); // B
        System.out.println("弹出后大小: " + steque.size()); // 1

        // 测试 enqueue 操作
        steque.enqueue("D");
        steque.enqueue("E");
        System.out.println("enqueue D, E 后大小: " + steque.size()); // 3

        // 继续 pop 验证顺序
        System.out.println("弹出: " + steque.pop()); // A
        System.out.println("弹出: " + steque.pop()); // D
        System.out.println("弹出: " + steque.pop()); // E
        System.out.println("弹出后是否为空: " + steque.isEmpty()); // true

        // 测试空栈 pop 异常（可选，注释掉避免程序终止）
        // steque.pop();
    }
}