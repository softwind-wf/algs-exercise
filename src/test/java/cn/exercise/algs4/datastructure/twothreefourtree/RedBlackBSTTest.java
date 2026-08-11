package cn.exercise.algs4.datastructure.twothreefourtree;

import cn.exercise.algs4.datastructure.twothreefourtree.RedBlackBST;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RedBlackBSTTest {

    private RedBlackBST<String, Integer> st;

    @BeforeEach
    void setUp() {
        st = new RedBlackBST<>();
    }

    // ========== 基本操作 ==========

    @Nested
    @DisplayName("空树测试")
    class EmptyTreeTests {
        @Test
        @DisplayName("新建树应为空")
        void shouldBeEmpty() {
            assertTrue(st.isEmpty());
            assertEquals(0, st.size());
        }

        @Test
        @DisplayName("空树get应抛异常")
        void getOnEmptyTreeShouldThrow() {
            assertThrows(IllegalArgumentException.class, () -> st.get(null));
        }

        @Test
        @DisplayName("空树min应抛异常")
        void minOnEmptyTreeShouldThrow() {
            assertThrows(NoSuchElementException.class, () -> st.min());
        }

        @Test
        @DisplayName("空树max应抛异常")
        void maxOnEmptyTreeShouldThrow() {
            assertThrows(NoSuchElementException.class, () -> st.max());
        }

        @Test
        @DisplayName("空树deleteMin应抛异常")
        void deleteMinOnEmptyShouldThrow() {
            assertThrows(NoSuchElementException.class, () -> st.deleteMin());
        }

        @Test
        @DisplayName("空树deleteMax应抛异常")
        void deleteMaxOnEmptyShouldThrow() {
            assertThrows(NoSuchElementException.class, () -> st.deleteMax());
        }

        @Test
        @DisplayName("空树floor应抛异常")
        void floorOnEmptyShouldThrow() {
            assertThrows(NoSuchElementException.class, () -> st.floor("A"));
        }

        @Test
        @DisplayName("空树ceiling应抛异常")
        void ceilingOnEmptyShouldThrow() {
            assertThrows(NoSuchElementException.class, () -> st.ceiling("A"));
        }

        @Test
        @DisplayName("空树keys应返回空集合")
        void keysOnEmptyShouldBeEmpty() {
            List<String> keys = toList(st.keys());
            assertTrue(keys.isEmpty());
        }

        @Test
        @DisplayName("空树height应为-1")
        void heightOnEmptyShouldBeMinusOne() {
            assertEquals(-1, st.height());
        }
    }

    // ========== put & get ==========

    @Nested
    @DisplayName("put和get测试")
    class PutGetTests {
        @Test
        @DisplayName("插入单个元素后可以get到")
        void putAndGetSingle() {
            st.put("A", 1);
            assertEquals(1, st.get("A"));
            assertEquals(1, st.size());
            assertFalse(st.isEmpty());
        }

        @Test
        @DisplayName("插入多个元素")
        void putMultiple() {
            st.put("S", 0);
            st.put("E", 1);
            st.put("A", 2);
            st.put("R", 3);
            st.put("C", 4);
            st.put("H", 5);
            assertEquals(6, st.size());
            assertEquals(0, st.get("S"));
            assertEquals(1, st.get("E"));
            assertEquals(2, st.get("A"));
            assertEquals(3, st.get("R"));
            assertEquals(4, st.get("C"));
            assertEquals(5, st.get("H"));
        }

        @Test
        @DisplayName("重复key应覆盖旧值")
        void putOverwrite() {
            st.put("A", 1);
            st.put("A", 2);
            assertEquals(2, st.get("A"));
            assertEquals(1, st.size());
        }

        @Test
        @DisplayName("get不存在的key返回null")
        void getNonExistent() {
            st.put("A", 1);
            assertNull(st.get("Z"));
        }

        @Test
        @DisplayName("put null key应抛异常")
        void putNullKeyShouldThrow() {
            assertThrows(IllegalArgumentException.class, () -> st.put(null, 1));
        }

        @Test
        @DisplayName("put null value应删除该key")
        void putNullValueShouldDelete() {
            st.put("A", 1);
            st.put("A", null);
            assertFalse(st.contains("A"));
            assertEquals(0, st.size());
        }

        @Test
        @DisplayName("get null key应抛异常")
        void getNullKeyShouldThrow() {
            assertThrows(IllegalArgumentException.class, () -> st.get(null));
        }
    }

    // ========== contains ==========

    @Nested
    @DisplayName("contains测试")
    class ContainsTests {
        @Test
        @DisplayName("contains存在的key返回true")
        void containsExistingKey() {
            st.put("A", 1);
            assertTrue(st.contains("A"));
        }

        @Test
        @DisplayName("contains不存在的key返回false")
        void containsNonExistingKey() {
            st.put("A", 1);
            assertFalse(st.contains("B"));
        }
    }

    // ========== min & max ==========

    @Nested
    @DisplayName("min和max测试")
    class MinMaxTests {
        @Test
        @DisplayName("单元素树min和max相同")
        void singleElement() {
            st.put("A", 1);
            assertEquals("A", st.min());
            assertEquals("A", st.max());
        }

        @Test
        @DisplayName("多元素树min和max正确")
        void multipleElements() {
            st.put("S", 0);
            st.put("E", 1);
            st.put("A", 2);
            st.put("R", 3);
            st.put("X", 4);
            assertEquals("A", st.min());
            assertEquals("X", st.max());
        }
    }

    // ========== floor & ceiling ==========

    @Nested
    @DisplayName("floor和ceiling测试")
    class FloorCeilingTests {
        @BeforeEach
        void setUpTree() {
            // 插入 A C E H R S X
            st.put("A", 0);
            st.put("C", 1);
            st.put("E", 2);
            st.put("H", 3);
            st.put("R", 4);
            st.put("S", 5);
            st.put("X", 6);
        }

        @Test
        @DisplayName("floor精确命中")
        void floorExactMatch() {
            assertEquals("E", st.floor("E"));
        }

        @Test
        @DisplayName("floor向下取整")
        void floorRoundDown() {
            assertEquals("C", st.floor("D"));
        }

        @Test
        @DisplayName("floor最小key以下应抛异常")
        void floorBelowMinShouldThrow() {
            assertThrows(NoSuchElementException.class, () -> st.floor("0"));
        }

        @Test
        @DisplayName("ceiling精确命中")
        void ceilingExactMatch() {
            assertEquals("E", st.ceiling("E"));
        }

        @Test
        @DisplayName("ceiling向上取整")
        void ceilingRoundUp() {
            assertEquals("H", st.ceiling("G"));
        }

        @Test
        @DisplayName("ceiling最大key以上应抛异常")
        void ceilingAboveMaxShouldThrow() {
            assertThrows(NoSuchElementException.class, () -> st.ceiling("Z"));
        }

        @Test
        @DisplayName("floor/ceiling null参数应抛异常")
        void nullArgShouldThrow() {
            assertThrows(IllegalArgumentException.class, () -> st.floor(null));
            assertThrows(IllegalArgumentException.class, () -> st.ceiling(null));
        }
    }

    // ========== select & rank ==========

    @Nested
    @DisplayName("select和rank测试")
    class SelectRankTests {
        @BeforeEach
        void setUpTree() {
            // 排序后: A C E H R S X
            st.put("S", 0);
            st.put("E", 1);
            st.put("A", 2);
            st.put("R", 3);
            st.put("X", 4);
            st.put("C", 5);
            st.put("H", 6);
        }

        @Test
        @DisplayName("select各rank返回正确key")
        void selectCorrect() {
            assertEquals("A", st.select(0));
            assertEquals("C", st.select(1));
            assertEquals("E", st.select(2));
            assertEquals("H", st.select(3));
            assertEquals("R", st.select(4));
            assertEquals("S", st.select(5));
            assertEquals("X", st.select(6));
        }

        @Test
        @DisplayName("rank各key返回正确排名")
        void rankCorrect() {
            assertEquals(0, st.rank("A"));
            assertEquals(1, st.rank("C"));
            assertEquals(2, st.rank("E"));
            assertEquals(3, st.rank("H"));
            assertEquals(4, st.rank("R"));
            assertEquals(5, st.rank("S"));
            assertEquals(6, st.rank("X"));
        }

        @Test
        @DisplayName("select越界应抛异常")
        void selectOutOfRange() {
            assertThrows(IllegalArgumentException.class, () -> st.select(-1));
            assertThrows(IllegalArgumentException.class, () -> st.select(7));
        }

        @Test
        @DisplayName("rank null应抛异常")
        void rankNullShouldThrow() {
            assertThrows(IllegalArgumentException.class, () -> st.rank(null));
        }
    }

    // ========== keys & range ==========

    @Nested
    @DisplayName("keys和范围查询测试")
    class KeysRangeTests {
        @BeforeEach
        void setUpTree() {
            st.put("A", 0);
            st.put("C", 1);
            st.put("E", 2);
            st.put("H", 3);
            st.put("R", 4);
            st.put("S", 5);
            st.put("X", 6);
        }

        @Test
        @DisplayName("keys返回所有有序key")
        void allKeys() {
            List<String> keys = toList(st.keys());
            assertEquals(Arrays.asList("A", "C", "E", "H", "R", "S", "X"), keys);
        }

        @Test
        @DisplayName("keys范围查询")
        void rangeKeys() {
            List<String> keys = toList(st.keys("C", "R"));
            assertEquals(Arrays.asList("C", "E", "H", "R"), keys);
        }

        @Test
        @DisplayName("size范围查询")
        void rangeSize() {
            assertEquals(4, st.size("C", "R"));
        }

        @Test
        @DisplayName("size范围 lo>hi 返回0")
        void rangeSizeInverted() {
            assertEquals(0, st.size("R", "C"));
        }

        @Test
        @DisplayName("keys null参数应抛异常")
        void keysNullShouldThrow() {
            assertThrows(IllegalArgumentException.class, () -> st.keys(null, "Z"));
            assertThrows(IllegalArgumentException.class, () -> st.keys("A", null));
        }
    }

    // ========== delete ==========

    @Nested
    @DisplayName("delete测试")
    class DeleteTests {
        @Test
        @DisplayName("删除存在的key")
        void deleteExistingKey() {
            st.put("A", 1);
            st.put("B", 2);
            st.put("C", 3);
            st.delete("B");
            assertFalse(st.contains("B"));
            assertEquals(2, st.size());
        }

        @Test
        @DisplayName("删除不存在的key不报错")
        void deleteNonExistingKey() {
            st.put("A", 1);
            st.delete("Z");
            assertEquals(1, st.size());
        }

        @Test
        @DisplayName("delete null应抛异常")
        void deleteNullShouldThrow() {
            assertThrows(IllegalArgumentException.class, () -> st.delete(null));
        }

        @Test
        @DisplayName("删除所有元素后树为空")
        void deleteAll() {
            st.put("A", 1);
            st.put("B", 2);
            st.put("C", 3);
            st.delete("A");
            st.delete("B");
            st.delete("C");
            assertTrue(st.isEmpty());
            assertEquals(0, st.size());
        }

        @Test
        @DisplayName("大量删除后结构仍正确")
        void bulkDeleteIntegrity() {
            String[] keys = {"S", "E", "A", "R", "C", "H", "X", "M", "P", "L"};
            for (int i = 0; i < keys.length; i++) {
                st.put(keys[i], i);
            }
            assertEquals(10, st.size());

            // 删除一半
            for (int i = 0; i < 5; i++) {
                st.delete(keys[i]);
            }
            assertEquals(5, st.size());

            // 验证剩余元素
            for (int i = 5; i < keys.length; i++) {
                assertTrue(st.contains(keys[i]));
                assertEquals(i, st.get(keys[i]));
            }
        }
    }

    // ========== deleteMin & deleteMax ==========

    @Nested
    @DisplayName("deleteMin和deleteMax测试")
    class DeleteMinMaxTests {
        @Test
        @DisplayName("deleteMin删除最小元素")
        void deleteMin() {
            st.put("C", 1);
            st.put("A", 2);
            st.put("B", 3);
            st.deleteMin();
            assertFalse(st.contains("A"));
            assertEquals("B", st.min());
            assertEquals(2, st.size());
        }

        @Test
        @DisplayName("deleteMax删除最大元素")
        void deleteMax() {
            st.put("C", 1);
            st.put("A", 2);
            st.put("B", 3);
            st.deleteMax();
            assertFalse(st.contains("C"));
            assertEquals("B", st.max());
            assertEquals(2, st.size());
        }

        @Test
        @DisplayName("反复deleteMin直到为空")
        void deleteMinUntilEmpty() {
            st.put("C", 1);
            st.put("A", 2);
            st.put("B", 3);
            st.deleteMin(); // remove A
            st.deleteMin(); // remove B
            st.deleteMin(); // remove C
            assertTrue(st.isEmpty());
        }

        @Test
        @DisplayName("反复deleteMax直到为空")
        void deleteMaxUntilEmpty() {
            st.put("C", 1);
            st.put("A", 2);
            st.put("B", 3);
            st.deleteMax(); // remove C
            st.deleteMax(); // remove B
            st.deleteMax(); // remove A
            assertTrue(st.isEmpty());
        }
    }

    // ========== height ==========

    @Nested
    @DisplayName("height测试")
    class HeightTests {
        @Test
        @DisplayName("单元素树height为0")
        void singleElement() {
            st.put("A", 1);
            assertEquals(0, st.height());
        }

        @Test
        @DisplayName("红黑树高度应为O(log n)")
        void heightIsLogarithmic() {
            for (int i = 0; i < 1000; i++) {
                st.put(String.format("%04d", i), i);
            }
            // 红黑树高度不超过 2*log2(n+1)
            int maxHeight = (int) (2 * Math.log(1001) / Math.log(2));
            assertTrue(st.height() <= maxHeight,
                    "height=" + st.height() + " should be <= " + maxHeight);
        }
    }

    // ========== 红黑树完整性验证 ==========

    @Nested
    @DisplayName("红黑树完整性验证")
    class IntegrityTests {
        @Test
        @DisplayName("大量插入后select/rank一致性")
        void selectRankConsistency() {
            int n = 200;
            for (int i = 0; i < n; i++) {
                st.put(String.format("%04d", i), i);
            }
            for (int i = 0; i < n; i++) {
                String key = String.format("%04d", i);
                assertEquals(i, st.rank(key));
                assertEquals(key, st.select(i));
            }
        }

        @Test
        @DisplayName("插入和删除混合操作后一致性")
        void mixedOperationsConsistency() {
            int n = 100;
            for (int i = 0; i < n; i++) {
                st.put(String.format("%04d", i), i);
            }
            // 删除偶数
            for (int i = 0; i < n; i += 2) {
                st.delete(String.format("%04d", i));
            }
            // 验证只剩奇数
            assertEquals(50, st.size());
            for (int i = 1; i < n; i += 2) {
                String key = String.format("%04d", i);
                assertTrue(st.contains(key));
                assertEquals(i, st.get(key));
            }
            // 验证rank/select一致性
            for (int i = 0; i < st.size(); i++) {
                String key = st.select(i);
                assertEquals(i, st.rank(key));
            }
        }

        @Test
        @DisplayName("Integer类型也能正常工作")
        void integerKeys() {
            RedBlackBST<Integer, String> intSt = new RedBlackBST<>();
            intSt.put(5, "five");
            intSt.put(3, "three");
            intSt.put(7, "seven");
            intSt.put(1, "one");
            assertEquals(4, intSt.size());
            assertEquals(1, intSt.min());
            assertEquals(7, intSt.max());
            assertEquals("three", intSt.get(3));

            intSt.delete(3);
            assertEquals(3, intSt.size());
            assertFalse(intSt.contains(3));
        }
    }

    // ========== 辅助方法 ==========

    private <T> List<T> toList(Iterable<T> iterable) {
        List<T> list = new ArrayList<>();
        for (T item : iterable) {
            list.add(item);
        }
        return list;
    }
}
