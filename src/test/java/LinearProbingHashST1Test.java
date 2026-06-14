

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import test3.LinearProbingHashST1;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LinearProbingHashST 的 JUnit5 测试类
 */
public class LinearProbingHashST1Test {

    // 测试空表的初始状态
    @Test
    @DisplayName("测试空表的初始状态")
    void testEmptyTable() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        assertTrue(st.isEmpty(), "新创建的表应该为空");
        assertEquals(0, st.size(), "新创建的表大小应为0");
    }

    // 测试插入单个键值对
    @Test
    @DisplayName("测试插入单个元素")
    void testInsertSingleElement() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        st.put("key", 1);
        assertFalse(st.isEmpty());
        assertEquals(1, st.size());
        assertEquals(1, st.get("key"));
        assertTrue(st.contains("key"));
    }

    // 测试插入多个不同的键
    @Test
    @DisplayName("测试插入多个不同键")
    void testInsertMultipleKeys() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        st.put("E", 0);
        st.put("A", 1);
        st.put("R", 2);
        st.put("C", 3);
        
        assertEquals(4, st.size());
        assertEquals(0, st.get("E"));
        assertEquals(1, st.get("A"));
        assertEquals(2, st.get("R"));
        assertEquals(3, st.get("C"));
    }

    // 测试更新已存在的键的值
    @Test
    @DisplayName("测试更新已存在的键")
    void testUpdateExistingKey() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        st.put("key", 1);
        assertEquals(1, st.get("key"));
        assertEquals(1, st.size());
        
        st.put("key", 2);
        assertEquals(2, st.get("key"), "更新后的值应该是2");
        assertEquals(1, st.size(), "更新后大小不变");
    }

    // 测试重复插入相同的键
    @Test
    @DisplayName("测试重复插入相同键")
    void testDuplicateKeys() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        st.put("E", 1);
        st.put("E", 6);
        st.put("E", 12);
        
        assertEquals(1, st.size(), "重复键只应计一次");
        assertEquals(12, st.get("E"), "应保留最后一次插入的值");
    }

    // 测试删除存在的键
    @Test
    @DisplayName("测试删除存在的键")
    void testDeleteExistingKey() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        st.put("A", 1);
        st.put("B", 2);
        st.put("C", 3);
        
        assertEquals(3, st.size());
        st.delete("B");
        assertEquals(2, st.size());
        assertFalse(st.contains("B"));
        assertNull(st.get("B"));
        assertEquals(1, st.get("A"));
        assertEquals(3, st.get("C"));
    }

    // 测试删除不存在的键
    @Test
    @DisplayName("测试删除不存在的键")
    void testDeleteNonExistentKey() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        st.put("A", 1);
        st.delete("Z");
        assertEquals(1, st.size(), "删除不存在的键不影响大小");
        assertTrue(st.contains("A"));
    }

    // 测试通过put(null值)删除键
    @Test
    @DisplayName("测试通过put传入null值删除键")
    void testDeleteByPutNull() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        st.put("A", 1);
        st.put("B", 2);
        
        st.put("A", null);
        assertFalse(st.contains("A"));
        assertEquals(1, st.size());
    }

    // 测试contains方法
    @Test
    @DisplayName("测试contains方法")
    void testContains() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        st.put("A", 1);
        st.put("B", 2);
        
        assertTrue(st.contains("A"));
        assertTrue(st.contains("B"));
        assertFalse(st.contains("C"));
    }

    // 测试遍历所有键
    @Test
    @DisplayName("测试遍历所有键")
    void testKeysIteration() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        st.put("A", 1);
        st.put("B", 2);
        st.put("C", 3);
        
        int count = 0;
        for (String key : st.keys()) {
            assertNotNull(key);
            count++;
        }
        assertEquals(3, count, "遍历的键数量应与size一致");
    }

    // 测试keys()返回的集合包含所有插入的键
    @Test
    @DisplayName("测试keys包含所有键")
    void testKeysContainAllInsertedKeys() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        st.put("X", 1);
        st.put("Y", 2);
        st.put("Z", 3);
        
        boolean foundX = false, foundY = false, foundZ = false;
        for (String key : st.keys()) {
            if (key.equals("X")) foundX = true;
            if (key.equals("Y")) foundY = true;
            if (key.equals("Z")) foundZ = true;
        }
        
        assertTrue(foundX && foundY && foundZ, "所有插入的键都应该在keys中");
    }

    // 测试键为null时抛出异常
    @Test
    @DisplayName("测试键为null时抛出异常")
    void testNullKeyThrowsException() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        
        assertThrows(IllegalArgumentException.class, () -> {
            st.put(null, 1);
        }, "put(null, value)应抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> {
            st.get(null);
        }, "get(null)应抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> {
            st.contains(null);
        }, "contains(null)应抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> {
            st.delete(null);
        }, "delete(null)应抛出异常");
    }

    // 测试整数键
    @Test
    @DisplayName("测试整数键")
    void testIntegerKeys() {
        LinearProbingHashST1<Integer, String> st = new LinearProbingHashST1<>();
        st.put(1, "one");
        st.put(2, "two");
        st.put(3, "three");
        
        assertEquals("one", st.get(1));
        assertEquals("two", st.get(2));
        assertEquals("three", st.get(3));
        assertEquals(3, st.size());
    }

    // 测试大量数据插入和查询
    @Test
    @DisplayName("测试大量数据")
    void testLargeDataSet() {
        LinearProbingHashST1<Integer, Integer> st = new LinearProbingHashST1<>();
        
        int n = 1000;
        for (int i = 0; i < n; i++) {
            st.put(i, i * 10);
        }
        
        assertEquals(n, st.size());
        
        for (int i = 0; i < n; i++) {
            assertEquals(i * 10, st.get(i), "键" + i + "对应的值应该正确");
        }
    }

    // 测试交替插入和删除
    @Test
    @DisplayName("测试交替插入和删除")
    void testAlternatingInsertAndDelete() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        
        st.put("A", 1);
        st.put("B", 2);
        st.put("C", 3);
        st.delete("B");
        st.put("D", 4);
        st.delete("A");
        
        assertEquals(2, st.size());
        assertFalse(st.contains("A"));
        assertFalse(st.contains("B"));
        assertTrue(st.contains("C"));
        assertTrue(st.contains("D"));
    }

    // 测试扩容机制（插入足够多的元素触发扩容）
    @Test
    @DisplayName("测试自动扩容")
    void testAutoResize() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>(8);
        
        for (int i = 0; i < 100; i++) {
            st.put("key" + i, i);
        }
        
        assertEquals(100, st.size());
        
        for (int i = 0; i < 100; i++) {
            assertEquals(i, st.get("key" + i));
        }
    }

    // 测试缩容机制
    @Test
    @DisplayName("测试自动缩容")
    void testAutoShrink() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        
        for (int i = 0; i < 100; i++) {
            st.put("key" + i, i);
        }
        
        for (int i = 0; i < 90; i++) {
            st.delete("key" + i);
        }
        
        assertEquals(10, st.size());
        
        for (int i = 90; i < 100; i++) {
            assertEquals(i, st.get("key" + i));
        }
    }

    // 测试空表遍历
    @Test
    @DisplayName("测试空表遍历")
    void testIterateEmptyTable() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        
        int count = 0;
        for (String key : st.keys()) {
            count++;
        }
        assertEquals(0, count, "空表遍历时不应有元素");
    }

    // 测试字符串键值对（实际应用场景）
    @Test
    @DisplayName("测试字符串键值对")
    void testStringKeyValuePairs() {
        LinearProbingHashST1<String, String> st = new LinearProbingHashST1<>();
        st.put("name", "Alice");
        st.put("email", "alice@example.com");
        st.put("phone", "123-456-7890");
        
        assertEquals("Alice", st.get("name"));
        assertEquals("alice@example.com", st.get("email"));
        assertEquals("123-456-7890", st.get("phone"));
        assertEquals(3, st.size());
    }

    // 测试书中示例完整流程
    @Test
    @DisplayName("测试书中示例完整流程")
    void testBookExample() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        
        String[] keys = {"E", "A", "R", "C", "H", "E", "X", "A", "M", "P", "L", "E"};
        for (int i = 0; i < keys.length; i++) {
            st.put(keys[i], i);
        }
        
        assertEquals(9, st.size(), "去重后应有9个键");
        assertEquals(11, st.get("E"), "E的最终值应为11");
        assertEquals(6, st.get("X"));
        assertTrue(st.contains("M"));
        
        st.delete("E");
        assertFalse(st.contains("E"));
        assertEquals(8, st.size());
    }

    // 测试从空表获取值
    @Test
    @DisplayName("测试从空表获取值")
    void testGetFromEmptyTable() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        assertNull(st.get("anyKey"), "从空表获取值应返回null");
    }

    // 测试连续删除操作
    @Test
    @DisplayName("测试连续删除操作")
    void testConsecutiveDeletes() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        
        for (int i = 0; i < 10; i++) {
            st.put("key" + i, i);
        }
        
        assertEquals(10, st.size());
        
        for (int i = 0; i < 5; i++) {
            st.delete("key" + i);
        }
        
        assertEquals(5, st.size());
        
        for (int i = 0; i < 5; i++) {
            assertFalse(st.contains("key" + i));
        }
        
        for (int i = 5; i < 10; i++) {
            assertTrue(st.contains("key" + i));
            assertEquals(i, st.get("key" + i));
        }
    }

    // 测试删除后重新插入相同的键
    @Test
    @DisplayName("测试删除后重新插入")
    void testReinsertAfterDelete() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>();
        
        st.put("A", 1);
        st.put("B", 2);
        st.delete("A");
        st.put("A", 10);
        
        assertEquals(2, st.size());
        assertEquals(10, st.get("A"));
        assertEquals(2, st.get("B"));
    }

    // 测试线性探测的正确性（处理冲突）
    @Test
    @DisplayName("测试线性探测处理冲突")
    void testLinearProbingCollisionHandling() {
        LinearProbingHashST1<String, Integer> st = new LinearProbingHashST1<>(4);
        
        st.put("A", 1);
        st.put("B", 2);
        st.put("C", 3);
        st.put("D", 4);
        
        assertEquals(4, st.size());
        assertEquals(1, st.get("A"));
        assertEquals(2, st.get("B"));
        assertEquals(3, st.get("C"));
        assertEquals(4, st.get("D"));
    }

    // 测试自定义对象作为键
    @Test
    @DisplayName("测试自定义对象作为键")
    void testCustomObjectAsKey() {
        LinearProbingHashST1<Person, String> st = new LinearProbingHashST1<>();
        
        Person p1 = new Person("Alice", 25);
        Person p2 = new Person("Bob", 30);
        Person p3 = new Person("Alice", 25);
        
        st.put(p1, "Engineer");
        st.put(p2, "Doctor");
        
        assertEquals(2, st.size());
        assertEquals("Engineer", st.get(p1));
        assertEquals("Doctor", st.get(p2));
        
        st.put(p3, "Teacher");
        assertEquals(2, st.size(), "p1和p3是相同的键，应更新值");
        assertEquals("Teacher", st.get(p3));
    }

    // 辅助类：用于测试自定义对象作为键
    private static class Person {
        private String name;
        private int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            return age == person.age && name.equals(person.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode() * 31 + age;
        }
    }
}
