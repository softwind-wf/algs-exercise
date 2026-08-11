package com.ds.datastructure.huffman;

import cn.exercise.algs4.datastructure.huffman.HuffmanCoding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HuffmanCoding 的 JUnit5 单元测试
 */
@DisplayName("HuffmanCoding 哈夫曼编码测试")
class HuffmanCodingTest {

    // ==================== 一、核心功能测试 ====================

    @Test
    @DisplayName("经典文本的编解码往返一致性")
    void testEncodeDecodeHelloWorld() {
        String original = "hello world";
        Object[] result = HuffmanCoding.fullEncode(original);
        HuffmanCoding.HuffmanNode root = (HuffmanCoding.HuffmanNode) result[0];
        Map<Character, String> codes = (Map<Character, String>) result[1];
        String encoded = (String) result[2];

        String decoded = HuffmanCoding.decode(encoded, root);
        assertEquals(original, decoded, "编解码往返应还原原文本");
        assertFalse(encoded.isEmpty(), "编码结果不应为空");
        assertTrue(codes.size() > 1, "应有多个编码条目");
    }

    @Test
    @DisplayName("仅含单个字符的文本")
    void testSingleCharText() {
        String original = "aaaaaa";
        Object[] result = HuffmanCoding.fullEncode(original);
        HuffmanCoding.HuffmanNode root = (HuffmanCoding.HuffmanNode) result[0];
        String encoded = (String) result[2];
        String decoded = HuffmanCoding.decode(encoded, root);

        assertEquals(original, decoded);
        // 单字符的编码应该全部由 0 组成（或所有位相同）
        assertTrue(encoded.matches("0+") || encoded.matches("1+"),
                "单字符编码应全为相同位");
    }

    @Test
    @DisplayName("两个字符交替的文本")
    void testTwoCharsText() {
        String original = "abababab";
        Object[] result = HuffmanCoding.fullEncode(original);
        HuffmanCoding.HuffmanNode root = (HuffmanCoding.HuffmanNode) result[0];
        Map<Character, String> codes = (Map<Character, String>) result[1];
        String encoded = (String) result[2];
        String decoded = HuffmanCoding.decode(encoded, root);

        assertEquals(original, decoded);
        assertEquals(2, codes.size(), "应只有两种编码");
        // 两个字符的编码长度应为 1
        for (String code : codes.values()) {
            assertEquals(1, code.length(), "两个字符时编码长度应为 1");
        }
    }

    @Test
    @DisplayName("多个不同字符的文本")
    void testMultipleCharsText() {
        String original = "abracadabra";
        Object[] result = HuffmanCoding.fullEncode(original);
        HuffmanCoding.HuffmanNode root = (HuffmanCoding.HuffmanNode) result[0];
        String decoded = HuffmanCoding.decode((String) result[2], root);
        assertEquals(original, decoded);
    }

    @Test
    @DisplayName("包含空格的文本")
    void testTextWithSpaces() {
        String original = "a b c d e f g";
        Object[] result = HuffmanCoding.fullEncode(original);
        HuffmanCoding.HuffmanNode root = (HuffmanCoding.HuffmanNode) result[0];
        String decoded = HuffmanCoding.decode((String) result[2], root);
        assertEquals(original, decoded);
    }

    // ==================== 二、边界条件测试 ====================

    @Test
    @DisplayName("空字符串")
    void testEmptyString() {
        // 频率统计
        Map<Character, Integer> freq = HuffmanCoding.getFrequencyMap("");
        assertTrue(freq.isEmpty(), "空文本的频率表应为空");

        // 建树
        assertNull(HuffmanCoding.buildTree(freq), "空频率表应返回 null");

        // 编码
        assertEquals("", HuffmanCoding.encode("", new java.util.HashMap<>()));
    }

    @Test
    @DisplayName("null 输入")
    void testNullInput() {
        // 频率统计
        assertTrue(HuffmanCoding.getFrequencyMap((String) null).isEmpty(),
                "null 文本的频率表应为空");

        // 建树
        assertNull(HuffmanCoding.buildTree(null), "null 频率表应返回 null");

        // 编码
        assertEquals("", HuffmanCoding.encode(null, new java.util.HashMap<>()));

        // 译码
        assertEquals("", HuffmanCoding.decode(null, null));
        assertEquals("", HuffmanCoding.decode("", null));
    }

    @Test
    @DisplayName("编解码空字符串")
    void testEncodeDecodeEmpty() {
        // 空字符串完整编码
        Map<Character, Integer> freq = HuffmanCoding.getFrequencyMap("");
        assertNull(HuffmanCoding.buildTree(freq));

        // 空字符串编码
        assertEquals("", HuffmanCoding.encode("", new java.util.HashMap<>()));

        // 空字符串译码（边界）
        assertEquals("", HuffmanCoding.decode("", null));
    }

    // ==================== 三、压缩率验证 ====================

    @Test
    @DisplayName("哈夫曼编码应节省空间（压缩比 <= 100%）")
    void testCompressionRatio() {
        String[] texts = {
                "hello world",
                "go go gopher",
                "abracadabra",
                "this is a compression test",
                "AAAAABBBBBCCCCCDDDDDEEEEE"
        };

        for (String text : texts) {
            Object[] result = HuffmanCoding.fullEncode(text);
            String encoded = (String) result[2];

            int originalBits = text.length() * 16;  // Java char = 16 bit
            int compressedBits = encoded.length();

            assertTrue(compressedBits <= text.length() * 8,
                    "文本 '" + text + "' 的哈夫曼编码(" + compressedBits + "bit) "
                            + "不应超过 8倍字符数(" + (text.length() * 8) + "bit)");
        }
    }

    @Test
    @DisplayName("频率最高的字符应获得最短编码")
    void testMostFrequentHasShortestCode() {
        // "aaaabbbccd" — a 频率最高，应获得最短编码
        String text = "aaaabbbccd";
        Object[] result = HuffmanCoding.fullEncode(text);
        Map<Character, String> codes = (Map<Character, String>) result[1];

        int aLen = codes.get('a').length();
        int bLen = codes.get('b').length();
        int cLen = codes.get('c').length();
        int dLen = codes.get('d').length();

        assertTrue(aLen <= bLen, "频率最高的 'a' 编码长度应 <= 'b'");
        assertTrue(aLen <= cLen, "频率最高的 'a' 编码长度应 <= 'c'");
        assertTrue(aLen <= dLen, "频率最高的 'a' 编码长度应 <= 'd'");
    }

    // ==================== 四、前缀码特性验证 ====================

    @Test
    @DisplayName("哈夫曼编码应满足前缀码特性（无歧义）")
    void testPrefixCodeProperty() {
        String text = "the quick brown fox jumps over the lazy dog";
        Map<Character, Integer> freq = HuffmanCoding.getFrequencyMap(text);
        HuffmanCoding.HuffmanNode root = HuffmanCoding.buildTree(freq);
        Map<Character, String> codes = HuffmanCoding.generateCodes(root);

        // 验证：没有任何编码是另一个编码的前缀
        for (Map.Entry<Character, String> e1 : codes.entrySet()) {
            for (Map.Entry<Character, String> e2 : codes.entrySet()) {
                if (e1.getKey() != e2.getKey()) {
                    assertFalse(e2.getValue().startsWith(e1.getValue()),
                            "编码 '" + e1.getValue() + "' 不应是 '" + e2.getValue() + "' 的前缀");
                }
            }
        }
    }

    // ==================== 五、多组数据往返测试 ====================

    @Test
    @DisplayName("多组文本编解码往返一致性")
    void testMultipleTextsRoundTrip() {
        String[] texts = {
                "a",
                "ab",
                "abc",
                "hello",
                "this is a test",
                "AAAAABBBBBCCCCCDDDDD",
                "the quick brown fox jumps over the lazy dog",
                "Huffman coding is optimal for symbol-by-symbol coding",
                "12345!@#$%",
                "  leading and trailing spaces  "
        };

        for (String original : texts) {
            Object[] result = HuffmanCoding.fullEncode(original);
            HuffmanCoding.HuffmanNode root = (HuffmanCoding.HuffmanNode) result[0];
            String encoded = (String) result[2];
            String decoded = HuffmanCoding.decode(encoded, root);

            assertEquals(original, decoded,
                    "编解码往返失败: '" + original + "'");
        }
    }

    @Test
    @DisplayName("完整流程：频率→建树→编码→译码")
    void testFullWorkflow() {
        String original = "哈夫曼编码测试";

        // 1. 频率统计
        Map<Character, Integer> freq = HuffmanCoding.getFrequencyMap(original);
        assertFalse(freq.isEmpty());
        int totalFreq = freq.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(original.length(), totalFreq, "频率总和应等于文本长度");

        // 2. 建树
        HuffmanCoding.HuffmanNode root = HuffmanCoding.buildTree(freq);
        assertNotNull(root);
        assertEquals(totalFreq, root.freq, "根节点频率应等于所有字符频率之和");

        // 3. 编码表
        Map<Character, String> codes = HuffmanCoding.generateCodes(root);
        assertEquals(freq.size(), codes.size(), "编码表大小应等于不同字符数");

        // 4. 编码
        String encoded = HuffmanCoding.encode(original, codes);
        assertFalse(encoded.isEmpty());

        // 5. 译码
        String decoded = HuffmanCoding.decode(encoded, root);
        assertEquals(original, decoded);
    }

    @Test
    @DisplayName("频率统计准确性")
    void testFrequencyMapAccuracy() {
        String text = "aabbccc";
        Map<Character, Integer> freq = HuffmanCoding.getFrequencyMap(text);

        assertEquals(2, freq.get('a').intValue());
        assertEquals(2, freq.get('b').intValue());
        assertEquals(3, freq.get('c').intValue());
        assertEquals(3, freq.size());
    }

    @Test
    @DisplayName("编码不应包含 '.' 等非 0/1 字符")
    void testEncodedOnlyContains01() {
        String text = "test encoded string";
        String encoded = (String) HuffmanCoding.fullEncode(text)[2];

        assertTrue(encoded.matches("[01]+"), "编码结果应只包含 0 和 1");
    }

    @Test
    @DisplayName("非法编码串应抛出异常")
    void testDecodeMalformedString() {
        // 单字符树 "aaaa"：根左子='a', 根右子=null, 编码="0"
        // 输入 "1" 会走到 null 子节点 → 抛出异常
        Object[] result = HuffmanCoding.fullEncode("aaaa");
        HuffmanCoding.HuffmanNode root = (HuffmanCoding.HuffmanNode) result[0];
        assertThrows(IllegalArgumentException.class, () -> {
            HuffmanCoding.decode("1", root);
        });

        // 正常编码应能正常译码
        String encoded = (String) result[2];
        assertEquals("aaaa", HuffmanCoding.decode(encoded, root));

        // 正常编码中包含非法字符的处理依赖树结构，不在此处断言异常；
        // 但 decode 实现中遇到 null 子节点时会抛出 IllegalArgumentException
    }
}
