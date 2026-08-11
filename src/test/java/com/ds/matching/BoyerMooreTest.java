package com.ds.matching;

import cn.exercise.algs4.datastructure.matching.BoyerMoore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BoyerMoore 的 JUnit5 单元测试
 */
@DisplayName("BoyerMoore 字符串匹配测试")
class BoyerMooreTest {

    private BoyerMoore boyerMoore;

    @BeforeEach
    void setUp() {
        boyerMoore = new BoyerMoore();
    }

    // ==================== 基础功能测试 ====================

    @Test
    @DisplayName("主方法示例：abcabx 在 abcabcabxabc 中匹配")
    void testMainExample() {
        assertEquals(3, boyerMoore.boyerMoore("abcabcabxabc", "abcabx"));
    }

    @Test
    @DisplayName("测试从开头匹配")
    void testStartMatch() {
        assertEquals(0, boyerMoore.boyerMoore("ABCDEF", "ABC"));
    }

    @Test
    @DisplayName("测试从中间匹配")
    void testMiddleMatch() {
        assertEquals(2, boyerMoore.boyerMoore("XXABCXX", "ABC"));
    }

    @Test
    @DisplayName("测试从末尾匹配")
    void testEndMatch() {
        assertEquals(2, boyerMoore.boyerMoore("XXABC", "ABC"));
    }

    @Test
    @DisplayName("测试未找到匹配")
    void testNoMatch() {
        assertEquals(-1, boyerMoore.boyerMoore("ABCDEFG", "XYZ"));
    }

    @Test
    @DisplayName("测试文本等于模式串")
    void testTextEqualsPattern() {
        assertEquals(0, boyerMoore.boyerMoore("ABC", "ABC"));
    }

    @Test
    @DisplayName("测试单字符模式串")
    void testSingleCharMatch() {
        assertEquals(2, boyerMoore.boyerMoore("XXAXX", "A"));
    }

    @Test
    @DisplayName("测试长模式串")
    void testLongPattern() {
        String pattern = "ABCDEFGHIJ";
        String text = "XXXABCDEFGHIJXXX";
        assertEquals(3, boyerMoore.boyerMoore(text, pattern));
    }

    // ==================== 边界情况测试 ====================

    @Test
    @DisplayName("测试空模式串")
    void testEmptyPattern() {
        assertEquals(0, boyerMoore.boyerMoore("ABC", ""));
    }

    @Test
    @DisplayName("测试空文本")
    void testEmptyText() {
        assertEquals(-1, boyerMoore.boyerMoore("", "ABC"));
    }

    @Test
    @DisplayName("测试文本长度小于模式串")
    void testTextShorterThanPattern() {
        assertEquals(-1, boyerMoore.boyerMoore("ABC", "ABCD"));
    }

    @Test
    @DisplayName("测试 null 文本")
    void testNullText() {
        assertEquals(-1, boyerMoore.boyerMoore(null, "ABC"));
    }

    @Test
    @DisplayName("测试 null 模式串")
    void testNullPattern() {
        assertEquals(-1, boyerMoore.boyerMoore("ABC", null));
    }

    // ==================== 特殊模式测试 ====================

    @Test
    @DisplayName("测试重复字符模式")
    void testRepeatingChars() {
        assertEquals(0, boyerMoore.boyerMoore("AAAAA", "AAAA"));
    }

    @Test
    @DisplayName("测试交替字符模式")
    void testAlternatingChars() {
        assertEquals(0, boyerMoore.boyerMoore("ABABAB", "ABAB"));
    }

    @Test
    @DisplayName("测试回文模式")
    void testPalindromePattern() {
        assertEquals(0, boyerMoore.boyerMoore("ABACACABA", "ABA"));
    }

    @Test
    @DisplayName("测试中文字符匹配")
    void testChineseChars() {
        assertEquals(2, boyerMoore.boyerMoore("你好世界你好", "世界"));
    }

    @Test
    @DisplayName("测试大规模文本")
    void testLargeText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("ABCD");
        }
        sb.append("TARGET");
        for (int i = 0; i < 1000; i++) {
            sb.append("WXYZ");
        }

        int pos = boyerMoore.boyerMoore(sb.toString(), "TARGET");
        assertTrue(pos >= 4000 && pos <= 4001);
    }

    // ==================== 与 Java indexOf 对比 ====================

    @Test
    @DisplayName("测试与 Java 原生 indexOf 对比")
    void testCompareWithIndexOf() {
        String text = "AABRAACADABRAACAADABRA";
        String pattern = "AACAA";
        assertEquals(text.indexOf(pattern), boyerMoore.boyerMoore(text, pattern));
    }

    @Test
    @DisplayName("测试 DNA 序列多模式串与 indexOf 一致")
    void testDnaVsIndexOf() {
        String text = "GCATCGCAGAGAGTATAGCAGAGAGTACG";
        String[] patterns = {"GCAGAGAG", "TATA", "GAGA", "XYZ", "A", text};

        for (String pattern : patterns) {
            assertEquals(text.indexOf(pattern), boyerMoore.boyerMoore(text, pattern),
                    "模式串 [" + pattern + "] 与 indexOf 结果不一致");
        }
    }

    @Test
    @DisplayName("随机测试与 indexOf 一致")
    void testRandomVsIndexOf() {
        Random random = new Random(42);
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        for (int t = 0; t < 500; t++) {
            String text = randomString(random, alphabet, 80);
            String pattern = randomString(random, alphabet, 10);
            int expected = text.indexOf(pattern);
            int actual = boyerMoore.boyerMoore(text, pattern);
            assertEquals(expected, actual,
                    "文本 [" + text + "] 模式串 [" + pattern + "] 与 indexOf 不一致");
        }
    }

    // ==================== 内部辅助方法测试（反射） ====================

    @Test
    @DisplayName("测试 badChar 数组初始化")
    void testInitBadChar() throws Exception {
        Method initBadChar = BoyerMoore.class.getDeclaredMethod("initBadChar", String.class);
        initBadChar.setAccessible(true);
        Field badCharField = BoyerMoore.class.getDeclaredField("badChar");
        badCharField.setAccessible(true);

        initBadChar.invoke(boyerMoore, "ABAC");
        int[] badChar = (int[]) badCharField.get(boyerMoore);

        assertEquals(2, badChar['A']);
        assertEquals(1, badChar['B']);
        assertEquals(3, badChar['C']);
        assertEquals(-1, badChar['D']);
    }

    @Test
    @DisplayName("测试 goodSuffix 与 isPrefix 数组初始化")
    void testInitGoodSuffixAndIsPrefix() throws Exception {
        Method init = BoyerMoore.class.getDeclaredMethod("initGoodSuffixAndIsPrefix", String.class);
        init.setAccessible(true);
        Field goodSuffixField = BoyerMoore.class.getDeclaredField("goodSuffix");
        goodSuffixField.setAccessible(true);
        Field isPrefixField = BoyerMoore.class.getDeclaredField("isPrefix");
        isPrefixField.setAccessible(true);

        init.invoke(boyerMoore, "ABAB");
        int[] goodSuffix = (int[]) goodSuffixField.get(boyerMoore);
        boolean[] isPrefix = (boolean[]) isPrefixField.get(boyerMoore);

        assertEquals(1, goodSuffix[3]);
        assertEquals(0, goodSuffix[2]);
        assertTrue(isPrefix[2]);
    }

    @Test
    @DisplayName("测试 badCharLength 坏字符规则")
    void testBadCharLength() throws Exception {
        Method initBadChar = BoyerMoore.class.getDeclaredMethod("initBadChar", String.class);
        initBadChar.setAccessible(true);
        Method badCharLength = BoyerMoore.class.getDeclaredMethod(
                "badCharLength", String.class, int.class, int.class);
        badCharLength.setAccessible(true);

        initBadChar.invoke(boyerMoore, "ABAC");

        int len = (int) badCharLength.invoke(boyerMoore, "XXBXX", 2, 2);
        assertEquals(1, len);

        len = (int) badCharLength.invoke(boyerMoore, "XXDXX", 2, 2);
        assertEquals(3, len);
    }

    @Test
    @DisplayName("测试 goodSuffixLength 好后缀规则")
    void testGoodSuffixLength() throws Exception {
        Method init = BoyerMoore.class.getDeclaredMethod("initGoodSuffixAndIsPrefix", String.class);
        init.setAccessible(true);
        Method goodSuffixLength = BoyerMoore.class.getDeclaredMethod(
                "goodSuffixLength", String.class, int.class);
        goodSuffixLength.setAccessible(true);

        init.invoke(boyerMoore, "ABAB");

        assertEquals(0, goodSuffixLength.invoke(boyerMoore, "ABAB", 3));

        int len = (int) goodSuffixLength.invoke(boyerMoore, "ABAB", 1);
        assertTrue(len > 0, "存在好后缀时应返回正数移动距离");
    }

    // ==================== 工具方法 ====================

    private String randomString(Random random, String alphabet, int maxLength) {
        int length = random.nextInt(maxLength) + 1;
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
