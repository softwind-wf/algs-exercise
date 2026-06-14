package exercise4.exercise4_1;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        OnePassSymbolGraph sg = new OnePassSymbolGraph("routes.txt", " ");

        System.out.println("顶点数：" + sg.V());
        System.out.println("边数：" + sg.E());

        // 查看 JFK 的邻接顶点
        int jfk = sg.index("JFK");
        System.out.println("JFK 的邻接顶点：");
        for (int w : sg.adj(jfk)) {
            System.out.println(sg.name(w));
        }
    }
}