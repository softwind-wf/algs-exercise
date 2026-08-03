package com.ds.matching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KMPDemo 的 JUnit5 单元测试
 *
 * 由于 KMPDemo 中的方法均为 private static，这里通过反射调用。
 */
@DisplayName("KMPDemo 字符串匹配测试")
class KMPDemoTest {

    private Method bruteForceNextMethod;
    private Method standardNextMethod;
    private Method kmpSearchMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        bruteForceNextMethod = KMPDemo.class.getDeclaredMethod("bruteForceNext", String.class);
        bruteForceNextMethod.setAccessible(true);

        standardNextMethod = KMPDemo.class.getDeclaredMethod("standardNext", String.class);
        standardNextMethod.setAccessible(true);

        kmpSearchMethod = KMPDemo.class.getDeclaredMethod("kmpSearch", String.class, String.class, int[].class);
        kmpSearchMethod.setAccessible(true);
    }

    // ==================== next 数组一致性测试 ====================

    @Test
    @DisplayName("两种 next 计算方法对多种模式串结果一致")
    void testNextMethodsConsistency() throws InvocationTargetException, IllegalAccessException {
        String[] patterns = {
                "aaaabcabd",
                "ABABAC",
                "GCAGAGAG",
                "AAAA",
                "ABAB",
                "ABA",
                "A",
                "ABC",
                "AACAA",
                "PARTICIPATE IN PARACHUTE",
                "aaaaaaaaab"
        };

        for (String pattern : patterns) {
            int[] bruteNext = (int[]) bruteForceNextMethod.invoke(null, pattern);
            int[] standardNext = (int[]) standardNextMethod.invoke(null, pattern);
            assertArrayEquals(bruteNext, standardNext,
                    "模式串 [" + pattern + "] 的两种 next 数组不一致");
        }
    }

    @Test
    @DisplayName("标准 next 数组计算结果符合预期")
    void testStandardNextValues() throws InvocationTargetException, IllegalAccessException {
        assertArrayEquals(new int[]{-1, 0, 1, 2, 3, 0, 0, 1, 0},
                (int[]) standardNextMethod.invoke(null, "aaaabcabd"));

        assertArrayEquals(new int[]{-1, 0, 0, 1, 2, 3},
                (int[]) standardNextMethod.invoke(null, "ABABAC"));

        assertArrayEquals(new int[]{-1, 0, 0, 0, 1, 0, 1, 0},
                (int[]) standardNextMethod.invoke(null, "GCAGAGAG"));

        assertArrayEquals(new int[]{-1, 0, 1, 2, 3},
                (int[]) standardNextMethod.invoke(null, "AAAAA"));

        assertArrayEquals(new int[]{-1, 0, 0, 1, 2},
                (int[]) standardNextMethod.invoke(null, "ABABA"));
    }

    @Test
    @DisplayName("暴力 next 数组计算结果符合预期")
    void testBruteForceNextValues() throws InvocationTargetException, IllegalAccessException {
        assertArrayEquals(new int[]{-1, 0, 1, 2, 3, 0, 0, 1, 0},
                (int[]) bruteForceNextMethod.invoke(null, "aaaabcabd"));

        assertArrayEquals(new int[]{-1, 0, 0, 1, 2, 3},
                (int[]) bruteForceNextMethod.invoke(null, "ABABAC"));
    }

    // ==================== KMP 搜索功能测试 ====================

    @Test
    @DisplayName("测试主串中间匹配")
    void testKmpSearchMiddleMatch() throws InvocationTargetException, IllegalAccessException {
        String mainStr = "eabaaaabcabdxyz";
        String pattern = "aaaabcabd";
        int[] next = (int[]) standardNextMethod.invoke(null, pattern);
        int result = (int) kmpSearchMethod.invoke(null, mainStr, pattern, next);
        assertEquals(3, result);
    }

    @Test
    @DisplayName("测试从主串开头匹配")
    void testKmpSearchStartMatch() throws InvocationTargetException, IllegalAccessException {
        String mainStr = "abcdefg";
        String pattern = "abc";
        int[] next = (int[]) standardNextMethod.invoke(null, pattern);
        assertEquals(0, kmpSearchMethod.invoke(null, mainStr, pattern, next));
    }

    @Test
    @DisplayName("测试匹配到主串末尾")
    void testKmpSearchEndMatch() throws InvocationTargetException, IllegalAccessException {
        String mainStr = "xyzabc";
        String pattern = "abc";
        int[] next = (int[]) standardNextMethod.invoke(null, pattern);
        assertEquals(3, kmpSearchMethod.invoke(null, mainStr, pattern, next));
    }

    @Test
    @DisplayName("测试未找到匹配")
    void testKmpSearchNoMatch() throws InvocationTargetException, IllegalAccessException {
        String mainStr = "abcdefg";
        String pattern = "xyz";
        int[] next = (int[]) standardNextMethod.invoke(null, pattern);
        assertEquals(-1, kmpSearchMethod.invoke(null, mainStr, pattern, next));
    }

    @Test
    @DisplayName("测试单字符模式串匹配")
    void testKmpSearchSingleCharMatch() throws InvocationTargetException, IllegalAccessException {
        String mainStr = "xxaxx";
        String pattern = "a";
        int[] next = (int[]) standardNextMethod.invoke(null, pattern);
        assertEquals(2, kmpSearchMethod.invoke(null, mainStr, pattern, next));
    }

    @Test
    @DisplayName("测试文本等于模式串")
    void testKmpSearchTextEqualsPattern() throws InvocationTargetException, IllegalAccessException {
        String text = "abcdef";
        String pattern = "abcdef";
        int[] next = (int[]) standardNextMethod.invoke(null, pattern);
        assertEquals(0, kmpSearchMethod.invoke(null, text, pattern, next));
    }

    @Test
    @DisplayName("测试文本长度小于模式串")
    void testKmpSearchTextShorterThanPattern() throws InvocationTargetException, IllegalAccessException {
        String text = "abc";
        String pattern = "abcdef";
        int[] next = (int[]) standardNextMethod.invoke(null, pattern);
        assertEquals(-1, kmpSearchMethod.invoke(null, text, pattern, next));
    }

    @Test
    @DisplayName("测试重复字符模式串")
    void testKmpSearchRepeatingPattern() throws InvocationTargetException, IllegalAccessException {
        String text = "aaaaa";
        String pattern = "aaa";
        int[] next = (int[]) standardNextMethod.invoke(null, pattern);
        assertEquals(0, kmpSearchMethod.invoke(null, text, pattern, next));
    }

    @Test
    @DisplayName("测试与 Java 原生 indexOf 结果对比")
    void testKmpSearchCompareWithIndexOf() throws InvocationTargetException, IllegalAccessException {
        String text = "AABRAACADABRAACAADABRA";
        String pattern = "AACAA";
        int[] next = (int[]) standardNextMethod.invoke(null, pattern);
        int kmpPos = (int) kmpSearchMethod.invoke(null, text, pattern, next);
        assertEquals(text.indexOf(pattern), kmpPos);
    }

    @Test
    @DisplayName("测试多种模式串均与 indexOf 一致")
    void testKmpSearchConsistentWithIndexOf() throws InvocationTargetException, IllegalAccessException {
        String text = "GCATCGCAGAGAGTATAGCAGAGAGTACG";
        String[] patterns = {"GCAGAGAG", "TATA", "GAGA", "XYZ", "A", "GCATCGCAGAGAGTATAGCAGAGAGTACG"};

        for (String pattern : patterns) {
            int[] next = (int[]) standardNextMethod.invoke(null, pattern);
            int kmpPos = (int) kmpSearchMethod.invoke(null, text, pattern, next);
            assertEquals(text.indexOf(pattern), kmpPos,
                    "模式串 [" + pattern + "] 搜索结果与 indexOf 不一致");
        }
    }

    // ==================== 边界情况测试 ====================

    @Test
    @DisplayName("测试长度为1的模式串")
    void testSingleCharPatternNext() throws InvocationTargetException, IllegalAccessException {
        assertArrayEquals(new int[]{-1},
                (int[]) standardNextMethod.invoke(null, "A"));
        assertArrayEquals(new int[]{-1},
                (int[]) bruteForceNextMethod.invoke(null, "A"));
    }
}
