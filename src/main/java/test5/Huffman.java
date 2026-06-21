package test5;

import edu.princeton.cs.algs4.BinaryStdIn;
import edu.princeton.cs.algs4.BinaryStdOut;
import edu.princeton.cs.algs4.MinPQ;

public class Huffman {
    // ASCII字符集总数
    private static final int R = 256;

    // ==================== 压缩方法 compress() ====================
    public static void compress() {
        // 1. 读取全部输入字符串
        String s = BinaryStdIn.readString();
        char[] input = s.toCharArray();

        // 2. 统计每个字符出现频率
        int[] freq = new int[R];
        for (int i = 0; i < input.length; i++) {
            freq[input[i]]++;
        }

        // 3. 根据频率构建霍夫曼树
        Node root = buildTrie(freq);

        // 4. 递归生成编码表 st[字符] = 01字符串
        String[] st = new String[R];
        buildCode(st, root, "");

        // 5. 前序遍历，把霍夫曼树写入比特流（解压时重建树）
        writeTrie(root);

        // 6. 写入原始字符总长度（解压时循环次数）
        BinaryStdOut.write(input.length);

        // 7. 遍历原文，查表写入对应编码比特
        for (int i = 0; i < input.length; i++) {
            String code = st[input[i]];
            for (int j = 0; j < code.length(); j++) {
                if (code.charAt(j) == '1') BinaryStdOut.write(true);
                else BinaryStdOut.write(false);
            }
        }
        BinaryStdOut.close();
    }

    // ==================== 解压方法 expand() ====================
    public static void expand() {
        // 1. 从比特流重建霍夫曼树
        Node root = readTrie();
        // 2. 读取原始文本字符总数
        int N = BinaryStdIn.readInt();
        // 3. 循环N次解码每个字符
        for (int i = 0; i < N; i++) {
            Node x = root;
            while (!x.isLeaf()) {
                // 0走左子树，1走右子树
                if (BinaryStdIn.readBoolean()) x = x.right;
                else x = x.left;
            }
            BinaryStdOut.write(x.ch);
        }
        BinaryStdOut.close();
    }

    // ==================== 建树 buildTrie：优先队列合并最小频率结点 ====================
    private static Node buildTrie(int[] freq) {
        MinPQ<Node> pq = new MinPQ<>();
        // 初始化所有存在的字符叶子结点
        for (char c = 0; c < R; c++) {
            if (freq[c] > 0) {
                pq.insert(new Node(c, freq[c], null, null));
            }
        }
        // 循环合并两棵最小频率树
        while (pq.size() > 1) {
            Node x = pq.delMin();
            Node y = pq.delMin();
            // 内部结点ch无意义，填'\0'，频率为两棵子树之和
            Node parent = new Node('\0', x.freq + y.freq, x, y);
            pq.insert(parent);
        }
        return pq.delMin();
    }

    // ==================== 递归生成编码表 buildCode ====================
    private static void buildCode(String[] st, Node x, String s) {
        // 到达叶子结点，记录当前路径编码
        if (x.isLeaf()) {
            st[x.ch] = s;
            return;
        }
        buildCode(st, x.left, s + '0');
        buildCode(st, x.right, s + '1');
    }

    // ==================== 前序遍历把树写入比特流 writeTrie ====================
    private static void writeTrie(Node x) {
        // 叶子结点：写1 + 字符8bit
        if (x.isLeaf()) {
            BinaryStdOut.write(true);
            BinaryStdOut.write(x.ch);
            return;
        }
        // 内部结点：写0，递归左右子树
        BinaryStdOut.write(false);
        writeTrie(x.left);
        writeTrie(x.right);
    }

    // ==================== 从比特流重建霍夫曼树 readTrie ====================
    private static Node readTrie() {
        // 读到1代表叶子结点，读取字符
        if (BinaryStdIn.readBoolean()) {
            return new Node(BinaryStdIn.readChar(), 0, null, null);
        }
        // 读到0代表内部结点，递归构建左右子树
        return new Node('\0', 0, readTrie(), readTrie());
    }

    // ==================== 程序入口 main ====================
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("使用说明：");
            System.out.println("压缩：java Huffman - 输入文本.txt 压缩文件.huf");
            System.out.println("解压：java Huffman + 压缩文件.huf 还原文本.txt");
            return;
        }
        String op = args[0];
        String inPath = args[1];
        String outPath = args[2];
        try {
            BinaryStdIn.setFile(inPath);
            BinaryStdOut.setFile(outPath);
            if ("-".equals(op)) {
                compress();
                System.out.println("霍夫曼压缩完成！");
            } else if ("+".equals(op)) {
                expand();
                System.out.println("霍夫曼解压完成！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            BinaryStdIn.close();
            BinaryStdOut.close();
        }
    }

    // 内部结点类 Node
    private static class Node implements Comparable<Node> {
        private char ch;
        private int freq;
        private final Node left, right;

        Node(char ch, int freq, Node left, Node right) {
            this.ch = ch;
            this.freq = freq;
            this.left = left;
            this.right = right;
        }

        public boolean isLeaf() {
            return left == null && right == null;
        }

        public int compareTo(Node that) {
            return this.freq - that.freq;
        }
    }
}