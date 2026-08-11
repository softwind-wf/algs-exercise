package cn.exercise.algs4.datastructure.huffman;

import cn.exercise.algs4.datastructure.heap.MyPriorityQueue;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * 哈夫曼编码 —— 基于字符频率构建最优前缀码，实现文本的无损压缩与译码
 *
 * <p>核心步骤：</p>
 * <ol>
 *   <li>统计字符出现频率</li>
 *   <li>用优先队列构建哈夫曼树（每次合并频率最小的两节点）</li>
 *   <li>从根节点 DFS 遍历，生成每个字符的二进制编码</li>
 *   <li>编码：将文本替换为二进制字符串，压缩为字节后写入磁盘</li>
 *   <li>译码：从磁盘读取字节，还原二进制字符串，按位遍历哈夫曼树译码</li>
 * </ol>
 *
 * <p>文件格式（.huf）：</p>
 * <pre>
 * ┌──────────────────────────────────────────┐
 * │ Magic: "HF" (2 bytes)                    │
 * ├──────────────────────────────────────────┤
 * │ Tree data length: int (4 bytes)          │
 * ├──────────────────────────────────────────┤
 * │ Tree serialization (preorder):           │
 * │   0=内部节点, 1=叶子+char(2B), 2=null  │
 * ├──────────────────────────────────────────┤
 * │ Padding bits: byte (0-7)                 │
 * ├──────────────────────────────────────────┤
 * │ Encoded data: byte[]                     │
 * └──────────────────────────────────────────┘
 * </pre>
 */
public class HuffmanCoding {

    // ====================== 哈夫曼树节点 ======================

    public static class HuffmanNode implements Comparable<HuffmanNode> {
        char ch;               // 字符（内部节点用 '\0'）
        int freq;              // 出现频率（反序列化后为 0，仅编译时需要）
        HuffmanNode left;      // 左子（编码 0）
        HuffmanNode right;     // 右子（编码 1）

        public HuffmanNode(char ch, int freq, HuffmanNode left, HuffmanNode right) {
            this.ch = ch;
            this.freq = freq;
            this.left = left;
            this.right = right;
        }

        public boolean isLeaf() {
            return left == null && right == null;
        }

        @Override
        public int compareTo(HuffmanNode o) {
            return Integer.compare(this.freq, o.freq);
        }

        @Override
        public String toString() {
            return (ch == '\0' ? "●" : "'" + ch + "'") + ":" + freq;
        }
    }

    // ====================== 频率统计 ======================

    /**
     * 统计文本中各字符的出现频率
     */
    public static Map<Character, Integer> getFrequencyMap(String text) {
        if (text == null) {
            return Collections.emptyMap();
        }
        Map<Character, Integer> freqMap = new HashMap<>();
        for (char ch : text.toCharArray()) {
            freqMap.put(ch, freqMap.getOrDefault(ch, 0) + 1);
        }
        return freqMap;
    }

    /**
     * 统计字节流中各字符的出现频率（支持任意编码，按 UTF-8 解码为 String 再统计）
     */
    public static Map<Character, Integer> getFrequencyMap(byte[] data) {
        return getFrequencyMap(new String(data, StandardCharsets.UTF_8));
    }

    // ====================== 构建哈夫曼树 ======================

    /**
     * 根据字符频率表构建哈夫曼树
     *
     * @param freqMap 字符→频率映射
     * @return 哈夫曼树根节点
     */
    public static HuffmanNode buildTree(Map<Character, Integer> freqMap) {
        if (freqMap == null || freqMap.isEmpty()) {
            return null;
        }

        // 将每个字符初始化为独立节点，加入优先队列（小顶堆）
        MyPriorityQueue<HuffmanNode> pq = new MyPriorityQueue<>();
        for (Map.Entry<Character, Integer> entry : freqMap.entrySet()) {
            pq.insert(new HuffmanNode(entry.getKey(), entry.getValue(), null, null));
        }

        // 边界情况：只有一个字符时，创建一个虚拟根节点
        if (pq.size() == 1) {
            HuffmanNode single = pq.delTop();
            return new HuffmanNode('\0', single.freq, single, null);
        }

        // 重复合并最小的两棵树，直到只剩一棵
        while (pq.size() > 1) {
            HuffmanNode left = pq.delTop();
            HuffmanNode right = pq.delTop();
            HuffmanNode parent = new HuffmanNode('\0', left.freq + right.freq, left, right);
            pq.insert(parent);
        }

        return pq.delTop();
    }

    // ====================== 生成编码表 ======================

    /**
     * 从哈夫曼树生成每个字符的二进制编码（左=0，右=1）
     *
     * @param root 哈夫曼树根
     * @return 字符→编码字符串的映射
     */
    public static Map<Character, String> generateCodes(HuffmanNode root) {
        Map<Character, String> codes = new HashMap<>();
        if (root != null) {
            generateCodesRecursive(root, "", codes);
        }
        return codes;
    }

    private static void generateCodesRecursive(HuffmanNode node, String code,
                                                Map<Character, String> codes) {
        if (node.isLeaf()) {
            codes.put(node.ch, code);
            return;
        }
        if (node.left != null) {
            generateCodesRecursive(node.left, code + "0", codes);
        }
        if (node.right != null) {
            generateCodesRecursive(node.right, code + "1", codes);
        }
    }

    // ====================== 编码 ======================

    /**
     * 将明文编码为二进制字符串
     *
     * @param text  明文
     * @param codes 编码表
     * @return 二进制编码字符串
     */
    public static String encode(String text, Map<Character, String> codes) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char ch : text.toCharArray()) {
            String code = codes.get(ch);
            if (code == null) {
                throw new IllegalArgumentException("字符 '" + ch + "' 不在编码表中");
            }
            sb.append(code);
        }
        return sb.toString();
    }

    // ====================== 译码 ======================

    /**
     * 将二进制编码字符串译码还原为明文
     *
     * @param encoded 二进制编码串
     * @param root    哈夫曼树根
     * @return 译码后的明文
     */
    public static String decode(String encoded, HuffmanNode root) {
        if (root == null || encoded == null || encoded.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        HuffmanNode current = root;

        for (int i = 0; i < encoded.length(); i++) {
            char bit = encoded.charAt(i);

            if (bit == '0') {
                current = current.left;
            } else {
                current = current.right;
            }

            if (current == null) {
                throw new IllegalArgumentException("译码失败：位置 " + i + " 位 '" + bit + "' 导致空指针");
            }

            if (current.isLeaf()) {
                result.append(current.ch);
                current = root;
            }
        }

        // 边界：如果最后没有回到根，说明编码串不完整
        if (current != root) {
            throw new IllegalArgumentException("译码失败：编码串不完整，结尾有未完成的路径");
        }

        return result.toString();
    }

    // ====================== 便捷方法：一键编码 ======================

    /**
     * 完整的编码过程：统计频率 → 建树 → 编码
     *
     * @return Object[]{根节点, 编码表, 编码结果}
     */
    public static Object[] fullEncode(String text) {
        Map<Character, Integer> freqMap = getFrequencyMap(text);
        HuffmanNode root = buildTree(freqMap);
        Map<Character, String> codes = generateCodes(root);
        String encoded = encode(text, codes);
        return new Object[]{root, codes, encoded};
    }

    // ====================== 二进制转换 ======================

    /**
     * 将二进制编码字符串（'0'/'1' 字符）压缩为字节数组。
     * 末尾不足 8 位的部分补 '0'，返回原始编码长度（bit 数）供记录 padding。
     *
     * @param bits 二进制编码字符串，如 "0101010101"
     * @return 字节数组（末尾含填充位）
     */
    private static byte[] bitsToBytes(String bits) {
        if (bits.isEmpty()) {
            return new byte[0];
        }
        int bitLen = bits.length();
        int padding = (8 - bitLen % 8) % 8;
        StringBuilder padded = new StringBuilder(bits);
        for (int i = 0; i < padding; i++) {
            padded.append('0');
        }

        int byteCount = padded.length() / 8;
        byte[] bytes = new byte[byteCount];
        for (int i = 0; i < byteCount; i++) {
            String byteStr = padded.substring(i * 8, i * 8 + 8);
            bytes[i] = (byte) Integer.parseInt(byteStr, 2);
        }
        return bytes;
    }

    /**
     * 将字节数组还原为二进制编码字符串，去掉末尾的填充位。
     *
     * @param bytes   字节数组
     * @param padding 最后 1 字节中属于填充的位数（0-7）
     * @return 原始二进制编码字符串
     */
    private static String bytesToBits(byte[] bytes, int padding) {
        if (bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String byteStr = String.format("%8s",
                    Integer.toBinaryString(bytes[i] & 0xFF)).replace(' ', '0');
            if (i == bytes.length - 1 && padding > 0) {
                sb.append(byteStr, 0, 8 - padding);
            } else {
                sb.append(byteStr);
            }
        }
        return sb.toString();
    }

    // ====================== 哈夫曼树序列化 / 反序列化 ======================

    /**
     * 序列化哈夫曼树到输出流（前序遍历）。
     * <ul>
     *   <li>内部节点 → 0</li>
     *   <li>叶子节点 → 1 + char (2 bytes, writeChar)</li>
     *   <li>null → 2</li>
     * </ul>
     */
    private static void serializeTree(HuffmanNode node, DataOutputStream dos) throws IOException {
        if (node == null) {
            dos.writeByte(2);       // null 标记
        } else if (node.isLeaf()) {
            dos.writeByte(1);       // 叶子标记
            dos.writeChar(node.ch); // 2 bytes
        } else {
            dos.writeByte(0);       // 内部节点标记
            serializeTree(node.left, dos);
            serializeTree(node.right, dos);
        }
    }

    /**
     * 从输入流反序列化哈夫曼树。
     *
     * @return 重建的哈夫曼树根节点（频率字段为 0）
     * @throws IOException 数据格式错误时抛出
     */
    private static HuffmanNode deserializeTree(DataInputStream dis) throws IOException {
        int marker = dis.readUnsignedByte();
        switch (marker) {
            case 2:  return null;
            case 1:  return new HuffmanNode(dis.readChar(), 0, null, null);
            case 0:
                HuffmanNode left  = deserializeTree(dis);
                HuffmanNode right = deserializeTree(dis);
                return new HuffmanNode('\0', 0, left, right);
            default:
                throw new IOException("无效的树节点标记: " + marker);
        }
    }

    // ====================== 树 ↔ 字节数组（持久化桥接） ======================

    /**
     * 将哈夫曼树序列化为字节数组，便于持久化到数据库或文件。
     * <p>格式与 {@link #encodeFile(String, String)} 中写入 .huf 的树部分完全一致。</p>
     *
     * @param root 哈夫曼树根
     * @return 序列化后的字节数组
     * @throws IOException 序列化异常
     * @see #bytesToTree(byte[])
     */
    public static byte[] treeToBytes(HuffmanNode root) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            serializeTree(root, dos);
        }
        return baos.toByteArray();
    }

    /**
     * 从字节数组反序列化哈夫曼树，与 {@link #treeToBytes(HuffmanNode)} 配对使用。
     *
     * @param data 序列化后的字节数组
     * @return 重建的哈夫曼树根节点（频率字段为 0）
     * @throws IOException 数据为空或格式错误时抛出
     * @see #treeToBytes(HuffmanNode)
     */
    public static HuffmanNode bytesToTree(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            throw new IOException("树数据为空");
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            return deserializeTree(in);
        }
    }

    // ====================== 文件 I/O（对外API） ======================

    /** 文件魔数，用于识别哈夫曼编码文件 */
    private static final byte[] MAGIC = {'H', 'F'};

    /**
     * 读取明文文件，编码压缩后写入 .huf 文件。
     * <p>
     * 哈夫曼树会随着编码数据一起序列化到 .huf 文件中，
     * 译码时可直接读取，无需额外传入字典。
     *
     * @param inputPath  输入文本文件路径（UTF-8 编码）
     * @param outputPath 输出编码文件路径（建议后缀 .huf）
     * @throws IOException 文件不存在或读写异常
     *
     * @see #decodeFile(String, String)
     */
    public static void encodeFile(String inputPath, String outputPath) throws IOException {
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new FileNotFoundException("明文文件不存在: " + inputPath);
        }
        if (!inputFile.isFile()) {
            throw new IOException("路径不是文件: " + inputPath);
        }

        // 1. 读取明文
        String plainText = new String(Files.readAllBytes(Paths.get(inputPath)), StandardCharsets.UTF_8);
        System.out.println("📖 读取明文文件: " + inputPath + " (" + plainText.length() + " 字符, "
                + inputFile.length() + " 字节)");

        // 2. 构建哈夫曼树并编码
        Map<Character, Integer> freqMap = getFrequencyMap(plainText);
        HuffmanNode root = buildTree(freqMap);
        Map<Character, String> codes = generateCodes(root);
        String bits = encode(plainText, codes);

        // 3. 二进制字符串 → 字节数组
        int bitLen = bits.length();
        int padding = (8 - bitLen % 8) % 8;
        byte[] dataBytes = bitsToBytes(bits);

        // 4. 序列化哈夫曼树
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            serializeTree(root, dos);
        }
        byte[] treeData = baos.toByteArray();

        // 5. 写入磁盘文件
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputPath)))) {
            out.write(MAGIC);                       // 魔数
            out.writeInt(treeData.length);          // 树数据长度
            out.write(treeData);                    // 树数据
            out.writeByte(padding);                 // 填充位数
            out.write(dataBytes);                   // 编码数据
        }

        // 打印统计
        long originalBytes = plainText.getBytes(StandardCharsets.UTF_8).length;
        int compressedBytes = treeData.length + dataBytes.length + 7;
        double ratio = (double) compressedBytes / originalBytes * 100;
        System.out.printf("✅ 编码完成: %s  (压缩率 %.1f%%)%n", outputPath, ratio);
    }

    /**
     * 读取 .huf 编码文件，译码还原后写入文本文件。
     * <p>
     * 从文件中反序列化哈夫曼树后自动进行译码，
     * 无需手动指定编码表或树结构。
     *
     * @param inputPath  输入编码文件路径（.huf）
     * @param outputPath 输出文本文件路径（UTF-8 编码）
     * @throws IOException 文件不存在、格式错误或数据损坏时抛出
     *
     * @see #encodeFile(String, String)
     */
    public static void decodeFile(String inputPath, String outputPath) throws IOException {
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new FileNotFoundException("编码文件不存在: " + inputPath);
        }

        // 1. 读取并验证魔数
        DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(inputPath)));

        byte[] magic = new byte[2];
        in.readFully(magic);
        if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1]) {
            in.close();
            throw new IOException("无效的哈夫曼编码文件（魔数不匹配，不是 .huf 文件）");
        }

        // 2. 反序列化哈夫曼树
        int treeLen = in.readInt();
        if (treeLen <= 0 || treeLen > 1_000_000) {
            in.close();
            throw new IOException("树数据长度异常: " + treeLen);
        }
        byte[] treeData = new byte[treeLen];
        in.readFully(treeData);

        HuffmanNode root;
        try (DataInputStream treeIn = new DataInputStream(
                new ByteArrayInputStream(treeData))) {
            root = deserializeTree(treeIn);
        }

        // 3. 读取填充位和编码数据
        int padding = in.readUnsignedByte();
        if (padding < 0 || padding > 7) {
            in.close();
            throw new IOException("无效的填充位数: " + padding);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        in.close();
        byte[] dataBytes = baos.toByteArray();

        // 4. 字节 → 二进制字符串 → 译码
        String bits = bytesToBits(dataBytes, padding);
        String decoded = decode(bits, root);

        // 5. 写入输出文件
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        Files.write(Paths.get(outputPath), decoded.getBytes(StandardCharsets.UTF_8));

        System.out.println("✅ 译码完成: " + outputPath + " (" + decoded.length() + " 字符, "
                + outputFile.length() + " 字节)");
    }

    // ====================== 打印工具 ======================

    /**
     * 打印编码表
     */
    public static void printCodes(Map<Character, String> codes) {
        List<Map.Entry<Character, String>> list = new ArrayList<>(codes.entrySet());
        list.sort(Map.Entry.comparingByKey());

        System.out.println("字符编码表：");
        for (Map.Entry<Character, String> entry : list) {
            char ch = entry.getKey();
            String display = (ch == ' ' ? "' '" :
                    ch == '\n' ? "\\n" :
                            ch == '\t' ? "\\t" :
                                    String.valueOf(ch));
            System.out.printf("  %-4s → %s%n", display, entry.getValue());
        }
    }

    /**
     * 树形打印哈夫曼树
     */
    public static void printTree(HuffmanNode node, String prefix, boolean isTail) {
        if (node == null) return;

        String label = node.isLeaf()
                ? (node.ch == ' ' ? "' '" : "'" + node.ch + "' ") + "[" + node.freq + "]"
                : "●[" + node.freq + "]";

        System.out.println(prefix + (isTail ? "└── " : "├── ") + label);

        if (node.isLeaf()) return;

        String childPrefix = prefix + (isTail ? "    " : "│   ");
        boolean hasRight = node.right != null;

        if (node.left != null) {
            printTree(node.left, childPrefix, !hasRight);
        }
        if (node.right != null) {
            printTree(node.right, childPrefix, true);
        }
    }

    // ====================== main 测试演示 ======================

    public static void main(String[] args) throws IOException {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║      哈夫曼编码 完整演示            ║");
        System.out.println("╚══════════════════════════════════════╝");

        // ---- 测试文本1：经典示例 ----
        String text1 = "go go gopher";
        System.out.println("\n【示例1】文本: \"" + text1 + "\"");
        demoHuffman(text1);

        // ---- 测试文本2：中文 ----
        String text2 = "哈夫曼编码是一种无损压缩算法";
        System.out.println("\n\n【示例2】中文文本: \"" + text2 + "\"");
        demoHuffman(text2);

        // ---- 测试文本3：不同字符集 ----
        String text3 = "abracadabra";
        System.out.println("\n\n【示例3】文本: \"" + text3 + "\"");
        demoHuffman(text3);

        // ---- 压缩率对比 ----
        System.out.println("\n\n╔══════════════════════════════════════╗");
        System.out.println("║      压缩率分析                     ║");
        System.out.println("╚══════════════════════════════════════╝");

        String[] samples = {"hello world", "this is a test", text1, text3, text2};

        for (String sample : samples) {
            Object[] result = fullEncode(sample);
            String encoded = (String) result[2];

            int originalBits = sample.length() * 8;  // ASCII: 每字符 8 bit（中文此处不准确，仅作演示对比）
            int compressedBits = encoded.length();    // Huffman: 变长编码
            double ratio = (double) compressedBits / originalBits * 100;

            System.out.printf("  %-20s 原=%3d B  压后=%3d bit  (%.1f%%)%n",
                    "'" + sample + "'", sample.length(), compressedBits, ratio);
        }

        // ====================== 文件 I/O 验证 ======================
        System.out.println("\n\n╔══════════════════════════════════════╗");
        System.out.println("║      文件 I/O 演示                   ║");
        System.out.println("╚══════════════════════════════════════╝");

        // ---- 写测试文件到临时目录 ----
        //String tempDir = System.getProperty("java.io.tmpdir");
        String plainFile  =  "huffman_io_test_plain.txt";
        String encodedFile =  "huffman_io_test.huf";
        String decodedFile =  "huffman_io_test_decoded.txt";

        // 准备测试文本（混合 ASCII 和中文）
        String testContent = "Hello, 哈夫曼编码! This is a test of file I/O for Huffman coding. "
                + "The quick brown fox jumps over the lazy dog. "
                + "哈夫曼编码是一种无损压缩算法，它基于字符出现的频率构建最优前缀码。\n"
                + "1234567890 !@#$%^&*() 测试 Test 测试。";

        Files.write(Paths.get(plainFile), testContent.getBytes(StandardCharsets.UTF_8));
        System.out.println("\n📝 创建测试明文: " + plainFile);

        // ---- 编码（明文文件 → .huf 文件） ----
        encodeFile(plainFile, encodedFile);

        // ---- 译码（.huf 文件 → 明文文件） ----
        decodeFile(encodedFile, decodedFile);

        // ---- 一致性校验 ----
        byte[] originalBytes  = Files.readAllBytes(Paths.get(plainFile));
        byte[] decodedBytes   = Files.readAllBytes(Paths.get(decodedFile));
        boolean match = Arrays.equals(originalBytes, decodedBytes);
        System.out.println("\n📊 一致性校验:");
        System.out.println("   原文: " + plainFile + " (" + originalBytes.length + " 字节)");
        System.out.println("   译码: " + decodedFile + " (" + decodedBytes.length + " 字节)");
        System.out.println("   结果: " + (match ? "✅ 完全一致！" : "❌ 不一致！"));

        // 详细对比（不一致时输出差异）
        if (!match) {
            String originalStr = new String(originalBytes, StandardCharsets.UTF_8);
            String decodedStr  = new String(decodedBytes, StandardCharsets.UTF_8);
            System.out.println("   原文内容: " + originalStr);
            System.out.println("   译码内容: " + decodedStr);
        }

        // ---- 清理临时文件 ----
        //Files.deleteIfExists(Paths.get(plainFile));
        //Files.deleteIfExists(Paths.get(encodedFile));
        //Files.deleteIfExists(Paths.get(decodedFile));
        //System.out.println("\n🧹 临时测试文件已清理");
    }

    private static void demoHuffman(String text) {
        // 1. 频率
        Map<Character, Integer> freqMap = getFrequencyMap(text);
        System.out.println("\n频率统计: " + freqMap);

        // 2. 建树
        HuffmanNode root = buildTree(freqMap);
        System.out.println("哈夫曼树根频率: " + root.freq);

        // 3. 树形结构
        System.out.println("树形结构（●=内部节点, 'x'=叶子节点）:");
        printTree(root, "", true);

        // 4. 编码表
        Map<Character, String> codes = generateCodes(root);
        printCodes(codes);

        // 5. 编码
        String encoded = encode(text, codes);
        System.out.println("编码结果: " + encoded);
        System.out.println("编码长度: " + encoded.length() + " bit");

        // 6. 译码
        String decoded = decode(encoded, root);
        System.out.println("译码还原: \"" + decoded + "\"");

        // 7. 验证
        boolean match = text.equals(decoded);
        System.out.println("一致性验证: " + (match ? "✓ 通过" : "✗ 失败"));
    }
}
