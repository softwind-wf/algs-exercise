package exercise1.exercise1_4;

import java.util.ArrayList;
import java.util.List;

public class ThreeSumToCollinear {

    // 点类：存储 (x, x³)
    static class Point {
        long x;
        long y;

        Point(long x) {
            this.x = x;
            this.y = x * x * x; // y = x³
        }
    }

    // 判断三点是否共线
    public static boolean isCollinear(Point p1, Point p2, Point p3) {
        // 斜率相等：(y2-y1)/(x2-x1) = (y3-y2)/(x3-x2)
        // 交叉相乘避免除法：(y2-y1)*(x3-x2) == (y3-y2)*(x2-x1)
        return (p2.y - p1.y) * (p3.x - p2.x) == (p3.y - p2.y) * (p2.x - p1.x);
    }

    // 用三点共线算法解决 3-sum 问题
    public static boolean hasThreeSum(long[] nums) {
        // 1. 映射为点集
        List<Point> points = new ArrayList<>();
        for (long num : nums) {
            points.add(new Point(num));
        }

        int n = points.size();
        int collinearCount = 0;

        // 2. 遍历所有三元组，统计共线组数
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                for (int k = j + 1; k < n; k++) {
                    if (isCollinear(points.get(i), points.get(j), points.get(k))) {
                        collinearCount++;
                        // 可提前返回，不必统计全部
                        return true;
                    }
                }
            }
        }

        // 无共线三元组 = 无 3-sum 解
        return collinearCount > 0;
    }

    public static void main(String[] args) {
        // 测试用例1：存在 3-sum 解（-1, 0, 1）
        long[] test1 = {-1, 0, 1, 2, -1, -4};
        System.out.println("Test 1 has 3-sum? " + hasThreeSum(test1)); // 输出 true

        // 测试用例2：不存在 3-sum 解
        long[] test2 = {1, 2, 3, 4};
        System.out.println("Test 2 has 3-sum? " + hasThreeSum(test2)); // 输出 false

        // 测试用例3：简单 3-sum 解
        long[] test3 = {3, -2, -1};
        System.out.println("Test 3 has 3-sum? " + hasThreeSum(test3)); // 输出 true
    }
}