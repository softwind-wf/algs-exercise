

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import test5.Quick3string;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Quick3string 三向字符串快速排序的单元测试
 */
@DisplayName("Quick3string 三向字符串快速排序测试")
class Quick3stringTest {

    // ==================== 基础功能测试 ====================

    @Test
    @DisplayName("测试空数组排序")
    void testEmptyArray() {
        String[] arr = {};
        assertDoesNotThrow(() -> Quick3string.sort(arr));
        assertEquals(0, arr.length);
    }

    @Test
    @DisplayName("测试单元素数组排序")
    void testSingleElement() {
        String[] arr = {"hello"};
        Quick3string.sort(arr);
        assertEquals(1, arr.length);
        assertEquals("hello", arr[0]);
    }

    @Test
    @DisplayName("测试已排序数组")
    void testAlreadySorted() {
        String[] arr = {"apple", "banana", "cherry", "date"};
        Quick3string.sort(arr);
        assertEquals("apple", arr[0]);
        assertEquals("banana", arr[1]);
        assertEquals("cherry", arr[2]);
        assertEquals("date", arr[3]);
    }

    @Test
    @DisplayName("测试逆序数组")
    void testReverseSorted() {
        String[] arr = {"zebra", "yellow", "xray", "wolf"};
        Quick3string.sort(arr);
        assertEquals("wolf", arr[0]);
        assertEquals("xray", arr[1]);
        assertEquals("yellow", arr[2]);
        assertEquals("zebra", arr[3]);
    }

    @Test
    @DisplayName("测试重复元素")
    void testDuplicateElements() {
        String[] arr = {"banana", "apple", "banana", "apple", "banana"};
        Quick3string.sort(arr);
        assertEquals("apple", arr[0]);
        assertEquals("apple", arr[1]);
        assertEquals("banana", arr[2]);
        assertEquals("banana", arr[3]);
        assertEquals("banana", arr[4]);
    }

    @Test
    @DisplayName("测试所有元素相同")
    void testAllSameElements() {
        String[] arr = {"test", "test", "test", "test"};
        Quick3string.sort(arr);
        for (String s : arr) {
            assertEquals("test", s);
        }
    }

    // ==================== 边界情况测试 ====================

    @Test
    @DisplayName("测试包含空字符串")
    void testWithEmptyStrings() {
        String[] arr = {"banana", "", "apple", "", "cherry"};
        Quick3string.sort(arr);
        assertEquals("", arr[0]);
        assertEquals("", arr[1]);
        assertEquals("apple", arr[2]);
        assertEquals("banana", arr[3]);
        assertEquals("cherry", arr[4]);
    }

    @Test
    @DisplayName("测试全部为空字符串")
    void testAllEmptyStrings() {
        String[] arr = {"", "", "", ""};
        Quick3string.sort(arr);
        for (String s : arr) {
            assertEquals("", s);
        }
    }

    @Test
    @DisplayName("测试不同长度的字符串")
    void testDifferentLengthStrings() {
        String[] arr = {"a", "abc", "ab", "abcd", "abcde"};
        Quick3string.sort(arr);
        assertEquals("a", arr[0]);
        assertEquals("ab", arr[1]);
        assertEquals("abc", arr[2]);
        assertEquals("abcd", arr[3]);
        assertEquals("abcde", arr[4]);
    }

    @Test
    @DisplayName("测试包含前缀相同的字符串")
    void testWithCommonPrefix() {
        String[] arr = {"test", "testing", "tester", "testers"};
        Quick3string.sort(arr);
        assertEquals("test", arr[0]);
        assertEquals("tester", arr[1]);
        assertEquals("testers", arr[2]);
        assertEquals("testing", arr[3]);
    }

    @Test
    @DisplayName("测试大小写敏感排序")
    void testCaseSensitive() {
        String[] arr = {"Apple", "apple", "Banana", "banana"};
        Quick3string.sort(arr);
        // ASCII 中大写字母小于小写字母
        assertTrue(arr[0].equals("Apple") || arr[0].equals("Banana"));
    }

    // ==================== 异常处理测试 ====================

    @Test
    @DisplayName("测试null数组抛出异常")
    void testNullArray() {
        assertThrows(NullPointerException.class, () -> {
            Quick3string.sort(null);
        });
    }

    @Test
    @DisplayName("测试数组中包含null元素抛出异常")
    void testArrayWithNullElement() {
        String[] arr = {"hello", null, "world"};
        assertThrows(NullPointerException.class, () -> {
            Quick3string.sort(arr);
        });
    }

    @Test
    @DisplayName("测试null数组异常信息")
    void testNullArrayExceptionMessage() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            Quick3string.sort(null);
        });
        assertTrue(exception.getMessage().contains("Input array cannot be null"));
    }

    @Test
    @DisplayName("测试null元素异常信息包含索引")
    void testNullElementExceptionMessage() {
        String[] arr = {"hello", null, "world"};
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            Quick3string.sort(arr);
        });
        assertTrue(exception.getMessage().contains("Array element at index"));
    }

    // ==================== 复杂场景测试 ====================

    @Test
    @DisplayName("测试大规模数据排序")
    void testLargeDataset() {
        int size = 10000;
        String[] arr = new String[size];
        for (int i = 0; i < size; i++) {
            arr[i] = "string" + (size - i); // 逆序生成
        }
        
        Quick3string.sort(arr);
        
        // 验证排序正确性
        for (int i = 0; i < size - 1; i++) {
            assertTrue(arr[i].compareTo(arr[i + 1]) <= 0, 
                "元素应该按字典序排列");
        }
    }

    @Test
    @DisplayName("测试随机字符串数组")
    void testRandomStrings() {
        int size = 1000;
        String[] arr = new String[size];
        for (int i = 0; i < size; i++) {
            arr[i] = generateRandomString((int)(Math.random() * 20) + 1);
        }
        
        Quick3string.sort(arr);
        
        // 验证排序正确性
        for (int i = 0; i < size - 1; i++) {
            assertTrue(arr[i].compareTo(arr[i + 1]) <= 0, 
                "元素应该按字典序排列");
        }
    }

    @Test
    @DisplayName("测试两个元素的数组")
    void testTwoElements() {
        String[] arr = {"banana", "apple"};
        Quick3string.sort(arr);
        assertEquals("apple", arr[0]);
        assertEquals("banana", arr[1]);
    }

    @Test
    @DisplayName("测试原书示例数据")
    void testBookExample() {
        String[] arr = {"she", "sells", "seashells", "by", "the", "sea", "shore",
                        "the", "shells", "she", "sells", "are", "surely", "seashells"};
        Quick3string.sort(arr);
        
        // 验证排序后的结果
        String[] expected = {"are", "by", "sea", "seashells", "seashells", 
                            "sells", "sells", "she", "she", "shells", 
                            "shore", "surely", "the", "the"};
        assertArrayEquals(expected, arr);
    }

    // ==================== 辅助方法 ====================

    /**
     * 生成随机字符串（仅包含小写字母）
     */
    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + Math.random() * 26));
        }
        return sb.toString();
    }

    // ==================== 可视化测试（可选）====================

    @Test
    @DisplayName("可视化排序过程")
    void testVisualizeSorting() {
        System.out.println("=== Quick3string 可视化测试 ===");
        String[] arr = {"she", "sells", "seashells", "by", "the", "sea", "shore"};
        
        System.out.print("排序前: ");
        for (String s : arr) {
            System.out.print(s + " ");
        }
        System.out.println();
        
        Quick3string.sort(arr);
        
        System.out.print("排序后: ");
        for (String s : arr) {
            System.out.print(s + " ");
        }
        System.out.println("\n✓ 排序完成");
    }
}
