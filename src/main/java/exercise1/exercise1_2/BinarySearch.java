package exercise1.exercise1_2;

import edu.princeton.cs.algs4.Counter;
import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.StdIn;
import edu.princeton.cs.algs4.StdOut;

import java.util.Arrays;

public class BinarySearch {

    // 二分查找的核心方法，接收一个 Counter 对象用于计数
    public static int rank(int key, int[] a, Counter counter) {
        int lo = 0;
        int hi = a.length - 1;
        while (lo <= hi) {
            // 每次比较前，计数器加1
            counter.increment();
            int mid = lo + (hi - lo) / 2;
            if (key < a[mid]) {
                hi = mid - 1;
            } else if (key > a[mid]) {
                lo = mid + 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        // 从文件读取数据并排序
        int[] whitelist = In.readInts(args[0]);
        Arrays.sort(whitelist);

        // 创建一个 Counter 对象，命名为 "keys examined"
        Counter counter = new Counter("keys examined");

        // 处理输入中的每个键，进行查找
        while (!StdIn.isEmpty()) {
            int key = StdIn.readInt();
            if (rank(key, whitelist, counter) == -1) {
                StdOut.println(key);
            }
        }

        // 打印总共比较过的键的数量
        StdOut.println(counter);
    }
}