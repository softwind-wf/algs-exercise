package exercise1.exercise1_3;

import java.util.Stack;

public class Buffer {
    private Stack<Character> left;  // 光标左侧的字符
    private Stack<Character> right; // 光标右侧的字符

    public Buffer() {
        left = new Stack<>();
        right = new Stack<>();
    }

    // 在光标位置插入字符 c
    public void insert(char c) {
        left.push(c);
    }

    // 删除并返回光标位置的字符（即 left 栈顶的字符）
    public char delete() {
        if (left.isEmpty()) {
            throw new IllegalStateException("光标左侧没有字符可删除");
        }
        return left.pop();
    }

    // 将光标向左移动 k 个位置
    public void left(int k) {
        for (int i = 0; i < k && !left.isEmpty(); i++) {
            right.push(left.pop());
        }
    }

    // 将光标向右移动 k 个位置
    public void right(int k) {
        for (int i = 0; i < k && !right.isEmpty(); i++) {
            left.push(right.pop());
        }
    }

    // 返回缓冲区中的字符数量
    public int size() {
        return left.size() + right.size();
    }

    // 辅助方法：打印缓冲区内容，用 '|' 表示光标位置
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // 先把 left 栈中的字符反转，再拼接
        for (Character c : left) {
            sb.append(c);
        }
        sb.append('|');
        // 再把 right 栈中的字符反转，再拼接
        Stack<Character> temp = new Stack<>();
        while (!right.isEmpty()) {
            temp.push(right.pop());
        }
        while (!temp.isEmpty()) {
            char c = temp.pop();
            sb.append(c);
            right.push(c);
        }
        return sb.toString();
    }


        public static void main(String[] args) {
            Buffer buffer = new Buffer();

            buffer.insert('H');
            buffer.insert('e');
            buffer.insert('l');
            buffer.insert('l');
            buffer.insert('o');
            System.out.println(buffer); // 输出: Hello|

            buffer.left(2);
            System.out.println(buffer); // 输出: Hel|lo

            buffer.insert('x');
            System.out.println(buffer); // 输出: Helx|lo

            buffer.delete();
            System.out.println(buffer); // 输出: Hel|lo

            buffer.right(2);
            System.out.println(buffer); // 输出: Hello|
        }






}