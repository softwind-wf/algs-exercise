package com.ds.datastructure.fenwicktree;

import cn.exercise.algs4.datastructure.fenwicktree.FenwickTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 树状数组单元测试
 * 重点覆盖 rangeSum 的 l=1 边界（修复前会因调用 prefixSum(0) 而抛异常），
 * 并验证单点更新、前缀和、区间和、findKth 等基本功能。
 * 注意：下标从1开始，数组0号位置闲置。
 */
@DisplayName("FenwickTree 树状数组测试")
class FenwickTreeTest {

    private static final int[] BASE = {0, 3, 5, 2, 4, 8, 6};

    @Nested
    @DisplayName("构造器测试")
    class ConstructorTest {

        @Test
        @DisplayName("null 数组抛出异常")
        void nullArrayThrows() {
            assertThrows(RuntimeException.class, () -> new FenwickTree(null));
        }

        @Test
        @DisplayName("长度小于 2 的数组抛出异常")
        void tooShortArrayThrows() {
            assertThrows(RuntimeException.class, () -> new FenwickTree(new int[1]));
        }

        @Test
        @DisplayName("构造后元素个数与单点取值正确")
        void initialState() {
            FenwickTree ft = new FenwickTree(BASE);
            assertEquals(6, ft.size());
            for (int i = 1; i < BASE.length; i++) {
                assertEquals(BASE[i], ft.pointQuery(i), "下标 " + i + " 初始值不一致");
            }
        }
    }

    @Nested
    @DisplayName("前缀和与区间和测试")
    class SumQueryTest {

        @Test
        @DisplayName("初始前缀和正确")
        void initialPrefixSum() {
            FenwickTree ft = new FenwickTree(BASE);
            assertEquals(3, ft.prefixSum(1));
            assertEquals(8, ft.prefixSum(2));
            assertEquals(10, ft.prefixSum(3));
            assertEquals(14, ft.prefixSum(4));
            assertEquals(28, ft.prefixSum(6));
        }

        @Test
        @DisplayName("rangeSum 左边界 l=1 正常返回（回归修复）")
        void rangeSumFromIndexOne() {
            FenwickTree ft = new FenwickTree(BASE);
            // 修复前此处会因调用 prefixSum(0) 抛异常
            assertEquals(3, ft.rangeSum(1, 1));
            assertEquals(3 + 5 + 2, ft.rangeSum(1, 3));
            assertEquals(28, ft.rangeSum(1, 6));
        }

        @Test
        @DisplayName("rangeSum 与 prefixSum 相减结果一致")
        void rangeSumConsistent() {
            FenwickTree ft = new FenwickTree(BASE);
            for (int l = 1; l < BASE.length; l++) {
                for (int r = l; r < BASE.length; r++) {
                    int expected = 0;
                    for (int i = l; i <= r; i++) {
                        expected += BASE[i];
                    }
                    assertEquals(expected, ft.rangeSum(l, r),
                            "区间 [" + l + "," + r + "] 与朴素求和不一致");
                }
            }
        }

        @Test
        @DisplayName("单点更新后区间和正确")
        void sumAfterUpdate() {
            FenwickTree ft = new FenwickTree(BASE);
            ft.update(3, 10);   // 下标3元素 2 -> 12
            assertEquals(3 + 5 + 12 + 4 + 8 + 6, ft.prefixSum(6));
            assertEquals(3 + 5 + 12 + 4, ft.rangeSum(1, 4));
            assertEquals(5 + 12, ft.rangeSum(2, 3));
        }
    }

    @Nested
    @DisplayName("findKth 测试")
    class FindKthTest {

        @Test
        @DisplayName("findKth 定位前缀和越过阈值的最小下标")
        void findKthWorks() {
            FenwickTree ft = new FenwickTree(BASE);   // 元素：3,5,2,4,8,6；前缀和：3,8,10,14,22,28
            assertEquals(1, ft.findKth(1));
            assertEquals(2, ft.findKth(7));
            assertEquals(4, ft.findKth(11));
            assertEquals(6, ft.findKth(28));
        }

        @Test
        @DisplayName("findKth 越界 k 抛出异常")
        void findKthOutOfRange() {
            FenwickTree ft = new FenwickTree(BASE);
            assertThrows(RuntimeException.class, () -> ft.findKth(0));
            assertThrows(RuntimeException.class, () -> ft.findKth(29));
        }
    }

    @Nested
    @DisplayName("异常与随机对拍测试")
    class EdgeAndStressTest {

        @Test
        @DisplayName("非法下标与区间抛出异常")
        void invalidArgumentsThrow() {
            FenwickTree ft = new FenwickTree(BASE);
            assertThrows(RuntimeException.class, () -> ft.prefixSum(0));
            assertThrows(RuntimeException.class, () -> ft.pointQuery(7));
            assertThrows(RuntimeException.class, () -> ft.rangeSum(0, 3));
            assertThrows(RuntimeException.class, () -> ft.rangeSum(5, 3));
            assertThrows(RuntimeException.class, () -> ft.rangeSum(1, 7));
        }

        @Test
        @DisplayName("随机单点更新后，单点/前缀/区间查询与朴素数组一致")
        void randomOperationsMatchNaiveModel() {
            Random random = new Random(42);
            int n = 100;
            int[] naive = new int[n + 1];            // 朴素数组模型，下标 1..n
            int[] arr = new int[n + 1];              // 树状数组原始数据，下标0闲置
            for (int i = 1; i <= n; i++) {
                int v = random.nextInt(200) - 100;
                naive[i] = v;
                arr[i] = v;
            }
            FenwickTree ft = new FenwickTree(arr);

            int ops = 2000;
            for (int t = 0; t < ops; t++) {
                int i = random.nextInt(n) + 1;
                switch (random.nextInt(3)) {
                    case 0: {   // 单点更新
                        int value = random.nextInt(50) - 25;
                        ft.update(i, value);
                        naive[i] += value;
                        break;
                    }
                    case 1: {   // 单点查询
                        assertEquals(naive[i], ft.pointQuery(i), "第 " + t + " 次操作单点查询不一致");
                        break;
                    }
                    default: {  // 区间和（含 l=1 边界）
                        int r = random.nextInt(n) + 1;
                        if (i > r) {
                            int tmp = i;
                            i = r;
                            r = tmp;
                        }
                        long expected = 0;
                        for (int j = i; j <= r; j++) {
                            expected += naive[j];
                        }
                        assertEquals(expected, ft.rangeSum(i, r), "第 " + t + " 次操作区间和查询不一致");
                        break;
                    }
                }
            }
        }
    }
}
