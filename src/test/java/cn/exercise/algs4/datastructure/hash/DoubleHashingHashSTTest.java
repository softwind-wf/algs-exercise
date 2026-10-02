package cn.exercise.algs4.datastructure.hash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 双重散列开放地址散列表单元测试。
 * 重点验证:H2 随关键字变化从而消除次级聚集、gcd(H2,m)=1 的覆盖性(及合数表长下的"有空位却插不进")、
 * 墓碑删除、扩容保持素数,以及经验失败 ASL 与随机探测理论值 1/(1-α) 的吻合程度。
 */
@DisplayName("DoubleHashingHashST 双重散列开放地址测试")
class DoubleHashingHashSTTest {

    private static DoubleHashingHashST<String> table(int m) {
        return new DoubleHashingHashST<String>(m, 1.0);
    }

    /** H1 = key mod capacity(与被测类默认值一致,用于只替换 H2 的场景) */
    private static DoubleHashingHashST.HashFunction modCapacity() {
        return new DoubleHashingHashST.HashFunction() {
            public int hash(long key, int capacity) {
                return (int) Math.floorMod(key, (long) capacity);
            }
        };
    }

    /** H2 恒为定值的步长函数(用于构造与表长不互素的场景) */
    private static DoubleHashingHashST.HashFunction constantStep(final int step) {
        return new DoubleHashingHashST.HashFunction() {
            public int hash(long key, int capacity) {
                return step;
            }
        };
    }

    @Nested
    @DisplayName("探测序列:次级聚集的消除")
    class ProbeSequenceTest {

        @Test
        @DisplayName("H1 相同的四个关键字走四条不同序列(m=11)")
        void differentStepsForSameHome() {
            DoubleHashingHashST<String> st = table(11);
            long[] keys = {0, 11, 22, 33};
            int[] expectedSteps = {1, 2, 3, 4};
            for (int i = 0; i < keys.length; i++) {
                assertEquals(0, st.hashAddress(keys[i]));
                assertEquals(expectedSteps[i], st.stepOf(keys[i]));
                assertEquals(11, st.probeSequence(keys[i]).length, "素数表长应覆盖全表");
            }
            assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, st.probeSequence(0));
            assertArrayEquals(new int[]{0, 2, 4, 6, 8, 10, 1, 3, 5, 7, 9}, st.probeSequence(11));
            assertNotEquals(st.probeSequence(0)[1], st.probeSequence(11)[1]);
        }

        @Test
        @DisplayName("对照:二次探测下同起点的序列完全相同(这正是双重散列要解决的问题)")
        void quadraticHasSecondaryClustering() {
            QuadraticProbeHashST<String> quad = new QuadraticProbeHashST<String>(11, 1.0);
            assertArrayEquals(quad.probeSequence(0), quad.probeSequence(0));
            assertEquals(0, quad.hashAddress(0));
            assertEquals(0, quad.hashAddress(11));
            // 两者 H1 相同 → 序列必然一样(只有起点参与)
            assertArrayEquals(quad.probeSequence(quad.hashAddress(0)), quad.probeSequence(quad.hashAddress(11)));
        }

        @Test
        @DisplayName("步长越界或为 0 时立刻报错")
        void stepValidation() {
            DoubleHashingHashST<String> zero = new DoubleHashingHashST<String>(11, 1.0, modCapacity(), constantStep(0));
            assertThrows(IllegalStateException.class, () -> zero.stepOf(1));
            assertThrows(IllegalStateException.class, () -> zero.put(1, "x"));

            DoubleHashingHashST<String> tooBig = new DoubleHashingHashST<String>(11, 1.0, modCapacity(), constantStep(11));
            assertThrows(IllegalStateException.class, () -> tooBig.stepOf(1));
        }

        @Test
        @DisplayName("H1 越界时报错")
        void hash1Validation() {
            DoubleHashingHashST<String> bad = new DoubleHashingHashST<String>(11, 1.0,
                    new DoubleHashingHashST.HashFunction() {
                        public int hash(long key, int capacity) {
                            return -1;
                        }
                    }, constantStep(1));
            assertThrows(IllegalStateException.class, () -> bad.put(1, "x"));
        }
    }

    @Nested
    @DisplayName("覆盖性 gcd(H2, m) = 1")
    class CoverageTest {

        @Test
        @DisplayName("素数表长:默认步长必然覆盖全表")
        void primeCapacityCoversAll() {
            for (int m : new int[]{7, 11, 13, 17}) {
                DoubleHashingHashST<String> st = table(m);
                assertTrue(st.fullCoverageGuaranteed(), "m = " + m);
                for (long key = 0; key < m; key++) {
                    assertEquals(m, st.probeSequence(key).length, "m = " + m + ", key = " + key);
                }
            }
        }

        @Test
        @DisplayName("合数表长 + 不互素步长:只覆盖 gcd 相关的那几个槽位")
        void compositeCapacityMayNotCover() {
            DoubleHashingHashST<String> st = new DoubleHashingHashST<String>(9, 1.0, modCapacity(), constantStep(3));
            assertFalse(st.fullCoverageGuaranteed());
            assertEquals(3, st.probeSequence(1).length); // {1,4,7}
            assertArrayEquals(new int[]{1, 4, 7}, st.probeSequence(1));
        }

        @Test
        @DisplayName("合数表长 + 不互素步长:表里还有空位却插不进")
        void insertionFailsWithFreeSlots() {
            DoubleHashingHashST<String> st = new DoubleHashingHashST<String>(9, 1.0, modCapacity(), constantStep(3));
            for (long key : new long[]{1, 10, 19}) { // H1 = key mod 9 = 1,轨道 {1,4,7}
                st.put(key, "v" + key);
            }
            assertEquals(3, st.size());
            assertThrows(IllegalStateException.class, () -> st.put(28, "v28"));
            assertEquals(3, st.size());
            assertNull(st.get(28));
        }
    }

    @Nested
    @DisplayName("墓碑与删除")
    class TombstoneTest {

        @Test
        @DisplayName("三个 H1 相同、H2 不同的关键字分布在不同槽位;删除后序列不断、墓碑可复用")
        void tombstoneLifecycle() {
            DoubleHashingHashST<String> st = table(11);
            st.put(0, "v0");   // H1 = 0, H2 = 1 → 槽 0
            st.put(11, "v11"); // H1 = 0, H2 = 2 → 槽 2
            st.put(22, "v22"); // H1 = 0, H2 = 3 → 槽 3
            assertEquals(0, st.slotOf(0));
            assertEquals(2, st.slotOf(11));
            assertEquals(3, st.slotOf(22));
            assertEquals(0, st.tombstones());

            st.delete(0);
            assertEquals(1, st.tombstones());
            assertEquals(2, st.size());
            assertTrue(st.contains(11));
            assertTrue(st.contains(22));
            assertEquals("v22", st.get(22));

            st.put(33, "v33"); // H1 = 0, H2 = 4 → 复用槽 0 的墓碑
            assertEquals(0, st.tombstones());
            assertEquals(0, st.slotOf(33));
            assertEquals(3, st.size());
        }

        @Test
        @DisplayName("删除不存在的关键字返回 null 且无副作用")
        void deleteMissing() {
            DoubleHashingHashST<String> st = table(11);
            st.put(3, "v");
            assertNull(st.delete(4));
            assertEquals(1, st.size());
            assertEquals(0, st.tombstones());
        }
    }

    @Nested
    @DisplayName("扩容与表满")
    class ResizeTest {

        @Test
        @DisplayName("扩容保持素数、元素全保留、墓碑清零")
        void growsKeepingPrimeCapacity() {
            DoubleHashingHashST<String> st = new DoubleHashingHashST<String>(7, 0.5);
            for (long i = 0; i < 100; i++) {
                st.put(i * 1000003L, "v" + i);
            }
            assertTrue(st.resizeCount() >= 2);
            assertTrue(OpenAddressHashST.nextPrime(st.capacity()) == st.capacity(), "容量应为素数");
            assertTrue(st.fullCoverageGuaranteed());
            assertTrue(st.loadFactor() <= st.maxLoadFactor());
            assertEquals(100, st.size());
            for (long i = 0; i < 100; i++) {
                assertEquals("v" + i, st.get(i * 1000003L));
            }

            DoubleHashingHashST<String> withTombstone = new DoubleHashingHashST<String>(7, 0.5);
            for (long key = 0; key < 3; key++) {
                withTombstone.put(key, "v" + key);
            }
            withTombstone.delete(0);
            assertEquals(1, withTombstone.tombstones());
            for (long key = 100; key < 104; key++) {
                withTombstone.put(key, "v" + key);
            }
            assertTrue(withTombstone.capacity() > 7);
            assertEquals(0, withTombstone.tombstones());
            assertFalse(withTombstone.contains(0));
        }

        @Test
        @DisplayName("不扩容时表真满才拒插(素数表长下序列覆盖全表)")
        void fullTableThrows() {
            DoubleHashingHashST<String> st = table(7);
            for (long key = 0; key < 7; key++) {
                st.put(key, "v" + key);
            }
            assertEquals(7, st.size());
            assertThrows(IllegalStateException.class, () -> st.put(7, "v7"));

            st.delete(3);
            st.put(7, "v7"); // 复用墓碑
            assertEquals(7, st.size());
            assertTrue(st.contains(7));
            assertThrows(IllegalStateException.class, () -> st.put(8, "v8"));
        }
    }

    @Nested
    @DisplayName("查找长度")
    class AslTest {

        @Test
        @DisplayName("经验失败 ASL 与随机探测理论值 1/(1-α) 吻合(误差 25% 以内)")
        void empiricalFailureMatchesRandomProbing() {
            Random random = new Random(20261002L);
            double[] alphas = {0.10, 0.30, 0.50, 0.70};
            int[] counts = {10, 30, 50, 70};
            long[] keys = new long[70];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = random.nextInt(1000000);
            }
            long[] absent = new long[2000];
            for (int i = 0; i < absent.length; i++) {
                absent[i] = 2000000L + random.nextInt(1000000);
            }

            for (int t = 0; t < counts.length; t++) {
                DoubleHashingHashST<String> st = table(101);
                for (int i = 0; i < counts[t]; i++) {
                    st.put(keys[i], "v" + i);
                }
                assertEquals(alphas[t], st.loadFactor(), 0.011);
                double measured = st.averageUnsuccessfulProbes(absent);
                double theory = st.randomProbingUnsuccessfulProbes();
                assertTrue(Math.abs(measured - theory) / theory < 0.25,
                        "α=" + st.loadFactor() + " 时经验 ASL=" + measured + " 与理论 " + theory + " 偏差过大");
            }
        }

        @Test
        @DisplayName("成功 ASL 与独立模拟的 (H1,H2) 序列一致")
        void successAslMatchesSimulation() {
            int m = 11;
            long[] keys = {0, 11, 22, 5};
            DoubleHashingHashST<String> st = table(m);
            int[] slots = new int[m];
            Arrays.fill(slots, -1);

            long successSum = 0;
            for (long key : keys) {
                st.put(key, "v" + key);
                int home = (int) Math.floorMod(key, (long) m);
                int step = 1 + (int) Math.floorMod(key, (long) (m - 1));
                int probes = 0;
                for (int i = 0; i < m; i++) {
                    int address = (int) Math.floorMod((long) home + (long) i * step, (long) m);
                    if (i > 0 && address == home) {
                        break;
                    }
                    probes++;
                    if (slots[address] == -1) {
                        slots[address] = 1;
                        break;
                    }
                }
                successSum += probes;
            }
            assertEquals(successSum, st.successfulProbeSum());
            assertEquals((double) successSum / keys.length, st.averageSuccessfulProbes(), 1e-12);
        }

        @Test
        @DisplayName("结构度量与随机探测理论值同向变化")
        void structuralMeasureGrowsWithAlpha() {
            DoubleHashingHashST<String> st = table(101);
            assertEquals(1.0, st.averageStructuralFailureProbes(), 1e-12);
            assertEquals(1.0, st.randomProbingUnsuccessfulProbes(), 1e-12);
            for (long key = 0; key < 50; key++) {
                st.put(key, "v" + key);
            }
            assertTrue(st.averageStructuralFailureProbes() > 1.0);
            assertTrue(st.randomProbingUnsuccessfulProbes() > 1.9);
        }
    }

    @Nested
    @DisplayName("基本操作与参数校验")
    class BasicTest {

        @Test
        @DisplayName("put/get/delete/contains/size/isEmpty/loadFactor/keys/toString")
        void basicApi() {
            DoubleHashingHashST<String> st = table(11);
            assertTrue(st.isEmpty());
            st.put(4, "v4");   // H1 = 4, H2 = 5 → 槽 4
            st.put(15, "v15"); // H1 = 4, H2 = 6 → 槽 4 占,探测 10 → 槽 10
            assertEquals(2, st.size());
            assertEquals(4, st.slotOf(4));
            assertEquals(10, st.slotOf(15));
            assertEquals(2.0 / 11, st.loadFactor(), 1e-12);
            assertEquals("v15", st.get(15));
            assertTrue(st.contains(4));
            assertFalse(st.contains(5));
            assertNull(st.get(5));

            List<Long> keys = new ArrayList<Long>();
            st.keys().forEach(keys::add);
            assertEquals(Arrays.asList(4L, 15L), keys);
            assertEquals("{4 v4, 15 v15}", st.toString());

            assertEquals("v4", st.delete(4));
            assertEquals(1, st.size());
            assertNull(st.delete(4));
        }

        @Test
        @DisplayName("重复插入只覆盖值;null 值抛异常;缺省参数")
        void overwriteNullAndDefaults() {
            DoubleHashingHashST<String> st = table(11);
            st.put(3, "旧");
            st.put(3, "新");
            assertEquals(1, st.size());
            assertEquals("新", st.get(3));
            assertThrows(NullPointerException.class, () -> st.put(3, null));

            DoubleHashingHashST<String> def = new DoubleHashingHashST<String>();
            assertEquals(17, def.capacity());
            assertEquals(0.75, def.maxLoadFactor(), 1e-12);
            assertTrue(def.fullCoverageGuaranteed());
            assertEquals(1, def.stepOf(0));            // 1 + 0 mod 16
            assertEquals(2, def.stepOf(17));           // 1 + 17 mod 16
        }

        @Test
        @DisplayName("构造参数校验")
        void constructorValidation() {
            assertThrows(IllegalArgumentException.class, () -> new DoubleHashingHashST<String>(1));
            assertThrows(IllegalArgumentException.class, () -> new DoubleHashingHashST<String>(0));
            assertThrows(IllegalArgumentException.class, () -> new DoubleHashingHashST<String>(8, 0.0));
            assertThrows(IllegalArgumentException.class, () -> new DoubleHashingHashST<String>(8, 1.5));
            assertThrows(NullPointerException.class,
                    () -> new DoubleHashingHashST<String>(8, 0.5, null, constantStep(1)));   // H1 为 null
            assertThrows(NullPointerException.class,
                    () -> new DoubleHashingHashST<String>(8, 0.5, modCapacity(), null));    // H2 为 null
            assertThrows(NullPointerException.class,
                    () -> new DoubleHashingHashST<String>(8, 0.5, null));                   // 三参构造的 H1 为 null
        }

        @Test
        @DisplayName("顺序无关:同一组关键字无论插入次序如何,内容一致")
        void orderIndependence() {
            long[] keys = {3, 14, 25, 36, 47, 58};
            DoubleHashingHashST<String> a = table(11);
            DoubleHashingHashST<String> b = table(11);
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
}
