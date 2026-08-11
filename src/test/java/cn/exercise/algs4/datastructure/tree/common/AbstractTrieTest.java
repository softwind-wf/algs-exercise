package cn.exercise.algs4.datastructure.tree.common;

import cn.exercise.algs4.datastructure.tree.common.AbstractTrie;
import cn.exercise.algs4.datastructure.tree.common.HashTrie;
import cn.exercise.algs4.datastructure.tree.common.LinkedTrie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字典树抽象层测试 —— 同一组断言对 LinkedTrie(数组存储) 与 HashTrie(Map 存储) 双跑，
 * 验证六个存储原语之上的全部通用逻辑在两种实现下行为一致
 */
@DisplayName("AbstractTrie 字典树抽象层(LinkedTrie + HashTrie)")
class AbstractTrieTest {

    /** 两种实现实例,同一组断言双跑 */
    private static AbstractTrie[] newInstances() {
        // LinkedTrie 用 range=123 覆盖 a-z(97-122);HashTrie 无 range 限制
        return new AbstractTrie[]{new LinkedTrie(123), new HashTrie()};
    }

    /** 对某个实例跑一组通用断言 */
    private void runCommon(AbstractTrie trie) {
        // ---- 空树状态 ----
        assertTrue(trie.isEmpty());
        assertEquals(0, trie.size());
        assertEquals(-1, trie.height(), "空树高度应为 -1");
        assertEquals(Collections.emptyList(), trie.getAllWords());

        // ---- 插入与计数 ----
        assertTrue(trie.insert("pencil"));
        trie.add("pen");
        trie.add("pencil");
        trie.add("pencil");
        // 重复 insert 返回 false(未新增不同单词),但按 Trie 语义仍累计计数
        assertFalse(trie.insert("pencil"), "重复插入应返回 false(仅计数增加)");
        assertEquals(2, trie.size(), "不同单词数应为 2");
        assertEquals(5, trie.wordCount(), "累计插入次数应为 5");
        assertEquals(4, trie.getCount("pencil"));
        assertEquals(1, trie.getCount("pen"));
        assertEquals(0, trie.getCount("abc"));

        // ---- 包含与前缀 ----
        assertTrue(trie.contains("pen"));
        assertFalse(trie.contains("pe"), "pe 只是前缀不是完整单词");
        assertTrue(trie.isPrefix("pe"));
        assertTrue(trie.isPrefix("p"));
        assertEquals(5, trie.countPrefix("p"), "前缀 p 的累计次数应为 5");
        assertEquals(4, trie.countPrefix("pencil"));
        assertEquals(0, trie.countPrefix("x"));

        // ---- 字典序全部单词 ----
        assertEquals(Arrays.asList("pen", "pencil"), trie.getAllWords());
        assertEquals(Arrays.asList("pen", "pencil"), trie.inorder(), "inorder 应与字典序单词一致");

        // ---- 删除与剪除 ----
        assertTrue(trie.remove("pen"));
        assertFalse(trie.remove("pen"), "再删不存在应返回 false");
        assertFalse(trie.contains("pen"));
        assertEquals(4, trie.getCount("pencil"), "删除 pen 不影响 pencil");
        assertEquals(1, trie.size());
        assertEquals(4, trie.wordCount());
        // "pen" 单词被删,但 p-e-n 路径仍被 "pencil" 使用,因此仍是前缀
        assertTrue(trie.isPrefix("pen"), "pen 路径仍被 pencil 使用,应是前缀");
        assertTrue(trie.isPrefix("pe"), "p-e 路径被 pencil 保留");

        // ---- Tree 接口一致性 ----
        assertEquals(6, trie.height(), "最长单词 pencil 长度为 6");

        // ---- 清空 ----
        trie.clear();
        assertTrue(trie.isEmpty());
        assertEquals(0, trie.size());
        assertEquals(0, trie.wordCount());
        assertEquals(-1, trie.height());
        assertEquals(Collections.emptyList(), trie.getAllWords());
    }

    @Nested
    @DisplayName("两个实现双跑通用行为")
    class CommonBehavior {

        @Test
        @DisplayName("LinkedTrie(数组存储)")
        void linkedTrie() {
            runCommon(new LinkedTrie(123));
        }

        @Test
        @DisplayName("HashTrie(Map存储)")
        void hashTrie() {
            runCommon(new HashTrie());
        }
    }

    @Nested
    @DisplayName("遍历与迭代")
    class TraversalTest {

        @Test
        @DisplayName("两种实现的层序/迭代/字典序一致")
        void traversalsConsistent() {
            for (AbstractTrie trie : newInstances()) {
                for (String w : new String[]{"ape", "apple", "apricot", "banana", "bat", "cat"}) {
                    trie.add(w);
                }
                assertEquals(Arrays.asList("ape", "apple", "apricot", "banana", "bat", "cat"),
                        trie.getAllWords(), "字典序输出错误");

                // 层序包含全部节点字符(不含虚拟根);共享前缀后共 21 个节点
                List<String> level = trie.levelOrder();
                assertEquals(21, level.size(), "层序应输出全部 21 个节点字符");
                assertTrue(level.contains("a") && level.contains("c"));

                // 迭代器与 inorder 一致
                List<String> collected = new ArrayList<>();
                for (Iterator<String> it = trie.iterator(); it.hasNext(); ) {
                    collected.add(it.next());
                }
                assertEquals(trie.inorder(), collected);
            }
        }
    }

    @Nested
    @DisplayName("边界与异常")
    class EdgeTest {

        @Test
        @DisplayName("null 单词插入/删除抛出异常")
        void nullThrows() {
            for (AbstractTrie trie : newInstances()) {
                assertThrows(IllegalArgumentException.class, () -> trie.insert(null));
                assertThrows(IllegalArgumentException.class, () -> trie.remove(null));
                assertEquals(0, trie.getCount(null));
                assertFalse(trie.isPrefix(null));
                assertEquals(0, trie.countPrefix(null));
            }
        }

        @Test
        @DisplayName("空串可作为单词插入")
        void emptyStringIsWord() {
            for (AbstractTrie trie : newInstances()) {
                trie.add("");
                assertTrue(trie.contains(""));
                assertTrue(trie.isPrefix(""));
                assertEquals(1, trie.getCount(""));
                assertEquals(1, trie.size());
                assertEquals(Arrays.asList(""), trie.getAllWords());
            }
        }

        @Test
        @DisplayName("LinkedTrie range 越界抛异常")
        void linkedTrieRangeViolation() {
            LinkedTrie t = new LinkedTrie(100);   // 'a'=97 OK,'~'=126 越界
            t.add("a");                           // 未越界,正常
            assertThrows(IllegalArgumentException.class, () -> t.add("~"));
        }

        @Test
        @DisplayName("LinkedTrie 非法 range 构造抛异常")
        void linkedTrieInvalidRange() {
            assertThrows(IllegalArgumentException.class, () -> new LinkedTrie(0));
        }

        @Test
        @DisplayName("HashTrie 支持中文")
        void hashTrieSupportsChinese() {
            HashTrie t = new HashTrie();
            t.add("苹果");
            t.add("苹果派");
            t.add("香蕉");
            assertEquals(3, t.size());
            assertEquals(2, t.countPrefix("苹"));
            assertEquals(1, t.getCount("苹果"));
            assertEquals(Arrays.asList("苹果", "苹果派", "香蕉"), t.getAllWords());
            assertEquals(3, t.height(), "最长单词'苹果派'长度为 3");
        }
    }

    @Nested
    @DisplayName("随机对拍与剪除")
    class StressTest {

        @Test
        @DisplayName("随机增删后两实现状态完全一致")
        void twoImplsStayInSync() {
            LinkedTrie lt = new LinkedTrie(123);
            HashTrie ht = new HashTrie();
            Random rnd = new Random(42);
            for (int step = 0; step < 3000; step++) {
                String w = randomWord(rnd, 1 + rnd.nextInt(5));
                if (rnd.nextBoolean()) {
                    assertEquals(lt.insert(w), ht.insert(w), "insert 返回值不一致");
                } else {
                    assertEquals(lt.remove(w), ht.remove(w), "remove 返回值不一致");
                }
                if (step % 100 == 0) {
                    assertEquals(lt.size(), ht.size(), "size 不一致");
                    assertEquals(lt.wordCount(), ht.wordCount(), "wordCount 不一致");
                    assertEquals(lt.getAllWords(), ht.getAllWords(), "单词集不一致");
                    assertEquals(lt.getCount(w), ht.getCount(w), "getCount 不一致");
                }
            }
        }

        @Test
        @DisplayName("删除后分支被正确剪除(前缀统计随剪除变化)")
        void pruningWorks() {
            for (AbstractTrie trie : newInstances()) {
                trie.add("cat");
                trie.add("car");
                trie.add("cart");
                assertEquals(3, trie.countPrefix("ca"));
                trie.remove("cart");
                // "cart" 的 't' 分支应被剪除,但 "cat"/"car" 保留
                assertFalse(trie.isPrefix("cart"));
                assertTrue(trie.isPrefix("car"));
                assertTrue(trie.isPrefix("cat"));
                assertEquals(2, trie.countPrefix("ca"));
                trie.remove("car");
                trie.remove("cat");
                assertFalse(trie.isPrefix("ca"), "全删后分支应剪除干净");
                assertTrue(trie.isEmpty());
            }
        }

        @Test
        @DisplayName("单词可重复插入与多次删除直到归零")
        void repeatedInsertRemove() {
            for (AbstractTrie trie : newInstances()) {
                trie.add("go");
                trie.add("go");
                trie.add("go");
                assertEquals(3, trie.getCount("go"));
                assertEquals(3, trie.wordCount());
                assertTrue(trie.remove("go"));
                assertEquals(2, trie.getCount("go"));
                assertTrue(trie.remove("go"));
                assertTrue(trie.remove("go"));
                assertFalse(trie.remove("go"));
                assertFalse(trie.contains("go"));
                assertTrue(trie.isEmpty());
            }
        }
    }

    /** 生成长度 1..maxLen 的随机小写单词(字符范围 97..120) */
    private static String randomWord(Random rnd, int maxLen) {
        int len = 1 + rnd.nextInt(maxLen);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append((char) ('a' + rnd.nextInt(24)));
        }
        return sb.toString();
    }
}
