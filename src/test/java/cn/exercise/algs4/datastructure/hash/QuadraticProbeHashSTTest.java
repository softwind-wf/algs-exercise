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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 二次探测开放地址散列表单元测试。
 * 重点验证:两条探测序列的覆盖范围(PLUS 下素数表长只覆盖 (m+1)/2;ALTERNATING 仅当 m ≡ 3 mod 4 覆盖全表)、
 * "有空位却插不进"的坑、墓碑删除、扩容保持 m ≡ 3 mod 4,以及与线性探测的聚集对比。
 */
@DisplayName("QuadraticProbeHashST 二次探测开放地址测试")
class QuadraticProbeHashSTTest {

    private static QuadraticProbeHashST<String> plus(int m) {
        return new QuadraticProbeHashST<String>(m, 1.0, QuadraticProbeHashST.Mode.PLUS_I_SQUARE);
    }

    private static QuadraticProbeHashST<String> alternating(int m) {
        return new QuadraticProbeHashST<String>(m, 1.0, QuadraticProbeHashST.Mode.ALTERNATING);
    }

    @Nested
    @DisplayName("探测序列与覆盖范围")
    class ProbeSequenceTest {

        @Test
        @DisplayName("PLUS 的序列内容正确(m=7、m=13)")
        void plusSequenceContent() {
            assertArrayEquals(new int[]{0, 1, 4, 2}, plus(7).probeSequence(0));
            assertArrayEquals(new int[]{0, 1, 4, 9, 3, 12, 10}, plus(13).probeSequence(0));
        }

        @Test
        @DisplayName("ALTERNATING 的序列内容正确(m=7,±1²,±2²,…)")
        void alternatingSequenceContent() {
            assertArrayEquals(new int[]{0, 1, 6, 4, 3, 2, 5}, alternating(7).probeSequence(0));
        }

        @Test
        @DisplayName("PLUS 下素数表长只覆盖 (m+1)/2 个槽位")
        void plusCoverage() {
            for (int m : new int[]{7, 11, 13, 17}) {
                assertEquals((m + 1) / 2, plus(m).probeSequence(0).length, "m = " + m);
                assertFalse(plus(m).fullCoverageGuaranteed());
            }
        }

        @Test
        @DisplayName("ALTERNATING 仅当 m ≡ 3 (mod 4) 时覆盖全表")
        void alternatingCoverage() {
            assertEquals(7, alternating(7).probeSequence(0).length);
            assertEquals(11, alternating(11).probeSequence(0).length);
            assertTrue(alternating(7).fullCoverageGuaranteed());
            assertTrue(alternating(11).fullCoverageGuaranteed());

            assertEquals(7, alternating(13).probeSequence(0).length);   // 13 ≡ 1 (mod 4)
            assertEquals(9, alternating(17).probeSequence(0).length);   // 17 ≡ 1 (mod 4)
            assertFalse(alternating(13).fullCoverageGuaranteed());
            assertFalse(alternating(17).fullCoverageGuaranteed());
        }

        @Test
        @DisplayName("相同起点的关键字走同一条序列(次级聚集)")
        void secondaryClustering() {
            QuadraticProbeHashST<String> st = plus(11);
            assertEquals(0, st.hashAddress(0));
            assertEquals(0, st.hashAddress(11));
            assertEquals(0, st.hashAddress(22));
            assertArrayEquals(st.probeSequence(0), st.probeSequence(0));
        }

        @Test
        @DisplayName("probeSequence 起点越界抛异常")
        void probeSequenceValidation() {
            assertThrows(IllegalArgumentException.class, () -> plus(7).probeSequence(-1));
            assertThrows(IllegalArgumentException.class, () -> plus(7).probeSequence(7));
        }
    }

    @Nested
    @DisplayName("探测序列覆盖不全导致的插入失败")
    class OrbitLimitedTest {

        @Test
        @DisplayName("m=7、PLUS:占满起点 0 的轨道后,即使还有 3 个空位也插不进")
        void plusFailsEvenWithFreeSlots() {
            QuadraticProbeHashST<String> st = plus(7); // 起点 0 的轨道 = {0,1,4,2}
            for (long key : new long[]{0, 1, 2, 4}) {
                st.put(key, "v" + key);
            }
            assertEquals(4, st.size());
            assertThrows(IllegalStateException.class, () -> st.put(7, "v7"));
            assertEquals(4, st.size());
            assertNull(st.get(7));
            assertFalse(st.contains(3)); // 槽位 3 是空的,但不在起点 0 的轨道上
        }

        @Test
        @DisplayName("同样的表改用 ALTERNATING:插入成功(全表覆盖)")
        void alternatingSucceeds() {
            QuadraticProbeHashST<String> st = alternating(7);
            for (long key : new long[]{0, 1, 2, 4}) {
                st.put(key, "v" + key);
            }
            st.put(7, "v7");
            assertEquals(5, st.size());
            assertEquals(6, st.slotOf(7));  // 轨道 0,1,6,…
            assertEquals("v7", st.get(7));
        }
    }

    @Nested
    @DisplayName("墓碑与删除")
    class TombstoneTest {

        @Test
        @DisplayName("ALTERNATING m=11:三个同起点关键字落位 0/1/10,删除后序列不断、墓碑可复用")
        void tombstoneLifecycle() {
            QuadraticProbeHashST<String> st = alternating(11);
            st.put(0, "v0");
            st.put(11, "v11");
            st.put(22, "v22");
            assertEquals(0, st.slotOf(0));
            assertEquals(1, st.slotOf(11));
            assertEquals(10, st.slotOf(22)); // −1² → 11−1
            assertEquals(0, st.tombstones());

            st.delete(0);
            assertEquals(1, st.tombstones());
            assertEquals(2, st.size());
            assertTrue(st.contains(22));
            assertEquals("v22", st.get(22));

            st.put(33, "v33"); // 起点 0,复用墓碑
            assertEquals(0, st.tombstones());
            assertEquals(0, st.slotOf(33));
            assertEquals(3, st.size());
        }

        @Test
        @DisplayName("删除不存在的关键字返回 null 且无副作用")
        void deleteMissing() {
            QuadraticProbeHashST<String> st = plus(7);
            st.put(1, "v");
            assertNull(st.delete(2));
            assertEquals(1, st.size());
            assertEquals(0, st.tombstones());
        }
    }

    @Nested
    @DisplayName("扩容")
    class ResizeTest {

        @Test
        @DisplayName("ALTERNATING 扩容保持 m ≡ 3 (mod 4);PLUS 取普通素数")
        void growthChoosesProperPrimes() {
            QuadraticProbeHashST<String> alt = new QuadraticProbeHashST<String>(7, 0.5,
                    QuadraticProbeHashST.Mode.ALTERNATING);
            for (long i = 0; i < 100; i++) {
                alt.put(i * 1000003L, "v" + i);
            }
            assertEquals(3, alt.capacity() % 4);
            assertTrue(alt.fullCoverageGuaranteed());
            assertEquals(100, alt.size());
            for (long i = 0; i < 100; i++) {
                assertEquals("v" + i, alt.get(i * 1000003L));
            }

            QuadraticProbeHashST<String> p = new QuadraticProbeHashST<String>(7, 0.5,
                    QuadraticProbeHashST.Mode.PLUS_I_SQUARE);
            for (long i = 0; i < 100; i++) {
                p.put(i * 1000003L, "v" + i);
            }
            assertTrue(p.capacity() > 7);
            assertEquals(1, p.capacity() % 2); // 扩容后是奇素数(> 2)
            assertEquals(100, p.size());
        }

        @Test
        @DisplayName("扩容后墓碑清零、元素全保留")
        void resizeDropsTombstones() {
            QuadraticProbeHashST<String> st = new QuadraticProbeHashST<String>(7, 0.5,
                    QuadraticProbeHashST.Mode.ALTERNATING);
            for (long key = 0; key < 3; key++) {
                st.put(key, "v" + key);
            }
            st.delete(0);
            assertEquals(1, st.tombstones());
            for (long key = 100; key < 104; key++) {
                st.put(key, "v" + key); // 触发扩容
            }
            assertTrue(st.capacity() > 7);
            assertEquals(0, st.tombstones());
            assertFalse(st.contains(0));
            assertEquals("v1", st.get(1));
            assertEquals("v2", st.get(2));
        }

        @Test
        @DisplayName("nextPrime3Mod4 的取值")
        void nextPrime3Mod4Values() {
            assertEquals(3, QuadraticProbeHashST.nextPrime3Mod4(2));
            assertEquals(3, QuadraticProbeHashST.nextPrime3Mod4(3));
            assertEquals(7, QuadraticProbeHashST.nextPrime3Mod4(4));
            assertEquals(19, QuadraticProbeHashST.nextPrime3Mod4(14));
            assertEquals(19, QuadraticProbeHashST.nextPrime3Mod4(19));
            assertEquals(43, QuadraticProbeHashST.nextPrime3Mod4(38));
            assertThrows(IllegalArgumentException.class, () -> QuadraticProbeHashST.nextPrime3Mod4(1));
        }
    }

    @Nested
    @DisplayName("与线性探测对比、ASL 统计")
    class AslTest {

        @Test
        @DisplayName("同一批关键字:二次探测的失败 ASL 与最长连续块都小于线性探测")
        void betterThanLinearProbing() {
            Random random = new Random(20261002L);
            long[] keys = new long[70];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = random.nextInt(1000000);
            }
            OpenAddressHashST<String> linear = new OpenAddressHashST<String>(101, 1.0);
            QuadraticProbeHashST<String> quad = new QuadraticProbeHashST<String>(101, 1.0,
                    QuadraticProbeHashST.Mode.PLUS_I_SQUARE);
            for (int i = 0; i < keys.length; i++) {
                linear.put(keys[i], "v" + i);
                quad.put(keys[i], "v" + i);
            }
            assertEquals(70, quad.size());
            assertEquals(70, linear.size());
            assertTrue(quad.averageUnsuccessfulProbes() < linear.averageUnsuccessfulProbes(),
                    "二次探测 " + quad.averageUnsuccessfulProbes()
                            + " 应小于线性探测 " + linear.averageUnsuccessfulProbes());
            assertTrue(quad.maxClusterLength() < linear.maxClusterLength(),
                    "二次探测最长连续块 " + quad.maxClusterLength()
                            + " 应小于线性探测 " + linear.maxClusterLength());
            for (long key : keys) {
                assertEquals(linear.get(key), quad.get(key));
            }
        }

        @Test
        @DisplayName("ASL 统计与独立模拟的二次探测一致")
        void probeStatisticsMatchSimulation() {
            int m = 7;
            long[] keys = {0, 1, 2, 4};
            QuadraticProbeHashST<String> st = plus(m);
            int[] slots = new int[m];
            Arrays.fill(slots, -1);

            long successSum = 0;
            for (long key : keys) {
                st.put(key, "v" + key);
                int index = st.hashAddress(key);
                int probes = 0;
                for (int i = 0; i < m; i++) {
                    int address = (int) Math.floorMod((long) index + (long) i * i, (long) m);
                    if (i > 0 && address == index) {
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

            long failureSum = 0;
            for (int start = 0; start < m; start++) {
                int probes = 0;
                for (int i = 0; i < m; i++) {
                    int address = (int) Math.floorMod((long) start + (long) i * i, (long) m);
                    if (i > 0 && address == start) {
                        break;
                    }
                    probes++;
                    if (slots[address] == -1) {
                        break;
                    }
                }
                failureSum += probes;
            }

            assertEquals(successSum, st.successfulProbeSum());
            assertEquals(failureSum, st.unsuccessfulProbeSum());
            assertEquals((double) successSum / keys.length, st.averageSuccessfulProbes(), 1e-12);
            assertEquals((double) failureSum / m, st.averageUnsuccessfulProbes(), 1e-12);
        }

        @Test
        @DisplayName("复合表长 m=8:轨道 {0,1,4} 提前闭合,ASL 按轨道长度计数")
        void compositeCapacityOrbitCloses() {
            int m = 8;
            QuadraticProbeHashST<String> st = plus(m);
            assertArrayEquals(new int[]{0, 1, 4}, st.probeSequence(0));

            long[] keys = {0, 1, 4};
            int[] slots = new int[m];
            Arrays.fill(slots, -1);
            long successSum = 0;
            for (long key : keys) {
                st.put(key, "v" + key);
                int home = st.hashAddress(key);
                int probes = 0;
                for (int i = 0; i < m; i++) {
                    int address = (int) Math.floorMod((long) home + (long) i * i, (long) m);
                    if (i > 0 && address == home) {
                        break; // 轨道闭合
                    }
                    probes++;
                    if (slots[address] == -1) {
                        slots[address] = 1;
                        break;
                    }
                }
                successSum += probes;
            }

            long failureSum = 0;
            for (int start = 0; start < m; start++) {
                int probes = 0;
                for (int i = 0; i < m; i++) {
                    int address = (int) Math.floorMod((long) start + (long) i * i, (long) m);
                    if (i > 0 && address == start) {
                        break;
                    }
                    probes++;
                    if (slots[address] == -1) {
                        break;
                    }
                }
                failureSum += probes;
            }

            assertEquals(successSum, st.successfulProbeSum());
            assertEquals(13L, failureSum); // 起点 0 贡献 4,起点 1/4 各 2,其余 5 个起点各 1
            assertEquals(failureSum, st.unsuccessfulProbeSum(), "失败探测必须按轨道提前闭合,而不是绕满整表");
        }

        @Test
        @DisplayName("查找不存在的关键字时探测次数为'到第一个空位'的次数")
        void lastProbesOnMiss() {
            QuadraticProbeHashST<String> st = plus(7);
            st.put(0, "v0");
            st.get(3); // 起点 3 的序列第一步就是空位
            assertEquals(1, st.lastProbes());
            st.get(0);
            assertEquals(1, st.lastProbes());
        }

        @Test
        @DisplayName("随机探测参考值与装填因子同向变化")
        void randomProbingReference() {
            QuadraticProbeHashST<String> st = new QuadraticProbeHashST<String>(101, 1.0,
                    QuadraticProbeHashST.Mode.PLUS_I_SQUARE);
            assertEquals(1.0, st.randomProbingSuccessfulProbes(), 1e-12);
            assertEquals(1.0, st.randomProbingUnsuccessfulProbes(), 1e-12);
            for (long key = 0; key < 50; key++) {
                st.put(key, "v" + key);
            }
            assertEquals(50.0 / 101, st.loadFactor(), 1e-12);
            assertTrue(st.randomProbingSuccessfulProbes() > 1.0);
            assertTrue(st.randomProbingUnsuccessfulProbes() > st.randomProbingSuccessfulProbes());
        }
    }

    @Nested
    @DisplayName("基本操作与参数校验")
    class BasicTest {

        @Test
        @DisplayName("put/get/delete/contains/size/isEmpty/loadFactor/keys/toString")
        void basicApi() {
            QuadraticProbeHashST<String> st = plus(7);
            assertTrue(st.isEmpty());
            st.put(0, "v0");  // 槽 0
            st.put(1, "v1");  // 槽 1
            st.put(7, "v7");  // 起点 0 → 探测 1(占)→4 → 槽 4
            assertEquals(3, st.size());
            assertEquals(3.0 / 7, st.loadFactor(), 1e-12);
            assertEquals(0, st.slotOf(0));
            assertEquals(1, st.slotOf(1));
            assertEquals(4, st.slotOf(7));
            assertEquals("v7", st.get(7));
            assertTrue(st.contains(1));
            assertFalse(st.contains(5));
            assertNull(st.get(5));

            List<Long> keys = new ArrayList<Long>();
            st.keys().forEach(keys::add);
            assertEquals(Arrays.asList(0L, 1L, 7L), keys);
            assertEquals("{0 v0, 1 v1, 7 v7}", st.toString());

            assertEquals("v1", st.delete(1));
            assertEquals(2, st.size());
            assertNull(st.delete(1));
        }

        @Test
        @DisplayName("重复插入只覆盖值;null 值抛异常;散列函数越界报错")
        void overwriteNullAndBadHash() {
            QuadraticProbeHashST<String> st = plus(7);
            st.put(3, "旧");
            st.put(3, "新");
            assertEquals(1, st.size());
            assertEquals("新", st.get(3));
            assertThrows(NullPointerException.class, () -> st.put(3, null));

            QuadraticProbeHashST<String> bad = new QuadraticProbeHashST<String>(7, 1.0,
                    new QuadraticProbeHashST.HashFunction() {
                        public int hash(long key, int capacity) {
                            return -1;
                        }
                    }, QuadraticProbeHashST.Mode.PLUS_I_SQUARE);
            assertThrows(IllegalStateException.class, () -> bad.put(1, "x"));
        }

        @Test
        @DisplayName("构造参数校验")
        void constructorValidation() {
            assertThrows(IllegalArgumentException.class, () -> new QuadraticProbeHashST<String>(1));
            assertThrows(IllegalArgumentException.class, () -> new QuadraticProbeHashST<String>(0));
            assertThrows(IllegalArgumentException.class, () -> new QuadraticProbeHashST<String>(8, 0.0));
            assertThrows(IllegalArgumentException.class, () -> new QuadraticProbeHashST<String>(8, 1.5));
            assertThrows(NullPointerException.class,
                    () -> new QuadraticProbeHashST<String>(8, 0.5, null));
            QuadraticProbeHashST.HashFunction ok = new QuadraticProbeHashST.HashFunction() {
                public int hash(long key, int capacity) {
                    return 0;
                }
            };
            assertThrows(NullPointerException.class,
                    () -> new QuadraticProbeHashST<String>(8, 0.5, ok, null));
        }

        @Test
        @DisplayName("nextPrime 与模式默认值")
        void defaultsAndNextPrime() {
            assertEquals(2, QuadraticProbeHashST.nextPrime(2));
            assertEquals(17, QuadraticProbeHashST.nextPrime(16));
            assertEquals(19, QuadraticProbeHashST.nextPrime(18));
            QuadraticProbeHashST<String> st = new QuadraticProbeHashST<String>();
            assertEquals(17, st.capacity());
            assertEquals(QuadraticProbeHashST.Mode.PLUS_I_SQUARE, st.mode());
            assertEquals(0.5, st.maxLoadFactor(), 1e-12);
        }
    }
}
