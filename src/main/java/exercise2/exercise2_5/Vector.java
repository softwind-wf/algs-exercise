package exercise2.exercise2_5;

import edu.princeton.cs.algs4.StdOut;

import java.util.Arrays;

/**
 * 2.5.21 多维排序 (Vector)
 * 编写 Vector 数据类型并将 d 维整型向量排序。
 * 排序规则：先按第一维数字排序，一维相同则按二维，二维相同则按三维，以此类推。
 *
 * @author 豆包 (代码实现)
 */
public class Vector implements Comparable<Vector> {
    private final int[] elements; // 存储向量的各个维度值

    // 构造函数：初始化 d 维向量
    public Vector(int[] elements) {
        this.elements = elements.clone(); // 防御性拷贝，防止外部修改
    }

    /**
     * 核心比较方法 (compareTo)
     * 实现多维字典序排序：
     * 1. 先比较第 1 维 (索引 0)
     * 2. 若相等，比较第 2 维 (索引 1)
     * 3. 依此类推...
     * 4. 若所有维度都相等，则两个向量相等
     */
    @Override
    public int compareTo(Vector that) {
        // 确保两个向量维度相同，否则无法比较
        if (this.elements.length != that.elements.length) {
            throw new IllegalArgumentException("Vectors must have the same dimension.");
        }

        // 逐维从左到右比较
        for (int i = 0; i < this.elements.length; i++) {
            // 注意：这里使用 Integer 的 compare 方法，避免直接相减可能导致的溢出
            int cmp = Integer.compare(this.elements[i], that.elements[i]);
            if (cmp != 0) {
                return cmp; // 一旦发现某一维不相等，立即返回比较结果
            }
            // 如果相等，继续比较下一维
        }
        return 0; // 所有维度都相等
    }

    // 为了方便打印输出，重写 toString 方法
    @Override
    public String toString() {
        return Arrays.toString(elements);
    }

    // 测试主类
    public static void main(String[] args) {
        // 定义一组 3 维向量数据
        Vector[] vectors = {
                new Vector(new int[]{3, 1, 4}),
                new Vector(new int[]{1, 5, 9}),
                new Vector(new int[]{2, 6, 5}),
                new Vector(new int[]{1, 2, 3}),
                new Vector(new int[]{3, 1, 2}),
                new Vector(new int[]{1, 5, 2})
        };

        // 排序前
        StdOut.println("排序前：");
        for (Vector v : vectors) {
            StdOut.print(v + " ");
        }
        StdOut.println("\n" + "-------------------------------------------------------");

        // 执行排序 (利用 Arrays.sort，底层依赖 compareTo 方法)
        Arrays.sort(vectors);

        // 排序后
        StdOut.println("排序后（多维字典序）：");
        for (Vector v : vectors) {
            StdOut.print(v + " ");
        }
    }
}