package test4;

import edu.princeton.cs.algs4.StdOut;
import edu.princeton.cs.algs4.SymbolDigraph;
import edu.princeton.cs.algs4.Digraph;
import edu.princeton.cs.algs4.DirectedCycle;
import edu.princeton.cs.algs4.DepthFirstOrder;

// 算法4.5 拓扑排序
public class Topological {
    private Iterable<Integer> order;  // 顶点的拓扑顺序

    public Topological(Digraph G) {
        DirectedCycle cyclefinder = new DirectedCycle(G);
        if (!cyclefinder.hasCycle()) {
            DepthFirstOrder dfs = new DepthFirstOrder(G);
            order = dfs.reversePost();
        }
    }

    public Iterable<Integer> order() { return order; }
    public boolean isDAG() { return order != null; }

    public static void main(String[] args) {
        String filename = args[0];
        String separator = args[1];
        SymbolDigraph sg = new SymbolDigraph(filename, separator);
        Topological top = new Topological(sg.G());
        for (int v : top.order())
            StdOut.println(sg.name(v));
    }
}
