package exercise1.exercise1_3;

import edu.princeton.cs.algs4.Stack;
import edu.princeton.cs.algs4.StdIn;
import edu.princeton.cs.algs4.StdOut;

public class Infix {
    public static void main(String[] args) {
        Stack<String> ops = new Stack<>();
        Stack<String> vals = new Stack<>();

        while (!StdIn.isEmpty()) {
            String s = StdIn.readString();
            if      (s.equals("("))               ; // 忽略左括号，题目输入中没有
            else if (s.equals("+") || s.equals("-") || s.equals("*") || s.equals("/"))
                ops.push(s);
            else if (s.equals(")")) {
                String op = ops.pop();
                String val1 = vals.pop();
                String val2 = vals.pop();
                vals.push("(" + val2 + " " + op + " " + val1 + ")");
            }
            else vals.push(s);
        }
        StdOut.println(vals.pop());
    }
}