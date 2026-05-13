package test;

import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.StdIn;
import edu.princeton.cs.algs4.StdOut;

public class Whitelist {
    public static void main(String[] args) {
        // 从命令行参数获取白名单文件路径
        int[] whitelist = In.readInts(args[0]);
        StaticSETofInts set = new StaticSETofInts(whitelist);

        // 从标准输入读取整数，输出不在白名单中的数
        while (!StdIn.isEmpty()) {
            int key = StdIn.readInt();
            if (!set.contains(key)) {
                StdOut.println(key);
            }
        }
    }
}