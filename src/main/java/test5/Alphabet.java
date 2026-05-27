package test5;

import java.util.ArrayList;
import java.util.List;

public class Alphabet {
    // 字符到索引的映射
    private final int[] charToIndex;
    // 索引到字符的映射
    private final char[] indexToChar;
    // 字母表基数 R
    private final int R;
    // 表示一个索引所需的比特数
    private final int lgR;

    // 内置常用字母表（方便直接调用）
    public static final Alphabet BINARY = new Alphabet("01");
    public static final Alphabet OCTAL = new Alphabet("01234567");
    public static final Alphabet DECIMAL = new Alphabet("0123456789");
    public static final Alphabet HEXADECIMAL = new Alphabet("0123456789ABCDEF");
    public static final Alphabet DNA = new Alphabet("ACGT");
    public static final Alphabet LOWERCASE = new Alphabet("abcdefghijklmnopqrstuvwxyz");
    public static final Alphabet UPPERCASE = new Alphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    public static final Alphabet PROTEIN = new Alphabet("ACDEFGHIKLMNPQRSTVWY");
    public static final Alphabet BASE64 = new Alphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/");
    public static final Alphabet ASCII = new Alphabet(128);
    public static final Alphabet EXTENDED_ASCII = new Alphabet(256);
    public static final Alphabet UNICODE16 = new Alphabet(65536);

    /**
     * 根据字符串s创建字母表
     * @param s 字母表包含的所有不重复字符
     */
    public Alphabet(String s) {
        // 去重校验
        boolean[] seen = new boolean[Character.MAX_VALUE + 1];
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (seen[c]) {
                throw new IllegalArgumentException("字母表中存在重复字符: " + c);
            }
            seen[c] = true;
        }

        // 初始化映射
        R = s.length();
        indexToChar = new char[R];
        charToIndex = new int[Character.MAX_VALUE + 1];
        for (int i = 0; i < charToIndex.length; i++) {
            charToIndex[i] = -1; // 初始化为无效索引
        }
        for (int i = 0; i < R; i++) {
            char c = s.charAt(i);
            indexToChar[i] = c;
            charToIndex[c] = i;
        }

        // 计算lgR：表示R所需的比特数（向上取整）
        if (R == 0) {
            throw new IllegalArgumentException("字母表不能为空");
        }
        int bits = 0;
        while ((1 << bits) < R) {
            bits++;
        }
        lgR = bits;
    }

    /**
     * 创建一个包含0到size-1的所有字符的字母表
     * @param size 字母表大小
     */
    private Alphabet(int size) {
        charToIndex = new int[size];
        indexToChar = new char[size];
        R = size;
        for (int i = 0; i < size; i++) {
            charToIndex[i] = i;
            indexToChar[i] = (char) i;
        }
        // 计算lgR
        int bits = 0;
        while ((1 << bits) < R) {
            bits++;
        }
        lgR = bits;
    }

    /**
     * 获取字母表中指定索引位置的字符
     * @param index 索引（0~R-1）
     * @return 对应字符
     */
    public char toChar(int index) {
        if (index < 0 || index >= R) {
            throw new IllegalArgumentException("索引超出范围: " + index);
        }
        return indexToChar[index];
    }

    /**
     * 获取字符在字母表中的索引
     * @param c 字符
     * @return 索引（0~R-1）
     */
    public int toIndex(char c) {
        if (!contains(c)) {
            throw new IllegalArgumentException("字符不在字母表中: " + c);
        }
        return charToIndex[c];
    }

    /**
     * 检查字符是否在字母表中
     * @param c 字符
     * @return 存在返回true，否则false
     */
    public boolean contains(char c) {
        return c < charToIndex.length && charToIndex[c] != -1;
    }

    /**
     * 获取字母表的基数（字符数量）
     * @return 基数R
     */
    public int R() {
        return R;
    }

    /**
     * 获取表示一个索引所需的比特数
     * @return 比特数lgR
     */
    public int lgR() {
        return lgR;
    }

    /**
     * 将字符串转换为基于该字母表的索引数组
     * @param s 输入字符串（所有字符都必须在字母表中）
     * @return 索引数组
     */
    public int[] toIndices(String s) {
        int[] indices = new int[s.length()];
        for (int i = 0; i < s.length(); i++) {
            indices[i] = toIndex(s.charAt(i));
        }
        return indices;
    }

    /**
     * 将索引数组转换为基于该字母表的字符串
     * @param indices 索引数组（每个元素都必须在0~R-1之间）
     * @return 转换后的字符串
     */
    public String toChars(int[] indices) {
        StringBuilder sb = new StringBuilder(indices.length);
        for (int idx : indices) {
            sb.append(toChar(idx));
        }
        return sb.toString();
    }

    // 简单测试用例
    public static void main(String[] args) {
        Alphabet dna = Alphabet.UNICODE16;
        String dnaStr = "ACGTACGTSDFSGSDGHDGYHTHRTYJTYUKLIKLDFGDHJFYJ";
        int[] indices = dna.toIndices(dnaStr);
        System.out.println("原字符串: " + dnaStr);
        System.out.print("索引数组: ");
        for (int idx : indices) {
            System.out.print(idx + " ");
        }
        System.out.println();
        System.out.println("还原字符串: " + dna.toChars(indices));
        System.out.println("DNA字母表基数: " + dna.R());
        System.out.println("每个索引所需比特数: " + dna.lgR());
    }
}