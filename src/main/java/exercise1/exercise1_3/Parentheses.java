package exercise1.exercise1_3;

import edu.princeton.cs.algs4.Stack;
import edu.princeton.cs.algs4.StdIn;
import edu.princeton.cs.algs4.StdOut;

public class Parentheses {
    public static void main(String[] args) {
        Stack<Character> stack = new Stack<>();

        while (!StdIn.isEmpty()) {
            char c = StdIn.readChar();
            if (c == '(' || c == '[' || c == '{') {
                stack.push(c);
            } else if (c == ')') {
                if (stack.isEmpty() || stack.pop() != '(') {
                    StdOut.println("false");
                    return;
                }
            } else if (c == ']') {
                if (stack.isEmpty() || stack.pop() != '[') {
                    StdOut.println("false");
                    return;
                }
            } else if (c == '}') {
                if (stack.isEmpty() || stack.pop() != '{') {
                    StdOut.println("false");
                    return;
                }
            }
            // 忽略其他字符
        }

        // 所有字符处理完毕，检查栈是否为空
        StdOut.println(stack.isEmpty());
    }
}