package test5;

import edu.princeton.cs.algs4.MinPQ;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 *  Huffman 编码压缩/解压算法实现。
 *  使用 256 符号字母表（每个字节值 0-255），支持内存中 byte[] 的直接压缩与解压。
 *
 *  核心流程：
 *  1. 统计输入中每个字节的出现频率
 *  2. 用最小优先队列 (MinPQ) 构建 Huffman 树
 *  3. 生成前缀码表
 *  4. 压缩：输出 树结构 + 原始长度 + 编码后的比特流
 *  5. 解压：读取树结构 → 解码比特流 → 还原原始数据
 */
public class HuffmanCoding {

    private static final int R = 256;

    private HuffmanCoding() { }

    // Huffman 树节点
    private static class Node implements Comparable<Node> {
        private final int ch;
        private final int freq;
        private final Node left, right;

        Node(int ch, int freq, Node left, Node right) {
            this.ch = ch;
            this.freq = freq;
            this.left = left;
            this.right = right;
        }

        boolean isLeaf() {
            return left == null && right == null;
        }

        public int compareTo(Node that) {
            return this.freq - that.freq;
        }
    }

    /**
     * 压缩字节数组，返回压缩后的字节数组。
     */
    public static byte[] compress(byte[] input) {
        int[] freq = new int[R];
        for (byte b : input)
            freq[b & 0xFF]++;

        Node root = buildTrie(freq);
        String[] codeTable = new String[R];
        buildCode(codeTable, root, "");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BitWriter writer = new BitWriter(bos);

        writeTrie(writer, root);
        writer.writeInt(input.length);

        for (byte b : input) {
            String code = codeTable[b & 0xFF];
            for (int j = 0; j < code.length(); j++)
                writer.writeBit(code.charAt(j) == '1');
        }
        writer.flush();
        return bos.toByteArray();
    }

    /**
     * 解压字节数组，返回原始数据。
     */
    public static byte[] expand(byte[] compressed) {
        ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
        BitReader reader = new BitReader(bis);

        Node root = readTrie(reader);
        int length = reader.readInt();

        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            Node x = root;
            while (!x.isLeaf()) {
                if (reader.readBit()) x = x.right;
                else                 x = x.left;
            }
            result[i] = (byte) x.ch;
        }
        return result;
    }

    // 根据频率构建 Huffman 树
    private static Node buildTrie(int[] freq) {
        MinPQ<Node> pq = new MinPQ<Node>();
        for (int c = 0; c < R; c++)
            if (freq[c] > 0)
                pq.insert(new Node(c, freq[c], null, null));

        while (pq.size() > 1) {
            Node left = pq.delMin();
            Node right = pq.delMin();
            pq.insert(new Node(-1, left.freq + right.freq, left, right));
        }
        return pq.isEmpty() ? null : pq.delMin();
    }

    // 递归生成前缀码表
    private static void buildCode(String[] st, Node x, String s) {
        if (x.isLeaf()) {
            st[x.ch] = (s.isEmpty() ? "0" : s);
            return;
        }
        buildCode(st, x.left, s + '0');
        buildCode(st, x.right, s + '1');
    }

    // 前序遍历写入树结构：叶子写 1 + 8bit 字符，内部节点写 0
    private static void writeTrie(BitWriter writer, Node x) {
        if (x == null) return;
        if (x.isLeaf()) {
            writer.writeBit(true);
            writer.writeByte(x.ch);
        } else {
            writer.writeBit(false);
            writeTrie(writer, x.left);
            writeTrie(writer, x.right);
        }
    }

    // 按前序读取树结构
    private static Node readTrie(BitReader reader) {
        if (reader.readBit()) {
            return new Node(reader.readByte(), -1, null, null);
        } else {
            return new Node(-1, -1, readTrie(reader), readTrie(reader));
        }
    }

    // ==================== BitWriter ====================
    private static class BitWriter {
        private final ByteArrayOutputStream out;
        private int buffer;
        private int bitCount;

        BitWriter(ByteArrayOutputStream out) {
            this.out = out;
            this.buffer = 0;
            this.bitCount = 0;
        }

        void writeBit(boolean bit) {
            buffer = (buffer << 1) | (bit ? 1 : 0);
            bitCount++;
            if (bitCount == 8) {
                out.write(buffer);
                buffer = 0;
                bitCount = 0;
            }
        }

        void writeByte(int value) {
            for (int i = 7; i >= 0; i--)
                writeBit(((value >> i) & 1) == 1);
        }

        void writeInt(int value) {
            writeByte((value >> 24) & 0xFF);
            writeByte((value >> 16) & 0xFF);
            writeByte((value >> 8) & 0xFF);
            writeByte(value & 0xFF);
        }

        void flush() {
            if (bitCount > 0) {
                buffer = buffer << (8 - bitCount);
                out.write(buffer);
            }
        }
    }

    // ==================== BitReader ====================
    private static class BitReader {
        private final ByteArrayInputStream in;
        private int buffer;
        private int bitCount;

        BitReader(ByteArrayInputStream in) {
            this.in = in;
            this.buffer = 0;
            this.bitCount = 0;
        }

        boolean readBit() {
            if (bitCount == 0) {
                buffer = in.read();
                if (buffer == -1) throw new RuntimeException("Unexpected end of stream");
                bitCount = 8;
            }
            bitCount--;
            return ((buffer >> bitCount) & 1) == 1;
        }

        int readByte() {
            int value = 0;
            for (int i = 0; i < 8; i++)
                value = (value << 1) | (readBit() ? 1 : 0);
            return value;
        }

        int readInt() {
            return (readByte() << 24) | (readByte() << 16) | (readByte() << 8) | readByte();
        }
    }

    /**
     * 测试用例：对比短文本和长文本的压缩效果。
     * 短文本因树结构开销大，压缩后反而膨胀；长文本才能体现压缩优势。
     */
    public static void main(String[] args) {
        // --- 短文本：压缩反而膨胀 ---
        String shortText = "ABRACADABRA!HUFFMAN CODING IS ELEGANT AND EFFICIENT.";
        testCompress("短文本", shortText.getBytes());

        // --- 长文本：明显压缩 ---
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++)
            sb.append("ABRACADABRA!HUFFMAN CODING IS ELEGANT AND EFFICIENT. ");
        testCompress("长文本（重复200次）", sb.toString().getBytes());
    }

    private static void testCompress(String label, byte[] input) {
        byte[] compressed = compress(input);
        byte[] expanded = expand(compressed);
        String result = new String(expanded);
        String original = new String(input);
        double ratio = 100.0 * compressed.length / input.length;
        String arrow = ratio < 100.0 ? "↓ 压缩" : "↑ 膨胀";

        System.out.println("=== " + label + " ===");
        System.out.printf("原始:   %d bytes\n", input.length);
        System.out.printf("压缩后: %d bytes (%.1f%% %s)\n", compressed.length, ratio, arrow);
        System.out.println("验证:   " + (original.equals(result) ? "通过 ✓" : "失败 ✗"));
        System.out.println();

        if (!original.equals(result))
            throw new RuntimeException(label + " 解压验证失败!");
    }

}
