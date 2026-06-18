package test5;

import edu.princeton.cs.algs4.Bag;
import edu.princeton.cs.algs4.Digraph;
import edu.princeton.cs.algs4.DirectedDFS;
import edu.princeton.cs.algs4.StdOut;

import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

public class NFA {
    private char[] re;
    private Digraph G;
    private int M;

    public NFA(String regexp) {
        re = regexp.toCharArray();
        M = re.length;
        G = new Digraph(M + 1);
        Stack<Integer> ops = new Stack<>();

        for (int i = 0; i < M; i++) {
            int lp = i; // 当前子表达式起始位置（用于闭包）

            if (re[i] == '(' || re[i] == '|') {
                ops.push(i);
            } else if (re[i] == ')') {
                // ========== 核心：正确处理多向 '|' ==========
                // 收集所有连续的 '|' 位置
                List<Integer> ors = new ArrayList<>();
                while (!ops.isEmpty() && re[ops.peek()] == '|') {
                    ors.add(ops.pop());
                }
                int left = ops.pop(); // 左括号位置

                // 第一个分支：左括号直接跳到其后的字符
                G.addEdge(left, left + 1);

                // 每个 '|' 分支：
                //   - 从左括号跳到该分支的起始字符（or+1）
                //   - 从该 '|' 跳到右括号（表示该分支结束）
                for (int or : ors) {
                    G.addEdge(left, or + 1);
                    G.addEdge(or, i);
                }

                lp = left; // 更新子表达式起始为左括号
                // ============================================
            }

            // ---------- 闭包 '*' 处理 ----------
            if (i < M - 1 && re[i + 1] == '*') {
                // lp 为当前子表达式的起始（字符或左括号）
                G.addEdge(lp, i + 1);   // 跳过子表达式（匹配零次）
                G.addEdge(i + 1, lp);   // 循环回到子表达式起始
            }

            // 为 '(' ')' '*' 添加 ε 边到下一状态（确保自动前进）
            if (re[i] == '(' || re[i] == ')' || re[i] == '*') {
                G.addEdge(i, i + 1);
            }
        }
    }

    // ---------- 模拟匹配（原版算法，无需修改） ----------
    public boolean recognizes(String txt) {
        Bag<Integer> pc = new Bag<>();
        DirectedDFS dfsInit = new DirectedDFS(G, 0);
        for (int v = 0; v < G.V(); v++)
            if (dfsInit.marked(v)) pc.add(v);

        for (int i = 0; i < txt.length(); i++) {
            Bag<Integer> match = new Bag<>();
            char c = txt.charAt(i);
            for (int v : pc) {
                if (v >= M) continue;
                if (re[v] == c || re[v] == '.')
                    match.add(v + 1);
            }
            pc = new Bag<>();
            DirectedDFS dfsMatch = new DirectedDFS(G, match);
            for (int v = 0; v < G.V(); v++)
                if (dfsMatch.marked(v)) pc.add(v);
        }

        for (int v : pc)
            if (v == M) return true;
        return false;
    }

    // ---------- 主测试入口 ----------
    public static void main(String[] args) {
        String pattern = "(.*AB((C|D|E)F)*G)";
        StdOut.println("=== 当前测试正则：" + pattern + " ===");
        NFA nfa = new NFA(pattern);

        StdOut.println("\n正则字符数组:");
        for (int i = 0; i < nfa.M; i++) {
            StdOut.printf("状态 %d: '%c'\n", i, nfa.re[i]);
        }

        StdOut.println("\nε转换图:");
        for (int v = 0; v < nfa.G.V(); v++) {
            StdOut.print("状态 " + v + " -> ");
            for (int w : nfa.G.adj(v)) {
                StdOut.print(w + " ");
            }
            StdOut.println();
        }

        String[] testCases = {
                "ABCDEFGHIJKLMNABEFEFEFEFEFEFEFEFG",
                "ABCDEFABCFCFCFCFCFG",
                "ABCABDFDFG",
                "XXABCFG",
                "ABDFDFG",
                "AABEEFG",
                "ABG",
                "ABCF",
                "AXBCFG",
                "ABXFG"
        };
        StdOut.println("\n===== 批量测试用例 =====");
        for (String s : testCases) {
            boolean res = nfa.recognizes(s);
            StdOut.printf("文本「%s」：%s%n", s, res ? "✅匹配成功" : "❌匹配失败");
        }
    }
}