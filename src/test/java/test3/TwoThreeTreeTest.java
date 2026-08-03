package test3;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * test3.TwoThreeTree 单元测试 —— 用行为验证（中序有序、无重复、get 正确）间接校验结构。
 */
@DisplayName("test3.TwoThreeTree 测试")
class TwoThreeTreeTest {

    private static List<Integer> keysOf(TwoThreeTree<Integer, Integer> t) {
        List<Integer> keys = new ArrayList<>();
        for (Integer k : t.keys()) {
            keys.add(k);
        }
        return keys;
    }

    @Test
    @DisplayName("递增插入 100 个唯一键，中序应严格升序")
    void insertSequential() {
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        for (int i = 1; i <= 100; i++) {
            t.put(i, i);
        }
        List<Integer> keys = keysOf(t);
        assertEquals(100, keys.size());
        for (int i = 1; i <= 100; i++) {
            assertEquals(i, keys.get(i - 1));
        }
    }

    @Test
    @DisplayName("递减插入 100 个唯一键")
    void insertReversed() {
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        for (int i = 100; i >= 1; i--) {
            t.put(i, i);
        }
        List<Integer> keys = keysOf(t);
        for (int i = 1; i <= 100; i++) {
            assertEquals(i, keys.get(i - 1));
        }
    }

    @Test
    @DisplayName("5000 个随机唯一键与 TreeSet 对照")
    void randomUniqueMatchesTreeSet() {
        Random rand = new Random(42);
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        Set<Integer> ref = new TreeSet<>();
        for (int i = 0; i < 5000; i++) {
            int v = rand.nextInt(100000);
            t.put(v, v);
            ref.add(v);
        }
        assertEquals(new ArrayList<>(ref), keysOf(t));
    }

    @Test
    @DisplayName("重复键应更新值而不新增结点")
    void duplicateKeysUpdateValueNotGrow() {
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        for (int i = 1; i <= 50; i++) {
            t.put(i, i);
        }
        // 重复插入所有键，值应更新、键数量不变
        for (int i = 1; i <= 50; i++) {
            t.put(i, i * 100);
        }
        List<Integer> keys = keysOf(t);
        assertEquals(50, keys.size(), "重复键不应让树里出现重复");
        for (int i = 1; i <= 50; i++) {
            assertEquals(i, keys.get(i - 1));
            assertEquals(i * 100, t.get(i), "值应被更新");
        }
    }

    @Test
    @DisplayName("3000 个随机键（大量重复）与 TreeSet 对照")
    void randomDuplicateKeys() {
        Random rand = new Random(7);
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        Set<Integer> ref = new TreeSet<>();
        for (int i = 0; i < 3000; i++) {
            int v = rand.nextInt(500);
            t.put(v, i);
            ref.add(v);
        }
        assertEquals(new ArrayList<>(ref), keysOf(t), "树中不应出现重复键");
        for (int v : ref) {
            assertNotNull(t.get(v));
        }
    }

    @Test
    @DisplayName("main 里的字符串例子")
    void stringExampleFromMain() {
        TwoThreeTree<String, Integer> st = new TwoThreeTree<>();
        String[] keys = {"S", "E", "A", "R", "C", "H", "X", "M", "P", "L"};
        for (int i = 0; i < keys.length; i++) {
            st.put(keys[i], i);
        }
        List<String> k = new ArrayList<>();
        for (String s : st.keys()) {
            k.add(s);
        }
        assertEquals(Arrays.asList("A", "C", "E", "H", "L", "M", "P", "R", "S", "X"), k);
        assertEquals(5, st.get("H"));   // H 在 keys[] 中是第 6 个，值=5
        assertEquals(7, st.get("M"));
        assertNull(st.get("Z"));
    }

    // ==================== 删除 ====================

    @Test
    @DisplayName("空树删除返回 false")
    void deleteFromEmpty() {
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        assertFalse(t.delete(1));
    }

    @Test
    @DisplayName("删除不存在的键返回 false，树不变")
    void deleteMissing() {
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        for (int i = 1; i <= 20; i++) {
            t.put(i, i);
        }
        assertFalse(t.delete(99));
        assertFalse(t.delete(0));
        assertEquals(20, keysOf(t).size());
    }

    @Test
    @DisplayName("从小到大依次删除，最终清空")
    void deleteAllSequential() {
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        for (int i = 1; i <= 50; i++) {
            t.put(i, i);
        }
        for (int i = 1; i <= 50; i++) {
            assertTrue(t.delete(i), "删除 " + i + " 应成功");
            assertEquals(50 - i, keysOf(t).size());
            // 每次删除后中序仍严格升序
            List<Integer> keys = keysOf(t);
            for (int k = 1; k < keys.size(); k++) {
                assertTrue(keys.get(k - 1) < keys.get(k));
            }
        }
        assertEquals(0, keysOf(t).size());
    }

    @Test
    @DisplayName("随机键全部删光")
    void deleteAllRandom() {
        Random rand = new Random(11);
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        List<Integer> keys = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            int v = rand.nextInt(1000);
            if (!keys.contains(v)) {
                keys.add(v);
            }
        }
        for (int v : keys) {
            t.put(v, v);
        }
        List<Integer> shuffled = new ArrayList<>(keys);
        java.util.Collections.shuffle(shuffled, new Random(31));
        for (int v : shuffled) {
            assertTrue(t.delete(v), "删除 " + v + " 应成功");
        }
        assertTrue(t.keys().iterator().hasNext() == false);
    }

    @Test
    @DisplayName("随机删除一半后与 TreeSet 对照")
    void deleteHalfMatchesTreeSet() {
        Random rand = new Random(17);
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        Set<Integer> ref = new TreeSet<>();
        for (int i = 0; i < 500; i++) {
            int v = rand.nextInt(5000);
            t.put(v, v);
            ref.add(v);
        }
        List<Integer> del = new ArrayList<>(ref);
        java.util.Collections.shuffle(del, new Random(29));
        int cnt = 0;
        for (int v : del) {
            if (cnt++ % 2 == 0) {
                assertTrue(t.delete(v));
                ref.remove(v);
                assertEquals(new ArrayList<>(ref), keysOf(t), "删除 " + v + " 后中序与 TreeSet 不一致");
            }
        }
    }

    @Test
    @DisplayName("删除后重新插入")
    void deleteThenReinsert() {
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        for (int i = 1; i <= 30; i++) {
            t.put(i, i);
        }
        assertTrue(t.delete(15));
        assertNull(t.get(15));
        assertFalse(t.delete(15));
        t.put(15, 150);
        assertEquals(150, t.get(15));
        List<Integer> keys = keysOf(t);
        assertEquals(30, keys.size());
        for (int i = 1; i <= 30; i++) {
            assertEquals(i, keys.get(i - 1));
        }
    }

    @Test
    @DisplayName("删除内部结点的键后 get 返回 null")
    void getAfterDeleteInternal() {
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        for (int i = 1; i <= 100; i++) {
            t.put(i, i);
        }
        // 50 位于内部结点
        assertNotNull(t.get(50));
        assertTrue(t.delete(50));
        assertNull(t.get(50));
        assertEquals(99, keysOf(t).size());
    }

    @Test
    @DisplayName("随机交错 put/delete/update 后与 TreeSet 对照")
    void interleavedOperationsMatchTreeSet() {
        Random rand = new Random(2024);
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        Set<Integer> ref = new TreeSet<>();
        for (int op = 0; op < 20000; op++) {
            int v = rand.nextInt(2000);
            switch (rand.nextInt(3)) {
                case 0: // 插入或更新
                    t.put(v, op);
                    ref.add(v);
                    break;
                case 1: // 删除
                    boolean found = t.delete(v);
                    boolean expected = ref.remove(v);
                    assertEquals(expected, found, "第 " + op + " 步删除 " + v + " 返回值不一致");
                    break;
                default: // 读取：存在性应与 TreeSet 一致
                    assertEquals(ref.contains(v), t.get(v) != null,
                            "第 " + op + " 步读取 " + v + " 存在性不一致");
                    break;
            }
            // 每隔一段校验一次整体有序性
            if (op % 2000 == 0) {
                assertEquals(new ArrayList<>(ref), keysOf(t), "第 " + op + " 步后中序与 TreeSet 不一致");
            }
        }
        assertEquals(new ArrayList<>(ref), keysOf(t));
    }

    // ==================== 完整符号表 API ====================

    @Test
    @DisplayName("size/isEmpty/contains")
    void sizeAndContains() {
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        assertTrue(t.isEmpty());
        assertEquals(0, t.size());
        assertFalse(t.contains(5));
        for (int i = 1; i <= 30; i++) {
            t.put(i, i);
        }
        assertFalse(t.isEmpty());
        assertEquals(30, t.size());
        assertTrue(t.contains(1));
        assertTrue(t.contains(30));
        assertFalse(t.contains(0));
        assertFalse(t.contains(31));
        // 更新键值不改变 size
        t.put(15, 999);
        assertEquals(30, t.size());
        assertEquals(999, t.get(15));
        // 删除改变 size
        assertTrue(t.delete(15));
        assertEquals(29, t.size());
        assertFalse(t.contains(15));
    }

    @Test
    @DisplayName("min/max")
    void minMax() {
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        assertNull(t.min());
        assertNull(t.max());
        int[] vals = {42, 17, 8, 99, 55, 3, 71};
        for (int v : vals) {
            t.put(v, v);
        }
        assertEquals(3, t.min());
        assertEquals(99, t.max());
        t.deleteMin();
        assertEquals(8, t.min());
        t.deleteMax();
        assertEquals(71, t.max());
    }

    @Test
    @DisplayName("floor/ceiling")
    void floorCeiling() {
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        for (int i = 0; i < 100; i += 10) {
            t.put(i, i);
        }
        // 键: 0,10,20,...,90
        assertEquals(0, t.floor(0));
        assertEquals(0, t.floor(5));
        assertEquals(10, t.floor(10));
        assertEquals(20, t.floor(25));
        assertEquals(90, t.floor(99));
        assertNull(t.floor(-1));

        assertEquals(0, t.ceiling(0));
        assertEquals(10, t.ceiling(5));
        assertEquals(10, t.ceiling(10));
        assertEquals(30, t.ceiling(25));
        assertNull(t.ceiling(100));
    }

    @Test
    @DisplayName("rank/select 互逆")
    void rankSelect() {
        Random rand = new Random(55);
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        List<Integer> keys = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            int v = rand.nextInt(1000);
            if (!keys.contains(v)) {
                keys.add(v);
            }
        }
        for (int v : keys) {
            t.put(v, v);
        }
        List<Integer> sorted = new ArrayList<>(keys);
        java.util.Collections.sort(sorted);
        assertEquals(sorted.size(), t.size());
        // rank(key) == 有序序列中的下标
        for (int i = 0; i < sorted.size(); i++) {
            assertEquals(i, t.rank(sorted.get(i)));
        }
        // select(i) == 有序序列第 i 个
        for (int i = 0; i < sorted.size(); i++) {
            assertEquals(sorted.get(i), t.select(i));
        }
        // 不存在的键的 rank
        assertEquals(0, t.rank(-1));
        assertEquals(sorted.size(), t.rank(99999));
        // 越界 select 返回 null
        assertNull(t.select(-1));
        assertNull(t.select(sorted.size()));
    }

    @Test
    @DisplayName("deleteMin/deleteMax")
    void deleteMinMax() {
        Random rand = new Random(77);
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        TreeSet<Integer> ref = new TreeSet<>();
        for (int i = 0; i < 300; i++) {
            int v = rand.nextInt(1000);
            t.put(v, v);
            ref.add(v);
        }
        for (int k = 0; k < 100; k++) {
            t.deleteMin();
            ref.remove(ref.first());
            assertEquals(new ArrayList<>(ref), keysOf(t));
        }
        for (int k = 0; k < 100; k++) {
            t.deleteMax();
            ref.remove(ref.last());
            assertEquals(new ArrayList<>(ref), keysOf(t));
        }
    }

    @Test
    @DisplayName("keys(lo,hi) 与 size(lo,hi)")
    void rangeQueries() {
        TwoThreeTree<Integer, Integer> t = new TwoThreeTree<>();
        for (int i = 0; i <= 100; i++) {
            t.put(i, i);
        }
        assertEquals(Arrays.asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30),
                t.keys(20, 30));
        assertEquals(11, t.size(20, 30));
        assertEquals(0, t.size(20, 10));  // lo > hi
        assertEquals(101, t.size(0, 100));
        assertEquals(Arrays.asList(50), t.keys(50, 50));
        assertEquals(1, t.size(50, 50));
        assertEquals(0, t.size(101, 200));
    }
}
