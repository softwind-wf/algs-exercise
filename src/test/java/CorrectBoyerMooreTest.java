

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import test5.CorrectBoyerMoore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CorrectBoyerMoore 字符串匹配算法的单元测试
 */
@DisplayName("CorrectBoyerMoore 字符串匹配测试")
class CorrectBoyerMooreTest {

    // ==================== 基础功能测试 ====================

    @Test
    @DisplayName("测试简单匹配-首次出现")
    void testSimpleSearch() {
        CorrectBoyerMoore bm = new CorrectBoyerMoore("ABABAC");
        assertEquals(0, bm.search("ABABACAABABACX"));
    }

    @Test
    @DisplayName("测试简单匹配-中间出现")
    void testMiddleSearch() {
        CorrectBoyerMoore bm = new CorrectBoyerMoore("GCAGAGAG");
        assertEquals(5, bm.search("GCATCGCAGAGAGTATAGCAGAGAGTACG"));
    }

    @Test
    @DisplayName("测试未找到匹配")
    void testNoMatch() {
        CorrectBoyerMoore bm = new CorrectBoyerMoore("XYZ");
        assertEquals(-1, bm.search("ABCDEFG"));
    }

    @Test
    @DisplayName("测试文本等于模式串")
    void testTextEqualsPattern() {
        CorrectBoyerMoore bm = new CorrectBoyerMoore("ABC");
        assertEquals(0, bm.search("ABC"));
    }

    @Test
    @DisplayName("测试单字符匹配")
    void testSingleCharMatch() {
        CorrectBoyerMoore bm = new CorrectBoyerMoore("A");
        assertEquals(2, bm.search("XXAXX"));
    }

    // ==================== searchAll 测试 ====================

    @Test
    @DisplayName("测试全部匹配-无重叠")
    void testSearchAllNoOverlap() {
        CorrectBoyerMoore bm = new CorrectBoyerMoore("AB");
        List<Integer> positions = bm.searchAll("ABABAB");
        assertEquals(3, positions.size());
        assertEquals(0, positions.get(0));
        assertEquals(2, positions.get(1));
        assertEquals(4, positions.get(2));
    }

    @Test
    @DisplayName("测试全部匹配-有重叠")
    void testSearchAllWithOverlap() {
        CorrectBoyerMoore bm = new CorrectBoyerMoore("AAA");
        List<Integer> positions = bm.searchAll("AAAA");
        assertEquals(2, positions.size());
        assertEquals(0, positions.get(0));
        assertEquals(1, positions.get(1));
    }

    @Test
    @DisplayName("测试全部匹配-单个结果")
    void testSearchAllSingleMatch() {
        CorrectBoyerMoore bm = new CorrectBoyerMoore("ABC");
        List<Integer> positions = bm.searchAll("XABCX");
        assertEquals(1, positions.size());
        assertEquals(1, positions.get(0));
    }

    @Test
    @DisplayName("测试全部匹配-无结果")
    void testSearchAllNoMatch() {
        CorrectBoyerMoore bm = new CorrectBoyerMoore("XYZ");
        List<Integer> positions = bm.searchAll("ABCDEFG");
        assertTrue(positions.isEmpty());
    }

    @Test
    @DisplayName("测试全部匹配-原书示例")
    void testSearchAllBookExample() {
        CorrectBoyerMoore bm = new CorrectBoyerMoore("ABABAC");
        List<Integer> positions = bm.searchAll("ABABACAABABACX");
        assertEquals(2, positions.size());
        assertEquals(0, positions.get(0));
        assertEquals(7, positions.get(1));
    }

    // ==================== 边界情况测试 ====================

    @Test
    @DisplayName("测试空模式串")
    void testEmptyPattern() {
        CorrectBoyerMoore bm = new CorrectBoyerMoore("");
        assertEquals(0, bm.search("ABC"));
        assertTrue(bm.searchAll("ABC").isEmpty());
    }

    @Test
    @DisplayName("测试空文本")
    void testEmptyText() {
        CorrectBoyerMoore bm = new CorrectBoyerMoore("ABC");
        assertEquals(-1, bm.search(""));
        assertTrue(bm.searchAll("").isEmpty());
    }

    @Test
    @DisplayName("测试文本长度小于模式串")
    void testTextShorterThanPattern() {
        CorrectBoyerMoore bm = new CorrectBoyerMoore("ABCD");
        assertEquals(-1, bm.search("ABC"));
        assertTrue(bm.searchAll("ABC").isEmpty());
    }

    @Test
    @DisplayName("测试长模式串")
    void testLongPattern() {
        String pattern = "ABCDEFGHIJ";
        String text = "XXXABCDEFGHIJXXX";
        CorrectBoyerMoore bm = new CorrectBoyerMoore(pattern);
        assertEquals(3, bm.search(text));
    }

    // ==================== 特殊模式测试 ====================

    @Test
    @DisplayName("测试重复字符模式")
    void testRepeatingChars() {
        CorrectBoyerMoore bm = new CorrectBoyerMoore("AAAA");
        assertEquals(0, bm.search("AAAAA"));
        List<Integer> positions = bm.searchAll("AAAAA");
        assertEquals(2, positions.size());
    }

    @Test
    @DisplayName("测试交替字符模式")
    void testAlternatingChars() {
        CorrectBoyerMoore bm = new CorrectBoyerMoore("ABAB");
        assertEquals(0, bm.search("ABABAB"));
        List<Integer> positions = bm.searchAll("ABABAB");
        assertEquals(2, positions.size());
    }

    @Test
    @DisplayName("测试回文模式")
    void testPalindromePattern() {
        CorrectBoyerMoore bm = new CorrectBoyerMoore("ABA");
        List<Integer> positions = bm.searchAll("ABACACABA");
        assertEquals(2, positions.size());
        assertEquals(0, positions.get(0));
        assertEquals(6, positions.get(1));
    }

    // ==================== 复杂场景测试 ====================

    @Test
    @DisplayName("测试DNA序列匹配")
    void testDNAMatch() {
        String dna = "GCATCGCAGAGAGTATAGCAGAGAGTACG";
        String pattern = "GCAGAGAG";
        CorrectBoyerMoore bm = new CorrectBoyerMoore(pattern);
        
        List<Integer> positions = bm.searchAll(dna);
        assertEquals(2, positions.size());
        assertEquals(5, positions.get(0));
        assertEquals(17, positions.get(1));
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
        
        CorrectBoyerMoore bm = new CorrectBoyerMoore("TARGET");
        int pos = bm.search(sb.toString());
        assertTrue(pos >= 4000 && pos <= 4001);
    }

    @Test
    @DisplayName("测试多个不同匹配位置")
    void testMultiplePositions() {
        CorrectBoyerMoore bm = new CorrectBoyerMoore("TEST");
        List<Integer> positions = bm.searchAll("TEST1TEST2TEST3");
        assertEquals(3, positions.size());
        assertEquals(0, positions.get(0));
        assertEquals(5, positions.get(1));
        assertEquals(10, positions.get(2));
    }

    @Test
    @DisplayName("测试与Java原生indexOf对比")
    void testCompareWithIndexOf() {
        String text = "AABRAACADABRAACAADABRA";
        String pattern = "AACAA";
        
        CorrectBoyerMoore bm = new CorrectBoyerMoore(pattern);
        int bmPos = bm.search(text);
        int javaPos = text.indexOf(pattern);
        
        assertEquals(javaPos, bmPos);
    }

    @Test
    @DisplayName("测试searchAll与循环indexOf对比")
    void testSearchAllVsIndexOf() {
        String text = "ABABABAABABAB";
        String pattern = "ABAB";
        
        CorrectBoyerMoore bm = new CorrectBoyerMoore(pattern);
        List<Integer> bmPositions = bm.searchAll(text);
        
        // 手动用indexOf查找所有位置
        List<Integer> expected = new java.util.ArrayList<>();
        int pos = 0;
        while ((pos = text.indexOf(pattern, pos)) != -1) {
            expected.add(pos);
            pos++;
        }
        
        assertEquals(expected.size(), bmPositions.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), bmPositions.get(i));
        }
    }
}
