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
 * 直接定址法散列表单元测试。
 * 重点验证:H(key) = a * key + b 的地址换算、地址空间大小、a != 0 与越界等约束,
 * 以及"同一范围内不同关键字必然不冲突"这一直接定址法的核心性质。
 */
@DisplayName("DirectAddressHashST 直接定址法散列表测试")
class DirectAddressHashSTTest {

    @Nested
    @DisplayName("散列函数 H(key) = a * key + b")
    class HashFunctionTest {

        @Test
        @DisplayName("默认构造 H(key) = key - minKey,地址从 0 开始")
        void defaultHashShiftsToZero() {
            DirectAddressHashST<String> st = new DirectAddressHashST<>(20210101, 20210110);
            assertEquals(0, st.hash(20210101));
            assertEquals(1, st.hash(20210102));
            assertEquals(9, st.hash(20210110));
            assertEquals(1L, st.coefficientA());
            assertEquals(-20210101L, st.constantB());
        }

        @Test
        @DisplayName("自定义系数 H(key) = 3 * key + 7,地址区间随之平移")
        void customCoefficients() {
            DirectAddressHashST<String> st = new DirectAddressHashST<>(1, 5, 3L, 7L);
            assertEquals(13, st.capacity()); // 3*1+7=10 到 3*5+7=22,共 13 个单元
            assertEquals(0, st.hash(1));
            assertEquals(6, st.hash(3));
            assertEquals(12, st.hash(5));
        }

        @Test
        @DisplayName("a 为负数时地址区间自动归一化,反向映射仍然正确")
        void negativeCoefficient() {
            DirectAddressHashST<String> st = new DirectAddressHashST<>(1, 5, -1L, 10L);
            assertEquals(5, st.capacity()); // H 值域为 [5, 9]
            assertEquals(0, st.hash(5));
            assertEquals(4, st.hash(1));

            st.put(1, "一");
            st.put(5, "五");
            List<Integer> keys = new ArrayList<>();
            st.keys().forEach(keys::add);
            assertEquals(Arrays.asList(5, 1), keys); // 按地址升序,即关键字降序
            assertEquals("五", st.get(5));
            assertEquals("一", st.get(1));
        }

        @Test
        @DisplayName("范围内的关键字地址互不相同(不冲突)")
        void addressesAreDistinct() {
            DirectAddressHashST<String> st = new DirectAddressHashST<>(1, 100, 7L, 3L);
            TreeSet<Integer> addresses = new TreeSet<>();
            for (int key = 1; key <= 100; key++) {
                assertTrue(addresses.add(st.hash(key)), "关键字 " + key + " 产生了地址冲突");
            }
            assertEquals(100, addresses.size());
            assertEquals(0, addresses.first());   // H(1) = 7*1+3-10 = 0
            assertEquals(693, addresses.last());  // H(100) = 7*100+3-10 = 693
        }
    }

    @Nested
    @DisplayName("构造参数校验")
    class ConstructorTest {

        @Test
        @DisplayName("a = 0 时抛异常")
        void zeroCoefficientThrows() {
            assertThrows(IllegalArgumentException.class, () -> new DirectAddressHashST<>(1, 10, 0L, 5L));
        }

        @Test
        @DisplayName("下界大于上界时抛异常")
        void invertedRangeThrows() {
            assertThrows(IllegalArgumentException.class, () -> new DirectAddressHashST<>(10, 1));
        }

        @Test
        @DisplayName("地址空间超过 int 上限时抛异常(不会真正分配数组)")
        void oversizedAddressSpaceThrows() {
            assertThrows(IllegalArgumentException.class, () -> new DirectAddressHashST<>(0, Integer.MAX_VALUE));
        }

        @Test
        @DisplayName("地址空间超过实用上限时抛异常(不会尝试分配而 OutOfMemoryError)")
        void tooLargeAddressSpaceThrows() {
            assertThrows(IllegalArgumentException.class, () -> new DirectAddressHashST<>(0, 100_000_000));
            assertThrows(IllegalArgumentException.class, () -> new DirectAddressHashST<>(0, Integer.MAX_VALUE));
        }

        @Test
        @DisplayName("单个关键字也能构造,容量为 1")
        void singleKey() {
            DirectAddressHashST<String> st = new DirectAddressHashST<>(42, 42);
            assertEquals(1, st.capacity());
            assertEquals(0, st.hash(42));
            st.put(42, "答案");
            assertEquals("答案", st.get(42));
        }
    }

    @Nested
    @DisplayName("符号表基本操作")
    class SymbolTableTest {

        @Test
        @DisplayName("put/get/contains/size/isEmpty 行为正确")
        void putAndGet() {
            DirectAddressHashST<String> st = new DirectAddressHashST<>(18, 65);
            assertTrue(st.isEmpty());
            assertEquals(0, st.size());

            st.put(19, "张三");
            st.put(23, "李四");
            st.put(65, "王五");

            assertEquals("张三", st.get(19));
            assertEquals("李四", st.get(23));
            assertEquals("王五", st.get(65));
            assertTrue(st.contains(23));
            assertFalse(st.contains(20));
            assertNull(st.get(20));
            assertEquals(3, st.size());
            assertFalse(st.isEmpty());
        }

        @Test
        @DisplayName("重复插入只覆盖值,不增加元素个数")
        void putOverwrites() {
            DirectAddressHashST<String> st = new DirectAddressHashST<>(1, 10);
            st.put(3, "旧值");
            st.put(3, "新值");
            assertEquals(1, st.size());
            assertEquals("新值", st.get(3));
        }

        @Test
        @DisplayName("delete 返回旧值并减少元素个数,删除不存在的键无副作用")
        void deleteRemovesEntry() {
            DirectAddressHashST<String> st = new DirectAddressHashST<>(1, 10);
            st.put(3, "值");
            assertEquals("值", st.delete(3));
            assertFalse(st.contains(3));
            assertTrue(st.isEmpty());
            assertNull(st.delete(3));
            assertEquals(0, st.size());
        }

        @Test
        @DisplayName("值为 null 时抛异常(null 用于表示空地址)")
        void nullValueRejected() {
            DirectAddressHashST<String> st = new DirectAddressHashST<>(1, 10);
            assertThrows(NullPointerException.class, () -> st.put(3, null));
        }

        @Test
        @DisplayName("越界关键字在 hash/put/get/contains 中一律抛异常")
        void outOfRangeKeyThrows() {
            DirectAddressHashST<String> st = new DirectAddressHashST<>(18, 65);
            assertThrows(IllegalArgumentException.class, () -> st.hash(17));
            assertThrows(IllegalArgumentException.class, () -> st.hash(66));
            assertThrows(IllegalArgumentException.class, () -> st.put(66, "x"));
            assertThrows(IllegalArgumentException.class, () -> st.get(17));
            assertThrows(IllegalArgumentException.class, () -> st.contains(100));
        }

        @Test
        @DisplayName("装填因子按 n / capacity 计算")
        void loadFactor() {
            DirectAddressHashST<String> st = new DirectAddressHashST<>(0, 3);
            assertEquals(0.0, st.loadFactor());
            st.put(0, "a");
            st.put(1, "b");
            assertEquals(0.5, st.loadFactor());
            st.put(2, "c");
            st.put(3, "d");
            assertEquals(1.0, st.loadFactor());
        }
    }

    @Nested
    @DisplayName("典型场景")
    class ScenarioTest {

        @Test
        @DisplayName("连续关键字把地址空间用满,零冲突")
        void denseKeysFillTable() {
            DirectAddressHashST<String> st = new DirectAddressHashST<>(20210101, 20210110);
            for (int i = 0; i < 10; i++) {
                st.put(20210101 + i, "学生" + i);
            }
            assertEquals(10, st.size());
            assertEquals(10, st.capacity());
            assertEquals(1.0, st.loadFactor());
            assertEquals("学生9", st.get(20210110));
        }

        @Test
        @DisplayName("稀疏关键字会浪费地址空间——直接定址法的固有缺点")
        void sparseKeysWasteSpace() {
            DirectAddressHashST<String> st = new DirectAddressHashST<>(1, 10000);
            st.put(1, "甲");
            st.put(1000, "乙");
            st.put(10000, "丙");
            assertEquals(10000, st.capacity());
            assertEquals(3, st.size());
            assertEquals(0.0003, st.loadFactor(), 1e-9);
        }

        @Test
        @DisplayName("keys() 按地址升序返回全部关键字,toString 格式与 algs4 一致")
        void keysAndToString() {
            DirectAddressHashST<String> st = new DirectAddressHashST<>(5, 9);
            st.put(7, "b");
            st.put(5, "a");
            st.put(9, "c");
            List<Integer> keys = new ArrayList<>();
            st.keys().forEach(keys::add);
            assertEquals(Arrays.asList(5, 7, 9), keys);
            assertEquals("{5 a, 7 b, 9 c}", st.toString());
        }
    }
}
