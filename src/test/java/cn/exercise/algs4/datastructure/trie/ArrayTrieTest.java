package cn.exercise.algs4.datastructure.trie;

import cn.exercise.algs4.datastructure.trie.ArrayTrie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ArrayTrie 数组字典树测试")
class ArrayTrieTest {

    private ArrayTrie trie;

    @BeforeEach
    void setUp() {
        // range=123 覆盖 a-z(97-122) 等 ASCII 字符
        trie = new ArrayTrie(0, 123);
    }

    // ==================== 构造器测试 ====================

    @Nested
    @DisplayName("构造器测试")
    class ConstructorTest {

        @Test
        @DisplayName("默认构造器：空字典树")
        void defaultConstructor() {
            ArrayTrie t = new ArrayTrie();
            assertEquals(0, t.size());
            assertEquals(0, t.wordCount());
            assertTrue(t.isEmpty());
        }

        @Test
        @DisplayName("单参数range构造器")
        void singleRangeConstructor() {
            ArrayTrie t = new ArrayTrie(128);
            assertEquals(0, t.size());
            assertTrue(t.isEmpty());
        }

        @Test
        @DisplayName("双参数构造器：指定初始容量和range")
        void twoArgConstructor() {
            ArrayTrie t = new ArrayTrie(100, 256);
            assertEquals(0, t.size());
            assertEquals(0, t.wordCount());
            assertTrue(t.isEmpty());
        }

        @Test
        @DisplayName("range<=0抛出IllegalArgumentException")
        void invalidRange() {
            assertThrows(IllegalArgumentException.class, () -> new ArrayTrie(0, 0));
            assertThrows(IllegalArgumentException.class, () -> new ArrayTrie(0, -1));
        }
    }

    // ==================== add / size / wordCount / isEmpty 测试 ====================

    @Nested
    @DisplayName("插入与基本统计测试")
    class AddAndSizeTest {

        @Test
        @DisplayName("插入单个单词后size和wordCount各为1")
        void addSingleWord() {
            trie.add("hello");
            assertEquals(1, trie.size());
            assertEquals(1, trie.wordCount());
            assertFalse(trie.isEmpty());
        }

        @Test
        @DisplayName("重复插入同一单词：size不变，wordCount累加")
        void addDuplicateWord() {
            trie.add("abc");
            trie.add("abc");
            trie.add("abc");
            assertEquals(1, trie.size());
            assertEquals(3, trie.wordCount());
        }

        @Test
        @DisplayName("插入多个不同单词：size正确计数")
        void addMultipleDistinctWords() {
            trie.add("apple");
            trie.add("banana");
            trie.add("cherry");
            assertEquals(3, trie.size());
            assertEquals(3, trie.wordCount());
        }

        @Test
        @DisplayName("插入有前缀关系的单词：各自独立计数")
        void addWordsWithPrefixRelation() {
            trie.add("pen");
            trie.add("pencil");
            assertEquals(2, trie.size());
            assertEquals(2, trie.wordCount());
        }

        @Test
        @DisplayName("插入空字符串不报错")
        void addEmptyString() {
            assertDoesNotThrow(() -> trie.add(""));
            assertEquals(1, trie.size());
            assertEquals(1, trie.wordCount());
        }

        @Test
        @DisplayName("插入null抛出IllegalArgumentException")
        void addNull() {
            assertThrows(IllegalArgumentException.class, () -> trie.add(null));
        }

        @Test
        @DisplayName("插入超出range的字符抛出IllegalArgumentException")
        void addOutOfRangeChar() {
            ArrayTrie smallRange = new ArrayTrie(0, 10);
            assertThrows(IllegalArgumentException.class, () -> smallRange.add("abc"));
        }

        @Test
        @DisplayName("自动扩容：插入大量不同单词不报错")
        void autoExpand() {
            for (int i = 0; i < 100; i++) {
                trie.add("word" + i);
            }
            assertEquals(100, trie.size());
            assertEquals(100, trie.wordCount());
        }
    }

    // ==================== getCount / contains 测试 ====================

    @Nested
    @DisplayName("查询与包含测试")
    class GetCountAndContainsTest {

        @Test
        @DisplayName("查询已插入单词的插入次数")
        void getCountExisting() {
            trie.add("test");
            trie.add("test");
            assertEquals(2, trie.getCount("test"));
        }

        @Test
        @DisplayName("查询未插入单词返回0")
        void getCountNotExisting() {
            assertEquals(0, trie.getCount("missing"));
        }

        @Test
        @DisplayName("查询null返回0")
        void getCountNull() {
            assertEquals(0, trie.getCount(null));
        }

        @Test
        @DisplayName("contains对已插入单词返回true")
        void containsExisting() {
            trie.add("hello");
            assertTrue(trie.contains("hello"));
        }

        @Test
        @DisplayName("contains对仅作为前缀的字符串返回false")
        void containsPrefixOnly() {
            trie.add("hello");
            assertFalse(trie.contains("hel"));
        }

        @Test
        @DisplayName("contains对未插入单词返回false")
        void containsNotExisting() {
            assertFalse(trie.contains("xyz"));
        }

        @Test
        @DisplayName("contains对null返回false")
        void containsNull() {
            assertFalse(trie.contains(null));
        }
    }

    // ==================== isPrefix 测试 ====================

    @Nested
    @DisplayName("前缀判断测试")
    class IsPrefixTest {

        @BeforeEach
        void addWords() {
            trie.add("pencil");
            trie.add("pen");
            trie.add("paper");
        }

        @Test
        @DisplayName("完整单词也是自己的前缀")
        void isPrefixFullWord() {
            assertTrue(trie.isPrefix("pen"));
            assertTrue(trie.isPrefix("pencil"));
        }

        @Test
        @DisplayName("部分前缀返回true")
        void isPrefixPartial() {
            assertTrue(trie.isPrefix("pe"));
            assertTrue(trie.isPrefix("p"));
            assertTrue(trie.isPrefix("pap"));
        }

        @Test
        @DisplayName("不存在的前缀返回false")
        void isPrefixNotExisting() {
            assertFalse(trie.isPrefix("xyz"));
            assertFalse(trie.isPrefix("pb"));
        }

        @Test
        @DisplayName("null前缀返回false")
        void isPrefixNull() {
            assertFalse(trie.isPrefix(null));
        }

        @Test
        @DisplayName("空前串是任何字典树的前缀")
        void isPrefixEmpty() {
            assertTrue(trie.isPrefix(""));
        }
    }

    // ==================== countPrefix 测试 ====================

    @Nested
    @DisplayName("前缀计数测试")
    class CountPrefixTest {

        @BeforeEach
        void addWords() {
            trie.add("pencil");
            trie.add("pencil");
            trie.add("pencil");
            trie.add("pen");
            trie.add("paper");
        }

        @Test
        @DisplayName("前缀p命中所有以p开头的单词")
        void countPrefixP() {
            // pencil x3 + pen x1 + paper x1 = 5
            assertEquals(5, trie.countPrefix("p"));
        }

        @Test
        @DisplayName("前缀pe命中pen和pencil")
        void countPrefixPe() {
            // pen x1 + pencil x3 = 4
            assertEquals(4, trie.countPrefix("pe"));
        }

        @Test
        @DisplayName("前缀pen命中pen和pencil")
        void countPrefixPen() {
            // pen x1 + pencil x3 = 4
            assertEquals(4, trie.countPrefix("pen"));
        }

        @Test
        @DisplayName("不存在的前缀返回0")
        void countPrefixNotExisting() {
            assertEquals(0, trie.countPrefix("xyz"));
        }

        @Test
        @DisplayName("null前缀返回0")
        void countPrefixNull() {
            assertEquals(0, trie.countPrefix(null));
        }
    }

    // ==================== remove 测试 ====================

    @Nested
    @DisplayName("删除测试")
    class RemoveTest {

        @Test
        @DisplayName("删除存在的单词返回true，count减1")
        void removeExistingWord() {
            trie.add("hello");
            trie.add("hello");
            assertTrue(trie.remove("hello"));
            assertEquals(1, trie.getCount("hello"));
            assertEquals(1, trie.wordCount());
            assertEquals(1, trie.size());
        }

        @Test
        @DisplayName("删除单词最后一次插入：size减1")
        void removeLastInsertion() {
            trie.add("hello");
            assertTrue(trie.remove("hello"));
            assertEquals(0, trie.getCount("hello"));
            assertEquals(0, trie.size());
            assertEquals(0, trie.wordCount());
        }

        @Test
        @DisplayName("删除不存在的单词返回false")
        void removeNotExisting() {
            assertFalse(trie.remove("missing"));
        }

        @Test
        @DisplayName("删除null抛出IllegalArgumentException")
        void removeNull() {
            assertThrows(IllegalArgumentException.class, () -> trie.remove(null));
        }

        @Test
        @DisplayName("删除后自动剪除无用分支并回收节点")
        void removePrunesAndRecycles() {
            trie.add("pen");
            trie.add("pencil");
            int nodesBefore = trie.nodeCount(); // 7: root+p+e+n+c+i+l
            trie.remove("pen");
            assertFalse(trie.contains("pen"));
            assertTrue(trie.contains("pencil"));
            // pen的e/n节点因pencil路径存在不会被回收
            assertTrue(trie.isPrefix("pe"));
            assertTrue(trie.isPrefix("pen"));
            // n节点仍有子节点c，不会被回收，nodeCount不变
            assertEquals(nodesBefore, trie.nodeCount());

            // 再删除pencil，此时pen路径上所有节点都会被回收
            trie.remove("pencil");
            assertTrue(trie.nodeCount() < nodesBefore);
            assertEquals(1, trie.nodeCount()); // 只剩根节点
        }

        @Test
        @DisplayName("删除共享前缀的单词不影响另一个单词")
        void removeDoesNotAffectSibling() {
            trie.add("pen");
            trie.add("pencil");
            trie.remove("pencil");
            assertTrue(trie.contains("pen"));
            assertFalse(trie.contains("pencil"));
            assertEquals(1, trie.size());
        }

        @Test
        @DisplayName("节点回收后再次插入复用回收编号")
        void nodeRecycling() {
            trie.add("abc");
            trie.add("abd");
            trie.add("xyz");
            int nodesBefore = trie.nodeCount(); // root+a+b+c+d+x+y+z = 9

            // 删除abc，c节点被回收（b节点因abd仍存在不被回收）
            assertTrue(trie.remove("abc"));
            assertFalse(trie.contains("abc"));
            assertTrue(trie.contains("abd"));
            // c被回收，nodeCount减少1
            assertEquals(nodesBefore - 1, trie.nodeCount());

            // 再次插入abc，复用回收的c节点编号
            trie.add("abc");
            assertTrue(trie.contains("abc"));
            assertEquals(nodesBefore, trie.nodeCount());
        }
    }

    // ==================== nodeCount 测试 ====================

    @Nested
    @DisplayName("节点计数测试")
    class NodeCountTest {

        @Test
        @DisplayName("空字典树只有根节点")
        void emptyTrieNodeCount() {
            assertEquals(1, trie.nodeCount());
        }

        @Test
        @DisplayName("插入单词后节点数增加")
        void nodeCountAfterInsert() {
            trie.add("abc");
            // root + a + b + c = 4
            assertEquals(4, trie.nodeCount());
        }

        @Test
        @DisplayName("共享前缀的单词共享节点")
        void sharedPrefixNodes() {
            trie.add("pen");
            // root + p + e + n = 4
            assertEquals(4, trie.nodeCount());
            trie.add("pencil");
            // + c + i + l = 7
            assertEquals(7, trie.nodeCount());
        }
    }

    // ==================== getAllWords 测试 ====================

    @Nested
    @DisplayName("获取全部单词测试")
    class GetAllWordsTest {

        @Test
        @DisplayName("空字典树返回空列表")
        void emptyTrie() {
            List<String> words = trie.getAllWords();
            assertTrue(words.isEmpty());
        }

        @Test
        @DisplayName("返回的单词按字典序排列")
        void wordsInLexicographicOrder() {
            trie.add("banana");
            trie.add("apple");
            trie.add("cherry");
            List<String> words = trie.getAllWords();
            assertEquals(Arrays.asList("apple", "banana", "cherry"), words);
        }

        @Test
        @DisplayName("重复插入的单词只返回一次")
        void duplicateWordReturnedOnce() {
            trie.add("test");
            trie.add("test");
            trie.add("test");
            List<String> words = trie.getAllWords();
            assertEquals(1, words.size());
            assertEquals("test", words.get(0));
        }

        @Test
        @DisplayName("有前缀关系的单词都能返回")
        void prefixRelatedWords() {
            trie.add("he");
            trie.add("hello");
            trie.add("her");
            List<String> words = trie.getAllWords();
            assertEquals(Arrays.asList("he", "hello", "her"), words);
        }
    }

    // ==================== clear 测试 ====================

    @Nested
    @DisplayName("清空测试")
    class ClearTest {

        @Test
        @DisplayName("清空后字典树恢复初始状态")
        void clearResetsTrie() {
            trie.add("hello");
            trie.add("world");
            trie.clear();
            assertEquals(0, trie.size());
            assertEquals(0, trie.wordCount());
            assertTrue(trie.isEmpty());
            assertFalse(trie.contains("hello"));
            assertTrue(trie.getAllWords().isEmpty());
        }

        @Test
        @DisplayName("清空后nodeCount恢复为1（仅根节点）")
        void clearResetsNodeCount() {
            trie.add("abc");
            trie.clear();
            assertEquals(1, trie.nodeCount());
        }

        @Test
        @DisplayName("清空后可以重新使用")
        void clearAndReuse() {
            trie.add("abc");
            trie.clear();
            trie.add("xyz");
            assertEquals(1, trie.size());
            assertTrue(trie.contains("xyz"));
            assertFalse(trie.contains("abc"));
        }
    }

    // ==================== toString 测试 ====================

    @Test
    @DisplayName("toString返回全部单词的列表形式")
    void toStringTest() {
        trie.add("cat");
        trie.add("car");
        String result = trie.toString();
        assertEquals("[car, cat]", result);
    }

    // ==================== 综合场景测试 ====================

    @Nested
    @DisplayName("综合场景测试")
    class IntegrationTest {

        @Test
        @DisplayName("main方法示例场景验证")
        void mainMethodScenario() {
            trie.add("penguin");
            trie.add("pencil");
            trie.add("pencil");
            trie.add("pear");
            trie.add("pine");

            assertEquals(2, trie.getCount("pencil"));
            assertEquals(1, trie.getCount("penguin"));
            assertEquals(0, trie.getCount("pen"));
            assertFalse(trie.contains("pen"));
            assertTrue(trie.isPrefix("pen"));
            assertEquals(3, trie.countPrefix("pen"));
            assertEquals(4, trie.size());
            assertEquals(5, trie.wordCount());
            assertEquals(16, trie.nodeCount());
            assertEquals(Arrays.asList("pear", "pencil", "penguin", "pine"), trie.getAllWords());

            // 删除pencil两次
            assertTrue(trie.remove("pencil"));
            assertTrue(trie.remove("pencil"));
            assertEquals(0, trie.getCount("pencil"));
            assertFalse(trie.remove("abc"));
            assertEquals(13, trie.nodeCount());

            // 再次插入复用节点
            trie.add("pencil");
            assertEquals(1, trie.getCount("pencil"));
            assertEquals(16, trie.nodeCount());
        }

        @Test
        @DisplayName("大量单词插入与查询")
        void bulkInsertAndQuery() {
            String[] words = {"a", "ab", "abc", "abcd", "abcde", "b", "bc", "bcd"};
            for (String w : words) {
                trie.add(w);
            }
            assertEquals(8, trie.size());
            assertEquals(8, trie.wordCount());
            for (String w : words) {
                assertTrue(trie.contains(w), "应包含: " + w);
            }
            assertFalse(trie.contains("abcdef"));
            // 前缀"a" 命中 a, ab, abc, abcd, abcde 共5个
            assertEquals(5, trie.countPrefix("a"));
        }

        @Test
        @DisplayName("删除与插入交替：验证节点回收与复用稳定性")
        void interleavedInsertAndDelete() {
            trie.add("abc");
            trie.add("abd");
            trie.add("xyz");

            // 删除abc，回收c节点
            assertTrue(trie.remove("abc"));
            assertFalse(trie.contains("abc"));
            assertTrue(trie.contains("abd"));
            assertTrue(trie.contains("xyz"));

            // 插入新单词复用回收节点
            trie.add("xyzw");
            assertTrue(trie.contains("xyzw"));
            assertEquals(3, trie.size());

            // 全部删除
            assertTrue(trie.remove("abd"));
            assertTrue(trie.remove("xyz"));
            assertTrue(trie.remove("xyzw"));
            assertEquals(0, trie.size());
            assertEquals(0, trie.wordCount());
            assertTrue(trie.isEmpty());
        }
    }
}
