package test4;


import java.util.Stack;

public class PathsTest {
    public static void main(String[] args) {
        Graph G = new Graph(6);
        G.addEdge(0, 1);
        G.addEdge(0, 2);
        G.addEdge(1, 2);
        G.addEdge(2, 3);
        G.addEdge(3, 4);
        G.addEdge(3, 5);

        int s = 0;
        BreadthFirstPaths search = new BreadthFirstPaths(G, s);

        for (int v = 0; v < G.V(); v++) {
            System.out.print(s + " to " + v + ": ");
            if (search.hasPathTo(v)) {
                // ✅ 正确写法：先拿栈，再逆序弹出（或者正确遍历）
                Stack<Integer> path = (Stack<Integer>) search.pathTo(v);
                while (!path.isEmpty()) {
                    if (path.size() == ((Stack<Integer>)search.pathTo(v)).size()) {
                        System.out.print(path.pop()); // 起点不加横杠
                    } else {
                        System.out.print("-" + path.pop());
                    }
                }
            } else {
                System.out.print("no path");
            }
            System.out.println();
        }
    }
}