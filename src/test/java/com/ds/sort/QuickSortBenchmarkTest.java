package com.ds.sort;

import cn.exercise.algs4.datastructure.sort.QuickSort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class QuickSortBenchmarkTest {

    private QuickSort sorter;
    private Random random;

    // 测试规模
    private static final int SIZE = 100_000;
    // 每种场景执行次数（取中位数更稳定，这里取平均）
    private static final int TRIALS = 5;
    // 预热次数
    private static final int WARMUP = 3;

    @BeforeEach
    void setUp() {
        sorter = new QuickSort();
        random = new Random(42);
    }

    @Test
    void benchmarkRandomArray() {
        System.out.println("\n========== 随机数组 (size=" + SIZE + ") ==========");
        runBenchmark("随机数组", this::generateRandomArray);
    }

    @Test
    void benchmarkManyDuplicates() {
        System.out.println("\n========== 大量重复元素 (size=" + SIZE + ", 仅10个不同值) ==========");
        runBenchmark("大量重复", this::generateManyDuplicatesArray);
    }

    @Test
    void benchmarkSortedArray() {
        System.out.println("\n========== 已排序数组 (size=" + SIZE + ") ==========");
        runBenchmark("已排序", this::generateSortedArray);
    }

    @Test
    void benchmarkReverseSortedArray() {
        System.out.println("\n========== 逆序数组 (size=" + SIZE + ") ==========");
        runBenchmark("逆序", this::generateReverseSortedArray);
    }

    @Test
    void benchmarkFewUniqueLargeRange() {
        System.out.println("\n========== 少量唯一值但范围大 (size=" + SIZE + ", 100个不同值) ==========");
        runBenchmark("少量唯一值", this::generateFewUniqueArray);
    }

    private void runBenchmark(String scenario, ArrayGenerator generator) {
        // 普通快排预热
        for (int i = 0; i < WARMUP; i++) {
            int[] data = generator.generate(SIZE);
            sorter.sort(data);
        }
        // 三向快排预热
        for (int i = 0; i < WARMUP; i++) {
            int[] data = generator.generate(SIZE);
            sorter.sort3Way(data);
        }

        long totalNormal = 0;
        long total3Way = 0;

        for (int t = 0; t < TRIALS; t++) {
            int[] origin = generator.generate(SIZE);
            int[] copyForNormal = Arrays.copyOf(origin, origin.length);
            int[] copyFor3Way = Arrays.copyOf(origin, origin.length);

            long startNormal = System.nanoTime();
            sorter.sort(copyForNormal);
            long endNormal = System.nanoTime();
            long normalTime = endNormal - startNormal;

            long start3Way = System.nanoTime();
            sorter.sort3Way(copyFor3Way);
            long end3Way = System.nanoTime();
            long threeWayTime = end3Way - start3Way;

            // 验证正确性
            Arrays.sort(origin);
            assertArrayEquals(origin, copyForNormal, scenario + " - 普通快排结果错误");
            assertArrayEquals(origin, copyFor3Way, scenario + " - 三向快排结果错误");

            totalNormal += normalTime;
            total3Way += threeWayTime;

            System.out.printf("  第%d轮: 普通快排=%s, 三向快排=%s%n",
                    t + 1, formatTime(normalTime), formatTime(threeWayTime));
        }

        long avgNormal = totalNormal / TRIALS;
        long avg3Way = total3Way / TRIALS;

        System.out.printf("  平均: 普通快排=%s, 三向快排=%s%n",
                formatTime(avgNormal), formatTime(avg3Way));

        if (avgNormal > 0) {
            double ratio = (double) avg3Way / avgNormal;
            System.out.printf("  三向快排 / 普通快排 = %.2f%n", ratio);
        }
    }

    // ========== 数据生成器 ==========

    private int[] generateRandomArray(int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = random.nextInt();
        }
        return arr;
    }

    private int[] generateManyDuplicatesArray(int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = random.nextInt(10); // 只有 0~9
        }
        return arr;
    }

    private int[] generateSortedArray(int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = i;
        }
        return arr;
    }

    private int[] generateReverseSortedArray(int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = size - i;
        }
        return arr;
    }

    private int[] generateFewUniqueArray(int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = random.nextInt(100); // 0~99
        }
        return arr;
    }

    private String formatTime(long nanos) {
        if (nanos < 1_000) {
            return nanos + " ns";
        } else if (nanos < 1_000_000) {
            return TimeUnit.NANOSECONDS.toMicros(nanos) + " μs";
        } else {
            return TimeUnit.NANOSECONDS.toMillis(nanos) + " ms";
        }
    }

    @FunctionalInterface
    private interface ArrayGenerator {
        int[] generate(int size);
    }
}
