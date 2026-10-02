package cn.exercise.algs4.datastructure.hash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 完全散列(两级 FKS)单元测试。
 * 重点验证:第二级逐桶无碰撞(因此查找最坏 ≤ 2 次比较)、Σ m_i ≤ 4n 的空间界、
 * 期望空间因子 < 2 的理论对照、构建可复现、以及静态结构对非法输入的拒绝。
 */
@DisplayName("PerfectHashingST 完全散列测试")
class PerfectHashingSTTest {

    private static final long[] STUDENTS = {20230101L, 20230102L, 20230105L, 20230210L,
            20230211L, 20230333L, 20230404L, 20230505L, 20230606L, 20230707L, 20230808L, 20230909L};

    /** 生成 n 个互不相同的随机关键字 */
    private static long[] distinctKeys(int n, long seed) {
        Random random = new Random(seed);
        TreeSet<Long> set = new TreeSet<Long>();
        while (set.size() < n) {
            set.add((long) random.nextInt((int) PerfectHashingST.MAX_KEY + 1));
        }
        long[] keys = new long[n];
        int i = 0;
        for (long key : set) {
            keys[i++] = key;
        }
        return keys;
    }

    private static Long[] boxed(long[] keys) {
        Long[] values = new Long[keys.length];
        for (int i = 0; i < keys.length; i++) {
            values[i] = keys[i];
        }
        return values;
    }

    @Nested
    @DisplayName("两级结构与查找")
    class StructureTest {

        @Test
        @DisplayName("所有关键字都能查到,且比较次数 ≤ 2")
        void allKeysFoundWithinTwoProbes() {
            PerfectHashingST<Long> st = PerfectHashingST.build(STUDENTS);
            assertEquals(12, st.size());
            assertFalse(st.isEmpty());
            for (long key : STUDENTS) {
                assertEquals(Long.valueOf(key), st.get(key), "关键字 " + key + " 查不到");
                assertTrue(st.lastProbes() <= 2, "命中比较次数应 ≤ 2,实际 " + st.lastProbes());
            }
            assertEquals(2, st.worstCaseProbes());
        }

        @Test
        @DisplayName("不在集合中的关键字一律返回 null(不出现假命中)")
        void absentKeysReturnNull() {
            PerfectHashingST<Long> st = PerfectHashingST.build(STUDENTS);
            Set<Long> present = new HashSet<Long>();
            for (long key : STUDENTS) {
                present.add(key);
            }
            int checked = 0;
            for (long key : distinctKeys(500, 42L)) {
                if (present.contains(key)) {
                    continue;
                }
                assertNull(st.get(key), "关键字 " + key + " 不在集合中,不应命中");
                checked++;
            }
            assertTrue(checked > 400);
            assertNull(st.get(-1));
            assertNull(st.get(PerfectHashingST.MAX_KEY + 1));
        }

        @Test
        @DisplayName("桶大小之和 = n,第二级槽位数 = n_i²(单关键字桶为 1)")
        void bucketSizesAndSubTableSizes() {
            PerfectHashingST<Long> st = PerfectHashingST.build(STUDENTS);
            int[] sizes = st.bucketSizes();
            assertEquals(st.bucketCount(), sizes.length);
            int sum = 0;
            long expectedSubSlots = 0;
            for (int i = 0; i < sizes.length; i++) {
                sum += sizes[i];
                if (sizes[i] == 0) {
                    assertEquals(0, st.subTableSize(i));
                } else if (sizes[i] == 1) {
                    assertEquals(1, st.subTableSize(i));
                    expectedSubSlots += 1;
                } else {
                    assertEquals(sizes[i] * sizes[i], st.subTableSize(i), "桶 " + i + " 应用 n_i² 个槽位");
                    expectedSubSlots += (long) sizes[i] * sizes[i];
                }
            }
            assertEquals(12, sum);
            assertEquals(expectedSubSlots, st.totalSubTableSlots());
            assertEquals(st.bucketCount() + st.totalSubTableSlots(), st.totalSlots());
        }

        @Test
        @DisplayName("第二级逐桶无碰撞(相同桶内的关键字落到互不相同的槽位)")
        void secondLevelHasNoCollision() {
            long[] keys = distinctKeys(200, 7L);
            PerfectHashingST<Long> st = PerfectHashingST.build(keys, boxed(keys), 11L);
            int[] sizes = st.bucketSizes();
            for (int bucket = 0; bucket < sizes.length; bucket++) {
                if (sizes[bucket] < 2) {
                    continue;
                }
                Set<Integer> slots = new HashSet<Integer>();
                for (long key : keys) {
                    if (st.firstLevelAddress(key) == bucket) {
                        assertTrue(slots.add(st.secondLevelAddress(bucket, key)),
                                "桶 " + bucket + " 内出现第二级碰撞");
                    }
                }
                assertEquals(sizes[bucket], slots.size());
            }
        }

        @Test
        @DisplayName("空间界 Σ m_i ≤ 4n 与空间因子")
        void spaceBound() {
            long[] keys = distinctKeys(1000, 3L);
            PerfectHashingST<Long> st = PerfectHashingST.build(keys, boxed(keys), 5L);
            assertTrue(st.totalSubTableSlots() <= 4 * st.size(),
                    "Σ m_i = " + st.totalSubTableSlots() + " 应 ≤ 4n = " + (4 * st.size()));
            assertTrue(st.spaceFactor() <= 4.0);
            assertEquals((double) st.totalSubTableSlots() / 1000, st.spaceFactor(), 1e-12);
            assertEquals((double) st.totalSlots() / 1000, st.spaceUsageRatio(), 1e-12);
        }

        @Test
        @DisplayName("地址范围:h1 ∈ [0,m),h_i ∈ [0,m_i)")
        void addressRanges() {
            long[] keys = distinctKeys(50, 9L);
            PerfectHashingST<Long> st = PerfectHashingST.build(keys, boxed(keys), 13L);
            for (long key : keys) {
                int bucket = st.firstLevelAddress(key);
                assertTrue(bucket >= 0 && bucket < st.bucketCount());
                int slot = st.secondLevelAddress(bucket, key);
                assertTrue(slot >= 0 && slot < st.subTableSize(bucket));
            }
            assertThrows(IllegalArgumentException.class, () -> st.subTableSize(st.bucketCount()));
            assertThrows(IllegalArgumentException.class, () -> st.secondLevelAddress(-1, 1));
        }

        @Test
        @DisplayName("keys() 与输入集合一致")
        void keysIterable() {
            PerfectHashingST<Long> st = PerfectHashingST.build(STUDENTS);
            Set<Long> actual = new TreeSet<Long>();
            st.keys().forEach(actual::add);
            Set<Long> expected = new TreeSet<Long>();
            for (long key : STUDENTS) {
                expected.add(key);
            }
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("构建与随机化")
    class BuildTest {

        @Test
        @DisplayName("同一种子构建结果完全可复现")
        void reproducibleWithSameSeed() {
            long[] keys = distinctKeys(300, 21L);
            PerfectHashingST<Long> first = PerfectHashingST.build(keys, boxed(keys), 20261003L);
            PerfectHashingST<Long> second = PerfectHashingST.build(keys, boxed(keys), 20261003L);
            assertArrayEqualsInt(first.bucketSizes(), second.bucketSizes());
            assertEquals(first.totalSubTableSlots(), second.totalSubTableSlots());
            assertEquals(first.level1Attempts(), second.level1Attempts());
            assertEquals(first.level2Attempts(), second.level2Attempts());
            assertEquals(first.maxLevel2Attempts(), second.maxLevel2Attempts());
            assertEquals(first.prime(), second.prime());
            for (int i = 0; i < first.bucketCount(); i++) {
                assertEquals(first.subTableSize(i), second.subTableSize(i));
            }
            for (long key : keys) {
                assertEquals(first.get(key), second.get(key));
                assertEquals(first.firstLevelAddress(key), second.firstLevelAddress(key));
            }
        }

        @Test
        @DisplayName("不同种子都满足空间界,且随机化次数很小")
        void randomisationStatistics() {
            long[] keys = distinctKeys(100, 33L);
            for (long seed = 0; seed < 50; seed++) {
                PerfectHashingST<Long> st = PerfectHashingST.build(keys, boxed(keys), seed);
                assertTrue(st.totalSubTableSlots() <= 4 * st.size(),
                        "seed = " + seed + " 时 Σ m_i = " + st.totalSubTableSlots());
                assertTrue(st.level1Attempts() >= 1);
                assertTrue(st.level1Attempts() <= 50, "第一级尝试次数异常:" + st.level1Attempts());
                assertTrue(st.maxLevel2Attempts() <= 50, "第二级尝试次数异常:" + st.maxLevel2Attempts());
                assertTrue(st.worstCaseProbes() <= 2);
                for (long key : keys) {
                    assertEquals(Long.valueOf(key), st.get(key));
                }
            }
        }

        @Test
        @DisplayName("期望空间因子 < 2(CLRS 理论:200 组构建平均)")
        void averageSpaceFactorBelowTwo() {
            double total = 0;
            int runs = 200;
            double max = 0;
            for (int t = 0; t < runs; t++) {
                long[] keys = distinctKeys(100, 5000L + t);
                PerfectHashingST<Long> st = PerfectHashingST.build(keys, boxed(keys), 1000L + t);
                double factor = st.spaceFactor();
                total += factor;
                max = Math.max(max, factor);
                assertTrue(factor <= 4.0, "空间因子 " + factor + " 超过 4n 界");
            }
            double average = total / runs;
            assertTrue(average < 2.3,
                    "200 组构建的平均空间因子 " + average + " 应接近理论期望 2 - 1/n ≈ 1.99");
            assertTrue(max <= 4.0);
        }

        @Test
        @DisplayName("最坏比较次数与 n 无关(链地址法最长链会增长)")
        void worstCaseIndependentOfSize() {
            for (int size : new int[]{10, 100, 1000, 5000}) {
                long[] keys = distinctKeys(size, 999L);
                PerfectHashingST<Long> perfect = PerfectHashingST.build(keys, boxed(keys), 999L);
                assertEquals(2, perfect.worstCaseProbes(), "n = " + size);
                for (long key : keys) {
                    perfect.get(key);
                    assertTrue(perfect.lastProbes() <= 2);
                }
            }
            long[] keys = distinctKeys(1000, 999L);
            SeparateChainingHashST<Long> chained = new SeparateChainingHashST<Long>(1009, 16.0);
            for (long key : keys) {
                chained.put(key, key);
            }
            assertTrue(chained.maxChainLength() >= 3, "对照用的链地址法最长链 = " + chained.maxChainLength());
        }
    }

    @Nested
    @DisplayName("边界与非法输入")
    class EdgeCaseTest {

        @Test
        @DisplayName("空集合与单元素集合")
        void emptyAndSingle() {
            PerfectHashingST<Long> empty = PerfectHashingST.build(new long[0]);
            assertEquals(0, empty.size());
            assertTrue(empty.isEmpty());
            assertNull(empty.get(5));
            assertEquals(0.0, empty.spaceFactor(), 1e-12);
            assertEquals(0, empty.worstCaseProbes());

            PerfectHashingST<Long> single = PerfectHashingST.build(new long[]{77L});
            assertEquals(1, single.size());
            assertEquals(1, single.bucketCount());
            assertEquals(1, single.subTableSize(0));
            assertEquals(1.0, single.spaceFactor(), 1e-12);
            assertEquals(Long.valueOf(77L), single.get(77L));
            assertEquals(1, single.worstCaseProbes());
        }

        @Test
        @DisplayName("重复关键字、越界关键字、长度不一致、null 一律拒绝")
        void invalidInputs() {
            assertThrows(IllegalArgumentException.class,
                    () -> PerfectHashingST.build(new long[]{1L, 2L, 1L}));
            assertThrows(IllegalArgumentException.class,
                    () -> PerfectHashingST.build(new long[]{-1L}));
            assertThrows(IllegalArgumentException.class,
                    () -> PerfectHashingST.build(new long[]{PerfectHashingST.MAX_KEY + 1}));
            assertThrows(IllegalArgumentException.class,
                    () -> PerfectHashingST.build(new long[]{1L, 2L}, new Long[]{1L}));
            assertThrows(NullPointerException.class,
                    () -> PerfectHashingST.build(new long[]{1L}, new Long[]{null}));
            assertThrows(NullPointerException.class,
                    () -> PerfectHashingST.build((long[]) null));
            assertThrows(NullPointerException.class,
                    () -> PerfectHashingST.build(new long[]{1L}, null));
        }

        @Test
        @DisplayName("关键字 0 与最大值都能正确处理")
        void boundaryKeys() {
            long[] keys = {0L, 1L, 2L, 1000L, PerfectHashingST.MAX_KEY};
            PerfectHashingST<Long> st = PerfectHashingST.build(keys);
            for (long key : keys) {
                assertEquals(Long.valueOf(key), st.get(key));
            }
            assertNull(st.get(3L));
            assertTrue(st.prime() > PerfectHashingST.MAX_KEY);
        }

        @Test
        @DisplayName("值可以是任意对象,且按关键字取回")
        void arbitraryValues() {
            long[] keys = distinctKeys(30, 4L);
            String[] values = new String[keys.length];
            for (int i = 0; i < keys.length; i++) {
                values[i] = "名字" + i;
            }
            PerfectHashingST<String> st = PerfectHashingST.build(keys, values);
            for (int i = 0; i < keys.length; i++) {
                assertEquals(values[i], st.get(keys[i]));
            }
            assertNull(st.get(keys[keys.length - 1] + 1)); // 最大关键字之后的键一定缺席
            assertNull(st.get(-1));
            assertNotNull(st.toString());
            assertTrue(st.toString().contains("空间因子"));
        }
    }

    private static void assertArrayEqualsInt(int[] expected, int[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], "下标 " + i);
        }
    }

    @Nested
    @DisplayName("与链地址法/开放地址法对比")
    class ComparisonTest {

        @Test
        @DisplayName("完全散列用更多空间换来最坏 O(1):比较次数上限 2")
        void spaceForWorstCaseConstant() {
            long[] keys = distinctKeys(1000, 999L);
            PerfectHashingST<Long> perfect = PerfectHashingST.build(keys, boxed(keys), 777L);
            SeparateChainingHashST<Long> chained = new SeparateChainingHashST<Long>(1009, 16.0);
            for (long key : keys) {
                chained.put(key, key);
            }
            assertTrue(perfect.spaceUsageRatio() > 1.0, "完全散列要额外空间");
            assertTrue(perfect.totalSlots() > chained.capacity());
            assertTrue(perfect.worstCaseProbes() < chained.maxChainLength(),
                    "完全散列最坏 " + perfect.worstCaseProbes() + " 次 vs 链地址法最长链 "
                            + chained.maxChainLength());
            List<Long> probes = new ArrayList<Long>();
            for (long key : keys) {
                assertEquals(Long.valueOf(key), perfect.get(key));
                probes.add((long) perfect.lastProbes());
            }
            assertEquals(keys.length, probes.size());
        }
    }
}
