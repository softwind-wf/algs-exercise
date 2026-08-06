package com.ds.fenwicktree;

/**
 * 差分化树状数组（Difference Fenwick Tree / Binary Indexed Tree）完整实现
 * <p>
 * 经典树状数组擅长"单点修改 + 区间查询"，而差分化树状数组解决的是对称问题：
 * <b>区间修改 + 单点查询</b>，并可进一步扩展为<b>区间修改 + 区间查询</b>。
 * 其核心是差分思想：对原始数组 a 构造差分数组 d，满足
 * d[i] = a[i] - a[i-1]（约定 a[0] = 0），
 * 那么区间 [l, r] 整体增加 value，等价于差分数组的两处单点修改：
 * d[l] += value，d[r+1] -= value；
 * 而单点取值 a[i] = Σ d[1..i]，即差分数组的前缀和。
 * </p>
 * <p>
 * 本实现进一步借助如下恒等式支持区间求和（需要维护两棵索引树）：
 * Σ a[1..x] = (x+1) * Σ d[1..x] - Σ (i * d[i])。
 * </p>
 * 包含功能：
 * <ul>
 *     <li>根据原始数组构建差分索引树（O(n log n)）；</li>
 *     <li>区间更新 rangeUpdate、单点查询 pointQuery；</li>
 *     <li>前缀和 prefixSum、区间和 rangeSum；</li>
 *     <li>元素个数 size。</li>
 * </ul>
 * 所有操作的时间复杂度均为 O(log n)。
 */
public class DifferenceFenwickTree {
    // 有效元素个数（不包含闲置的下标0）
    private final int size;
    // tree1：维护差分数组 d[i] 的索引树
    private final int[] tree1;
    // tree2：维护 i * d[i] 的索引树，用于区间求和公式
    private final int[] tree2;

    /**
     * 构造方法：根据传入原始数组构建差分化树状数组
     * 沿用书中约定：下标从1开始，数组0号位置闲置不用
     * @param array 原始数据数组，array[0] 闲置，array[1..n] 为有效元素
     */
    public DifferenceFenwickTree(int[] array) {
        if (array == null) {
            throw new RuntimeException("用于构建树状数组的数组不能为null");
        }
        if (array.length <= 1) {
            throw new RuntimeException("用于构建树状数组的数组长度不能小于 2（下标0闲置）");
        }
        this.size = array.length - 1;
        // 差分树状数组需要操作到 d[size+1]，故索引树长度比原数组多 1（下标有效范围 1..size+1）
        this.tree1 = new int[array.length + 1];
        this.tree2 = new int[array.length + 1];
        // 用差分值 d[i] = a[i] - a[i-1] 构建两棵索引树
        for (int i = 1; i <= size; i++) {
            int diff = array[i] - array[i - 1];
            updateTree(tree1, i, diff);
            updateTree(tree2, i, i * diff);
        }
    }

    /**
     * 区间更新：将 [l, r] 区间内所有元素统一增加 value
     * 利用差分数组，区间更新被转化为两处单点修改：d[l] += value，d[r+1] -= value
     * @param l 区间左边界
     * @param r 区间右边界
     * @param value 要增加的值（可为负）
     */
    public void rangeUpdate(int l, int r, int value) {
        checkRange(l, r);
        updateTree(tree1, l, value);
        updateTree(tree1, r + 1, -value);
        updateTree(tree2, l, l * value);
        updateTree(tree2, r + 1, -(r + 1) * value);
    }

    /**
     * 单点取值查询：返回 array[i] 的当前值
     * 差分数组前缀和 Σ d[1..i] 即为 a[i]
     * @param i 下标
     * @return array[i] 当前值
     */
    public int pointQuery(int i) {
        checkIndex(i);
        return prefixQuery(tree1, i);
    }

    /**
     * 前缀和查询：计算 array[1..x] 区间元素和并返回
     * 公式：Σ a[1..x] = (x+1) * Σ d[1..x] - Σ (i * d[i])
     * @param x 前缀结束下标
     * @return [1,x] 区间和
     */
    public long prefixSum(int x) {
        checkIndex(x);
        return (long) (x + 1) * prefixQuery(tree1, x) - prefixQuery(tree2, x);
    }

    /**
     * 区间和查询：计算 array[l..r] 区间元素和
     * 利用前缀和相减：sum(l,r)=prefixSum(r)-prefixSum(l-1)
     * @param l 区间左边界
     * @param r 区间右边界
     * @return [l,r] 区间元素和
     */
    public long rangeSum(int l, int r) {
        if (l <= 0 || l > r || r > size) {
            throw new RuntimeException("区间和下标范围错误");
        }
        // l=1 时 [1,l-1] 前缀和为 0，不调用 prefixSum(0)（0 为无效下标）
        return prefixSum(r) - (l > 1 ? prefixSum(l - 1) : 0);
    }

    /** 返回树中元素个数（不包含闲置的下标0） */
    public int size() {
        return size;
    }

    /** 校验下标合法性：有效范围为 [1, size] */
    private void checkIndex(int i) {
        if (i <= 0 || i > size) {
            throw new RuntimeException("下标错误，有效范围 [1, " + size + "]");
        }
    }

    /** 校验区间更新合法性：有效范围为 [1, size] */
    private void checkRange(int l, int r) {
        if (l <= 0 || l > r || r > size) {
            throw new RuntimeException("区间更新下标范围错误");
        }
    }

    /**
     * 向索引树 index[i] 及 index[i] 的所有祖先节点累加 value
     * @param tree 目标索引树
     * @param i 下标
     * @param value 要累加的值
     */
    private void updateTree(int[] tree, int i, int value) {
        for (; i < tree.length; i += lowBit(i)) {
            tree[i] += value;
        }
    }

    /**
     * 索引树前缀和查询：返回 Σ tree[1..i]
     * @param tree 目标索引树
     * @param i 前缀结束下标
     * @return [1,i] 区间和
     */
    private int prefixQuery(int[] tree, int i) {
        int sum = 0;
        for (; i > 0; i -= lowBit(i)) {
            sum += tree[i];
        }
        return sum;
    }

    /**
     * lowBit操作：取出n二进制最右侧1对应数值
     * @param n 非负整数
     * @return lowBit(n)
     */
    private int lowBit(int n) {
        return n & -n;
    }

    // ========== 测试示例 ==========
    public static void main(String[] args) {
        // 注意：书中下标从1开始，数组0位置闲置不用
        int[] arr = new int[]{0, 1, 3, 5, 2};
        DifferenceFenwickTree dt = new DifferenceFenwickTree(arr);

        System.out.println("元素个数：" + dt.size());                         // 4
        System.out.println("初始区间和 [2,4] = " + dt.rangeSum(2, 4));       // 3+5+2=10

        // 区间更新：将 [2,3] 整体增加 10
        dt.rangeUpdate(2, 3, 10);
        System.out.println("更新后单点取值 array[2] = " + dt.pointQuery(2)); // 13
        System.out.println("更新后单点取值 array[4] = " + dt.pointQuery(4)); // 2（区间外不受影响）
        System.out.println("更新后前缀和 [1,4] = " + dt.prefixSum(4));       // 1+13+15+2=31
        System.out.println("更新后区间和 [2,3] = " + dt.rangeSum(2, 3));     // 13+15=28
    }
}
