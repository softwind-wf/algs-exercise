package cn.exercise.algs4.datastructure.hash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 链地址法(拉链法)散列表单元测试。
 * 重点验证:教材经典例题的链长与 ASL(15/12 与 12/11)、ASL 与链内顺序无关、
 * 删除不需要墓碑、装填因子可以大于 1、扩容重新散列,以及与开放地址法的对比。
 */
@DisplayName("SeparateChainingHashST 链地址法测试")
class SeparateChainingHashSTTest {

    /** 教材经典关键字序列 */
    private static final int[] CLASSIC_KEYS = {19, 14, 23, 1, 68, 20, 84, 27, 55, 11, 10, 79};

    private static SeparateChainingHashST<String> classic(int m) {
        SeparateChainingHashST<String> st = new SeparateChainingHashST<String>(m, 16.0);
        for (int key : CLASSIC_KEYS) {
            st.put(key, "v" + key);
        }
        return st;
    }

    @Nested
    @DisplayName("教材经典例题(m = 11,H(key) = key mod 11)")
    class ClassicExampleTest {

        @Test
        @DisplayName("各桶链长与教材一致,ASL成功 = 15/12 = 1.25")
        void chainLengthsAndSuccessAsl() {
            SeparateChainingHashST<String> st = classic(11);
            assertArrayEquals(new int[]{2, 2, 2, 1, 0, 1, 0, 1, 1, 1, 1}, st.chainLengths());
            assertEquals(12, st.size());
            assertEquals(2, st.maxChainLength());
            assertEquals(2, st.emptyBucketCount());
            assertEquals(15L, st.successfulProbeSum());
            assertEquals(1.25, st.averageSuccessfulProbes(), 1e-12);
            assertEquals(12.0 / 11, st.loadFactor(), 1e-12);
        }

        @Test
        @DisplayName("ASL失败 = 12/11(不计判空);计判空口径为 23/11")
        void failureAsl() {
            SeparateChainingHashST<String> st = classic(11);
            assertEquals(12L, st.failureComparisonSum());
            assertEquals(12.0 / 11, st.averageUnsuccessfulProbes(), 1e-12);
            assertEquals(23.0 / 11, st.averageUnsuccessfulProbesWithEmptyCheck(), 1e-12);
            // 不变式:ASL失败(不计判空)恒等于装填因子
            assertEquals(st.loadFactor(), st.averageUnsuccessfulProbes(), 1e-12);
        }

        @Test
        @DisplayName("头插:同桶内最近插入的结点在链头")
        void headInsertion() {
            SeparateChainingHashST<String> st = classic(11);
            assertEquals(1, st.positionOf(11));  // 11 后插入 → 链头
            assertEquals(2, st.positionOf(55));
            assertEquals(1, st.positionOf(1));
            assertEquals(2, st.positionOf(23));
            assertEquals(1, st.positionOf(79));
            assertEquals(2, st.positionOf(68));
            assertEquals(-1, st.positionOf(999));
        }

        @Test
        @DisplayName("ASL成功与链内顺序无关(只取决于各桶链长)")
        void aslIndependentOfChainOrder() {
            SeparateChainingHashST<String> forward = classic(11);
            SeparateChainingHashST<String> backward = new SeparateChainingHashST<String>(11, 16.0);
            for (int i = CLASSIC_KEYS.length - 1; i >= 0; i--) {
                backward.put(CLASSIC_KEYS[i], "v" + CLASSIC_KEYS[i]);
            }
            assertEquals(forward.successfulProbeSum(), backward.successfulProbeSum());
            assertEquals(forward.averageSuccessfulProbes(), backward.averageSuccessfulProbes(), 1e-12);
        }
    }

    @Nested
    @DisplayName("删除:直接摘链,不需要墓碑")
    class DeleteTest {

        @Test
        @DisplayName("删除链头、链中、链尾都正确")
        void deleteHeadMiddleTail() {
            SeparateChainingHashST<String> st = new SeparateChainingHashST<String>(1, 16.0);
            st.put(1, "a");
            st.put(2, "b");
            st.put(3, "c");
            assertArrayEquals(new int[]{3}, st.chainLengths());
            assertEquals(1, st.positionOf(3));
            assertEquals(2, st.positionOf(2));
            assertEquals(3, st.positionOf(1));

            assertEquals("b", st.delete(2)); // 链中
            assertEquals(2, st.size());
            assertEquals(1, st.positionOf(3));
            assertEquals(2, st.positionOf(1));

            assertEquals("c", st.delete(3)); // 链头
            assertEquals(1, st.size());
            assertEquals(1, st.positionOf(1));

            assertEquals("a", st.delete(1)); // 链尾
            assertTrue(st.isEmpty());
            assertNull(st.delete(1));
        }

        @Test
        @DisplayName("删除后同桶其它关键字仍可查(对比开放地址法必需墓碑)")
        void othersRemainReachable() {
            SeparateChainingHashST<String> st = classic(11);
            assertEquals("v1", st.delete(1));
            assertTrue(st.contains(23));
            assertTrue(st.contains(79));
            assertEquals("v79", st.get(79));
            assertEquals(11, st.size());
            assertEquals(-1, st.positionOf(1));
        }
    }

    @Nested
    @DisplayName("装填因子 α > 1")
    class HighLoadFactorTest {

        @Test
        @DisplayName("α 可以大于 1,链变长但仍能全部查回")
        void worksBeyondAlphaOne() {
            SeparateChainingHashST<String> st = new SeparateChainingHashST<String>(3, 16.0);
            for (long key = 0; key < 10; key++) {
                st.put(key, "v" + key);
            }
            assertEquals(10, st.size());
            assertTrue(st.loadFactor() > 3.0);
            assertTrue(st.maxChainLength() >= 3);
            assertEquals(10.0 / 3, st.loadFactor(), 1e-12);
            for (long key = 0; key < 10; key++) {
                assertEquals("v" + key, st.get(key));
            }
            assertEquals(st.loadFactor(), st.averageUnsuccessfulProbes(), 1e-12);
        }

        @Test
        @DisplayName("实测 ASL成功与教材近似 1 + α/2 接近")
        void measuredMatchesApproximation() {
            Random random = new Random(20261003L);
            SeparateChainingHashST<String> st = new SeparateChainingHashST<String>(211, 16.0);
            for (long i = 0; i < 200; i++) {
                st.put(random.nextInt(10000000), "v" + i);
            }
            assertEquals(200, st.size());
            double measured = st.averageSuccessfulProbes();
            double theory = 1.0 + st.loadFactor() / 2.0;
            assertTrue(Math.abs(measured - theory) / theory < 0.25,
                    "实测 ASL成功 " + measured + " 与近似 " + theory + " 偏差过大");
        }
    }

    @Nested
    @DisplayName("扩容与统计")
    class ResizeAndStatsTest {

        @Test
        @DisplayName("扩容到素数桶数、元素全保留、α 回落到上限内")
        void growKeepsAllElements() {
            SeparateChainingHashST<String> st = new SeparateChainingHashST<String>(7, 0.75);
            Random random = new Random(20261003L);
            TreeSet<Long> expected = new TreeSet<Long>();
            for (int i = 0; i < 500; i++) {
                long key = random.nextInt(1000000);
                st.put(key, "v" + key);
                expected.add(key);
            }
            assertTrue(st.resizeCount() > 0);
            assertEquals(OpenAddressHashST.nextPrime(st.capacity()), st.capacity(), "桶数应为素数");
            assertTrue(st.loadFactor() <= st.maxLoadFactor());
            assertEquals(expected.size(), st.size());
            TreeSet<Long> actual = new TreeSet<Long>();
            st.keys().forEach(actual::add);
            assertEquals(expected, actual);
            for (long key : expected) {
                assertEquals("v" + key, st.get(key));
            }
        }

        @Test
        @DisplayName("链长统计自洽:总和 = n,空桶 + 非空桶 = 桶数")
        void chainStatisticsAreConsistent() {
            SeparateChainingHashST<String> st = new SeparateChainingHashST<String>(17, 16.0);
            Random random = new Random(7L);
            for (int i = 0; i < 100; i++) {
                st.put(random.nextInt(100000), "v" + i);
            }
            int[] lengths = st.chainLengths();
            int sum = 0;
            int nonEmpty = 0;
            long expectedSuccessSum = 0;
            for (int len : lengths) {
                sum += len;
                if (len > 0) {
                    nonEmpty++;
                }
                expectedSuccessSum += (long) len * (len + 1) / 2;
            }
            assertEquals(st.size(), sum);
            assertEquals(st.capacity(), nonEmpty + st.emptyBucketCount());
            assertEquals(expectedSuccessSum, st.successfulProbeSum());
            assertEquals(st.size(), st.failureComparisonSum());
            assertEquals(st.loadFactor(), st.averageChainLength(), 1e-12);
        }
    }

    @Nested
    @DisplayName("与开放地址法对比")
    class ComparisonTest {

        @Test
        @DisplayName("同一批关键字、同一散列函数:链地址法的 ASL 明显更小")
        void smallerAslThanLinearProbing() {
            SeparateChainingHashST<String> chained =
                    new SeparateChainingHashST<String>(16, 16.0, modThirteenSC());
            OpenAddressHashST<String> linear =
                    new OpenAddressHashST<String>(16, 1.0, modThirteenOA());
            for (int key : CLASSIC_KEYS) {
                chained.put(key, "v" + key);
                linear.put(key, "v" + key);
            }
            assertEquals(1.75, chained.averageSuccessfulProbes(), 1e-12);
            assertEquals(2.5, linear.averageSuccessfulProbes(), 1e-12);
            assertTrue(chained.averageSuccessfulProbes() < linear.averageSuccessfulProbes());
            assertTrue(chained.averageUnsuccessfulProbes() < linear.averageUnsuccessfulProbes());
            assertEquals(4, chained.maxChainLength());
        }
    }

    @Nested
    @DisplayName("基本操作与参数校验")
    class BasicTest {

        @Test
        @DisplayName("put/get/delete/contains/size/isEmpty/loadFactor/keys/toString")
        void basicApi() {
            SeparateChainingHashST<String> st = new SeparateChainingHashST<String>(5, 16.0);
            assertTrue(st.isEmpty());
            st.put(0, "v0");
            st.put(5, "v5");   // 与 0 同桶,头插到链头
            st.put(2, "v2");
            assertEquals(3, st.size());
            assertEquals(0.6, st.loadFactor(), 1e-12);
            assertEquals("v5", st.get(5));
            assertTrue(st.contains(2));
            assertFalse(st.contains(3));
            assertNull(st.get(3));
            assertEquals(1, st.positionOf(5));
            assertEquals(2, st.positionOf(0));

            List<Long> keys = new ArrayList<Long>();
            st.keys().forEach(keys::add);
            assertEquals(Arrays.asList(5L, 0L, 2L), keys);
            assertEquals("{5 v5, 0 v0, 2 v2}", st.toString());

            assertEquals("v0", st.delete(0));
            assertEquals(2, st.size());
            assertNull(st.delete(0));
        }

        @Test
        @DisplayName("重复插入只覆盖值,不新增结点")
        void overwriteDoesNotGrowChain() {
            SeparateChainingHashST<String> st = new SeparateChainingHashST<String>(5, 16.0);
            st.put(1, "旧");
            st.put(6, "另一键");
            st.put(1, "新");
            assertEquals(2, st.size());
            assertEquals(2, st.chainLengths()[1]); // 1 与 6 同桶
            assertEquals("新", st.get(1));
            assertThrows(NullPointerException.class, () -> st.put(1, null));
        }

        @Test
        @DisplayName("散列函数越界时报错;可插拔")
        void hashFunctionValidation() {
            SeparateChainingHashST<String> bad = new SeparateChainingHashST<String>(5, 16.0,
                    new SeparateChainingHashST.HashFunction() {
                        public int hash(long key, int capacity) {
                            return capacity;
                        }
                    });
            assertThrows(IllegalStateException.class, () -> bad.put(1, "x"));

            SeparateChainingHashST<String> custom = new SeparateChainingHashST<String>(5, 16.0,
                    new SeparateChainingHashST.HashFunction() {
                        public int hash(long key, int capacity) {
                            return 0; // 全部同桶
                        }
                    });
            custom.put(1, "a");
            custom.put(2, "b");
            assertEquals(2, custom.maxChainLength());
            assertEquals(2.0 / 5, custom.averageUnsuccessfulProbes(), 1e-12);
        }

        @Test
        @DisplayName("构造参数校验")
        void constructorValidation() {
            assertThrows(IllegalArgumentException.class, () -> new SeparateChainingHashST<String>(0));
            assertThrows(IllegalArgumentException.class,
                    () -> new SeparateChainingHashST<String>((1 << 26) + 1));
            assertThrows(IllegalArgumentException.class,
                    () -> new SeparateChainingHashST<String>(8, 0.0));
            assertThrows(IllegalArgumentException.class,
                    () -> new SeparateChainingHashST<String>(8, 16.1));
            assertThrows(NullPointerException.class,
                    () -> new SeparateChainingHashST<String>(8, 1.0, null));
            SeparateChainingHashST<String> ok = new SeparateChainingHashST<String>(8, 16.0);
            assertEquals(16.0, ok.maxLoadFactor(), 1e-12);
        }

        @Test
        @DisplayName("插入次序不同不影响内容")
        void orderIndependence() {
            long[] keys = {3, 14, 25, 36, 47};
            SeparateChainingHashST<String> a = new SeparateChainingHashST<String>(7, 16.0);
            SeparateChainingHashST<String> b = new SeparateChainingHashST<String>(7, 16.0);
            for (long key : keys) {
                a.put(key, "v" + key);
            }
            for (int i = keys.length - 1; i >= 0; i--) {
                b.put(keys[i], "v" + keys[i]);
            }
            assertEquals(a.size(), b.size());
            for (long key : keys) {
                assertEquals(a.get(key), b.get(key));
            }
        }
    }

    private static SeparateChainingHashST.HashFunction modThirteenSC() {
        return new SeparateChainingHashST.HashFunction() {
            public int hash(long key, int capacity) {
                return (int) Math.floorMod(key, 13L);
            }
        };
    }

    private static OpenAddressHashST.HashFunction modThirteenOA() {
        return new OpenAddressHashST.HashFunction() {
            public int hash(long key, int capacity) {
                return (int) Math.floorMod(key, 13L);
            }
        };
    }
}
