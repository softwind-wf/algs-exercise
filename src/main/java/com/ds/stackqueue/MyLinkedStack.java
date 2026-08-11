package com.ds.stackqueue;

import java.util.Objects;

/**
 * 基于链表的栈（Stack）实现 —— 后进先出（LIFO）
 *
 * 核心思想：
 * 栈是一种受限的线性表，只允许在表的一端（栈顶）进行插入和删除操作。
 * 最后进入栈的元素最先被取出，即"后进先出"（Last In First Out）。
 *
 * 链表实现栈的优势在于不需要提前指定容量，可以动态增长，
 * 且入栈/出栈操作始终为 O(1)。
 *
 * 生活中的类比：一摞盘子——你只能放最上面、拿最上面。
 *
 * @param <E> 元素类型
 */
public class MyLinkedStack<E> {

    // ==================== 节点内部类 ====================

    /**
     * 单向链表的节点，持有数据域和指向后继节点的引用
     */
    private static class Node<E> {
        E data;
        Node<E> next;

        Node(E data, Node<E> next) {
            this.data = data;
            this.next = next;
        }
    }

    // ==================== 成员变量 ====================

    private Node<E> top;   // 栈顶指针（链表的头节点）
    private int size;      // 栈中元素数量

    // ==================== 构造方法 ====================

    public MyLinkedStack() {
        top = null;
        size = 0;
    }

    // ==================== 基础查询 ====================

    /**
     * @return 栈中元素的数量
     */
    public int size() {
        return size;
    }

    /**
     * @return 栈是否为空
     */
    public boolean isEmpty() {
        return size == 0;
    }

    // ==================== 核心操作 ====================

    /**
     * 入栈 —— 将元素压入栈顶
     *
     * 时间复杂度：O(1)
     *
     * @param data 要入栈的元素
     */
    public void push(E data) {
        // 新节点指向原栈顶，成为新的栈顶
        Node<E> newNode = new Node<>(data, top);
        top = newNode;
        size++;
    }

    /**
     * 出栈 —— 移除并返回栈顶元素
     *
     * 时间复杂度：O(1)
     *
     * @return 栈顶元素，栈为空时返回 null
     */
    public E pop() {
        if (isEmpty()) {
            return null;
        }
        E data = top.data;
        top = top.next;  // 栈顶指针下移
        size--;
        return data;
    }

    /**
     * 查看栈顶元素但不移除
     *
     * 时间复杂度：O(1)
     *
     * @return 栈顶元素，栈为空时返回 null
     */
    public E peek() {
        if (isEmpty()) {
            return null;
        }
        return top.data;
    }

    // ==================== 批量操作 ====================

    /**
     * 批量入栈
     */
    @SuppressWarnings("unchecked")
    public void pushAll(E... items) {
        for (E item : items) {
            push(item);
        }
    }

    /**
     * 清空栈
     */
    public void clear() {
        // 断开所有引用，帮助 GC
        Node<E> current = top;
        while (current != null) {
            Node<E> next = current.next;
            current.next = null;
            current = next;
        }
        top = null;
        size = 0;
    }

    // ==================== 查询操作 ====================

    /**
     * 判断栈中是否包含指定元素
     * 从栈顶向下遍历
     */
    public boolean contains(E data) {
        Node<E> current = top;
        while (current != null) {
            if (Objects.equals(data, current.data)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    /**
     * 查找元素在栈中的深度（栈顶深度为 0，往下递增）
     * 不存在返回 -1
     */
    public int search(E data) {
        Node<E> current = top;
        int depth = 0;
        while (current != null) {
            if (Objects.equals(data, current.data)) {
                return depth;
            }
            current = current.next;
            depth++;
        }
        return -1;
    }

    // ==================== 转换操作 ====================

    /**
     * 将栈内容转换为数组（数组第 0 个元素为栈底，最后一个为栈顶）
     */
    @SuppressWarnings("unchecked")
    public E[] toArray() {
        Object[] result = new Object[size];
        Node<E> current = top;
        // 栈顶在链表头，栈底在链表尾，倒序填充
        for (int i = size - 1; i >= 0; i--) {
            result[i] = current.data;
            current = current.next;
        }
        return (E[]) result;
    }

    // ==================== 遍历操作 ====================

    /**
     * 从栈顶到栈底遍历并打印元素
     */
    public void traversal() {
        if (isEmpty()) {
            System.out.println("栈为空");
            return;
        }
        Node<E> current = top;
        int index = 0;
        while (current != null) {
            System.out.println("[" + index + "] " + current.data + (index == 0 ? " ← 栈顶" : ""));
            current = current.next;
            index++;
        }
    }

    /**
     * 从栈底到栈顶遍历（反转视角）
     */
    public void traversalReverse() {
        if (isEmpty()) {
            System.out.println("栈为空");
            return;
        }
        reversePrint(top);
    }

    /**
     * 递归实现逆序打印（栈底→栈顶）
     */
    private void reversePrint(Node<E> node) {
        if (node == null) {
            return;
        }
        reversePrint(node.next);
        System.out.print(node.data + " ");
    }

    // ==================== 实用工具 ====================

    /**
     * 安全的出栈 —— 如果栈为空，返回指定的默认值
     */
    public E popOrDefault(E defaultValue) {
        if (isEmpty()) {
            return defaultValue;
        }
        return pop();
    }

    /**
     * 安全的查看栈顶 —— 如果栈为空，返回指定的默认值
     */
    public E peekOrDefault(E defaultValue) {
        if (isEmpty()) {
            return defaultValue;
        }
        return top.data;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[] (空栈)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("栈顶 → ");
        Node<E> current = top;
        while (current != null) {
            sb.append("[").append(current.data).append("]");
            current = current.next;
            if (current != null) {
                sb.append("\n       ");
            }
        }
        sb.append(" → 栈底");
        return sb.toString();
    }

    // ==================== 表达式求值 ====================

    /**
     * 表达式求值（Dijkstra 双栈算法 —— 需完全括号化）
     *
     * 算法流程：
     * 使用两个栈 —— 操作数栈 vals 和 运算符栈 ops。
     * 从左到右扫描表达式：
     *   - 遇到 '(' ：忽略
     *   - 遇到运算符：推入 ops 栈
     *   - 遇到数字：推入 vals 栈
     *   - 遇到 ')' ：从 ops 弹出一个运算符，从 vals 弹出两个操作数，
     *                计算结果并推回 vals 栈
     *
     * 输入格式：每个运算必须用括号包裹，如 ((1+2)*(3+4))
     * 支持运算符：+ - * /
     * 支持小数：3.14, 2.5
     *
     * @param expression 完全括号化的算术表达式
     * @return 计算结果
     */
    public static double evaluate(String expression) {
        MyLinkedStack<Double> vals = new MyLinkedStack<>();  // 操作数栈
        MyLinkedStack<Character> ops = new MyLinkedStack<>(); // 运算符栈

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (c == ' ') {
                continue;  // 跳过空格
            } else if (c == '(') {
                // 左括号：忽略（标志着一次运算的开始）
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                ops.push(c);  // 运算符入栈
            } else if (c == ')') {
                // 右括号：执行一次运算
                char op = ops.pop();
                double v2 = vals.pop();  // 注意：第二个弹出的是右操作数
                double v1 = vals.pop();
                vals.push(compute(op, v1, v2));
            } else if (Character.isDigit(c) || c == '.') {
                // 解析数字（支持多位数和小数）
                int j = i;
                while (j < expression.length()
                        && (Character.isDigit(expression.charAt(j))
                         || expression.charAt(j) == '.')) {
                    j++;
                }
                double num = Double.parseDouble(expression.substring(i, j));
                vals.push(num);
                i = j - 1;  // 外层 for 会 i++，此处 -1 保证下次从数字结束位置之后开始
            }
        }

        return vals.pop();  // 最终 vals 栈中只剩结果
    }

    /**
     * 表达式求值（支持运算符优先级，无需完全括号化）
     *
     * 算法流程：
     * 在 Dijkstra 双栈算法的基础上增加运算符优先级处理。
     * 运算符优先级：* / 高于 + -
     * 当遇到新运算符时，先将 ops 栈中优先级 >= 当前运算符的全部弹出计算，
     * 再将当前运算符入栈。
     *
     * 输入格式：标准中缀表达式，如 1+2*3 → 7, (1+2)*3 → 9
     * 支持运算符：+ - * /
     * 支持括号改变优先级
     * 支持小数
     *
     * @param expression 标准中缀算术表达式
     * @return 计算结果
     */
    public static double evaluateWithPrecedence(String expression) {
        MyLinkedStack<Double> vals = new MyLinkedStack<>();
        MyLinkedStack<Character> ops = new MyLinkedStack<>();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (c == ' ') {
                continue;
            } else if (c == '(') {
                ops.push(c);
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                // 弹出并计算优先级 >= 当前运算符的所有运算符
                while (!ops.isEmpty() && ops.peek() != '('
                        && precedence(ops.peek()) >= precedence(c)) {
                    executeTop(ops, vals);
                }
                ops.push(c);
            } else if (c == ')') {
                // 弹出并计算直到遇到 '('
                while (!ops.isEmpty() && ops.peek() != '(') {
                    executeTop(ops, vals);
                }
                ops.pop();  // 弹出 '('
            } else if (Character.isDigit(c) || c == '.') {
                // 解析数字
                int j = i;
                while (j < expression.length()
                        && (Character.isDigit(expression.charAt(j))
                         || expression.charAt(j) == '.')) {
                    j++;
                }
                vals.push(Double.parseDouble(expression.substring(i, j)));
                i = j - 1;
            }
        }

        // 计算剩余的运算符
        while (!ops.isEmpty()) {
            executeTop(ops, vals);
        }

        return vals.pop();
    }

    /**
     * 执行 ops 栈顶的一次运算：
     * 弹出 1 个运算符和 2 个操作数，计算结果推回 vals
     */
    private static void executeTop(MyLinkedStack<Character> ops, MyLinkedStack<Double> vals) {
        char op = ops.pop();
        double v2 = vals.pop();
        double v1 = vals.pop();
        vals.push(compute(op, v1, v2));
    }

    /**
     * 执行一次算术运算
     */
    private static double compute(char op, double v1, double v2) {
        switch (op) {
            case '+': return v1 + v2;
            case '-': return v1 - v2;
            case '*': return v1 * v2;
            case '/':
                if (v2 == 0) {
                    throw new ArithmeticException("除数不能为 0");
                }
                return v1 / v2;
            default:
                throw new IllegalArgumentException("不支持的运算符: " + op);
        }
    }

    /**
     * 获取运算符优先级
     * * / 优先级为 2，+ - 优先级为 1
     */
    private static int precedence(char op) {
        if (op == '*' || op == '/') {
            return 2;
        }
        if (op == '+' || op == '-') {
            return 1;
        }
        return 0;
    }

    // ==================== 中缀转后缀 ====================

    /**
     * 中缀表达式转后缀表达式（逆波兰表示法）
     *
     * 例如: "1+2*3" → "1 2 3 * +"
     *       "(1+2)*3" → "1 2 + 3 *"
     *
     * @param infix 中缀表达式
     * @return 后缀表达式（空格分隔）
     */
    public static String infixToPostfix(String infix) {
        MyLinkedStack<Character> ops = new MyLinkedStack<>();
        StringBuilder postfix = new StringBuilder();

        for (int i = 0; i < infix.length(); i++) {
            char c = infix.charAt(i);

            if (c == ' ') {
                continue;
            } else if (Character.isDigit(c) || c == '.') {
                // 输出完整的数字
                int j = i;
                while (j < infix.length()
                        && (Character.isDigit(infix.charAt(j))
                         || infix.charAt(j) == '.')) {
                    j++;
                }
                postfix.append(infix, i, j).append(' ');
                i = j - 1;
            } else if (c == '(') {
                ops.push(c);
            } else if (c == ')') {
                while (!ops.isEmpty() && ops.peek() != '(') {
                    postfix.append(ops.pop()).append(' ');
                }
                ops.pop();  // 弹出 '('
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                while (!ops.isEmpty() && ops.peek() != '('
                        && precedence(ops.peek()) >= precedence(c)) {
                    postfix.append(ops.pop()).append(' ');
                }
                ops.push(c);
            }
        }

        // 输出剩余的运算符
        while (!ops.isEmpty()) {
            postfix.append(ops.pop()).append(' ');
        }

        return postfix.toString().trim();
    }

    /**
     * 计算后缀表达式（逆波兰表示法）
     *
     * 算法：扫描后缀表达式，遇数字则入栈，遇运算符则弹出两个操作数计算后入栈
     *
     * @param postfix 后缀表达式
     * @return 计算结果
     */
    public static double evaluatePostfix(String postfix) {
        MyLinkedStack<Double> vals = new MyLinkedStack<>();
        String[] tokens = postfix.split(" ");

        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            char firstChar = token.charAt(0);
            if (token.length() == 1 && (firstChar == '+' || firstChar == '-'
                    || firstChar == '*' || firstChar == '/')) {
                double v2 = vals.pop();
                double v1 = vals.pop();
                vals.push(compute(firstChar, v1, v2));
            } else {
                vals.push(Double.parseDouble(token));
            }
        }

        return vals.pop();
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        System.out.println("========== 栈测试 ==========\n");

        MyLinkedStack<Integer> stack = new MyLinkedStack<>();

        // 1. 入栈测试
        System.out.println("--- 1. 入栈测试 ---");
        stack.push(10);
        stack.push(20);
        stack.push(30);
        stack.push(40);
        stack.push(50);
        System.out.println("入栈 10, 20, 30, 40, 50");
        System.out.println("size: " + stack.size());
        System.out.println("栈内容:");
        stack.traversal();
        System.out.println();

        // 2. 查看栈顶
        System.out.println("--- 2. 查看栈顶 ---");
        System.out.println("peek(): " + stack.peek());
        System.out.println("peek 后 size(应不变): " + stack.size());
        System.out.println();

        // 3. 出栈测试
        System.out.println("--- 3. 出栈测试 ---");
        System.out.println("pop(): " + stack.pop());
        System.out.println("pop(): " + stack.pop());
        System.out.println("出栈 2 次后 size: " + stack.size());
        System.out.println("当前栈顶: " + stack.peek());
        System.out.println("栈内容:");
        stack.traversal();
        System.out.println();

        // 4. 逆序遍历
        System.out.println("--- 4. 逆序遍历（栈底→栈顶）---");
        stack.traversalReverse();
        System.out.println("\n");

        // 5. 搜索测试
        System.out.println("--- 5. 搜索测试 ---");
        System.out.println("search(30): 深度 = " + stack.search(30));
        System.out.println("search(40): 深度 = " + stack.search(40));
        System.out.println("search(10): 深度 = " + stack.search(10));
        System.out.println("search(99): " + stack.search(99));
        System.out.println("contains(30): " + stack.contains(30));
        System.out.println("contains(99): " + stack.contains(99));
        System.out.println();

        // 6. toString
        System.out.println("--- 6. toString ---");
        System.out.println(stack);
        System.out.println();

        // 7. 批量入栈
        System.out.println("--- 7. 批量入栈 ---");
        stack.pushAll(100, 200, 300);
        System.out.println("pushAll(100, 200, 300) 后 size: " + stack.size());
        System.out.println(stack);
        System.out.println();

        // 8. 到数组
        System.out.println("--- 8. 转换为数组 ---");
        Object[] arr = stack.toArray();
        System.out.print("数组 (栈底→栈顶): ");
        for (Object o : arr) {
            System.out.print(o + " ");
        }
        System.out.println("\n");

        // 9. 清空
        System.out.println("--- 9. 清空 ---");
        stack.clear();
        System.out.println("clear 后 isEmpty: " + stack.isEmpty());
        System.out.println("clear 后 size: " + stack.size());
        System.out.println(stack);
        System.out.println();

        // 10. 边界测试
        System.out.println("--- 10. 空栈边界测试 ---");
        MyLinkedStack<String> empty = new MyLinkedStack<>();
        System.out.println("空栈 peek(): " + empty.peek());
        System.out.println("空栈 pop(): " + empty.pop());
        System.out.println("空栈 contains(\"a\"): " + empty.contains("a"));
        System.out.println("空栈 search(\"a\"): " + empty.search("a"));
        System.out.println("popOrDefault(\"默认\"): " + empty.popOrDefault("默认"));
        System.out.println("peekOrDefault(\"默认\"): " + empty.peekOrDefault("默认"));
        empty.traversal();
        System.out.println();

        // 11. 经典应用：括号匹配
        System.out.println("--- 11. 栈应用：括号匹配 ---");
        System.out.println("()[]{} : " + isBalanced("()[]{}"));
        System.out.println("([{}]) : " + isBalanced("([{}])"));
        System.out.println("([)]   : " + isBalanced("([)]"));
        System.out.println("((())) : " + isBalanced("((()))"));
        System.out.println("(()     : " + isBalanced("(()"));
        System.out.println();

        // 12. 表达式求值 —— Dijkstra 双栈算法（完全括号化）
        System.out.println("--- 12. Dijkstra 双栈算法（完全括号化）---");
        System.out.println("((1+2)*((3-4)*(5-6))) = " + evaluate("((1+2)*((3-4)*(5-6)))"));
        System.out.println("((1+2)*(3+4)) = " + evaluate("((1+2)*(3+4))"));
        System.out.println("((5*(3+2))/2) = " + evaluate("((5*(3+2))/2)"));
        System.out.println("(1+(2+(3+(4+5)))) = " + evaluate("(1+(2+(3+(4+5))))"));
        System.out.println("(10.5 + 20.5 无括号 = " + evaluate("(10.5+20.5)"));
        System.out.println("((2.5*4)/2) = " + evaluate("((2.5*4)/2)"));
        System.out.println();

        // 13. 表达式求值 —— 运算符优先级（无需完全括号化）
        System.out.println("--- 13. 运算符优先级求值 ---");
        System.out.println("1+2*3     = " + evaluateWithPrecedence("1+2*3"));
        System.out.println("(1+2)*3   = " + evaluateWithPrecedence("(1+2)*3"));
        System.out.println("3+4*5-6/2 = " + evaluateWithPrecedence("3+4*5-6/2"));
        System.out.println("2*3+4*5   = " + evaluateWithPrecedence("2*3+4*5"));
        System.out.println("10/2+3*4  = " + evaluateWithPrecedence("10/2+3*4"));
        System.out.println("(3+4)*(5-2)/3 = " + evaluateWithPrecedence("(3+4)*(5-2)/3"));
        System.out.println("1.5+2.5*2 = " + evaluateWithPrecedence("1.5+2.5*2"));
        System.out.println();

        // 14. 中缀转后缀
        System.out.println("--- 14. 中缀 → 后缀（逆波兰）---");
        String[] infixes = {"1+2*3", "(1+2)*3", "3+4*5-6/2", "(3+4)*(5-2)/3"};
        for (String infix : infixes) {
            String postfix = infixToPostfix(infix);
            System.out.println("中缀: " + infix);
            System.out.println("后缀: " + postfix);
            System.out.println("后缀求值: " + evaluatePostfix(postfix));
            System.out.println();
        }

        // 15. 非法输入测试
        System.out.println("--- 15. 异常测试 ---");
        try {
            evaluateWithPrecedence("1/0");
        } catch (ArithmeticException e) {
            System.out.println("预期异常: " + e.getMessage());
        }

        System.out.println();
        System.out.println("========== 测试完成 ==========");
    }

    /**
     * 栈的经典应用 —— 判断括号是否匹配
     * 遍历字符串，遇左括号入栈，遇右括号则检查栈顶是否匹配
     */
    public static boolean isBalanced(String expression) {
        MyLinkedStack<Character> stack = new MyLinkedStack<>();
        for (char c : expression.toCharArray()) {
            if (c == '(' || c == '[' || c == '{') {
                stack.push(c);  // 左括号入栈
            } else if (c == ')' || c == ']' || c == '}') {
                if (stack.isEmpty()) {
                    return false;  // 右括号多余
                }
                char top = stack.pop();
                if (!matches(top, c)) {
                    return false;  // 括号类型不匹配
                }
            }
        }
        return stack.isEmpty();  // 栈为空说明全部匹配
    }

    private static boolean matches(char open, char close) {
        return (open == '(' && close == ')')
            || (open == '[' && close == ']')
            || (open == '{' && close == '}');
    }
}
