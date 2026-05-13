import exercise1.exercise1_3.RandomBag;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class RandomBagStatisticsTest {

    // 统计测试：验证迭代器的排列是均匀随机的
    @Test
    void testRandomnessUniformity() {
        // 测试配置
        final int N = 4; // 背包中元素的数量
        final int TOTAL_TRIALS = 1000000; // 总迭代次数（越多越准确）

        // 准备背包和元素
        RandomBag<Integer> bag = new RandomBag<>();
        List<Integer> elements = Arrays.asList(1, 2, 3, 4);
        for (int e : elements) {
            bag.add(e);
        }

        // 统计每种排列出现的次数
        Map<String, Integer> permutationCount = new HashMap<>();
        int totalUniquePermutations = factorial(N); // 理论上的排列总数

        for (int trial = 0; trial < TOTAL_TRIALS; trial++) {
            // 收集一次迭代的顺序
            List<Integer> order = new ArrayList<>();
            for (int num : bag) {
                order.add(num);
            }
            // 将顺序转换为字符串作为键
            String key = order.toString();
            permutationCount.put(key, permutationCount.getOrDefault(key, 0) + 1);
        }

        // 验证统计结果
        // 1. 所有可能的排列都应该出现过（在足够多的试验下）
        assertEquals(totalUniquePermutations, permutationCount.size(),
                "Not all permutations were observed. Increase TOTAL_TRIALS.");

        // 2. 计算期望次数和标准差
        double expected = (double) TOTAL_TRIALS / totalUniquePermutations;
        double variance = expected * (1 - 1.0 / totalUniquePermutations);
        double stdDev = Math.sqrt(variance);

        // 3. 检查每个排列的出现次数是否在合理范围内（3个标准差内）
        for (int count : permutationCount.values()) {
            assertTrue(count >= expected - 3 * stdDev && count <= expected + 3 * stdDev,
                    "Permutation count " + count + " is outside the expected range ["
                            + (expected - 3 * stdDev) + ", " + (expected + 3 * stdDev) + "]");
        }

        System.out.println("Randomness test passed!");
        System.out.printf("Expected occurrences per permutation: %.2f%n", expected);
        System.out.printf("Standard deviation: %.2f%n", stdDev);
    }

    // 计算阶乘 N!
    private int factorial(int n) {
        if (n <= 1) return 1;
        return n * factorial(n - 1);
    }
}