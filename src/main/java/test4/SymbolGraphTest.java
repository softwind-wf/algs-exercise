package test4;

import java.util.Scanner;

public class SymbolGraphTest {
    public static void main(String[] args) {
        // 用法：java SymbolGraphTest <filename> <delimiter>
//        if (args.length < 2) {
//            System.out.println("用法：java SymbolGraphTest <文件名> <分隔符>");
//            return;
//        }

        String filename = "routes.txt";
        String delim = " ";
        SymbolGraph sg = new SymbolGraph(filename, delim);
        Graph G = sg.G();

        Scanner sc = new Scanner(System.in);
        System.out.println("输入顶点名（输入 exit 退出）：");
        while (sc.hasNextLine()) {
            String source = sc.nextLine().trim();
            if ("exit".equalsIgnoreCase(source)) break;

            if (!sg.contains(source)) {
                System.out.println("顶点不存在：" + source);
                continue;
            }

            int v = sg.index(source);
            System.out.println("与 " + source + " 相邻的顶点：");
            for (int w : G.adj(v)) {
                System.out.println("  " + sg.name(w));
            }
        }
        sc.close();
    }
}