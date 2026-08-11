package cn.exercise.algs4.datastructure.fenwicktree;

import cn.exercise.algs4.datastructure.fenwicktree.DifferenceFenwickTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 差分化树状数组单元测试
 * 覆盖：构造校验、区间更新+单点查询、区间和查询、异常边界、与朴素模型随机对拍。
 * 注意：下标从1开始，数组0号位置闲置。
 */
@DisplayName("DifferenceFenwickTree 差分化树状数组测试")
class DifferenceFenwickTreeTest {

    private static final int[] BASE = {0, 1, 3, 5, 2, 8, 6, 4};

    @Nested
    @DisplayName("构造器测试")
    class ConstructorTest {

        @Test
        @DisplayName("null 数组抛出异常")
        void nullArrayThrows() {
            assertThrows(RuntimeException.class, () -> new DifferenceFenwickTree(null));
        }

        @Test
        @DisplayName("长度小于 2 的数组抛出异常")
        void tooShortArrayThrows() {
            assertThrows(RuntimeException.class, () -> new DifferenceFenwickTree(new int[1]));
            assertThrows(RuntimeException.class, () -> new DifferenceFenwickTree(new int[0]));
        }

        @Test
        @DisplayName("构造后元素个数正确")
        void sizeMatches() {
            assertEquals(7, new DifferenceFenwickTree(BASE).size());
        }

        @Test
        @DisplayName("构造后单点取值等于原数组")
        void initialPointQuery() {
            DifferenceFenwickTree dt = new DifferenceFenwickTree(BASE);
            for (int i = 1; i < BASE.length; i++) {
                assertEquals(BASE[i], dt.pointQuery(i), "下标 " + i + " 初始值不一致");
            }
        }

        @Test
        @DisplayName("构造后区间和等于原数组区间和")
        void initialRangeSum() {
            DifferenceFenwickTree dt = new DifferenceFenwickTree(BASE);
            for (int l = 1; l < BASE.length; l++) {
                for (int r = l; r < BASE.length; r++) {
                    int expected = 0;
                    for (int i = l; i <= r; i++) {
                        expected += BASE[i];
                    }
                    assertEquals(expected, dt.rangeSum(l, r), "区间 [" + l + "," + r + "] 初始和不一致");
                }
            }
        }
    }

    @Nested
    @DisplayName("区间更新 + 单点查询")
    class RangeUpdatePointQueryTest {

        @Test
        @DisplayName("区间内全部加上 value，区间外不受影响")
        void updateRange() {
            DifferenceFenwickTree dt = new DifferenceFenwickTree(BASE);
            dt.rangeUpdate(2, 5, 10);

            assertEquals(BASE[1], dt.pointQuery(1), "区间外元素不应变化");
            for (int i = 2; i <= 5; i++) {
                assertEquals(BASE[i] + 10, dt.pointQuery(i), "下标 " + i + " 更新后取值错误");
            }
            assertEquals(BASE[6], dt.pointQuery(6), "区间外元素不应变化");
            assertEquals(BASE[7], dt.pointQuery(7), "区间外元素不应变化");
        }

        @Test
        @DisplayName("更新整个区间 [1, size]")
        void updateWholeRange() {
            DifferenceFenwickTree dt = new DifferenceFenwickTree(BASE);
            dt.rangeUpdate(1, 7, 5);
            for (int i = 1; i <= 7; i++) {
                assertEquals(BASE[i] + 5, dt.pointQuery(i), "下标 " + i + " 更新后取值错误");
            }
        }

        @Test
        @DisplayName("更新单个点（l == r）")
        void updateSinglePoint() {
            DifferenceFenwickTree dt = new DifferenceFenwickTree(BASE);
            dt.rangeUpdate(4, 4, 7);
            assertEquals(BASE[4] + 7, dt.pointQuery(4));
            assertEquals(BASE[3], dt.pointQuery(3));
            assertEquals(BASE[5], dt.pointQuery(5));
        }

        @Test
        @DisplayName("支持负值更新")
        void updateNegative() {
            DifferenceFenwickTree dt = new DifferenceFenwickTree(BASE);
            dt.rangeUpdate(1, 4, -3);
            for (int i = 1; i <= 4; i++) {
                assertEquals(BASE[i] - 3, dt.pointQuery(i), "下标 " + i + " 负值更新错误");
            }
            assertEquals(BASE[5], dt.pointQuery(5));
        }

        @Test
        @DisplayName("多次区间更新结果可叠加")
        void multipleUpdatesAccumulate() {
            DifferenceFenwickTree dt = new DifferenceFenwickTree(BASE);
            dt.rangeUpdate(1, 3, 10);
            dt.rangeUpdate(2, 4, 5);
            dt.rangeUpdate(3, 5, 2);

            assertEquals(BASE[1] + 10, dt.pointQuery(1));
            assertEquals(BASE[2] + 15, dt.pointQuery(2));
            assertEquals(BASE[3] + 17, dt.pointQuery(3));
            assertEquals(BASE[4] + 7, dt.pointQuery(4));
            assertEquals(BASE[5] + 2, dt.pointQuery(5));
        }
    }

    @Nested
    @DisplayName("前缀和与区间和查询")
    class SumQueryTest {

        @Test
        @DisplayName("更新后前缀和与区间和正确")
        void sumAfterUpdate() {
            DifferenceFenwickTree dt = new DifferenceFenwickTree(BASE);
            dt.rangeUpdate(2, 6, 4);

            // 更新后元素：1,7,9,6,12,10,4
            assertEquals(1 + 7 + 9, dt.prefixSum(3));
            assertEquals(1 + 7 + 9 + 6 + 12, dt.prefixSum(5));
            assertEquals(1 + 7 + 9 + 6 + 12 + 10 + 4, dt.prefixSum(7));
            assertEquals(7 + 9 + 6, dt.rangeSum(2, 4));
            assertEquals(12 + 10, dt.rangeSum(5, 6));
            assertEquals(4, dt.rangeSum(7, 7));
        }

        @Test
        @DisplayName("区间和与单点求和保持一致")
        void rangeSumConsistentWithPointQuery() {
            DifferenceFenwickTree dt = new DifferenceFenwickTree(BASE);
            dt.rangeUpdate(1, 3, 6);
            dt.rangeUpdate(4, 7, -2);

            for (int l = 1; l <= 7; l++) {
                for (int r = l; r <= 7; r++) {
                    long expected = 0;
                    for (int i = l; i <= r; i++) {
                        expected += dt.pointQuery(i);
                    }
                    assertEquals(expected, dt.rangeSum(l, r), "区间 [" + l + "," + r + "] 和与逐点求和不一致");
                }
            }
        }
    }

    @Nested
    @DisplayName("异常与边界测试")
    class ExceptionTest {

        @Test
        @DisplayName("非法单点下标抛出异常")
        void invalidIndexThrows() {
            DifferenceFenwickTree dt = new DifferenceFenwickTree(BASE);
            assertThrows(RuntimeException.class, () -> dt.pointQuery(0));
            assertThrows(RuntimeException.class, () -> dt.pointQuery(-1));
            assertThrows(RuntimeException.class, () -> dt.pointQuery(8));
        }

        @Test
        @DisplayName("非法前缀和下标抛出异常")
        void invalidPrefixIndexThrows() {
            DifferenceFenwickTree dt = new DifferenceFenwickTree(BASE);
            assertThrows(RuntimeException.class, () -> dt.prefixSum(0));
            assertThrows(RuntimeException.class, () -> dt.prefixSum(8));
        }

        @Test
        @DisplayName("非法区间抛出异常")
        void invalidRangeThrows() {
            DifferenceFenwickTree dt = new DifferenceFenwickTree(BASE);
            assertThrows(RuntimeException.class, () -> dt.rangeUpdate(0, 3, 1));
            assertThrows(RuntimeException.class, () -> dt.rangeUpdate(5, 3, 1));
            assertThrows(RuntimeException.class, () -> dt.rangeUpdate(1, 8, 1));
            assertThrows(RuntimeException.class, () -> dt.rangeSum(0, 3));
            assertThrows(RuntimeException.class, () -> dt.rangeSum(5, 3));
            assertThrows(RuntimeException.class, () -> dt.rangeSum(1, 8));
        }
    }

    @Nested
    @DisplayName("随机对拍测试")
    class RandomStressTest {

        @Test
        @DisplayName("随机区间更新后，单点查询与区间和均与朴素数组一致")
        void randomOperationsMatchNaiveModel() {
            Random random = new Random(42);
            int n = 100;
            int[] naive = new int[n + 1];            // 朴素数组模型，下标 1..n
            int[] arr = new int[n + 1];              // 树状数组原始数据，下标0闲置
            for (int i = 1; i <= n; i++) {
                int v = random.nextInt(200) - 100;   // -100 ~ 99
                naive[i] = v;
                arr[i] = v;
            }
            DifferenceFenwickTree dt = new DifferenceFenwickTree(arr);

            int ops = 2000;
            for (int t = 0; t < ops; t++) {
                int l = random.nextInt(n) + 1;
                int r = random.nextInt(n) + 1;
                if (l > r) {
                    int tmp = l;
                    l = r;
                    r = tmp;
                }
                switch (random.nextInt(3)) {
                    case 0: {   // 区间更新
                        int value = random.nextInt(50) - 25;
                        dt.rangeUpdate(l, r, value);
                        for (int i = l; i <= r; i++) {
                            naive[i] += value;
                        }
                        break;
                    }
                    case 1: {   // 单点查询
                        int i = random.nextInt(n) + 1;
                        assertEquals(naive[i], dt.pointQuery(i), "第 " + t + " 次操作单点查询不一致");
                        break;
                    }
                    default: {  // 区间和
                        long expected = 0;
                        for (int i = l; i <= r; i++) {
                            expected += naive[i];
                        }
                        assertEquals(expected, dt.rangeSum(l, r), "第 " + t + " 次操作区间和查询不一致");
                        break;
                    }
                }
            }
        }
    }
}
