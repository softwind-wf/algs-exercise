package cn.exercise.algs4.datastructure.gist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GiST 区间树")
class GiSTTest {

    private static Interval randomInterval(Random random, int span) {
        long low = random.nextInt(span);
        return Interval.of(low, low + 1 + random.nextInt(20));
    }

    private static Set<String> bruteForce(List<Interval> keys, List<String> values, Interval query) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).overlaps(query)) {
                result.add(values.get(i));
            }
        }
        return result;
    }

    @Test
    @DisplayName("空树:查询为空,高度为 0")
    void emptyTree() {
        GiST<Interval, String, Interval> tree = new GiST<>(new IntervalGiSTOps());
        assertTrue(tree.isEmpty());
        assertEquals(0, tree.size());
        assertEquals(0, tree.height());
        assertEquals(0, tree.nodeCount());
        assertEquals(0, tree.leafCount());
        assertTrue(tree.search(Interval.of(0, 10)).isEmpty());
        assertTrue(tree.checkCoverage());
        assertTrue(tree.checkBalanced());
    }

    @Test
    @DisplayName("单条插入:重叠命中,不重叠不命中")
    void singleInsert() {
        GiST<Interval, String, Interval> tree = new GiST<>(new IntervalGiSTOps());
        tree.insert(Interval.of(100, 200), "only");
        assertEquals(1, tree.size());
        assertEquals(1, tree.height());
        assertTrue(tree.search(Interval.of(150, 160)).contains("only"));
        assertTrue(tree.search(Interval.of(200, 300)).contains("only"));
        assertTrue(tree.search(Interval.of(250, 300)).isEmpty());
        assertTrue(tree.checkCoverage());
        assertTrue(tree.checkBalanced());
    }

    @Test
    @DisplayName("相同区间重复插入:10 条全部可命中")
    void duplicates() {
        GiST<Interval, String, Interval> tree = new GiST<>(new IntervalGiSTOps(), 3);
        for (int i = 0; i < 10; i++) {
            tree.insert(Interval.of(5, 15), "v" + i);
        }
        assertEquals(10, tree.size());
        Set<String> hits = new HashSet<>(tree.search(Interval.of(10, 10)));
        assertEquals(10, hits.size());
        assertTrue(tree.checkCoverage());
        assertTrue(tree.checkBalanced());
    }

    @Test
    @DisplayName("400 条随机区间:200 次随机查询与暴力扫描一致且不变式成立")
    void randomized() {
        Random random = new Random(20261108L);
        GiST<Interval, String, Interval> tree = new GiST<>(new IntervalGiSTOps());
        List<Interval> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            Interval key = randomInterval(random, 500);
            String value = "v" + i;
            tree.insert(key, value);
            keys.add(key);
            values.add(value);
        }
        assertEquals(400, tree.size());
        assertTrue(tree.checkCoverage());
        assertTrue(tree.checkBalanced());
        assertTrue(tree.height() < 400);
        for (int round = 0; round < 200; round++) {
            Interval query = randomInterval(random, 520);
            Set<String> expected = bruteForce(keys, values, query);
            Set<String> actual = new HashSet<>(tree.search(query));
            assertEquals(expected, actual, "查询不一致: " + query);
        }
        assertEquals(400, tree.size());
    }

    @Test
    @DisplayName("页容量 3 的极小页:仍保持平衡与覆盖")
    void tinyPages() {
        Random random = new Random(7L);
        GiST<Interval, String, Interval> tree = new GiST<>(new IntervalGiSTOps(), 3);
        List<Interval> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            Interval key = randomInterval(random, 200);
            tree.insert(key, "v" + i);
            keys.add(key);
            values.add("v" + i);
        }
        assertTrue(tree.checkCoverage());
        assertTrue(tree.checkBalanced());
        assertTrue(tree.height() >= 2);
        for (int round = 0; round < 50; round++) {
            Interval query = randomInterval(random, 220);
            assertEquals(bruteForce(keys, values, query), new HashSet<>(tree.search(query)));
        }
    }

    @Test
    @DisplayName("顺序递增区间(偏斜输入)保持正确")
    void sequential() {
        GiST<Interval, String, Interval> tree = new GiST<>(new IntervalGiSTOps(), 4);
        List<Interval> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            Interval key = Interval.of(i * 10L, i * 10L + 15);
            tree.insert(key, "v" + i);
            keys.add(key);
            values.add("v" + i);
        }
        assertTrue(tree.checkCoverage());
        assertTrue(tree.checkBalanced());
        Interval query = Interval.of(700, 780);
        assertEquals(bruteForce(keys, values, query), new HashSet<>(tree.search(query)));
        assertFalse(tree.search(Interval.of(10000, 10100)).contains("v0"));
    }
}
