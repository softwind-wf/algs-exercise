package exercise1.exercise1_3;

import java.util.*;
import java.util.Deque;

public class InfixToPostfix {

    // 定义运算符优先级
    private static final Map<Character, Integer> precedence = new HashMap<>();
    static {
        precedence.put('+', 1);
        precedence.put('-', 1);
        precedence.put('*', 2);
        precedence.put('/', 2);
    }

    public static String infixToPostfix(String infixExpr) {
        Deque<Character> stack = new ArrayDeque<>(); // 使用Deque作为栈
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < infixExpr.length(); i++) {
            char token = infixExpr.charAt(i);

            if(token==' '){
                continue;
            }

            if (Character.isLetterOrDigit(token)) {
                // 处理操作数（字母或数字）
                output.append(token).append(' ');
            } else if (token == '(') {
                // 处理左括号
                stack.push(token);
            } else if (token == ')') {
                // 处理右括号
                while (!stack.isEmpty() && stack.peek() != '(') {
                    output.append(stack.pop()).append(' ');
                }
                stack.pop(); // 弹出左括号 '('
            } else {
                // 处理运算符
                while (!stack.isEmpty() && stack.peek() != '(' && precedence.get(stack.peek()) >= precedence.get(token)) {
                    output.append(stack.pop()).append(' ');
                }
                stack.push(token);
            }
        }

        // 弹出栈中剩余的所有运算符
        while (!stack.isEmpty()) {
            output.append(stack.pop()).append(' ');
        }

        return output.toString().trim(); // 去除末尾多余的空格
    }

    public static void main(String[] args) {
        String infix = "( ( 1 + 2 ) * ( ( 3 - 4 ) * ( 5 - 6 ) ) )";
        String postfix = infixToPostfix(infix);
        System.out.println("中序表达式: " + infix);
        System.out.println("后序表达式: " + postfix); // 输出: a b c * + d e / -
    }
}