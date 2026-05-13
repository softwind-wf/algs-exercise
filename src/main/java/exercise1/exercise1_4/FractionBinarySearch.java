package exercise1.exercise1_4;

public class FractionBinarySearch {
    
    /**
     * 有理数比较器接口，用于判断目标分数是否小于给定的分数
     */
    public interface RationalComparator {
        /**
         * 判断目标分数是否小于 p/q
         * @param p 分子
         * @param q 分母
         * @return true 如果目标 < p/q, false 如果目标 >= p/q
         */
        boolean isLessThan(int p, int q);
    }
    
    /**
     * 寻找满足 0 < p/q < 1 且 p < q < N 的有理数
     * @param N 分母上限
     * @param comparator 比较器，用于判断目标分数与给定分数的大小关系
     * @return 找到的有理数，以长度为2的数组返回 [分子, 分母]
     */
    public static int[] findRational(int N, RationalComparator comparator) {
        if (N <= 2) {
            throw new IllegalArgumentException("N must be at least 3");
        }
        
        // 初始化边界为 0/1 和 1/1
        int leftNum = 0;
        int leftDen = 1;
        int rightNum = 1;
        int rightDen = 1;
        
        while (true) {
            // 计算中项分数 (mediant)
            int midNum = leftNum + rightNum;
            int midDen = leftDen + rightDen;
            
            // 如果分母超出范围，说明目标就在当前边界附近
            if (midDen >= N) {
                // 比较两个边界与目标的大小，返回更接近的那个
                // 首先判断目标是否小于 leftNum/leftDen
                if (comparator.isLessThan(leftNum, leftDen)) {
                    // 目标 < leftNum/leftDen，但这种情况理论上不会发生
                    // 因为 left 是下界，目标应该 >= left
                    // 这里做防御性处理
                    return new int[]{leftNum, leftDen};
                }
                
                // 判断目标是否小于 rightNum/rightDen
                if (comparator.isLessThan(rightNum, rightDen)) {
                    // left <= 目标 < right
                    // 比较 left 和 right 哪个更接近目标
                    // 这里我们返回 left 作为下界
                    return new int[]{leftNum, leftDen};
                } else {
                    // 目标 >= right，但 right 是上界，这种情况也不会发生
                    return new int[]{rightNum, rightDen};
                }
            }
            
            // 进行关键的比较：目标是否小于 midNum/midDen?
            if (comparator.isLessThan(midNum, midDen)) {
                // 目标 < midNum/midDen，目标在左半区间
                rightNum = midNum;
                rightDen = midDen;
            } else {
                // 目标 >= midNum/midDen，目标在右半区间
                leftNum = midNum;
                leftDen = midDen;
            }
            
            // 检查是否找到了精确匹配
            // 注意：由于比较器只提供小于判断，我们无法直接判断相等
            // 但在实际应用中，当区间足够小时，我们就认为找到了
        }
    }
    
    /**
     * 测试用例：寻找一个已知的有理数
     */
    public static void main(String[] args) {
        // 测试1：寻找 2/5
        int N = 10;
        int targetNum = 2;
        int targetDen = 5;
        
        System.out.println("寻找目标: " + targetNum + "/" + targetDen);

        int finalTargetNum = targetNum;
        int finalTargetDen = targetDen;
        RationalComparator comparator = new RationalComparator() {
            @Override
            public boolean isLessThan(int p, int q) {
                // 比较 targetNum/targetDen 是否小于 p/q
                // 即 targetNum * q < p * targetDen
                return finalTargetNum * q < p * finalTargetDen;
            }
        };
        
        int[] result = findRational(N, comparator);
        System.out.println("找到结果: " + result[0] + "/" + result[1]);
        System.out.println("是否匹配: " + (result[0] == targetNum && result[1] == targetDen));
        System.out.println();
        
        // 测试2：寻找 3/7
        targetNum = 3;
        targetDen = 7;
        
        System.out.println("寻找目标: " + targetNum + "/" + targetDen);

        int finalTargetNum1 = targetNum;
        int finalTargetDen1 = targetDen;
        comparator = new RationalComparator() {
            @Override
            public boolean isLessThan(int p, int q) {
                return finalTargetNum1 * q < p * finalTargetDen1;
            }
        };
        
        result = findRational(N, comparator);
        System.out.println("找到结果: " + result[0] + "/" + result[1]);
        System.out.println("是否匹配: " + (result[0] == targetNum && result[1] == targetDen));
        System.out.println();
        
        // 测试3：寻找 4/9
        targetNum = 4;
        targetDen = 9;
        
        System.out.println("寻找目标: " + targetNum + "/" + targetDen);

        int finalTargetNum2 = targetNum;
        int finalTargetDen2 = targetDen;
        comparator = new RationalComparator() {
            @Override
            public boolean isLessThan(int p, int q) {
                return finalTargetNum2 * q < p * finalTargetDen2;
            }
        };
        
        result = findRational(N, comparator);
        System.out.println("找到结果: " + result[0] + "/" + result[1]);
        System.out.println("是否匹配: " + (result[0] == targetNum && result[1] == targetDen));
        System.out.println();
        
        // 测试4：随机测试，验证算法的正确性
        System.out.println("随机测试：");
        testRandom(N, 10);
    }
    
    /**
     * 随机测试
     */
    private static void testRandom(int N, int testCount) {
        java.util.Random random = new java.util.Random();
        
        for (int t = 0; t < testCount; t++) {
            // 生成一个随机的目标分数
            int den;
            do {
                den = random.nextInt(N - 2) + 2; // 2 到 N-1
            } while (den < 2);
            
            int num = random.nextInt(den - 1) + 1; // 1 到 den-1
            
            System.out.print("目标: " + num + "/" + den + " -> ");

            int finalDen = den;
            RationalComparator comparator = new RationalComparator() {
                @Override
                public boolean isLessThan(int p, int q) {
                    return num * q < p * finalDen;
                }
            };
            
            int[] result = findRational(N, comparator);
            
            if (result[0] == num && result[1] == den) {
                System.out.println("✓ 找到 " + result[0] + "/" + result[1]);
            } else {
                System.out.println("✗ 找到 " + result[0] + "/" + result[1] + "，期望 " + num + "/" + den);
            }
        }
    }
    
    /**
     * 增强版：带迭代次数统计的实现
     */
    public static int[] findRationalWithStats(int N, RationalComparator comparator) {
        if (N <= 2) {
            throw new IllegalArgumentException("N must be at least 3");
        }
        
        int leftNum = 0, leftDen = 1;
        int rightNum = 1, rightDen = 1;
        int iterations = 0;
        
        while (true) {
            iterations++;
            
            int midNum = leftNum + rightNum;
            int midDen = leftDen + rightDen;
            
            if (midDen >= N) {
                System.out.println("迭代次数: " + iterations);
                
                // 判断应该返回哪个边界
                // 计算目标相对于两个边界的距离
                if (comparator.isLessThan(leftNum, leftDen)) {
                    return new int[]{leftNum, leftDen};
                }
                
                // 检查目标是否正好等于 left
                if (!comparator.isLessThan(leftNum, leftDen) && 
                    comparator.isLessThan(leftNum + 1, leftDen)) {
                    // 这里简化处理，实际应用中可能需要更精确的判断
                }
                
                return new int[]{leftNum, leftDen};
            }
            
            if (comparator.isLessThan(midNum, midDen)) {
                rightNum = midNum;
                rightDen = midDen;
            } else {
                leftNum = midNum;
                leftDen = midDen;
            }
            
            // 可选：检查是否找到了精确匹配
            // 由于我们只有小于比较，无法直接判断相等
            // 但可以通过两个连续的比较来推断
            if (!comparator.isLessThan(leftNum, leftDen) && 
                comparator.isLessThan(leftNum + 1, leftDen)) {
                // 目标可能等于 leftNum/leftDen
                // 继续搜索直到边界确定
            }
        }
    }
}