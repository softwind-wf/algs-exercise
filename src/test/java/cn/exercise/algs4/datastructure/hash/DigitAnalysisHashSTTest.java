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
 * 数字分析法散列表单元测试。
 * 重点验证:位分布统计与归一化熵、按均匀度选位与地址空间上限、按选位取数字拼地址、
 * 选位造成的冲突由拉链法正确处理,以及 60 个学号样本的选位结果。
 */
@DisplayName("DigitAnalysisHashST 数字分析法散列表测试")
class DigitAnalysisHashSTTest {

    /** 十位数字样本:个位恒为 0,十位恰好取遍 0~9 */
    private static long[] tensUniform() {
        long[] sample = new long[10];
        for (int i = 0; i < sample.length; i++) {
            sample[i] = i * 10L;
        }
        return sample;
    }

    /** 三位样本:第 0 位恒为 5,第 1、2 位恰好取遍 0~9 */
    private static long[] twoUniformDigits() {
        long[] sample = new long[10];
        for (int i = 0; i < sample.length; i++) {
            sample[i] = i * 100L + i * 10L + 5L;
        }
        return sample;
    }

    /** 0~99 样本:两位都均匀 */
    private static long[] zeroToNinetyNine() {
        long[] sample = new long[100];
        for (int i = 0; i < sample.length; i++) {
            sample[i] = i;
        }
        return sample;
    }

    /** 60 个 8 位学号样本:前两位恒为 "20",年级 21~24,专业分布不均,序号近似均匀 */
    private static long[] studentIds() {
        int[] majors = {1, 1, 2, 2, 2, 3};
        long[] sample = new long[60];
        for (int i = 0; i < sample.length; i++) {
            int grade = 21 + (i % 4);
            int major = majors[i % majors.length];
            int seq = (i * 7) % 100;
            sample[i] = Long.parseLong(String.format("20%02d%02d%02d", grade, major, seq));
        }
        return sample;
    }

    @Nested
    @DisplayName("位的分布统计与均匀度")
    class AnalysisTest {

        @Test
        @DisplayName("恒定不变的位均匀度为 0,各数字等频的位均匀度为 1")
        void uniformityExtremes() {
            DigitAnalysisHashST<String> st = new DigitAnalysisHashST<String>(tensUniform(), 2, 100);
            assertEquals(0.0, st.uniformity(0), 1e-12);  // 个位恒为 0
            assertEquals(1.0, st.uniformity(1), 1e-12);  // 十位 0~9 各一次
            assertEquals(10, st.distribution(0)[0]);
            for (int digit = 0; digit < 10; digit++) {
                assertEquals(1, st.distribution(1)[digit]);
            }
        }

        @Test
        @DisplayName("分布不均的位得分介于 0 和 1 之间(如 20/30/10 的三值分布)")
        void skewedDigitScoresBelowOne() {
            long[] sample = new long[60];
            int[] majors = {1, 1, 2, 2, 2, 3};
            for (int i = 0; i < sample.length; i++) {
                sample[i] = Long.parseLong(String.format("20%02d%02d%02d", 21 + (i % 4), majors[i % 6], (i * 7) % 100));
            }
            DigitAnalysisHashST<String> st = new DigitAnalysisHashST<String>(sample, 8, 1000);
            assertEquals(0, st.distribution(2)[0]);
            assertEquals(20, st.distribution(2)[1]);
            assertEquals(30, st.distribution(2)[2]);
            assertEquals(10, st.distribution(2)[3]);
            double skewed = st.uniformity(2);
            assertTrue(skewed > 0.0 && skewed < 1.0, "三值分布的均匀度应介于 0 与 1 之间,实际 = " + skewed);
        }

        @Test
        @DisplayName("distribution/uniformity 的位号越界抛异常")
        void positionOutOfRange() {
            DigitAnalysisHashST<String> st = new DigitAnalysisHashST<String>(tensUniform(), 2, 100);
            assertThrows(IllegalArgumentException.class, () -> st.distribution(-1));
            assertThrows(IllegalArgumentException.class, () -> st.distribution(2));
            assertThrows(IllegalArgumentException.class, () -> st.uniformity(2));
        }
    }

    @Nested
    @DisplayName("选位与地址空间")
    class SelectionTest {

        @Test
        @DisplayName("优先选均匀度最高的位,并受地址空间上限约束")
        void picksMostUniformPositionsWithinCapacity() {
            DigitAnalysisHashST<String> wide = new DigitAnalysisHashST<String>(tensUniform(), 2, 100);
            assertArrayEquals(new int[]{0, 1}, wide.chosenPositions());
            assertEquals(100, wide.addressSpace());

            DigitAnalysisHashST<String> narrow = new DigitAnalysisHashST<String>(tensUniform(), 2, 10);
            assertArrayEquals(new int[]{1}, narrow.chosenPositions());
            assertEquals(10, narrow.addressSpace());
        }

        @Test
        @DisplayName("学号样本:选中序号两位与年级个位,绕开恒定的 \"20\" 两位")
        void studentIdSelection() {
            DigitAnalysisHashST<String> st = new DigitAnalysisHashST<String>(studentIds(), 8, 1000);
            assertArrayEquals(new int[]{0, 1, 4}, st.chosenPositions());
            assertEquals(1000, st.addressSpace());
            assertEquals(0.0, st.uniformity(6), 1e-12);  // "20" 的 2
            assertEquals(0.0, st.uniformity(7), 1e-12);  // "20" 的 0
            for (int pos : st.chosenPositions()) {
                assertTrue(pos == 0 || pos == 1 || pos == 4, "不应选中恒定或高度不均的位: " + pos);
            }
        }

        @Test
        @DisplayName("选位结果可复现:同样的样本得到同样的选位")
        void deterministicSelection() {
            DigitAnalysisHashST<String> a = new DigitAnalysisHashST<String>(studentIds(), 8, 1000);
            DigitAnalysisHashST<String> b = new DigitAnalysisHashST<String>(studentIds(), 8, 1000);
            assertArrayEquals(a.chosenPositions(), b.chosenPositions());
            for (long key : studentIds()) {
                assertEquals(a.hash(key), b.hash(key));
            }
        }
    }

    @Nested
    @DisplayName("散列函数:按选位拼地址")
    class HashFunctionTest {

        @Test
        @DisplayName("地址由选中位的数字按原高位到低位拼成")
        void addressComposition() {
            DigitAnalysisHashST<String> st = new DigitAnalysisHashST<String>(twoUniformDigits(), 3, 100);
            assertArrayEquals(new int[]{1, 2}, st.chosenPositions());
            assertEquals(37, st.hash(370));   // 第 2 位 = 3,第 1 位 = 7
            assertEquals(0, st.hash(0));
            assertEquals(99, st.hash(990));
        }

        @Test
        @DisplayName("未入选的位不参与地址:两者地址相同(设计如此),由拉链法区分")
        void unselectedDigitsDoNotAffectAddress() {
            DigitAnalysisHashST<String> st = new DigitAnalysisHashST<String>(twoUniformDigits(), 3, 100);
            assertEquals(st.hash(370), st.hash(371));
            assertEquals(st.hash(370), st.hash(379));
            st.put(370, "a");
            st.put(371, "b");
            st.put(379, "c");
            assertEquals(3, st.size());
            assertEquals("a", st.get(370));
            assertEquals("b", st.get(371));
            assertEquals("c", st.get(379));
            assertEquals(3, st.maxChainLength()); // 三个键同桶,靠链区分
            assertTrue(st.contains(371));
        }

        @Test
        @DisplayName("地址始终落在 [0, addressSpace)")
        void addressInRange() {
            DigitAnalysisHashST<String> st = new DigitAnalysisHashST<String>(studentIds(), 8, 1000);
            for (long key = 20210000L; key < 20210100L; key++) {
                int address = st.hash(key);
                assertTrue(address >= 0 && address < st.addressSpace(), "key = " + key + " 地址越界: " + address);
            }
        }

        @Test
        @DisplayName("关键字非法时抛异常:负数、超出位数上限")
        void invalidKeys() {
            DigitAnalysisHashST<String> st = new DigitAnalysisHashST<String>(tensUniform(), 2, 100);
            assertThrows(IllegalArgumentException.class, () -> st.hash(-1));
            assertThrows(IllegalArgumentException.class, () -> st.hash(100));
            assertThrows(IllegalArgumentException.class, () -> st.put(-5, "x"));
            assertThrows(IllegalArgumentException.class, () -> st.get(1000));
        }
    }

    @Nested
    @DisplayName("冲突统计与拉链法")
    class CollisionTest {

        @Test
        @DisplayName("地址空间被压到 10 时,100 个样本产生 90 次冲突、只用到 10 个地址")
        void sampleCollisionStatistics() {
            DigitAnalysisHashST<String> st = new DigitAnalysisHashST<String>(zeroToNinetyNine(), 2, 10);
            assertArrayEquals(new int[]{0}, st.chosenPositions());
            assertEquals(100, st.sampleSize());
            assertEquals(90, st.sampleCollisionCount());
            assertEquals(10, st.distinctAddressCount());
            assertEquals(5, st.hash(5));
            assertEquals(5, st.hash(15));
            assertEquals(5, st.hash(95));
        }

        @Test
        @DisplayName("选位得当(地址空间 1000)时 60 个学号零冲突")
        void wellChosenDigitsHaveNoCollisions() {
            DigitAnalysisHashST<String> st = new DigitAnalysisHashST<String>(studentIds(), 8, 1000);
            assertEquals(60, st.sampleSize());
            assertEquals(0, st.sampleCollisionCount());
            assertEquals(60, st.distinctAddressCount());
        }

        @Test
        @DisplayName("同桶多键:put/get/delete/contains 在链上正确工作")
        void chainingWorks() {
            DigitAnalysisHashST<String> st = new DigitAnalysisHashST<String>(zeroToNinetyNine(), 2, 10);
            st.put(5, "a");
            st.put(15, "b");
            st.put(25, "c");
            assertEquals(3, st.size());
            assertEquals(3, st.maxChainLength());
            assertEquals("b", st.get(15));
            assertTrue(st.contains(25));
            assertEquals("b", st.delete(15));
            assertFalse(st.contains(15));
            assertEquals(2, st.size());
            assertEquals(2, st.maxChainLength());
            assertNull(st.delete(15));
            assertEquals("a", st.get(5));
            assertEquals("c", st.get(25));
        }

        @Test
        @DisplayName("重复插入只覆盖值,不增加元素个数")
        void putOverwrites() {
            DigitAnalysisHashST<String> st = new DigitAnalysisHashST<String>(zeroToNinetyNine(), 2, 10);
            st.put(7, "旧");
            st.put(7, "新");
            assertEquals(1, st.size());
            assertEquals("新", st.get(7));
        }
    }

    @Nested
    @DisplayName("符号表接口与参数校验")
    class SymbolTableTest {

        @Test
        @DisplayName("size/isEmpty/loadFactor/keys/toString")
        void basicApi() {
            DigitAnalysisHashST<String> st = new DigitAnalysisHashST<String>(zeroToNinetyNine(), 2, 100);
            assertTrue(st.isEmpty());
            assertEquals(0.0, st.loadFactor(), 1e-12);
            st.put(5, "a");
            st.put(3, "c");
            st.put(7, "b");
            assertEquals(3, st.size());
            assertFalse(st.isEmpty());
            assertEquals(0.03, st.loadFactor(), 1e-12);
            List<Long> keys = new ArrayList<Long>();
            st.keys().forEach(keys::add);
            assertEquals(Arrays.asList(3L, 5L, 7L), keys);
            assertEquals("{3 c, 5 a, 7 b}", st.toString());
        }

        @Test
        @DisplayName("构造参数非法时抛异常")
        void invalidArguments() {
            assertThrows(NullPointerException.class,
                    () -> new DigitAnalysisHashST<String>((long[]) null, 2, 100));
            assertThrows(IllegalArgumentException.class,
                    () -> new DigitAnalysisHashST<String>(new long[0], 2, 100));
            assertThrows(IllegalArgumentException.class,
                    () -> new DigitAnalysisHashST<String>(new long[]{1}, 1, 2, 10));   // 基数 < 2
            assertThrows(IllegalArgumentException.class,
                    () -> new DigitAnalysisHashST<String>(new long[]{1}, 0, 10));      // 位数 < 1
            assertThrows(IllegalArgumentException.class,
                    () -> new DigitAnalysisHashST<String>(new long[]{1}, 2, 5));       // 上限 < 基数
            assertThrows(IllegalArgumentException.class,
                    () -> new DigitAnalysisHashST<String>(new long[]{-1}, 2, 100));    // 负关键字
            assertThrows(IllegalArgumentException.class,
                    () -> new DigitAnalysisHashST<String>(new long[]{100}, 2, 100));   // 超出位数
            assertThrows(NullPointerException.class,
                    () -> new DigitAnalysisHashST<String>(zeroToNinetyNine(), 2, 100).put(5, null));
        }

        @Test
        @DisplayName("基数可配置:十六进制样本也能分析")
        void otherRadix() {
            long[] sample = new long[16];
            for (int i = 0; i < sample.length; i++) {
                sample[i] = i;                       // 十六进制下低位取遍 0~15
            }
            DigitAnalysisHashST<String> st = new DigitAnalysisHashST<String>(sample, 16, 1, 16);
            assertArrayEquals(new int[]{0}, st.chosenPositions());
            assertEquals(16, st.addressSpace());
            assertEquals(1.0, st.uniformity(0), 1e-12);
            assertEquals(15, st.hash(15));
        }
    }
}
