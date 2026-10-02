package cn.exercise.algs4.datastructure.hash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 随机数法散列表单元测试。
 * 重点验证:伪随机函数的<b>确定性</b>(同一关键字同一地址)、31 位非负与地址范围、
 * 雪崩效应与分布均匀性、种子参与、负数与极值的可用性,以及线性探测/墓碑/满表行为。
 */
@DisplayName("RandomHashST 随机数法散列表测试")
class RandomHashSTTest {

    @Nested
    @DisplayName("伪随机函数 random(key)")
    class RandomFunctionTest {

        @Test
        @DisplayName("同一关键字确定性地得到同一结果(可用不同实例验证)")
        void deterministic() {
            RandomHashST<String> a = new RandomHashST<String>(16);
            RandomHashST<String> b = new RandomHashST<String>(16);
            for (long key = -50; key <= 50; key++) {
                assertEquals(a.randomBits(key), b.randomBits(key), "key = " + key);
                assertEquals(a.hash(key), b.hash(key), "key = " + key);
            }
            assertEquals(a.randomBits(1000), a.randomBits(1000));
        }

        @Test
        @DisplayName("random 结果为 31 位非负,地址落在 [0, tableSize)")
        void nonNegativeAndInRange() {
            int[] sizes = {1, 2, 16, 1024};
            long[] keys = {0, 1, -1, 12345, -98765, Long.MIN_VALUE, Long.MAX_VALUE};
            for (int size : sizes) {
                RandomHashST<String> st = new RandomHashST<String>(size);
                for (long key : keys) {
                    int bits = st.randomBits(key);
                    assertTrue(bits >= 0, "random 结果为负: " + bits);
                    int address = st.hash(key);
                    assertTrue(address >= 0 && address < size, "key = " + key + " 地址越界: " + address);
                }
            }
        }

        @Test
        @DisplayName("种子参与运算:同种子结果相同,换种子后地址一般不同")
        void seedMatters() {
            RandomHashST<String> a = new RandomHashST<String>(16);
            RandomHashST<String> same = new RandomHashST<String>(16, a.seed());
            RandomHashST<String> other = new RandomHashST<String>(16, a.seed() + 1);

            assertEquals(a.hash(1000), same.hash(1000));
            assertEquals(a.seed(), same.seed());
            assertEquals(13, a.hash(1000));      // 固定种子下的回归值
            assertEquals(12, other.hash(1000));  // 换种子后落在别处
            assertEquals(a.seed() + 1, other.seed());
        }

        @Test
        @DisplayName("雪崩效应:相邻关键字约有一半的位不同")
        void avalanche() {
            RandomHashST<String> st = new RandomHashST<String>(16);
            long total = 0;
            int pairs = 5000;
            int min = 32;
            int max = 0;
            for (long key = 0; key < pairs; key++) {
                int diff = Integer.bitCount(st.randomBits(key) ^ st.randomBits(key + 1));
                total += diff;
                if (diff < min) {
                    min = diff;
                }
                if (diff > max) {
                    max = diff;
                }
            }
            double average = (double) total / pairs;
            assertTrue(average > 13.0 && average < 19.0, "相邻关键字的平均位差应在 16 附近,实际 = " + average);
            assertTrue(max - min > 4, "位差不应几乎恒定:min = " + min + ", max = " + max);
        }

        @Test
        @DisplayName("分布均匀:1 万个连续关键字投到 1024 个地址,无明显聚集")
        void uniformDistribution() {
            int buckets = 1024;
            int keys = 10000;
            RandomHashST<String> st = new RandomHashST<String>(buckets);
            int[] load = new int[buckets];
            for (int i = 0; i < keys; i++) {
                load[st.hash(i)]++;
            }
            int maxLoad = 0;
            int empty = 0;
            int sum = 0;
            for (int count : load) {
                maxLoad = Math.max(maxLoad, count);
                if (count == 0) {
                    empty++;
                }
                sum += count;
            }
            assertEquals(keys, sum);
            assertTrue(maxLoad <= 30, "最忙地址装载过多: " + maxLoad);
            assertTrue(empty <= 5, "空地址过多,分布不均: " + empty);
        }
    }

    @Nested
    @DisplayName("构造与参数校验")
    class ConstructorTest {

        @Test
        @DisplayName("表长与种子的基本约束")
        void validArguments() {
            assertEquals(16, new RandomHashST<String>(16).tableSize());
            assertEquals(1, new RandomHashST<String>(1).tableSize());
            assertEquals(42L, new RandomHashST<String>(16, 42L).seed());
            assertThrows(IllegalArgumentException.class, () -> new RandomHashST<String>(0));
            assertThrows(IllegalArgumentException.class, () -> new RandomHashST<String>(-1));
            assertThrows(IllegalArgumentException.class, () -> new RandomHashST<String>((1 << 26) + 1));
        }
    }

    @Nested
    @DisplayName("线性探测、墓碑与满表")
    class ProbingTest {

        @Test
        @DisplayName("同地址的关键字相邻落位,删除中间键后探测链不断")
        void probingAndTombstone() {
            RandomHashST<String> st = new RandomHashST<String>(4);
            assertEquals(0, st.hash(1));
            assertEquals(0, st.hash(-1));  // 与 1 同地址
            st.put(1, "a");
            st.put(-1, "b");
            assertEquals(0, st.slotOf(1));
            assertEquals(1, st.slotOf(-1));
            assertEquals(2, st.lastProbes());

            st.delete(1);
            assertEquals(1, st.tombstones());
            assertTrue(st.contains(-1), "删除链首元素后不应断裂");
            assertEquals("b", st.get(-1));

            long third = -1;
            for (long key = 2; key < 100000; key++) {
                if (st.hash(key) == 0) {
                    third = key;
                    break;
                }
            }
            assertTrue(third > 0, "没找到第三个同地址的关键字");
            st.put(third, "c");
            assertEquals(0, st.slotOf(third), "应复用墓碑");
            assertEquals(0, st.tombstones());
            assertEquals(2, st.size());
        }

        @Test
        @DisplayName("表长 1 时全部关键字同地址:满表拒插,腾出墓碑后可插入")
        void fullTable() {
            RandomHashST<String> st = new RandomHashST<String>(1);
            st.put(1, "a");
            assertThrows(IllegalStateException.class, () -> st.put(2, "b"));
            assertEquals(1, st.size());
            assertEquals(0, st.tombstones());   // 满表时没有墓碑
            assertEquals("a", st.delete(1));
            st.put(2, "b");                          // 复用墓碑
            assertEquals(1, st.size());
            assertEquals("b", st.get(2));
            assertFalse(st.contains(1));
        }

        @Test
        @DisplayName("ASL 统计与独立模拟的线性探测一致")
        void probeStatisticsMatchSimulation() {
            int m = 16;
            long[] keys = {1, -1, 2};
            RandomHashST<String> st = new RandomHashST<String>(m);
            int[] slots = new int[m];
            Arrays.fill(slots, -1);

            long successSum = 0;
            for (long key : keys) {
                st.put(key, "v" + key);
                int index = st.hash(key);
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
    }

    @Nested
    @DisplayName("符号表操作")
    class SymbolTableTest {

        @Test
        @DisplayName("put/get/delete/contains/size/isEmpty/loadFactor/keys/toString")
        void basicApi() {
            RandomHashST<String> st = new RandomHashST<String>(16);
            assertTrue(st.isEmpty());
            assertEquals(0.0, st.loadFactor(), 1e-12);

            st.put(1, "v1");   // 地址 0
            st.put(2, "v2");   // 地址 5
            st.put(0, "v0");   // 地址 10
            assertEquals(3, st.size());
            assertEquals(3.0 / 16.0, st.loadFactor(), 1e-12);
            assertEquals("v2", st.get(2));
            assertTrue(st.contains(0));
            assertFalse(st.contains(3));
            assertNull(st.get(3));
            assertEquals(0, st.slotOf(1));
            assertEquals(5, st.slotOf(2));
            assertEquals(10, st.slotOf(0));

            List<Long> keys = new ArrayList<Long>();
            st.keys().forEach(keys::add);
            assertEquals(Arrays.asList(1L, 2L, 0L), keys); // 地址升序:0 < 5 < 10
            assertEquals("{1 v1, 2 v2, 0 v0}", st.toString());

            assertEquals("v2", st.delete(2));
            assertFalse(st.contains(2));
            assertEquals(2, st.size());
            assertEquals(1, st.tombstones());
            assertNull(st.delete(2));
            assertEquals(2, st.size());
        }

        @Test
        @DisplayName("重复插入只覆盖值;null 值抛异常")
        void overwriteAndNull() {
            RandomHashST<String> st = new RandomHashST<String>(16);
            st.put(1000, "旧");
            st.put(1000, "新");
            assertEquals(1, st.size());
            assertEquals("新", st.get(1000));
            assertThrows(NullPointerException.class, () -> st.put(1, null));
        }

        @Test
        @DisplayName("keys() 返回全部已存关键字,不受删除影响")
        void keysAfterDeletes() {
            RandomHashST<String> st = new RandomHashST<String>(32);
            for (long key = 0; key < 20; key++) {
                st.put(key, "v" + key);
            }
            for (long key = 0; key < 20; key += 2) {
                st.delete(key);
            }
            TreeSet<Long> actual = new TreeSet<Long>();
            st.keys().forEach(actual::add);
            TreeSet<Long> expected = new TreeSet<Long>();
            for (long key = 1; key < 20; key += 2) {
                expected.add(key);
            }
            assertEquals(expected, actual);
            assertEquals(10, st.size());
        }
    }
}
