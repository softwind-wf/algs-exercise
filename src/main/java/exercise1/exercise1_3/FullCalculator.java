package exercise1.exercise1_3;

import java.util.*;
import java.util.Deque;

public class FullCalculator {

    private static final Map<Character, Integer> precedence = new HashMap<>();
    static {
        precedence.put('+', 1);
        precedence.put('-', 1);
        precedence.put('*', 2);
        precedence.put('/', 2);
    }

    // ===== 1. 修复版：中缀转后缀（支持多位数、小数） =====
    public static String infixToPostfix(String infixExpr) {
        java.util.Deque<Character> stack = new ArrayDeque<>();
        StringBuilder output = new StringBuilder();
        int i = 0;

        while (i < infixExpr.length()) {
            char c = infixExpr.charAt(i);

            // 跳过空格
            if (c == ' ') {
                i++;
                continue;
            }

            // 【核心修复】如果是数字或小数点，读取整个完整的数字
            if (Character.isDigit(c) || c == '.') {
                StringBuilder number = new StringBuilder();
                // 一直读，直到不是数字/小数点为止
                while (i < infixExpr.length() && (Character.isDigit(infixExpr.charAt(i)) || infixExpr.charAt(i) == '.')) {
                    number.append(infixExpr.charAt(i));
                    i++;
                }
                output.append(number).append(' '); // 把完整数字（如10）加入输出
            }
            // 处理左括号
            else if (c == '(') {
                stack.push(c);
                i++;
            }
            // 处理右括号
            else if (c == ')') {
                while (!stack.isEmpty() && stack.peek() != '(') {
                    output.append(stack.pop()).append(' ');
                }
                stack.pop(); // 弹出左括号
                i++;
            }
            // 处理运算符
            else {
                while (!stack.isEmpty() && stack.peek() != '(' && precedence.get(stack.peek()) >= precedence.get(c)) {
                    output.append(stack.pop()).append(' ');
                }
                stack.push(c);
                i++;
            }
        }

        // 弹出剩余运算符
        while (!stack.isEmpty()) {
            output.append(stack.pop()).append(' ');
        }

        return output.toString().trim();
    }

    // ===== 2. 后缀表达式求值（不变，因为它本来就支持多位数） =====
    public static double evaluatePostfix(String postfixExpr) {
        Deque<Double> stack = new ArrayDeque<>();
        String[] tokens = postfixExpr.trim().split("\\s+");

        for (String token : tokens) {
            if (isOperator(token)) {
                double right = stack.pop();
                double left = stack.pop();
                switch (token) {
                    case "+": stack.push(left + right); break;
                    case "-": stack.push(left - right); break;
                    case "*": stack.push(left * right); break;
                    case "/": stack.push(left / right); break;
                }
            } else {
                stack.push(Double.parseDouble(token));
            }
        }
        return stack.pop();
    }

    private static boolean isOperator(String token) {
        return token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/");
    }

    // ===== 3. 总入口 =====
    public static double calculate(String infixExpr) {
        String postfix = infixToPostfix(infixExpr);
        System.out.println("后缀表达式：" + postfix); // 打印看看对不对
        return evaluatePostfix(postfix);
    }

    public static void main(String[] args) {
        String infix = "2 + 3 * 4 - 10 / 2";
        System.out.println("中缀表达式：" + infix);
        
        double result = calculate(infix);
        System.out.println("计算结果：" + result);
    }
}