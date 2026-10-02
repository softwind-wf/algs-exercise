package cn.exercise.algs4.datastructure.hash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 开放地址法(线性探测)散列表单元测试。
 * 重点验证:线性探测落位顺序与教材经典例题、墓碑删除与复用、表满、自动扩容与再散列、
 * 可插拔散列函数、聚集度量,以及 ASL 统计与独立模拟一致。
 */
@DisplayName("OpenAddressHashST 开放地址法(线性探测)测试")
class OpenAddressHashSTTest {

    /** 教材经典例题的散列函数:H(key) = key mod 13(忽略表长,表长取 16) */
    private static OpenAddressHashST.HashFunction modThirteen() {
        return new OpenAddressHashST.HashFunction() {
            public int hash(long key, int capacity) {
                return (int) Math.floorMod(key, 13L);
            }
        };
    }

    @Nested
    @DisplayName("线性探测落位")
    class LinearProbingTest {

        @Test
        @DisplayName("教材经典例题:地址与探测次数逐键一致,ASL成功 30/12、ASL失败 94/16")
        void classicExample() {
            OpenAddressHashST<String> st = new OpenAddressHashST<String>(16, 1.0, modThirteen());
            int[] keys = {19, 14, 23, 1, 68, 20, 84, 27, 55, 11, 10, 79};
            int[] slots = {6, 1, 10, 2, 3, 7, 8, 4, 5, 11, 12, 9};
            int[] probes = {1, 1, 1, 2, 1, 1, 3, 4, 3, 1, 3, 9};
            for (int i = 0; i < keys.length; i++) {
                st.put(keys[i], "v" + keys[i]);
                assertEquals(slots[i], st.slotOf(keys[i]), "关键字 " + keys[i] + " 的落位不对");
                assertEquals(probes[i], st.lastProbes(), "关键字 " + keys[i] + " 的探测次数不对");
            }
            assertEquals(12, st.size());
            assertEquals(30L, st.successfulProbeSum());
            assertEquals(2.5, st.averageSuccessfulProbes(), 1e-12);
            assertEquals(94L, st.unsuccessfulProbeSum());
            assertEquals(5.875, st.averageUnsuccessfulProbes(), 1e-12);
            assertEquals(0.75, st.loadFactor(), 1e-12);
        }

        @Test
        @DisplayName("默认散列函数为 key mod capacity,可插拔")
        void pluggableHashFunction() {
            OpenAddressHashST<String> def = new OpenAddressHashST<String>(16, 1.0);
            assertEquals(3, def.hashAddress(3));
            assertEquals(3, def.hashAddress(19)); // 19 mod 16

            OpenAddressHashST<String> custom = new OpenAddressHashST<String>(16, 1.0, modThirteen());
            assertEquals(6, custom.hashAddress(19)); // 19 mod 13
            assertEquals(1, custom.hashAddress(14));
        }

        @Test
        @DisplayName("散列函数返回越界地址时立刻报错")
        void invalidHashFunctionIsRejected() {
            OpenAddressHashST<String> st = new OpenAddressHashST<String>(8, 1.0, new OpenAddressHashST.HashFunction() {
                public int hash(long key, int capacity) {
                    return capacity; // 越界
                }
            });
            assertThrows(IllegalStateException.class, () -> st.put(1, "x"));
            assertThrows(IllegalStateException.class, () -> st.get(1));
        }

        @Test
        @DisplayName("同地址的关键字依次落在连续槽位")
        void consecutivePlacement() {
            OpenAddressHashST<String> st = new OpenAddressHashST<String>(8, 1.0);
            st.put(0, "a");
            st.put(8, "b");
            st.put(16, "c");
            assertEquals(0, st.slotOf(0));
            assertEquals(1, st.slotOf(8));
            assertEquals(2, st.slotOf(16));
            assertEquals(3, st.maxClusterLength());
            assertEquals(1, st.clusterCount());
        }
    }

    @Nested
    @DisplayName("墓碑删除")
    class TombstoneTest {

        @Test
        @DisplayName("删除链中元素后探测链不断,墓碑可复用")
        void deleteLeavesTombstoneAndReuse() {
            OpenAddressHashST<String> st = new OpenAddressHashST<String>(8, 1.0);
            st.put(0, "a");
            st.put(8, "b");
            st.put(16, "c");
            assertEquals(0, st.tombstones());

            st.delete(0);
            assertEquals(1, st.tombstones());
            assertEquals(2, st.size());
            assertTrue(st.contains(8), "删除链首后不应断裂");
            assertEquals("c", st.get(16));

            st.put(24, "d"); // 24 mod 8 = 0,复用墓碑
            assertEquals(0, st.tombstones());
            assertEquals(0, st.slotOf(24));
            assertEquals(3, st.size());
        }

        @Test
        @DisplayName("删除不存在的关键字返回 null 且无副作用")
        void deleteMissing() {
            OpenAddressHashST<String> st = new OpenAddressHashST<String>(8, 1.0);
            st.put(3, "v");
            assertNull(st.delete(4));
            assertEquals(1, st.size());
            assertEquals(0, st.tombstones());
        }

        @Test
        @DisplayName("表满(不自动扩容)时 put 抛异常,腾出墓碑后可继续插入")
        void fullTableThrows() {
            OpenAddressHashST<String> st = new OpenAddressHashST<String>(4, 1.0);
            st.put(0, "a");
            st.put(1, "b");
            st.put(2, "c");
            st.put(3, "d");
            assertEquals(4, st.size());
            assertThrows(IllegalStateException.class, () -> st.put(4, "e"));

            st.delete(0);
            st.put(4, "e"); // 复用墓碑
            assertEquals(4, st.size());
            assertTrue(st.contains(4));
            assertThrows(IllegalStateException.class, () -> st.put(5, "f"));
        }
    }

    @Nested
    @DisplayName("自动扩容与再散列")
    class ResizeTest {

        @Test
        @DisplayName("装填因子超过上限时扩容到素数容量,元素全部保留、墓碑清零")
        void growsAtThreshold() {
            OpenAddressHashST<String> st = new OpenAddressHashST<String>(17, 0.75);
            for (long key = 0; key < 12; key++) {
                st.put(key, "v" + key);
            }
            assertEquals(17, st.capacity());
            assertEquals(0, st.resizeCount());
            st.put(12, "v12"); // (12+1)/17 = 0.7647 > 0.75 → 先扩容
            assertEquals(1, st.resizeCount());
            assertTrue(st.capacity() >= 34, "新容量应不小于 2 倍旧容量,实际 " + st.capacity());
            assertEquals(37, st.capacity()); // nextPrime(34)
            assertEquals(13, st.size());
            for (long key = 0; key <= 12; key++) {
                assertEquals("v" + key, st.get(key), "扩容后关键字 " + key + " 丢了");
            }
            assertEquals(0, st.tombstones(), "重建时应丢弃墓碑");
        }

        @Test
        @DisplayName("扩容后墓碑被丢弃")
        void tombstonesDroppedOnResize() {
            OpenAddressHashST<String> st = new OpenAddressHashST<String>(8, 0.5);
            for (long key = 0; key < 4; key++) {
                st.put(key, "v" + key);
            }
            st.delete(0);
            st.delete(1);
            st.put(4, "v4");
            st.put(5, "v5");
            assertEquals(2, st.tombstones());
            st.put(6, "v6"); // 触发扩容
            assertTrue(st.capacity() > 8);
            assertEquals(0, st.tombstones());
            assertEquals(5, st.size());
            assertEquals("v2", st.get(2));
            assertFalse(st.contains(0));
        }

        @Test
        @DisplayName("大量插入:自动扩容后所有元素仍可查回")
        void manyInsertsSurviveResizes() {
            OpenAddressHashST<String> st = new OpenAddressHashST<String>();
            Random random = new Random(20261002L);
            TreeSet<Long> expected = new TreeSet<Long>();
            for (int i = 0; i < 10000; i++) {
                long key = random.nextInt(1000000);
                st.put(key, "v" + key);
                expected.add(key);
            }
            assertTrue(st.resizeCount() > 0);
            assertTrue(st.loadFactor() <= st.maxLoadFactor(), "扩容后 α 必须回到上限以内");
            assertEquals(expected.size(), st.size());
            TreeSet<Long> actual = new TreeSet<Long>();
            st.keys().forEach(actual::add);
            assertEquals(expected, actual);
            for (long key : expected) {
                assertEquals("v" + key, st.get(key));
            }
        }
    }

    @Nested
    @DisplayName("聚集度量与 ASL")
    class ClusterAndAslTest {

        @Test
        @DisplayName("全部关键字同址时连成一块;空表没有块")
        void clusterMetrics() {
            OpenAddressHashST<String> empty = new OpenAddressHashST<String>(8, 1.0);
            assertEquals(0, empty.maxClusterLength());
            assertEquals(0, empty.clusterCount());

            OpenAddressHashST<String> st = new OpenAddressHashST<String>(8, 1.0);
            for (long key = 0; key < 5; key++) {
                st.put(key * 8, "v" + key); // 全落地址 0
            }
            assertEquals(5, st.maxClusterLength());
            assertEquals(1, st.clusterCount());
        }

        @Test
        @DisplayName("ASL 统计与独立模拟的线性探测一致")
        void probeStatisticsMatchSimulation() {
            int m = 16;
            long[] keys = {3, 19, 35}; // 默认散列下都是地址 3
            OpenAddressHashST<String> st = new OpenAddressHashST<String>(m, 1.0);
            int[] slots = new int[m];
            Arrays.fill(slots, -1);

            long successSum = 0;
            for (long key : keys) {
                st.put(key, "v" + key);
                int index = st.hashAddress(key);
                int probes = 0;
                while (true) {
                    probes++;
                    if (slots[index] == -1) {
                        slots[index] = 1;
                        break;
                    }
                    index = (index + 1) % m;
                }
                successSum += probes;
            }

            long failureSum = 0;
            for (int start = 0; start < m; start++) {
                int probes = 1;
                int index = start;
                while (slots[index] != -1) {
                    probes++;
                    index = (index + 1) % m;
                }
                failureSum += probes;
            }

            assertEquals(successSum, st.successfulProbeSum());
            assertEquals(failureSum, st.unsuccessfulProbeSum());
            assertEquals((double) successSum / keys.length, st.averageSuccessfulProbes(), 1e-12);
            assertEquals((double) failureSum / m, st.averageUnsuccessfulProbes(), 1e-12);
        }

        @Test
        @DisplayName("教材近似式:空表为 1,α 越大越大,满表为无穷")
        void theoreticalFormulas() {
            OpenAddressHashST<String> empty = new OpenAddressHashST<String>(8, 1.0);
            assertEquals(1.0, empty.theoreticalSuccessfulProbes(), 1e-12);
            assertEquals(1.0, empty.theoreticalUnsuccessfulProbes(), 1e-12);

            OpenAddressHashST<String> half = new OpenAddressHashST<String>(8, 1.0);
            for (long key = 0; key < 4; key++) {
                half.put(key, "v" + key);
            }
            assertEquals(0.5, half.loadFactor(), 1e-12);
            assertEquals(0.5 * (1 + 1 / 0.5), half.theoreticalSuccessfulProbes(), 1e-12);   // 1.5
            assertEquals(0.5 * (1 + 1 / 0.25), half.theoreticalUnsuccessfulProbes(), 1e-12); // 2.5

            OpenAddressHashST<String> full = new OpenAddressHashST<String>(4, 1.0);
            for (long key = 0; key < 4; key++) {
                full.put(key, "v" + key);
            }
            assertTrue(Double.isInfinite(full.theoreticalSuccessfulProbes()));
            assertTrue(Double.isInfinite(full.theoreticalUnsuccessfulProbes()));
        }
    }

    @Nested
    @DisplayName("基本操作与参数校验")
    class BasicTest {

        @Test
        @DisplayName("put/get/delete/contains/size/isEmpty/loadFactor/keys/toString")
        void basicApi() {
            OpenAddressHashST<String> st = new OpenAddressHashST<String>(8, 1.0);
            assertTrue(st.isEmpty());
            assertEquals(0.0, st.loadFactor(), 1e-12);

            st.put(1, "v1"); // 地址 1
            st.put(2, "v2"); // 地址 2
            st.put(10, "v10"); // 10 mod 8 = 2 → 探测到 3
            assertEquals(3, st.size());
            assertEquals(0.375, st.loadFactor(), 1e-12);
            assertEquals(1, st.slotOf(1));
            assertEquals(2, st.slotOf(2));
            assertEquals(3, st.slotOf(10));
            assertEquals("v10", st.get(10));
            assertTrue(st.contains(2));
            assertFalse(st.contains(5));
            assertNull(st.get(5));

            List<Long> keys = new ArrayList<Long>();
            st.keys().forEach(keys::add);
            assertEquals(Arrays.asList(1L, 2L, 10L), keys);
            assertEquals("{1 v1, 2 v2, 10 v10}", st.toString());

            assertEquals("v10", st.delete(10));
            assertEquals(2, st.size());
            assertNull(st.delete(10));
        }

        @Test
        @DisplayName("重复插入只覆盖值;null 值抛异常")
        void overwriteAndNull() {
            OpenAddressHashST<String> st = new OpenAddressHashST<String>(8, 1.0);
            st.put(3, "旧");
            st.put(3, "新");
            assertEquals(1, st.size());
            assertEquals("新", st.get(3));
            assertThrows(NullPointerException.class, () -> st.put(3, null));
        }

        @Test
        @DisplayName("构造参数校验")
        void constructorValidation() {
            assertThrows(IllegalArgumentException.class, () -> new OpenAddressHashST<String>(1));
            assertThrows(IllegalArgumentException.class, () -> new OpenAddressHashST<String>(0));
            assertThrows(IllegalArgumentException.class,
                    () -> new OpenAddressHashST<String>((1 << 26) + 1));
            assertThrows(IllegalArgumentException.class, () -> new OpenAddressHashST<String>(8, 0.0));
            assertThrows(IllegalArgumentException.class, () -> new OpenAddressHashST<String>(8, 1.5));
            assertThrows(NullPointerException.class,
                    () -> new OpenAddressHashST<String>(8, 0.75, null));
        }

        @Test
        @DisplayName("nextPrime:不小于给定值的最小素数")
        void nextPrimeWorks() {
            assertEquals(2, OpenAddressHashST.nextPrime(2));
            assertEquals(3, OpenAddressHashST.nextPrime(3));
            assertEquals(5, OpenAddressHashST.nextPrime(4));
            assertEquals(17, OpenAddressHashST.nextPrime(16));
            assertEquals(37, OpenAddressHashST.nextPrime(34));
            assertEquals(163, OpenAddressHashST.nextPrime(158));
            assertThrows(IllegalArgumentException.class, () -> OpenAddressHashST.nextPrime(1));
        }
    }
}
