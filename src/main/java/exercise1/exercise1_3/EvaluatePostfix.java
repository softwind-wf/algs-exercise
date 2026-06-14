package exercise1.exercise1_3;

import java.util.*;
import java.util.Deque;

public class EvaluatePostfix {

    public static double evaluatePostfix(String postfixExpr) {
        Deque<Double> stack = new ArrayDeque<>();
        String[] tokens = postfixExpr.split("\\s+"); // 按任意空白字符分割

        for (String token : tokens) {
            if (token.isEmpty()) {
                continue; // 跳过空字符串
            }

            // 检查是否是运算符
            if (token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/")) {
                if (stack.size() < 2) {
                    throw new IllegalArgumentException("无效的后序表达式：运算符数量多于操作数。");
                }
                double rightOperand = stack.pop();
                double leftOperand = stack.pop();
                double result;

                switch (token) {
                    case "+":
                        result = leftOperand + rightOperand;
                        break;
                    case "-":
                        result = leftOperand - rightOperand;
                        break;
                    case "*":
                        result = leftOperand * rightOperand;
                        break;
                    case "/":
                        if (rightOperand == 0) {
                            throw new ArithmeticException("除数不能为零。");
                        }
                        result = leftOperand / rightOperand;
                        break;
                    default:
                        throw new IllegalArgumentException("未知的运算符: " + token);
                }
                stack.push(result);
            } else {
                // 是操作数，转换为 double 并压入栈
                try {
                    double number = Double.parseDouble(token);
                    stack.push(number);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("无效的操作数: " + token, e);
                }
            }
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException("无效的后序表达式：操作数数量多于运算符。");
        }

        return stack.pop();
    }

    public static void main(String[] args) {
        // 示例 1: 对应中序表达式 "a + b * c - d / e"，假设 a=2, b=3, c=4, d=10, e=2
        String postfix1 = "2 3 4 * + 10 2 / -";
        double result1 = evaluatePostfix(postfix1);
        System.out.println("后序表达式: " + postfix1);
        System.out.println("计算结果: " + result1); // 输出: 9.0

        // 示例 2: 对应中序表达式 "(a + b) * (c - d)"，假设 a=5, b=3, c=10, d=2
        String postfix2 = "5 3 + 10 2 - *";
        double result2 = evaluatePostfix(postfix2);
        System.out.println("\n后序表达式: " + postfix2);
        System.out.println("计算结果: " + result2); // 输出: 64.0
    }
}