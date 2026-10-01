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
 * 除留余数法散列表单元测试。
 * 重点验证:H(key) = key mod p 的取模(含负数)、模数自动取质数、
 * 线性探测的地址序列、墓碑删除的正确性,以及教材经典例题的 ASL 计算。
 */
@DisplayName("DivisionHashST 除留余数法散列表测试")
class DivisionHashSTTest {

    /** 教材经典关键字序列 */
    private static final int[] CLASSIC_KEYS = {19, 14, 23, 1, 68, 20, 84, 27, 55, 11, 10, 79};

    /** 经典例题中每个关键字的实际地址(表长 16、p = 13、线性探测) */
    private static final int[] CLASSIC_SLOTS = {6, 1, 10, 2, 3, 7, 8, 4, 5, 11, 12, 9};

    /** 经典例题中每个关键字的探测次数 */
    private static final int[] CLASSIC_PROBES = {1, 1, 1, 2, 1, 1, 3, 4, 3, 1, 3, 9};

    @Nested
    @DisplayName("散列函数 H(key) = key mod p")
    class HashFunctionTest {

        @Test
        @DisplayName("正数直接取模")
        void positiveKeys() {
            DivisionHashST<String> st = new DivisionHashST<>(16);
            assertEquals(13, st.modulus());
            assertEquals(6, st.hash(19));
            assertEquals(1, st.hash(14));
            assertEquals(10, st.hash(23));
            assertEquals(0, st.hash(13));
            assertEquals(0, st.hash(0));
        }

        @Test
        @DisplayName("负数用 floorMod 取模,地址仍落在 [0, p-1]")
        void negativeKeys() {
            DivisionHashST<String> st = new DivisionHashST<>(13);
            assertEquals(12, st.hash(-1));
            assertEquals(0, st.hash(-13));
            assertEquals(0, st.hash(-26));
            assertEquals(7, st.hash(-6));
        }

        @Test
        @DisplayName("地址始终落在 [0, modulus) 内")
        void addressInRange() {
            DivisionHashST<String> st = new DivisionHashST<>(16, 13);
            for (int key = -100; key <= 100; key++) {
                int addr = st.hash(key);
                assertTrue(addr >= 0 && addr < st.modulus(), "key = " + key + " 得到非法地址 " + addr);
            }
        }
    }

    @Nested
    @DisplayName("模数 p 的选取")
    class ModulusTest {

        @Test
        @DisplayName("单参构造自动取不大于表长的最大质数")
        void defaultModulusIsLargestPrime() {
            assertEquals(13, new DivisionHashST<String>(16).modulus());
            assertEquals(13, new DivisionHashST<String>(14).modulus());
            assertEquals(13, new DivisionHashST<String>(13).modulus());
            assertEquals(17, new DivisionHashST<String>(17).modulus());
            assertEquals(19, new DivisionHashST<String>(20).modulus());
            assertEquals(97, new DivisionHashST<String>(100).modulus());
            assertEquals(2, new DivisionHashST<String>(2).modulus());
            assertEquals(3, new DivisionHashST<String>(3).modulus());
            assertEquals(3, new DivisionHashST<String>(4).modulus());
        }

        @Test
        @DisplayName("largestPrimeNotGreaterThan 的边界")
        void largestPrimeHelper() {
            assertEquals(2, DivisionHashST.largestPrimeNotGreaterThan(2));
            assertEquals(3, DivisionHashST.largestPrimeNotGreaterThan(3));
            assertEquals(3, DivisionHashST.largestPrimeNotGreaterThan(4));
            assertEquals(7, DivisionHashST.largestPrimeNotGreaterThan(9));
            assertEquals(97, DivisionHashST.largestPrimeNotGreaterThan(97));
            assertEquals(97, DivisionHashST.largestPrimeNotGreaterThan(100));
            assertThrows(IllegalArgumentException.class, () -> DivisionHashST.largestPrimeNotGreaterThan(1));
            assertThrows(IllegalArgumentException.class, () -> DivisionHashST.largestPrimeNotGreaterThan(0));
        }

        @Test
        @DisplayName("可以显式指定 p < m(如教材的 m = 16 配 p = 13)")
        void explicitModulus() {
            DivisionHashST<String> st = new DivisionHashST<>(16, 12);
            assertEquals(12, st.modulus());
            assertEquals(16, st.tableSize());
        }

        @Test
        @DisplayName("表长超过实用上限时抛异常(不会尝试分配而 OutOfMemoryError)")
        void tooLargeTableSizeThrows() {
            assertThrows(IllegalArgumentException.class, () -> new DivisionHashST<String>(1 << 27));
            assertThrows(IllegalArgumentException.class, () -> new DivisionHashST<String>(1 << 27, 3));
        }

        @Test
        @DisplayName("表长或模数非法时抛异常")
        void invalidArguments() {
            assertThrows(IllegalArgumentException.class, () -> new DivisionHashST<String>(1));
            assertThrows(IllegalArgumentException.class, () -> new DivisionHashST<String>(0));
            assertThrows(IllegalArgumentException.class, () -> new DivisionHashST<String>(-5));
            assertThrows(IllegalArgumentException.class, () -> new DivisionHashST<String>(16, 0));
            assertThrows(IllegalArgumentException.class, () -> new DivisionHashST<String>(16, 17));
        }
    }

    @Nested
    @DisplayName("线性探测与经典例题")
    class LinearProbingTest {

        @Test
        @DisplayName("经典例题:每个关键字的地址与探测次数与手算一致")
        void classicExampleSlotsAndProbes() {
            DivisionHashST<String> st = new DivisionHashST<>(16);
            for (int key : CLASSIC_KEYS) {
                st.put(key, "v" + key);
            }
            for (int i = 0; i < CLASSIC_KEYS.length; i++) {
                assertEquals(CLASSIC_SLOTS[i], st.slotOf(CLASSIC_KEYS[i]),
                        "关键字 " + CLASSIC_KEYS[i] + " 的地址不对");
                st.get(CLASSIC_KEYS[i]);
                assertEquals(CLASSIC_PROBES[i], st.lastProbes(),
                        "关键字 " + CLASSIC_KEYS[i] + " 的探测次数不对");
            }
        }

        @Test
        @DisplayName("经典例题:ASL成功 = 30/12 = 2.5,ASL失败 = 94/16 = 5.875")
        void classicExampleAsl() {
            DivisionHashST<String> st = new DivisionHashST<>(16);
            for (int key : CLASSIC_KEYS) {
                st.put(key, "v" + key);
            }
            assertEquals(12, st.size());
            assertEquals(0.75, st.loadFactor());
            assertEquals(30L, st.successfulProbeSum());
            assertEquals(2.5, st.averageSuccessfulProbes());
            assertEquals(94L, st.unsuccessfulProbeSum());
            assertEquals(5.875, st.averageUnsuccessfulProbes());
        }

        @Test
        @DisplayName("keys() 按数组下标升序返回,toString 格式与 algs4 一致")
        void keysInSlotOrder() {
            DivisionHashST<String> st = new DivisionHashST<>(16);
            for (int key : CLASSIC_KEYS) {
                st.put(key, "v" + key);
            }
            List<Integer> keys = new ArrayList<>();
            st.keys().forEach(keys::add);
            assertEquals(Arrays.asList(14, 1, 68, 27, 55, 19, 20, 84, 79, 23, 11, 10), keys);
            assertTrue(st.toString().startsWith("{14 v14, 1 v1, 68 v68"));
        }

        @Test
        @DisplayName("查找不存在的关键字返回 null,探测次数为遇到第一个空单元的次数")
        void unsuccessfulSearch() {
            DivisionHashST<String> st = new DivisionHashST<>(16, 13);
            assertNull(st.get(19));
            st.put(19, "v19");
            st.put(14, "v14");
            assertNull(st.get(7));       // H(7) = 7,空单元,1 次探测
            assertEquals(1, st.lastProbes());
            assertNull(st.get(13));      // H(13) = 0,空单元,1 次探测
            assertEquals(1, st.lastProbes());
            assertEquals(-1, st.slotOf(999));
            assertFalse(st.contains(999));
        }

        @Test
        @DisplayName("空表的 ASL成功为 0")
        void emptyTableAsl() {
            DivisionHashST<String> st = new DivisionHashST<>(16);
            assertTrue(st.isEmpty());
            assertEquals(0.0, st.averageSuccessfulProbes());
            assertEquals(0, st.size());
        }
    }

    @Nested
    @DisplayName("插入、删除与墓碑")
    class InsertDeleteTest {

        @Test
        @DisplayName("重复插入只覆盖值,不增加元素个数")
        void putOverwrites() {
            DivisionHashST<String> st = new DivisionHashST<>(7);
            st.put(3, "旧值");
            st.put(3, "新值");
            assertEquals(1, st.size());
            assertEquals("新值", st.get(3));
        }

        @Test
        @DisplayName("删除留墓碑,后续关键字的探测链不断裂")
        void tombstoneKeepsProbeChain() {
            DivisionHashST<String> st = new DivisionHashST<>(7);
            st.put(1, "a");
            st.put(8, "b");   // H = 1,冲突 -> 2
            st.put(15, "c");  // H = 1,冲突 -> 3
            assertEquals(1, st.slotOf(1));
            assertEquals(2, st.slotOf(8));
            assertEquals(3, st.slotOf(15));

            assertEquals("a", st.delete(1));
            assertEquals(1, st.tombstones());
            assertEquals(2, st.size());
            assertTrue(st.contains(8));   // 探测经过墓碑后仍能找到
            assertTrue(st.contains(15));
            assertEquals("b", st.get(8));

            st.put(22, "d");              // H = 1,复用墓碑
            // 插入时:第 1 次探测到墓碑并记住,第 2、3 次被 8、15 占用,第 4 次遇到空单元后回填墓碑
            assertEquals(4, st.lastProbes());
            assertEquals(1, st.slotOf(22));
            assertEquals(0, st.tombstones());
            assertEquals(3, st.size());
        }

        @Test
        @DisplayName("删除不存在的关键字返回 null 且无副作用")
        void deleteMissingKey() {
            DivisionHashST<String> st = new DivisionHashST<>(7);
            st.put(3, "v");
            assertNull(st.delete(4));
            assertEquals(1, st.size());
            assertEquals(0, st.tombstones());
        }

        @Test
        @DisplayName("表满且无墓碑时插入抛异常,腾出墓碑后可继续插入")
        void fullTableThrows() {
            DivisionHashST<String> st = new DivisionHashST<>(2);
            st.put(0, "a");
            st.put(2, "b");   // H(2) = 0,冲突 -> 1
            assertEquals(2, st.size());
            assertThrows(IllegalStateException.class, () -> st.put(4, "c"));

            st.delete(0);
            st.put(4, "c");   // 复用墓碑
            assertEquals(2, st.size());
            assertEquals(0, st.slotOf(4));
            assertTrue(st.contains(4));
        }

        @Test
        @DisplayName("值为 null 时抛异常")
        void nullValueRejected() {
            DivisionHashST<String> st = new DivisionHashST<>(7);
            assertThrows(NullPointerException.class, () -> st.put(3, null));
        }
    }
}
