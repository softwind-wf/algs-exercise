package cn.exercise.algs4.datastructure.hash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 折叠法散列表单元测试。
 * 重点验证:分段规则(低位起、最高段可短)、两种叠加方式(含偶数段按固定宽度补零反序)、
 * 折叠和取模得到地址,以及"末几位相同的关键字被折叠打散"这一性质。
 */
@DisplayName("FoldingHashST 折叠法散列表测试")
class FoldingHashSTTest {

    private static FoldingHashST<String> shift(int segmentDigits, int tableSize) {
        return new FoldingHashST<String>(segmentDigits, tableSize, FoldingHashST.Mode.SHIFT);
    }

    private static FoldingHashST<String> boundary(int segmentDigits, int tableSize) {
        return new FoldingHashST<String>(segmentDigits, tableSize, FoldingHashST.Mode.BOUNDARY);
    }

    @Nested
    @DisplayName("分段与叠加")
    class FoldingTest {

        @Test
        @DisplayName("按段长从低位切分,最高段可以短于段长")
        void segments() {
            FoldingHashST<String> st = shift(3, 1000);
            assertArrayEquals(new int[]{789, 456, 123}, st.segments(123456789));
            assertArrayEquals(new int[]{321, 654, 87}, st.segments(87654321));
            assertArrayEquals(new int[]{0, 1}, st.segments(1000));
            assertArrayEquals(new int[]{623, 45}, st.segments(45623));
            assertArrayEquals(new int[]{7}, st.segments(7));
            assertArrayEquals(new int[]{0}, st.segments(0));
        }

        @Test
        @DisplayName("移位叠加:各段正序相加")
        void shiftSum() {
            FoldingHashST<String> st = shift(3, 1000);
            assertEquals(1368L, st.foldedSum(123456789));   // 789 + 456 + 123
            assertEquals(1062L, st.foldedSum(87654321));    // 321 + 654 + 87
            assertEquals(1L, st.foldedSum(1000));           // 0 + 1
            assertEquals(0L, st.foldedSum(0));
        }

        @Test
        @DisplayName("分界叠加:奇数段正序、偶数段反序")
        void boundarySum() {
            FoldingHashST<String> st = boundary(3, 1000);
            assertEquals(1566L, st.foldedSum(123456789));   // 789 + 654 + 123
            assertEquals(864L, st.foldedSum(87654321));     // 321 + 456 + 87
            assertEquals(1221L, st.foldedSum(1234567890123L)); // 123 + 098 + 567 + 432 + 1
        }

        @Test
        @DisplayName("偶数段反序按固定宽度处理:缺位补零")
        void reversePadsWithZeros() {
            FoldingHashST<String> st = boundary(3, 1000);
            assertEquals(100L, st.foldedSum(1000));   // 段2 = "001" → 反序 "100"
            assertEquals(1000, st.tableSize());
            assertEquals(100, st.hash(1000));
            FoldingHashST<String> st2 = boundary(3, 100);
            assertEquals(0, st2.hash(1000));          // 100 mod 100 = 0
        }

        @Test
        @DisplayName("折叠和取模得到地址(表长 10^k 时即取后 k 位)")
        void hashByModulo() {
            assertEquals(368, shift(3, 1000).hash(123456789));
            assertEquals(566, boundary(3, 1000).hash(123456789));
            assertEquals(68, shift(3, 100).hash(123456789));    // 1368 mod 100
            assertEquals(66, boundary(3, 100).hash(123456789)); // 1566 mod 100
            assertEquals(1, shift(3, 10).hash(1000));           // 折叠和 1 → 1 mod 10
        }

        @Test
        @DisplayName("地址始终落在 [0, tableSize)")
        void addressInRange() {
            FoldingHashST<String> st = shift(2, 97);
            for (long key = 0; key < 1000; key++) {
                int address = st.hash(key);
                assertTrue(address >= 0 && address < 97, "key = " + key + " 地址越界: " + address);
            }
        }

        @Test
        @DisplayName("末三位相同的关键字被折叠打散")
        void foldingSpreadsCollideProneKeys() {
            long[] keys = {1001, 2001, 3001, 4001, 5001, 6001, 7001, 8001};
            FoldingHashST<String> st = shift(3, 1000);
            boolean[] used = new boolean[st.tableSize()];
            int collisions = 0;
            for (long key : keys) {
                assertEquals(1, key % 1000); // "取末三位"会全部落在地址 1
                int address = st.hash(key);
                if (used[address]) {
                    collisions++;
                }
                used[address] = true;
            }
            assertEquals(0, collisions);
        }

        @Test
        @DisplayName("同一关键字计算结果稳定,两种叠加方式结果不同")
        void deterministicAndModeDependent() {
            assertEquals(shift(3, 1000).hash(123456789), shift(3, 1000).hash(123456789));
            assertTrue(shift(3, 1000).hash(123456789) != boundary(3, 1000).hash(123456789));
            assertEquals(FoldingHashST.Mode.SHIFT, shift(3, 10).mode());
            assertEquals(FoldingHashST.Mode.BOUNDARY, boundary(3, 10).mode());
        }
    }

    @Nested
    @DisplayName("构造与参数校验")
    class ConstructorTest {

        @Test
        @DisplayName("段长与表长的基本约束")
        void validArguments() {
            FoldingHashST<String> st = shift(3, 1000);
            assertEquals(3, st.segmentDigits());
            assertEquals(1000, st.tableSize());
            assertEquals(1, shift(1, 1).tableSize());
            assertThrows(IllegalArgumentException.class, () -> shift(0, 100));
            assertThrows(IllegalArgumentException.class, () -> shift(-1, 100));
            assertThrows(IllegalArgumentException.class, () -> shift(3, 0));
            assertThrows(IllegalArgumentException.class, () -> shift(3, -5));
            assertThrows(NullPointerException.class, () -> new FoldingHashST<String>(3, 100, null));
        }

        @Test
        @DisplayName("负数关键字抛异常")
        void negativeKeyRejected() {
            FoldingHashST<String> st = shift(3, 1000);
            assertThrows(IllegalArgumentException.class, () -> st.hash(-1));
            assertThrows(IllegalArgumentException.class, () -> st.foldedSum(-123));
            assertThrows(IllegalArgumentException.class, () -> st.segments(-1));
            assertThrows(IllegalArgumentException.class, () -> st.put(-5, "x"));
            assertThrows(IllegalArgumentException.class, () -> st.get(-5));
        }
    }

    @Nested
    @DisplayName("符号表操作(拉链法)")
    class SymbolTableTest {

        @Test
        @DisplayName("put/get/delete/contains/size/isEmpty/loadFactor/keys/toString")
        void basicApi() {
            FoldingHashST<String> st = shift(3, 1000);
            assertTrue(st.isEmpty());
            assertEquals(0.0, st.loadFactor(), 1e-12);

            long[] keys = {1001, 2001, 3001};
            for (long key : keys) {
                st.put(key, "v" + key);
            }
            assertEquals(3, st.size());
            assertEquals(0.003, st.loadFactor(), 1e-12);
            assertEquals("v2001", st.get(2001));
            assertTrue(st.contains(3001));
            assertFalse(st.contains(4001));
            assertNull(st.get(4001));

            List<Long> actual = new ArrayList<Long>();
            st.keys().forEach(actual::add);
            assertEquals(Arrays.asList(1001L, 2001L, 3001L), actual); // 地址升序:2 < 3 < 4
            assertEquals("{1001 v1001, 2001 v2001, 3001 v3001}", st.toString());

            assertEquals("v3001", st.delete(3001));
            assertFalse(st.contains(3001));
            assertEquals(2, st.size());
            assertNull(st.delete(3001));
            assertEquals(2, st.size());
        }

        @Test
        @DisplayName("同地址多键挂链,删除链中元素后其余键仍可查")
        void chaining() {
            FoldingHashST<String> st = shift(1, 10);
            assertEquals(6, st.hash(6));    // 6 → 6
            assertEquals(6, st.hash(123));  // 1+2+3 = 6
            assertEquals(6, st.hash(60));   // 6+0 = 6
            st.put(6, "a");
            st.put(123, "b");
            st.put(60, "c");
            assertEquals(3, st.size());
            assertEquals(3, st.maxChainLength());
            assertEquals("b", st.get(123));
            assertEquals("b", st.delete(123));
            assertFalse(st.contains(123));
            assertEquals(2, st.size());
            assertEquals("a", st.get(6));
            assertEquals("c", st.get(60));
            assertEquals(2, st.maxChainLength());
        }

        @Test
        @DisplayName("重复插入只覆盖值;null 值抛异常")
        void overwriteAndNull() {
            FoldingHashST<String> st = shift(3, 1000);
            st.put(123456789, "旧");
            st.put(123456789, "新");
            assertEquals(1, st.size());
            assertEquals("新", st.get(123456789));
            assertThrows(NullPointerException.class, () -> st.put(1, null));
        }
    }
}
