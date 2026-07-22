import com.ds.sort.InsertionSort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InsertionSortTest {

    private InsertionSort sorter;

    @BeforeEach
    void setUp() {
        sorter = new InsertionSort();
    }

    // 基础版插入排序
    @Test
    void testSortRandomArray() {
        int[] actual = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        sorter.sort(actual);
        int[] expected = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        assertArrayEquals(expected, actual);
    }

    @Test
    void testSortEmptyArray() {
        int[] actual = {};
        sorter.sort(actual);
        assertArrayEquals(new int[0], actual);
    }

    @Test
    void testSortSingleElement() {
        int[] actual = {42};
        sorter.sort(actual);
        assertArrayEquals(new int[]{42}, actual);
    }

    @Test
    void testSortAlreadySorted() {
        int[] actual = {1, 2, 3, 4, 5};
        sorter.sort(actual);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, actual);
    }

    @Test
    void testSortReverseSorted() {
        int[] actual = {9, 8, 7, 6, 5, 4, 3, 2, 1};
        sorter.sort(actual);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, actual);
    }

    @Test
    void testSortWithDuplicates() {
        int[] actual = {4, 2, 4, 1, 2, 3, 1, 3};
        sorter.sort(actual);
        assertArrayEquals(new int[]{1, 1, 2, 2, 3, 3, 4, 4}, actual);
    }

    @Test
    void testSortWithNegatives() {
        int[] actual = {3, -1, 0, -5, 2};
        sorter.sort(actual);
        assertArrayEquals(new int[]{-5, -1, 0, 2, 3}, actual);
    }

    @Test
    void testSortTwoElements() {
        int[] actual = {2, 1};
        sorter.sort(actual);
        assertArrayEquals(new int[]{1, 2}, actual);
    }

    @Test
    void testSortLargeRandomArray() {
        int[] actual = new int[100];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = (int) (Math.random() * 1000);
        }
        int[] expected = actual.clone();
        sorter.sort(actual);
        Arrays.sort(expected);
        assertArrayEquals(expected, actual);
    }

    // 交换版
    @Test
    void testSortSwapRandomArray() {
        int[] actual = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        sorter.sortSwap(actual);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, actual);
    }

    @Test
    void testSortSwapEmptyArray() {
        int[] actual = {};
        sorter.sortSwap(actual);
        assertArrayEquals(new int[0], actual);
    }

    @Test
    void testSortSwapWithDuplicates() {
        int[] actual = {4, 2, 4, 1, 2, 3, 1, 3};
        sorter.sortSwap(actual);
        assertArrayEquals(new int[]{1, 1, 2, 2, 3, 3, 4, 4}, actual);
    }

    @Test
    void testSortSwapReverseSorted() {
        int[] actual = {9, 8, 7, 6, 5, 4, 3, 2, 1};
        sorter.sortSwap(actual);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, actual);
    }

    // 二分插入排序
    @Test
    void testBinarySortRandomArray() {
        int[] actual = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        sorter.binarySort(actual);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, actual);
    }

    @Test
    void testBinarySortEmptyArray() {
        int[] actual = {};
        sorter.binarySort(actual);
        assertArrayEquals(new int[0], actual);
    }

    @Test
    void testBinarySortWithDuplicates() {
        int[] actual = {4, 2, 4, 1, 2, 3, 1, 3};
        sorter.binarySort(actual);
        assertArrayEquals(new int[]{1, 1, 2, 2, 3, 3, 4, 4}, actual);
    }

    @Test
    void testBinarySortAlreadySorted() {
        int[] actual = {1, 2, 3, 4, 5};
        sorter.binarySort(actual);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, actual);
    }

    // 希尔排序
    @Test
    void testShellSortRandomArray() {
        int[] actual = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        sorter.shellSort(actual);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, actual);
    }

    @Test
    void testShellSortEmptyArray() {
        int[] actual = {};
        sorter.shellSort(actual);
        assertArrayEquals(new int[0], actual);
    }

    @Test
    void testShellSortWithDuplicates() {
        int[] actual = {4, 2, 4, 1, 2, 3, 1, 3};
        sorter.shellSort(actual);
        assertArrayEquals(new int[]{1, 1, 2, 2, 3, 3, 4, 4}, actual);
    }

    @Test
    void testShellSortReverseSorted() {
        int[] actual = {9, 8, 7, 6, 5, 4, 3, 2, 1};
        sorter.shellSort(actual);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, actual);
    }

    @Test
    void testShellSortLargeRandomArray() {
        int[] actual = new int[100];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = (int) (Math.random() * 1000);
        }
        int[] expected = actual.clone();
        sorter.shellSort(actual);
        Arrays.sort(expected);
        assertArrayEquals(expected, actual);
    }

    // 区间排序 [lo, hi]
    @Test
    void testSortRangePartial() {
        int[] actual = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        sorter.sort(actual, 2, 6);
        assertArrayEquals(new int[]{5, 2, 1, 3, 4, 8, 9, 7, 6}, actual);
    }

    @Test
    void testSortRangeSingleElement() {
        int[] actual = {5, 2, 8, 3, 1};
        sorter.sort(actual, 2, 2);
        assertArrayEquals(new int[]{5, 2, 8, 3, 1}, actual);
    }

    @Test
    void testSortRangeFullArray() {
        int[] actual = {5, 2, 8, 3, 1, 9, 4, 7, 6};
        sorter.sort(actual, 0, actual.length - 1);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, actual);
    }

    @Test
    void testSortRangeWithDuplicates() {
        int[] actual = {4, 2, 4, 1, 2, 3, 1, 3};
        sorter.sort(actual, 1, 6);
        int[] expected = {4, 1, 1, 2, 2, 3, 4, 3};
        assertArrayEquals(expected, actual);
    }

    @Test
    void testSortRangeBoundaries() {
        int[] actual = {3, 1, 4, 1, 5, 9, 2, 6};
        sorter.sort(actual, 0, 0);
        assertEquals(3, actual[0]);
        sorter.sort(actual, actual.length - 1, actual.length - 1);
        assertEquals(6, actual[actual.length - 1]);
    }
}
