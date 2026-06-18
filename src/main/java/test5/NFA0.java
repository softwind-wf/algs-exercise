package test5;

import edu.princeton.cs.algs4.*;

import java.util.Stack;

// ========== 核心NFA正则匹配类 ==========
public class NFA0
{
    private char[] re;              // 匹配转换
    private Digraph G;              // epsilon空转换有向图
    private int M;                 // 正则长度=状态总数

    public NFA0(String regexp)
    {
        Stack<Integer> ops = new Stack<>();
        re = regexp.toCharArray();
        M = re.length;
        G = new Digraph(M + 1); // 0~M 共M+1个状态

        for (int i = 0; i < M; i++)
        {
            int lp = i; // 记录左括号位置
            if (re[i] == '(' || re[i] == '|')
                ops.push(i);
            else if (re[i] == ')')
            {
                int or = ops.pop();
                // 处理 | 或分支
                if (re[or] == '|')
                {
                    lp = ops.pop();
                    G.addEdge(lp, or + 1); // 左括号指向或后分支
                    G.addEdge(or, i);      // 或位置指向右括号
                }
                else lp = or;
            }
            // 处理闭包 *
            if (i < M - 1 && re[i + 1] == '*')
            {
                G.addEdge(lp, i + 1);
                G.addEdge(i + 1, lp);
            }
            // ( * ) 都可以向后走空边
            if (re[i] == '(' || re[i] == '*' || re[i] == ')')
                G.addEdge(i, i + 1);
        }
    }

    /** NFA模拟，判断文本txt是否能被正则匹配 */
    public boolean recognizes(String txt)
    {
        Bag<Integer> pc = new Bag<>();
        DirectedDFS dfs = new DirectedDFS(G, 0);
        // 初始状态：0号状态能通过ε到达的所有点
        for (int v = 0; v < G.V(); v++)
            if (dfs.marked(v)) pc.add(v);

        for (int i = 0; i < txt.length(); i++)
        {
            Bag<Integer> match = new Bag<>();
            // 读取当前字符，寻找合法转移
            for (int v : pc)
            {
                if (v >= M) continue;
                if (re[v] == txt.charAt(i) || re[v] == '.')
                    match.add(v + 1);
            }
            // 对match集合做ε闭包，更新pc
            pc = new Bag<>();
            dfs = new DirectedDFS(G, match);
            for (int v = 0; v < G.V(); v++)
                if (dfs.marked(v)) pc.add(v);
        }
        // 终止状态M是否在可达集合中
        for (int v : pc)
            if (v == M) return true;
        return false;
    }
}

// ========== GREP 命令行工具主类 ==========
class GREP
{
    public static void main(String[] args)
    {
        if (args.length == 0) {
            StdOut.println("用法：java GREP 正则表达式");
            return;
        }
        // 包裹 .* 实现包含匹配，等价于grep功能
        String regexp = "(.*" + args[0] + ".*)";
        NFA0 nfa = new NFA0(regexp);
        StdOut.println("输入文本，匹配行将被打印，Ctrl+D结束输入：");
        while (StdIn.hasNextLine())
        {
            String txt = StdIn.readLine();
            if (nfa.recognizes(txt))
                StdOut.println("匹配行：" + txt);
        }
    }
}