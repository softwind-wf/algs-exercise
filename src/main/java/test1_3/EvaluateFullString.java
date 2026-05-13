package test1_3;

import java.util.Stack;

public class EvaluateFullString {
    public static void main(String[] args) {
        // 测试带优先级的表达式
        String expression = "1 + (2 + 3) * (4 * 5) - sqrt(16)";
        double result = evaluate(expression);
        System.out.println(result); // 输出 97.0
    }

    // 获取运算符优先级
    private static int getPriority(String op) {
        switch (op) {
            case "sqrt":
                return 3;
            case "*":
            case "/":
                return 2;
            case "+":
            case "-":
                return 1;
            default:
                return 0;
        }
    }

    public static double evaluate(String expression) {
        // 去掉所有空格
        String expr = expression.replaceAll("\\s+", "");
        Stack<String> ops = new Stack<>();
        Stack<Double> vals = new Stack<>();
        int n = expr.length();
        int i = 0;

        while (i < n) {
            char c = expr.charAt(i);
            if (c == '(') {
                ops.push("(");
                i++;
            } else if (c == ')') {
                // 遇到右括号，一直计算到左括号
                while (!ops.peek().equals("(")) {
                    String op = ops.pop();
                    double v = vals.pop();
                    switch (op) {
                        case "+": v = vals.pop() + v; break;
                        case "-": v = vals.pop() - v; break;
                        case "*": v = vals.pop() * v; break;
                        case "/": v = vals.pop() / v; break;
                        case "sqrt": v = Math.sqrt(v); break;
                    }
                    vals.push(v);
                }
                ops.pop(); // 弹出左括号
                i++;
            } else if (c == 's') {
                // 识别 sqrt
                ops.push("sqrt");
                i += 4;
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                // 处理运算符优先级
                String currentOp = String.valueOf(c);
                while (!ops.isEmpty() && !ops.peek().equals("(")
                        && getPriority(ops.peek()) >= getPriority(currentOp)) {
                    String op = ops.pop();
                    double v = vals.pop();
                    switch (op) {
                        case "+": v = vals.pop() + v; break;
                        case "-": v = vals.pop() - v; break;
                        case "*": v = vals.pop() * v; break;
                        case "/": v = vals.pop() / v; break;
                    }
                    vals.push(v);
                }
                ops.push(currentOp);
                i++;
            } else if (Character.isDigit(c) || c == '.') {
                // 提取完整数字
                int j = i;
                while (j < n && (Character.isDigit(expr.charAt(j)) || expr.charAt(j) == '.')) {
                    j++;
                }
                double num = Double.parseDouble(expr.substring(i, j));
                vals.push(num);
                i = j;
            } else {
                throw new IllegalArgumentException("非法字符: " + c);
            }
        }

        // 处理栈中剩余的运算符
        while (!ops.isEmpty()) {
            String op = ops.pop();
            double v = vals.pop();
            switch (op) {
                case "+": v = vals.pop() + v; break;
                case "-": v = vals.pop() - v; break;
                case "*": v = vals.pop() * v; break;
                case "/": v = vals.pop() / v; break;
                case "sqrt": v = Math.sqrt(v); break;
            }
            vals.push(v);
        }

        return vals.pop();
    }
}