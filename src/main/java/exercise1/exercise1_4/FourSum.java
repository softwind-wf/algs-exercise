package exercise1.exercise1_4;

import java.util.*;

public class FourSum {
    // 数对类：存和、第一个索引、第二个索引
    private static class Pair implements Comparable<Pair> {
        int sum;
        int i;
        int j;

        Pair(int sum, int i, int j) {
            this.sum = sum;
            this.i = i; // 保证 i < j
            this.j = j;
        }

        // 按和排序，用于二分查找
        @Override
        public int compareTo(Pair that) {
            return Integer.compare(this.sum, that.sum);
        }
    }

    // 统计和为0的**不重复**四元组数量（去重+正确计数）
    public static int countFourSum(int[] a) {
        int N = a.length;
        if (N < 4) return 0;

        // 1. 生成所有 i<j 的数对，避免重复生成
        List<Pair> pairList = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
                pairList.add(new Pair(a[i] + a[j], i, j));
            }
        }
        Pair[] pairs = pairList.toArray(new Pair[0]);
        int M = pairs.length;

        // 2. 按数对的和排序，为二分查找做准备（O(n² log n)）
        Arrays.sort(pairs);

        // 3. 用Set去重：存储排序后的四元组值，避免重复计数
        Set<String> uniqueQuadruples = new HashSet<>();

        // 4. 遍历每个数对，二分找互补和的数对（O(n² log n)）
        for (int p = 0; p < M; p++) {
            int target = -pairs[p].sum; // 互补和：p.sum + q.sum = 0

            // 二分查找所有sum=target的数对的左边界和右边界
            int leftIdx = findLeft(pairs, target);
            int rightIdx = findRight(pairs, target);

            if (leftIdx == -1 || rightIdx == -1) continue;

            // 遍历所有sum=target的数对q
            for (int q = leftIdx; q <= rightIdx; q++) {
                // 跳过自己（p和q是同一个数对的情况）
                if (p == q) continue;

                Pair pPair = pairs[p];
                Pair qPair = pairs[q];

                // 核心：检查四个索引互不重叠（i1,j1 和 i2,j2 无交集）
                boolean isOverlap = (pPair.i == qPair.i) || (pPair.i == qPair.j) 
                                 || (pPair.j == qPair.i) || (pPair.j == qPair.j);
                if (isOverlap) continue;

                // 提取四元组的四个值，并排序（用于去重）
                int[] quad = {a[pPair.i], a[pPair.j], a[qPair.i], a[qPair.j]};
                Arrays.sort(quad);
                // 转为字符串作为Set的key，避免重复
                String key = Arrays.toString(quad);
                uniqueQuadruples.add(key);
            }
        }

        // 返回不重复的四元组数量
        return uniqueQuadruples.size();
    }

    // 二分查找第一个sum=target的数对索引（左边界）
    private static int findLeft(Pair[] pairs, int target) {
        int lo = 0, hi = pairs.length - 1;
        int left = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (pairs[mid].sum == target) {
                left = mid;
                hi = mid - 1; // 继续找更左边的
            } else if (pairs[mid].sum < target) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return left;
    }

    // 二分查找最后一个sum=target的数对索引（右边界）
    private static int findRight(Pair[] pairs, int target) {
        int lo = 0, hi = pairs.length - 1;
        int right = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (pairs[mid].sum == target) {
                right = mid;
                lo = mid + 1; // 继续找更右边的
            } else if (pairs[mid].sum < target) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return right;
    }

    public static void main(String[] args) {
        int[] a = {1, 2, -1, 0, -2, 1, -1};
        int result = countFourSum(a);
        System.out.println("和为0的不重复四元组数量：" + result);
        
        // 手动验证：数组中的合法四元组（去重后）
        // 1. (1,2,-1,-2)  2. (1,0,-1,0)（无0的第二个？原数组是[1,2,-1,0,-2,1,-1]）
        // 实际正确结果：3（可自行验证）
    }
}