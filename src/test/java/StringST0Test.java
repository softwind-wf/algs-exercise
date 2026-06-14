

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import test5.StringST0;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StringST 单词查找树符号表的单元测试
 */
@DisplayName("StringST 单词查找树符号表测试")
class StringST0Test {

    private StringST0<Integer> st;

    @BeforeEach
    void setUp() {
        st = new StringST0<>();
    }

    // ==================== 基础功能测试 ====================

    @Test
    @DisplayName("测试新建符号表为空")
    void testInitialState() {
        assertTrue(st.isEmpty());
        assertEquals(0, st.size());
    }

    @Test
    @DisplayName("测试插入单个键值对")
    void testPutSingle() {
        st.put("hello", 1);
        assertFalse(st.isEmpty());
        assertEquals(1, st.size());
        assertEquals(1, st.get("hello"));
    }

    @Test
    @DisplayName("测试插入多个键值对")
    void testPutMultiple() {
        st.put("she", 0);
        st.put("sells", 1);
        st.put("sea", 2);
        st.put("shells", 3);

        assertEquals(4, st.size());
        assertEquals(0, st.get("she"));
        assertEquals(1, st.get("sells"));
        assertEquals(2, st.get("sea"));
        assertEquals(3, st.get("shells"));
    }

    @Test
    @DisplayName("测试更新已存在的键")
    void testUpdateExistingKey() {
        st.put("hello", 1);
        assertEquals(1, st.get("hello"));

        st.put("hello", 2);
        assertEquals(2, st.get("hello"));
        assertEquals(1, st.size()); // 大小不变
    }

    @Test
    @DisplayName("测试获取不存在的键返回null")
    void testGetNonExistent() {
        st.put("hello", 1);
        assertNull(st.get("world"));
        assertNull(st.get(""));
    }

    @Test
    @DisplayName("测试contains方法")
    void testContains() {
        st.put("hello", 1);
        assertTrue(st.contains("hello"));
        assertFalse(st.contains("world"));
        assertFalse(st.contains(""));
    }

    // ==================== 删除操作测试 ====================

    @Test
    @DisplayName("测试删除存在的键")
    void testDeleteExisting() {
        st.put("hello", 1);
        st.put("world", 2);
        assertEquals(2, st.size());

        st.delete("hello");
        assertEquals(1, st.size());
        assertNull(st.get("hello"));
        assertFalse(st.contains("hello"));
        assertEquals(2, st.get("world"));
    }

    @Test
    @DisplayName("测试删除不存在的键")
    void testDeleteNonExistent() {
        st.put("hello", 1);
        st.delete("world"); // 不应抛异常
        assertEquals(1, st.size());
        assertTrue(st.contains("hello"));
    }

    @Test
    @DisplayName("测试通过put null值删除键")
    void testPutNullToDelete() {
        st.put("hello", 1);
        assertEquals(1, st.size());

        st.put("hello", null);
        assertEquals(0, st.size());
        assertFalse(st.contains("hello"));
        assertNull(st.get("hello"));
    }

    @Test
    @DisplayName("测试删除后符号表为空")
    void testDeleteAll() {
        st.put("a", 1);
        st.put("b", 2);
        st.put("c", 3);

        st.delete("a");
        st.delete("b");
        st.delete("c");

        assertTrue(st.isEmpty());
        assertEquals(0, st.size());
    }

    // ==================== 边界情况测试 ====================

    @Test
    @DisplayName("测试空字符串键")
    void testEmptyStringKey() {
        st.put("", 100);
        assertEquals(1, st.size());
        assertEquals(100, st.get(""));
        assertTrue(st.contains(""));
    }

    @Test
    @DisplayName("测试单字符键")
    void testSingleCharKeys() {
        st.put("a", 1);
        st.put("b", 2);
        st.put("c", 3);

        assertEquals(3, st.size());
        assertEquals(1, st.get("a"));
        assertEquals(2, st.get("b"));
        assertEquals(3, st.get("c"));
    }

    @Test
    @DisplayName("测试长字符串键")
    void testLongStringKeys() {
        String longKey = "averylongstringthatisquiteunique";
        st.put(longKey, 999);
        assertEquals(1, st.size());
        assertEquals(999, st.get(longKey));
    }

    // ==================== 异常处理测试 ====================

    @Test
    @DisplayName("测试put null键抛出异常")
    void testPutNullKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            st.put(null, 1);
        });
    }

    @Test
    @DisplayName("测试get null键抛出异常")
    void testGetNullKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            st.get(null);
        });
    }

    @Test
    @DisplayName("测试delete null键抛出异常")
    void testDeleteNullKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            st.delete(null);
        });
    }

    @Test
    @DisplayName("测试contains null键抛出异常")
    void testContainsNullKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            st.contains(null);
        });
    }

    // ==================== longestPrefixOf 测试 ====================

    @Test
    @DisplayName("测试最长前缀-完全匹配")
    void testLongestPrefixExactMatch() {
        st.put("she", 0);
        st.put("sells", 1);
        st.put("shells", 3);

        assertEquals("shells", st.longestPrefixOf("shells"));
    }

    @Test
    @DisplayName("测试最长前缀-部分匹配")
    void testLongestPrefixPartialMatch() {
        st.put("she", 0);
        st.put("sells", 1);
        st.put("sea", 2);
        st.put("shells", 3);

        assertEquals("she", st.longestPrefixOf("shell"));
    }

    @Test
    @DisplayName("测试最长前缀-无匹配")
    void testLongestPrefixNoMatch() {
        st.put("she", 0);
        st.put("sells", 1);

        assertEquals("", st.longestPrefixOf("xyz"));
    }

    @Test
    @DisplayName("测试最长前缀-空字符串")
    void testLongestPrefixEmptyString() {
        st.put("she", 0);
        assertEquals("", st.longestPrefixOf(""));
    }

    @Test
    @DisplayName("测试最长前缀-多个可能前缀")
    void testLongestPrefixMultipleCandidates() {
        st.put("by", 4);
        st.put("bye", 5);
        st.put("byes", 6);

        assertEquals("byes", st.longestPrefixOf("byeser"));
        assertEquals("bye", st.longestPrefixOf("byeee"));
        assertEquals("by", st.longestPrefixOf("byzzz"));
        System.out.println(st.longestPrefixOf("b"));
    }

    @Test
    @DisplayName("测试longestPrefixOf null参数抛出异常")
    void testLongestPrefixOfNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            st.longestPrefixOf(null);
        });
    }

    // ==================== keysWithPrefix 测试 ====================

    @Test
    @DisplayName("测试keysWithPrefix-基本前缀匹配")
    void testKeysWithPrefixBasic() {
        st.put("she", 0);
        st.put("sells", 1);
        st.put("shells", 3);

        List<String> results = toList(st.keysWithPrefix("sh"));
        assertTrue(results.contains("she"));
        assertTrue(results.contains("shells"));
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("测试keysWithPrefix-无前缀匹配")
    void testKeysWithPrefixNoMatch() {
        st.put("she", 0);
        st.put("sells", 1);

        List<String> results = toList(st.keysWithPrefix("xyz"));
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("测试keysWithPrefix-空前缀返回所有键")
    void testKeysWithPrefixEmptyPrefix() {
        st.put("she", 0);
        st.put("sells", 1);
        st.put("sea", 2);

        List<String> results = toList(st.keysWithPrefix(""));
        assertEquals(3, results.size());
        assertTrue(results.contains("she"));
        assertTrue(results.contains("sells"));
        assertTrue(results.contains("sea"));
    }

    @Test
    @DisplayName("测试keysWithPrefix-完整键作为前缀")
    void testKeysWithPrefixCompleteKey() {
        st.put("she", 0);
        st.put("shells", 1);

        List<String> results = toList(st.keysWithPrefix("she"));
        assertTrue(results.contains("she"));
        assertTrue(results.contains("shells"));
        assertEquals(2, results.size());
    }

    // ==================== keysThatMatch 测试（通配符）====================

    @Test
    @DisplayName("测试keysThatMatch-无通配符精确匹配")
    void testKeysThatMatchNoWildcard() {
        st.put("she", 0);
        st.put("sells", 1);
        st.put("sea", 2);

        List<String> results = toList(st.keysThatMatch("she"));
        assertEquals(1, results.size());
        assertTrue(results.contains("she"));
    }

    @Test
    @DisplayName("测试keysThatMatch-单字符通配符")
    void testKeysThatMatchSingleWildcard() {
        st.put("she", 0);
        st.put("sho", 1);
        st.put("shy", 2);
        st.put("sea", 3);

        List<String> results = toList(st.keysThatMatch("sh."));
        assertEquals(3, results.size());
        assertTrue(results.contains("she"));
        assertTrue(results.contains("sho"));
        assertTrue(results.contains("shy"));
    }

    @Test
    @DisplayName("测试keysThatMatch-多个通配符")
    void testKeysThatMatchMultipleWildcards() {
        st.put("sea", 0);
        st.put("see", 1);
        st.put("set", 2);
        st.put("sed", 3);

        List<String> results = toList(st.keysThatMatch("se."));
        assertEquals(4, results.size());
        assertTrue(results.contains("sea"));
        assertTrue(results.contains("see"));
        assertTrue(results.contains("set"));
        assertTrue(results.contains("sed"));
    }

    @Test
    @DisplayName("测试keysThatMatch-通配符在开头")
    void testKeysThatMatchWildcardAtStart() {
        st.put("she", 0);
        st.put("the", 1);
        st.put("he", 2);

        List<String> results = toList(st.keysThatMatch(".he"));
        assertEquals(2, results.size());
        assertTrue(results.contains("she"));
        assertTrue(results.contains("the"));
    }

    @Test
    @DisplayName("测试keysThatMatch-模式长度超过所有键")
    void testKeysThatMatchPatternTooLong() {
        st.put("ab", 0);
        st.put("cd", 1);

        List<String> results = toList(st.keysThatMatch("abc"));
        assertTrue(results.isEmpty());
    }

    // ==================== keys 方法测试 ====================

    @Test
    @DisplayName("测试keys返回所有键")
    void testKeysReturnsAll() {
        st.put("she", 0);
        st.put("sells", 1);
        st.put("sea", 2);

        List<String> allKeys = toList(st.keys());
        assertEquals(3, allKeys.size());
        assertTrue(allKeys.contains("she"));
        assertTrue(allKeys.contains("sells"));
        assertTrue(allKeys.contains("sea"));
    }

    @Test
    @DisplayName("测试空符号表的keys")
    void testKeysEmpty() {
        List<String> allKeys = toList(st.keys());
        assertTrue(allKeys.isEmpty());
    }

    // ==================== 综合场景测试 ====================

    @Test
    @DisplayName("测试原书示例数据")
    void testBookExample() {
        st.put("she", 0);
        st.put("sells", 1);
        st.put("sea", 2);
        st.put("shells", 3);
        st.put("by", 4);
        st.put("the", 5);
        st.put("shore", 6);

        assertEquals(7, st.size());

        // 测试 longestPrefixOf
        assertEquals("she", st.longestPrefixOf("shell"));
        assertEquals("shells", st.longestPrefixOf("shells"));

        // 测试 keysWithPrefix
        List<String> shKeys = toList(st.keysWithPrefix("sh"));
        assertTrue(shKeys.contains("she"));
        assertTrue(shKeys.contains("shells"));

        // 测试 keysThatMatch
        List<String> matchKeys = toList(st.keysThatMatch("s.."));
        assertTrue(matchKeys.contains("she"));
        assertTrue(matchKeys.contains("sea"));
    }

    @Test
    @DisplayName("测试大量数据插入和查询")
    void testLargeDataset() {
        int size = 1000;
        for (int i = 0; i < size; i++) {
            st.put("key" + i, i);
        }

        assertEquals(size, st.size());

        // 随机验证一些键
        assertEquals(0, st.get("key0"));
        assertEquals(500, st.get("key500"));
        assertEquals(999, st.get("key999"));

        // 验证不存在的键
        assertNull(st.get("key1000"));
    }

    @Test
    @DisplayName("测试具有相同前缀的大量键")
    void testManyKeysWithSamePrefix() {
        st.put("test", 0);
        st.put("testing", 1);
        st.put("tester", 2);
        st.put("testers", 3);
        st.put("tests", 4);

        List<String> testKeys = toList(st.keysWithPrefix("test"));
        assertEquals(5, testKeys.size());
        assertTrue(testKeys.contains("test"));
        assertTrue(testKeys.contains("testing"));
        assertTrue(testKeys.contains("tester"));
        assertTrue(testKeys.contains("testers"));
        assertTrue(testKeys.contains("tests"));
    }

    // ==================== 辅助方法 ====================

    /**
     * 将 Iterable<String> 转换为 List<String>
     */
    private List<String> toList(Iterable<String> iterable) {
        List<String> list = new ArrayList<>();
        for (String item : iterable) {
            list.add(item);
        }
        return list;
    }
}
