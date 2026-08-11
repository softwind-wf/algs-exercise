package cn.exercise.algs4.datastructure.fenwicktree;

/**
 * 树状数组（FenwickTree / Binary Indexed Tree）完整实现
 * <p>
 * 树状数组擅长处理"单点修改 + 前缀和/区间和查询"的经典问题，
 * 修改与查询的时间复杂度均为 O(log n)。
 * 本实现沿用书中约定：下标从1开始，数组0号位置闲置不用。
 * </p>
 * 包含功能：
 * <ul>
 *     <li>根据原始数组构建索引树（O(n log n)）；</li>
 *     <li>单点更新 update、前缀和 prefixSum、区间和 rangeSum；</li>
 *     <li>单点取值 pointQuery、元素个数 size；</li>
 *     <li>第 k 个位置二分查找 findKth（要求元素非负）。</li>
 * </ul>
 */
public class FenwickTree {
    // 原始数组部分（1-based，下标0闲置）
    private int[] array;
    // 树状数组的索引树(森林)
    private int[] index;

    /**
     * 构造方法：根据传入原始数组构建树状数组
     * 构造时会对原始数组做一份拷贝，之后对该树的操作不会影响外部数组
     * @param array 原始数据数组
     */
    public FenwickTree(int[] array) {
        if (array == null) {
            throw new RuntimeException("用于构建树状数组的数组不能为null");
        }
        if (array.length <= 1) {
            throw new RuntimeException("用于构建树状数组的数组长度不能小于 2（下标0闲置）");
        }
        // 拷贝一份原始数组，避免外部修改造成索引树与数据不一致
        this.array = array.clone();
        // 为数组构建索引树
        buildIndex();
    }

    /**
     * 构建索引树
     */
    private void buildIndex() {
        // 创建与数组等长的索引节点数组
        this.index = new int[this.array.length];
        // 根据下标 i 计算每位索引节点，保存数组元素区间和的取值
        for (int i = 1; i < this.index.length; i++) {
            // 向索引节点 index[i] 及 index[i] 的所有祖先节点累加 array[i] 的取值
            updateIndex(i, this.array[i]);
        }
    }

    /**
     * 向索引节点 index[i] 及 index[i] 的所有祖先节点累加 value
     * 用于构建树 & 单点修改
     * @param i 下标
     * @param value 要累加的值
     */
    private void updateIndex(int i, int value) {
        while (i < this.index.length) {
            this.index[i] += value;
            // 迭代 index[i] 的直接父节点下标
            i += lowBit(i);
        }
    }

    /** 校验下标合法性：有效范围为 [1, array.length-1] */
    private void checkIndex(int i) {
        if (i <= 0 || i >= array.length) {
            throw new RuntimeException("下标错误，有效范围 [1, " + (array.length - 1) + "]");
        }
    }

    /**
     * 更新数组中下标为 i 的元素：array[i] += value，同步更新索引树
     * @param i 单点更新下标
     * @param value 要增加的值
     */
    public void update(int i, int value) {
        checkIndex(i);
        array[i] += value;
        for (; i < index.length; i += lowBit(i)) {
            index[i] += value;
        }
    }

    /**
     * 前缀和查询：计算 array[1..i] 区间元素和并返回
     * @param i 前缀结束下标
     * @return [1,i] 区间和
     */
    public int prefixSum(int i) {
        checkIndex(i);
        int sum = 0;
        for (; i > 0; i -= lowBit(i)) {
            sum += index[i];
        }
        return sum;
    }

    /**
     * 区间和查询：计算 array[l..r] 区间元素和
     * 利用前缀和相减：sum(l,r)=prefixSum(r)-prefixSum(l-1)
     * @param l 区间左边界
     * @param r 区间右边界
     * @return [l,r]区间元素和
     */
    public int rangeSum(int l, int r) {
        // 有效下标范围为 [1, array.length-1]，此处 r>=array.length 一并拦截
        if (l <= 0 || l > r || r >= array.length) {
            throw new RuntimeException("区间和下标范围错误");
        }
        // l=1 时 [1,l-1] 前缀和为 0，不调用 prefixSum(0)（0 为无效下标）
        return prefixSum(r) - (l > 1 ? prefixSum(l - 1) : 0);
    }

    /**
     * 单点取值查询：返回 array[i] 的当前值
     * 由于内部维护了原始数组 array（构造时拷贝、update时同步），直接读取即为 O(1)。
     * 注：prefixSum(i)-prefixSum(i-1) 是通用的"前缀和差分"公式，
     * 只有当实现不保存原数组时才需要借助它来求单点值。
     * @param i 下标
     * @return array[i] 当前值
     */
    public int pointQuery(int i) {
        checkIndex(i);
        return array[i];
    }

    /** 返回树中元素个数（不包含闲置的下标0） */
    public int size() {
        return array.length - 1;
    }

    /**
     * 二分查找：返回最小的下标 pos，使得 prefixSum(pos) >= k（即累计第 k 个位置所在下标）
     * <p>利用树状数组可以 O(log n) 定位"前缀和越过阈值 k 的第一个下标"，
     * 是经典问题的进阶操作（如求第 k 小元素、按权值二分）。
     * 要求元素非负，保证前缀和单调不减。</p>
     * @param k 排名，取值范围 1 ~ 总元素和
     * @return 满足条件的最小下标
     */
    public int findKth(int k) {
        if (k <= 0 || k > prefixSum(array.length - 1)) {
            throw new RuntimeException("k 超出有效范围 [1, " + prefixSum(array.length - 1) + "]");
        }
        int pos = 0;
        // 从不超过最大下标的最大 2 的幂开始逐位尝试
        int step = Integer.highestOneBit(array.length - 1);
        for (; step > 0; step >>= 1) {
            int next = pos + step;
            if (next < array.length && index[next] < k) {
                pos = next;
                k -= index[next];
            }
        }
        // pos 是最后一个满足 prefixSum(pos) < k 的下标，答案是其下一个位置
        return pos + 1;
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
        int[] arr = new int[]{0, 3, 5, 2, 4};
        FenwickTree ft = new FenwickTree(arr);

        System.out.println("元素个数：" + ft.size());                        // 4
        System.out.println("前缀和 [1,4] = " + ft.prefixSum(4));             // 3+5+2+4=14
        System.out.println("区间和 [2,3] = " + ft.rangeSum(2, 3));           // 5+2=7
        System.out.println("单点取值 array[3] = " + ft.pointQuery(3));       // 2

        // 单点更新：下标3元素 + 10
        ft.update(3, 10);
        System.out.println("更新后前缀和 [1,4] = " + ft.prefixSum(4));       // 24
        System.out.println("更新后单点取值 array[3] = " + ft.pointQuery(3)); // 12

        // 第k个位置二分查找（元素非负时可用）
        // 更新后元素：3,5,12,4；前缀和：3,8,20,24
        System.out.println("prefixSum[pos]>=7 的最小下标：" + ft.findKth(7));   // 2
        System.out.println("prefixSum[pos]>=11 的最小下标：" + ft.findKth(11)); // 3
    }
}
