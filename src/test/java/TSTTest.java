import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import test5.StringST;
import test5.TST;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TST 三向单词查找树的单元测试
 */
@DisplayName("TST 三向单词查找树测试")
class TSTTest {

    private StringST<Integer> tst;

    @BeforeEach
    void setUp() {
        tst = new TST<>();
    }

    // ==================== 基础功能测试 ====================

    @Test
    @DisplayName("测试新建TST为空")
    void testInitialState() {
        assertTrue(tst.isEmpty());
        assertEquals(0, tst.size());
    }

    @Test
    @DisplayName("测试插入单个键值对")
    void testPutSingle() {
        tst.put("hello", 1);
        assertFalse(tst.isEmpty());
        assertEquals(1, tst.size());
        assertEquals(1, tst.get("hello"));
        assertTrue(tst.contains("hello"));
    }

    @Test
    @DisplayName("测试插入多个键值对")
    void testPutMultiple() {
        tst.put("she", 0);
        tst.put("sells", 1);
        tst.put("sea", 2);
        tst.put("shells", 3);

        assertEquals(4, tst.size());
        assertEquals(0, tst.get("she"));
        assertEquals(1, tst.get("sells"));
        assertEquals(2, tst.get("sea"));
        assertEquals(3, tst.get("shells"));
    }

    @Test
    @DisplayName("测试更新已存在的键")
    void testUpdateExistingKey() {
        tst.put("hello", 1);
        assertEquals(1, tst.get("hello"));
        assertEquals(1, tst.size());

        tst.put("hello", 2);
        assertEquals(2, tst.get("hello"));
        assertEquals(1, tst.size()); // 大小不变
    }

    @Test
    @DisplayName("测试获取不存在的键返回null")
    void testGetNonExistent() {
        tst.put("hello", 1);
        assertNull(tst.get("world"));
        assertFalse(tst.contains("world"));
    }

    // ==================== 删除操作测试 ====================

    @Test
    @DisplayName("测试删除存在的键")
    void testDeleteExisting() {
        tst.put("hello", 1);
        tst.put("world", 2);
        assertEquals(2, tst.size());

        tst.delete("hello");
        assertEquals(1, tst.size());
        assertNull(tst.get("hello"));
        assertFalse(tst.contains("hello"));
        assertEquals(2, tst.get("world"));
    }

    @Test
    @DisplayName("测试删除不存在的键")
    void testDeleteNonExistent() {
        tst.put("hello", 1);
        tst.delete("world"); // 不应抛异常
        assertEquals(1, tst.size());
        assertTrue(tst.contains("hello"));
    }

    @Test
    @DisplayName("测试通过put null值删除键")
    void testPutNullToDelete() {
        tst.put("hello", 1);
        assertEquals(1, tst.size());

        tst.put("hello", null);
        assertEquals(0, tst.size());
        assertFalse(tst.contains("hello"));
        assertNull(tst.get("hello"));
    }

    @Test
    @DisplayName("测试删除后符号表为空")
    void testDeleteAll() {
        tst.put("a", 1);
        tst.put("b", 2);
        tst.put("c", 3);

        tst.delete("a");
        tst.delete("b");
        tst.delete("c");

        assertTrue(tst.isEmpty());
        assertEquals(0, tst.size());
    }

    // ==================== 边界情况测试 ====================

    @Test
    @DisplayName("测试单字符键")
    void testSingleCharKeys() {
        tst.put("a", 1);
        tst.put("b", 2);
        tst.put("c", 3);

        assertEquals(3, tst.size());
        assertEquals(1, tst.get("a"));
        assertEquals(2, tst.get("b"));
        assertEquals(3, tst.get("c"));
    }

    @Test
    @DisplayName("测试长字符串键")
    void testLongStringKeys() {
        String longKey = "averylongstringthatisquiteunique";
        tst.put(longKey, 999);
        assertEquals(1, tst.size());
        assertEquals(999, tst.get(longKey));
    }

    @Test
    @DisplayName("测试具有相同前缀的键")
    void testKeysWithCommonPrefix() {
        tst.put("test", 0);
        tst.put("testing", 1);
        tst.put("tester", 2);
        tst.put("testers", 3);

        assertEquals(4, tst.size());
        assertEquals(0, tst.get("test"));
        assertEquals(1, tst.get("testing"));
        assertEquals(2, tst.get("tester"));
        assertEquals(3, tst.get("testers"));
    }

    // ==================== 异常处理测试 ====================

    @Test
    @DisplayName("测试put null键抛出异常")
    void testPutNullKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            tst.put(null, 1);
        });
    }

    @Test
    @DisplayName("测试put空字符串抛出异常")
    void testPutEmptyKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            tst.put("", 1);
        });
    }

    @Test
    @DisplayName("测试get null键抛出异常")
    void testGetNullKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            tst.get(null);
        });
    }

    @Test
    @DisplayName("测试get空字符串抛出异常")
    void testGetEmptyKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            tst.get("");
        });
    }

    @Test
    @DisplayName("测试delete null键抛出异常")
    void testDeleteNullKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            tst.delete(null);
        });
    }

    @Test
    @DisplayName("测试delete空字符串抛出异常")
    void testDeleteEmptyKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            tst.delete("");
        });
    }

    @Test
    @DisplayName("测试contains null键抛出异常")
    void testContainsNullKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            tst.contains(null);
        });
    }

    @Test
    @DisplayName("测试contains空字符串抛出异常")
    void testContainsEmptyKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            tst.contains("");
        });
    }

    // ==================== longestPrefixOf 测试 ====================

    @Test
    @DisplayName("测试最长前缀-完全匹配")
    void testLongestPrefixExactMatch() {
        tst.put("she", 0);
        tst.put("sells", 1);
        tst.put("shells", 3);

        assertEquals("shells", tst.longestPrefixOf("shells"));
    }

    @Test
    @DisplayName("测试最长前缀-部分匹配")
    void testLongestPrefixPartialMatch() {
        tst.put("she", 0);
        tst.put("sells", 1);
        tst.put("sea", 2);
        tst.put("shells", 3);

        assertEquals("she", tst.longestPrefixOf("shell"));
    }

    @Test
    @DisplayName("测试最长前缀-无匹配")
    void testLongestPrefixNoMatch() {
        tst.put("she", 0);
        tst.put("sells", 1);

        assertEquals("",tst.longestPrefixOf("xyz"));
    }

    @Test
    @DisplayName("测试最长前缀-空字符串")
    void testLongestPrefixEmptyString() {
        tst.put("she", 0);
        assertNull(tst.longestPrefixOf(""));
    }

    @Test
    @DisplayName("测试最长前缀-null参数")
    void testLongestPrefixNull() {
        tst.put("she", 0);
        assertNull(tst.longestPrefixOf(null));
    }

    @Test
    @DisplayName("测试最长前缀-多个可能前缀")
    void testLongestPrefixMultipleCandidates() {
        tst.put("by", 4);
        tst.put("bye", 5);
        tst.put("byes", 6);

        assertEquals("byes", tst.longestPrefixOf("byeser"));
        assertEquals("bye", tst.longestPrefixOf("byeee"));
        assertEquals("by", tst.longestPrefixOf("byzzz"));
    }

    @Test
    @DisplayName("测试最长前缀-原书示例")
    void testLongestPrefixBookExample() {
        tst.put("she", 0);
        tst.put("sells", 1);
        tst.put("sea", 2);
        tst.put("shells", 3);

        assertEquals("shells", tst.longestPrefixOf("shellsort"));
    }

    // ==================== keysWithPrefix 测试 ====================

    @Test
    @DisplayName("测试keysWithPrefix-基本前缀匹配")
    void testKeysWithPrefixBasic() {
        tst.put("she", 0);
        tst.put("sells", 1);
        tst.put("shells", 3);

        List<String> results = toList(tst.keysWithPrefix("sh"));
        assertTrue(results.contains("she"));
        assertTrue(results.contains("shells"));
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("测试keysWithPrefix-无前缀匹配")
    void testKeysWithPrefixNoMatch() {
        tst.put("she", 0);
        tst.put("sells", 1);

        List<String> results = toList(tst.keysWithPrefix("xyz"));
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("测试keysWithPrefix-空前缀返回所有键")
    void testKeysWithPrefixEmptyPrefix() {
        tst.put("she", 0);
        tst.put("sells", 1);
        tst.put("sea", 2);

        List<String> results = toList(tst.keysWithPrefix(""));
        assertEquals(3, results.size());
        assertTrue(results.contains("she"));
        assertTrue(results.contains("sells"));
        assertTrue(results.contains("sea"));
    }

    @Test
    @DisplayName("测试keysWithPrefix-完整键作为前缀")
    void testKeysWithPrefixCompleteKey() {
        tst.put("she", 0);
        tst.put("shells", 1);

        List<String> results = toList(tst.keysWithPrefix("she"));
        assertTrue(results.contains("she"));
        assertTrue(results.contains("shells"));
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("测试keysWithPrefix-单字符前缀")
    void testKeysWithPrefixSingleChar() {
        tst.put("by", 4);
        tst.put("the", 5);
        tst.put("shore", 6);

        List<String> bKeys = toList(tst.keysWithPrefix("b"));
        assertEquals(1, bKeys.size());
        assertTrue(bKeys.contains("by"));
    }

    // ==================== keysThatMatch 测试（通配符）====================

    @Test
    @DisplayName("测试keysThatMatch-无通配符精确匹配")
    void testKeysThatMatchNoWildcard() {
        tst.put("she", 0);
        tst.put("sells", 1);
        tst.put("sea", 2);

        List<String> results = toList(tst.keysThatMatch("she"));
        assertEquals(1, results.size());
        assertTrue(results.contains("she"));
    }

    @Test
    @DisplayName("测试keysThatMatch-单字符通配符")
    void testKeysThatMatchSingleWildcard() {
        tst.put("she", 0);
        tst.put("sho", 1);
        tst.put("shy", 2);
        tst.put("sea", 3);

        List<String> results = toList(tst.keysThatMatch("sh."));
        assertEquals(3, results.size());
        assertTrue(results.contains("she"));
        assertTrue(results.contains("sho"));
        assertTrue(results.contains("shy"));
    }

    @Test
    @DisplayName("测试keysThatMatch-多个通配符")
    void testKeysThatMatchMultipleWildcards() {
        tst.put("sea", 0);
        tst.put("see", 1);
        tst.put("set", 2);
        tst.put("sed", 3);

        List<String> results = toList(tst.keysThatMatch("se."));
        assertEquals(4, results.size());
        assertTrue(results.contains("sea"));
        assertTrue(results.contains("see"));
        assertTrue(results.contains("set"));
        assertTrue(results.contains("sed"));
    }

    @Test
    @DisplayName("测试keysThatMatch-通配符在开头")
    void testKeysThatMatchWildcardAtStart() {
        tst.put("she", 0);
        tst.put("the", 1);
        tst.put("he", 2);

        List<String> results = toList(tst.keysThatMatch(".he"));
        assertEquals(2, results.size());
        assertTrue(results.contains("she"));
        assertTrue(results.contains("the"));
    }

    @Test
    @DisplayName("测试keysThatMatch-模式长度超过所有键")
    void testKeysThatMatchPatternTooLong() {
        tst.put("ab", 0);
        tst.put("cd", 1);

        List<String> results = toList(tst.keysThatMatch("abc"));
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("测试keysThatMatch-原书示例")
    void testKeysThatMatchBookExample() {
        tst.put("she", 0);
        tst.put("sells", 1);
        tst.put("sea", 2);
        tst.put("shells", 3);

        List<String> results = toList(tst.keysThatMatch(".ea"));
        assertEquals(1, results.size());
        assertTrue(results.contains("sea"));
    }

    // ==================== keys 方法测试 ====================

    @Test
    @DisplayName("测试keys返回所有键")
    void testKeysReturnsAll() {
        tst.put("she", 0);
        tst.put("sells", 1);
        tst.put("sea", 2);

        List<String> allKeys = toList(tst.keys());
        assertEquals(3, allKeys.size());
        assertTrue(allKeys.contains("she"));
        assertTrue(allKeys.contains("sells"));
        assertTrue(allKeys.contains("sea"));
    }

    @Test
    @DisplayName("测试空TST的keys")
    void testKeysEmpty() {
        List<String> allKeys = toList(tst.keys());
        assertTrue(allKeys.isEmpty());
    }

    @Test
    @DisplayName("测试keys按字典序排列")
    void testKeysInSortedOrder() {
        tst.put("banana", 1);
        tst.put("apple", 2);
        tst.put("cherry", 3);
        tst.put("date", 4);

        List<String> allKeys = toList(tst.keys());
        assertEquals(4, allKeys.size());
        assertEquals("apple", allKeys.get(0));
        assertEquals("banana", allKeys.get(1));
        assertEquals("cherry", allKeys.get(2));
        assertEquals("date", allKeys.get(3));
    }

    // ==================== 综合场景测试 ====================

    @Test
    @DisplayName("测试原书示例数据")
    void testBookExample() {
        String[] keys = {"she", "sells", "sea", "shells", "by", "the", "shore"};
        for (int i = 0; i < keys.length; i++) {
            tst.put(keys[i], i);
        }

        assertEquals(7, tst.size());

        // 测试 keysWithPrefix
        List<String> sheKeys = toList(tst.keysWithPrefix("she"));
        assertTrue(sheKeys.contains("she"));
        assertTrue(sheKeys.contains("shells"));

        // 测试 longestPrefixOf
        assertEquals("shells", tst.longestPrefixOf("shellsort"));

        // 测试 keysThatMatch
        List<String> matchKeys = toList(tst.keysThatMatch(".ea"));
        assertEquals(1, matchKeys.size());
        assertTrue(matchKeys.contains("sea"));

        // 测试删除
        tst.delete("sea");
        assertEquals(6, tst.size());
        assertFalse(tst.contains("sea"));
    }

    @Test
    @DisplayName("测试大量数据插入和查询")
    void testLargeDataset() {
        int size = 1000;
        for (int i = 0; i < size; i++) {
            tst.put("key" + i, i);
        }

        assertEquals(size, tst.size());

        // 随机验证一些键
        assertEquals(0, tst.get("key0"));
        assertEquals(500, tst.get("key500"));
        assertEquals(999, tst.get("key999"));

        // 验证不存在的键
        assertNull(tst.get("key1000"));
    }

    @Test
    @DisplayName("测试具有相同前缀的大量键")
    void testManyKeysWithSamePrefix() {
        tst.put("test", 0);
        tst.put("testing", 1);
        tst.put("tester", 2);
        tst.put("testers", 3);
        tst.put("tests", 4);

        List<String> testKeys = toList(tst.keysWithPrefix("test"));
        assertEquals(5, testKeys.size());
        assertTrue(testKeys.contains("test"));
        assertTrue(testKeys.contains("testing"));
        assertTrue(testKeys.contains("tester"));
        assertTrue(testKeys.contains("testers"));
        assertTrue(testKeys.contains("tests"));
    }

    @Test
    @DisplayName("测试create工厂方法")
    void testCreateFactoryMethod() {
        StringST<Integer> newSt = tst.create();
        assertNotNull(newSt);
        assertTrue(newSt.isEmpty());
        assertEquals(0, newSt.size());
        
        // 验证创建的是新的实例
        tst.put("hello", 1);
        assertFalse(tst.contains("hello") == false);
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
