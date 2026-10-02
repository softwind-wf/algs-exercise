package cn.exercise.algs4.datastructure.hash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 平方取中法散列表单元测试。
 * 重点验证:取中间 k 位的取位规则(含平方位数不足 k 位的情形)、平方溢出防护、
 * 线性探测与墓碑、ASL 统计与独立模拟一致,以及"末两位相同则平方取中仍能打散"的性质。
 */
@DisplayName("MidSquareHashST 平方取中法散列表测试")
class MidSquareHashSTTest {

    @Nested
    @DisplayName("散列函数:取 key² 的中间 k 位")
    class HashFunctionTest {

        @Test
        @DisplayName("k = 2 的取位结果与手算一致")
        void twoDigits() {
            MidSquareHashST<String> st = new MidSquareHashST<String>(2);
            assertEquals(100, st.tableSize());
            assertEquals(1522756L, st.square(1234));
            assertEquals(27, st.hash(1234));   // 1522756 → 丢 2 位 → 15227 → 27
            assertEquals(39, st.hash(5678));   // 32239684 → 丢 3 位 → 32239 → 39
            assertEquals(80, st.hash(99));     // 9801 → 丢 1 位 → 980 → 80
            assertEquals(0, st.hash(100));     // 10000 → 丢 1 位 → 1000 → 00
            assertEquals(0, st.hash(0));
        }

        @Test
        @DisplayName("平方位数不足 k 位时直接用平方值作地址")
        void fewerDigitsThanRequested() {
            MidSquareHashST<String> st = new MidSquareHashST<String>(2);
            assertEquals(9L, st.square(3));
            assertEquals(9, st.hash(3));
            assertEquals(1, st.hash(1));
            assertEquals(4, st.hash(2));
        }

        @Test
        @DisplayName("k = 1 与 k = 3 的窗口位置不同")
        void differentWindowSizes() {
            MidSquareHashST<String> one = new MidSquareHashST<String>(1);
            assertEquals(4, one.hash(2));      // 4 → 4
            assertEquals(4, one.hash(12));     // 144 → 丢 1 位 → 14 → 4
            assertEquals(4, one.hash(38));     // 1444 → 丢 1 位 → 144 → 4

            MidSquareHashST<String> three = new MidSquareHashST<String>(3);
            assertEquals(227, three.hash(1234)); // 1522756 → 丢 2 位 → 15227 → 227
        }

        @Test
        @DisplayName("负数与其相反数地址相同(平方的性质),但作为不同的键分别保存")
        void negativeKeysShareAddress() {
            MidSquareHashST<String> st = new MidSquareHashST<String>(2);
            assertEquals(st.hash(1234), st.hash(-1234));
            assertEquals(27, st.hash(-1234));
            st.put(1234, "正");
            st.put(-1234, "负");
            assertEquals(2, st.size());          // 两个不同的键,靠线性探测放在相邻槽
            assertEquals("正", st.get(1234));
            assertEquals("负", st.get(-1234));
            assertEquals(27, st.slotOf(1234));
            assertEquals(28, st.slotOf(-1234));
        }

        @Test
        @DisplayName("地址始终落在 [0, tableSize)")
        void addressInRange() {
            MidSquareHashST<String> st = new MidSquareHashST<String>(2);
            for (long key = -500; key <= 500; key++) {
                int address = st.hash(key);
                assertTrue(address >= 0 && address < st.tableSize(), "key = " + key + " 地址越界: " + address);
            }
        }

        @Test
        @DisplayName("平方溢出 long 时抛异常,边界值可用")
        void overflowIsRejected() {
            MidSquareHashST<String> st = new MidSquareHashST<String>(2);
            assertEquals(9223372030926249001L, st.square(3037000499L));
            assertTrue(st.hash(3037000499L) >= 0 && st.hash(3037000499L) < 100);
            assertThrows(IllegalArgumentException.class, () -> st.square(3037000500L));
            assertThrows(IllegalArgumentException.class, () -> st.hash(3037000500L));
            assertThrows(IllegalArgumentException.class, () -> st.put(-3037000500L, "x"));
        }

        @Test
        @DisplayName("末两位相同的关键字,平方取中后不再挤在同一地址")
        void midSquareSpreadsCollideProneKeys() {
            MidSquareHashST<String> st = new MidSquareHashST<String>(2);
            long[] keys = {1201, 2301, 3401, 4501, 5601, 6701, 7801, 8901};
            boolean[] used = new boolean[st.tableSize()];
            int collisions = 0;
            for (long key : keys) {
                int address = st.hash(key);
                if (used[address]) {
                    collisions++;
                }
                used[address] = true;
                assertEquals(1, Math.floorMod(key, 100)); // 末两位取法会全部落在地址 1
            }
            assertEquals(0, collisions);
        }

        @Test
        @DisplayName("同一关键字多次计算结果稳定")
        void deterministic() {
            MidSquareHashST<String> a = new MidSquareHashST<String>(3);
            MidSquareHashST<String> b = new MidSquareHashST<String>(3);
            for (long key = 0; key < 50; key++) {
                assertEquals(a.hash(key), b.hash(key));
            }
        }
    }

    @Nested
    @DisplayName("构造参数")
    class ConstructorTest {

        @Test
        @DisplayName("表长 = 10^k")
        void tableSizeIsPowerOfTen() {
            assertEquals(10, new MidSquareHashST<String>(1).tableSize());
            assertEquals(100, new MidSquareHashST<String>(2).tableSize());
            assertEquals(1000000, new MidSquareHashST<String>(6).tableSize());
            assertEquals(2, new MidSquareHashST<String>(2).addressDigits());
        }

        @Test
        @DisplayName("地址位数越界抛异常")
        void invalidDigits() {
            assertThrows(IllegalArgumentException.class, () -> new MidSquareHashST<String>(0));
            assertThrows(IllegalArgumentException.class, () -> new MidSquareHashST<String>(-1));
            assertThrows(IllegalArgumentException.class, () -> new MidSquareHashST<String>(8));
        }
    }

    @Nested
    @DisplayName("符号表基本操作")
    class SymbolTableTest {

        @Test
        @DisplayName("put/get/delete/contains/size/isEmpty/loadFactor/keys/toString")
        void basicApi() {
            MidSquareHashST<String> st = new MidSquareHashST<String>(2);
            assertTrue(st.isEmpty());
            assertEquals(0.0, st.loadFactor(), 1e-12);

            st.put(1234, "a"); // 27
            st.put(5678, "b"); // 39
            st.put(99, "c");   // 80
            assertEquals(3, st.size());
            assertFalse(st.isEmpty());
            assertEquals(0.03, st.loadFactor(), 1e-12);
            assertEquals("a", st.get(1234));
            assertTrue(st.contains(99));
            assertFalse(st.contains(1));
            assertNull(st.get(1));
            assertEquals(27, st.slotOf(1234));
            assertEquals(39, st.slotOf(5678));
            assertEquals(80, st.slotOf(99));

            List<Long> keys = new ArrayList<Long>();
            st.keys().forEach(keys::add);
            assertEquals(Arrays.asList(1234L, 5678L, 99L), keys);
            assertEquals("{1234 a, 5678 b, 99 c}", st.toString());

            assertEquals("b", st.delete(5678));
            assertFalse(st.contains(5678));
            assertEquals(2, st.size());
            assertEquals(1, st.tombstones());
            assertNull(st.delete(5678));
            assertEquals(2, st.size());
        }

        @Test
        @DisplayName("重复插入只覆盖值,不增加元素个数")
        void putOverwrites() {
            MidSquareHashST<String> st = new MidSquareHashST<String>(2);
            st.put(1234, "旧");
            st.put(1234, "新");
            assertEquals(1, st.size());
            assertEquals("新", st.get(1234));
        }

        @Test
        @DisplayName("值为 null 抛异常")
        void nullValueRejected() {
            MidSquareHashST<String> st = new MidSquareHashST<String>(2);
            assertThrows(NullPointerException.class, () -> st.put(1234, null));
        }
    }

    @Nested
    @DisplayName("线性探测、墓碑与满表")
    class ProbingTest {

        @Test
        @DisplayName("同地址的三键依次落在连续槽位,删除中间键后探测链不断")
        void probingAndTombstone() {
            MidSquareHashST<String> st = new MidSquareHashST<String>(1); // 表长 10,三键都映射到 4
            st.put(2, "a");
            st.put(12, "b");
            st.put(38, "c");
            assertEquals(4, st.slotOf(2));
            assertEquals(5, st.slotOf(12));
            assertEquals(6, st.slotOf(38));
            assertEquals(3, st.lastProbes());

            st.delete(12);
            assertEquals(1, st.tombstones());
            assertEquals(2, st.size());
            assertTrue(st.contains(38), "删除中间元素后探测链不应断裂");
            assertEquals("c", st.get(38));

            st.put(102, "d"); // 102² = 10404 → 丢 2 位 → 104 → 4,复用墓碑
            assertEquals(5, st.slotOf(102));
            assertEquals(0, st.tombstones());
            assertEquals(3, st.size());
        }

        @Test
        @DisplayName("表满后拒插,腾出墓碑即可继续插入")
        void fullTableBehavesWell() {
            MidSquareHashST<String> st = new MidSquareHashST<String>(1);
            boolean full = false;
            for (long key = 0; key < 500 && !full; key++) {
                try {
                    st.put(key, "v" + key);
                } catch (IllegalStateException e) {
                    full = true;
                    assertEquals(10, st.size(), "拒绝插入时表必须已满");
                }
            }
            assertTrue(full, "循环结束仍未把表填满,测试强度不足");

            long victim = 0;
            for (long key : st.keys()) {
                victim = key;
                break;
            }
            st.delete(victim);
            st.put(123456789L, "x"); // 复用墓碑,必须成功
            assertTrue(st.contains(123456789L));
            assertEquals(10, st.size());
        }

        @Test
        @DisplayName("ASL 统计与独立模拟的线性探测一致")
        void probeStatisticsMatchSimulation() {
            int m = 10;
            long[] keys = {2, 12, 38};
            MidSquareHashST<String> st = new MidSquareHashST<String>(1);
            int[] slots = new int[m];
            Arrays.fill(slots, -1);

            long successSum = 0;
            for (long key : keys) {
                st.put(key, "v" + key);
                int index = st.hash(key);
                int probes = 0;
                while (true) { // 独立编写的线性探测模拟
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
        @DisplayName("空表的 ASL成功为 0")
        void emptyTableAsl() {
            MidSquareHashST<String> st = new MidSquareHashST<String>(2);
            assertTrue(st.isEmpty());
            assertEquals(0.0, st.averageSuccessfulProbes());
            assertEquals(1.0, st.averageUnsuccessfulProbes(), 1e-12); // 空表:100 个起点各 1 次探测
        }
    }
}
