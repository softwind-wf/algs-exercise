package cn.exercise.algs4.datastructure.hash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 数字分析法(Digit Analysis / Radix Analysis)散列表。
 *
 * <p><b>适用前提</b>:关键字集合是**已知的静态集合**,且关键字是位数较多的同基数数字编码
 * (学号、工号、电话号码、身份证号等)。与除留余数法、直接定址法不同,数字分析法的散列函数
 * 不是一条公式,而是**从样本中挑位**得来的:</p>
 *
 * <ol>
 *   <li>统计每一位上各数字(key 在该位上的取值)出现的次数;</li>
 *   <li>用归一化熵给每一位的"分布均匀程度"打分:恒定不变的位 = 0,各数字等频出现 = 1;</li>
 *   <li>挑选最均匀的若干位,按原有位序拼成散列地址。</li>
 * </ol>
 *
 * <p>地址空间由**选中的位数**决定(radix^位数),而不是由关键字取值范围或表长决定;
 * 目标是让样本内冲突尽量少。选位后仍可能有冲突,本类采用**拉链法**(分离链接)处理。</p>
 *
 * <p>位序约定:第 0 位是最低位(个位),位号越大越靠左。例:8 位学号 20230214 的
 * 第 0、1 位是 "14",第 2、3 位是 "02",第 4、5 位是 "23",第 6、7 位是 "20"。</p>
 *
 * <p>关键字必须是非负整数且不超过 digitCount 位,否则抛 {@link IllegalArgumentException}。
 * 本类不是线程安全的。</p>
 *
 * @param <Value> 关键字关联的值类型
 */
public class DigitAnalysisHashST<Value> {

    /** 默认基数:十进制数字 */
    private static final int DEFAULT_RADIX = 10;

    /** 地址空间上限:2^26 = 67108864 个单元,超过则拒绝,避免分配超大数组导致 OOM */
    private static final int MAX_ADDRESS_SPACE = 1 << 26;

    /** 拉链法的链结点 */
    private static final class Node<Value> {
        final long key;
        Value value;
        Node<Value> next;

        Node(long key, Value value) {
            this.key = key;
            this.value = value;
        }
    }

    /** 基数(进制),通常为 10 */
    private final int radix;

    /** 参与分析的关键字位数 */
    private final int digitCount;

    /** 关键字上界(不含):radix^digitCount */
    private final long keyLimit;

    /** 选中的位号,升序(低位在前) */
    private final int[] positions;

    /** 地址空间大小 = radix^选位数 */
    private final int addressSpace;

    /** counts[位号][数字] = 该位上该数字出现的次数 */
    private final int[][] counts;

    /** 各位的归一化熵,取值 [0,1] */
    private final double[] uniformity;

    /** 样本关键字个数 */
    private final int sampleSize;

    /** 样本中映射到同一地址的重复次数(样本量 − 不同地址数) */
    private final int sampleCollisions;

    /** 拉链法的桶数组 */
    private final Node<Value>[] buckets;

    /** 已存关键字个数 */
    private int n;

    /**
     * 用十进制基数构造(最常用)。
     *
     * @param sample      已知的关键字样本集合,不能为空
     * @param digitCount  参与分析的位数
     * @param maxCapacity 地址空间上限(选位后 radix^选位数 不超过它)
     */
    public DigitAnalysisHashST(long[] sample, int digitCount, int maxCapacity) {
        this(sample, DEFAULT_RADIX, digitCount, maxCapacity);
    }

    /**
     * 分析样本并构造散列表:统计各位数字分布 → 按均匀度选位 → 建立拉链桶数组。
     *
     * @param sample      已知的关键字样本集合,不能为空且元素必须合法
     * @param radix       基数,不小于 2(通常为 10)
     * @param digitCount  参与分析的位数,至少为 1
     * @param maxCapacity 地址空间上限,不能小于 radix
     * @throws IllegalArgumentException 参数非法或关键字越界
     */
    public DigitAnalysisHashST(long[] sample, int radix, int digitCount, int maxCapacity) {
        Objects.requireNonNull(sample, "样本集合不能为 null(数字分析法必须基于已知的关键字集合)");
        if (sample.length == 0) {
            throw new IllegalArgumentException("样本集合不能为空:数字分析法必须基于已知的关键字集合");
        }
        if (radix < 2) {
            throw new IllegalArgumentException("基数必须不小于 2,当前为 " + radix);
        }
        if (digitCount < 1) {
            throw new IllegalArgumentException("位数必须至少为 1,当前为 " + digitCount);
        }
        if (maxCapacity < radix) {
            throw new IllegalArgumentException("地址空间上限不能小于基数 " + radix + ",当前为 " + maxCapacity);
        }

        this.radix = radix;
        this.digitCount = digitCount;
        this.keyLimit = power(radix, digitCount);
        this.sampleSize = sample.length;
        this.counts = new int[digitCount][radix];

        for (long key : sample) {
            checkKey(key);
            for (int pos = 0; pos < digitCount; pos++) {
                counts[pos][digitOf(key, pos)]++;
            }
        }

        this.uniformity = new double[digitCount];
        for (int pos = 0; pos < digitCount; pos++) {
            uniformity[pos] = normalizedEntropy(counts[pos]);
        }

        this.positions = selectPositions(maxCapacity);
        long space = power(radix, positions.length);
        if (space > MAX_ADDRESS_SPACE) {
            throw new IllegalArgumentException("选位后地址空间过大:" + space + "(上限 " + MAX_ADDRESS_SPACE
                    + "),请减少分析位数或降低基数,或改用除留余数法");
        }
        this.addressSpace = (int) space;
        this.buckets = newNodeArray(addressSpace);
        this.sampleCollisions = countSampleCollisions(sample);
    }

    /**
     * 散列函数:按选中的位号从高位到低位取出数字,拼成地址。
     *
     * @param key 关键字
     * @return 地址,落在 [0, addressSpace)
     * @throws IllegalArgumentException 关键字为负或超过 digitCount 位
     */
    public int hash(long key) {
        checkKey(key);
        int address = 0;
        for (int i = positions.length - 1; i >= 0; i--) {
            address = address * radix + digitOf(key, positions[i]);
        }
        return address;
    }

    /**
     * 插入或更新键值对。地址冲突时挂到同一个桶的链上。
     *
     * @param key   关键字
     * @param value 值,不能为 null
     */
    public void put(long key, Value value) {
        Objects.requireNonNull(value, "值不能为 null");
        int address = hash(key);
        for (Node<Value> node = buckets[address]; node != null; node = node.next) {
            if (node.key == key) {
                node.value = value;
                return;
            }
        }
        Node<Value> head = new Node<Value>(key, value);
        head.next = buckets[address];
        buckets[address] = head;
        n++;
    }

    /**
     * 查找关键字对应的值。
     *
     * @param key 关键字
     * @return 值;不存在时返回 null
     */
    public Value get(long key) {
        int address = hash(key);
        for (Node<Value> node = buckets[address]; node != null; node = node.next) {
            if (node.key == key) {
                return node.value;
            }
        }
        return null;
    }

    /**
     * 判断关键字是否存在。
     *
     * @param key 关键字
     * @return 存在返回 true
     */
    public boolean contains(long key) {
        int address = hash(key);
        for (Node<Value> node = buckets[address]; node != null; node = node.next) {
            if (node.key == key) {
                return true;
            }
        }
        return false;
    }

    /**
     * 删除关键字及其值。
     *
     * @param key 关键字
     * @return 被删除的值;不存在时返回 null
     */
    public Value delete(long key) {
        int address = hash(key);
        Node<Value> prev = null;
        Node<Value> node = buckets[address];
        while (node != null) {
            if (node.key == key) {
                if (prev == null) {
                    buckets[address] = node.next;
                } else {
                    prev.next = node.next;
                }
                n--;
                return node.value;
            }
            prev = node;
            node = node.next;
        }
        return null;
    }

    /**
     * @return 已存关键字个数
     */
    public int size() {
        return n;
    }

    /**
     * @return 散列表为空返回 true
     */
    public boolean isEmpty() {
        return n == 0;
    }

    /**
     * @return 基数(进制)
     */
    public int radix() {
        return radix;
    }

    /**
     * @return 参与分析的关键字位数
     */
    public int digitCount() {
        return digitCount;
    }

    /**
     * @return 选中的位号副本,升序(0 = 个位)
     */
    public int[] chosenPositions() {
        return positions.clone();
    }

    /**
     * @return 地址空间大小 = radix^选位数
     */
    public int addressSpace() {
        return addressSpace;
    }

    /**
     * 某一位上各数字出现的次数。
     *
     * @param position 位号,0 = 个位
     * @return 长度 radix 的计数数组副本
     */
    public int[] distribution(int position) {
        checkPosition(position);
        return counts[position].clone();
    }

    /**
     * 某一位的归一化熵:0 表示该位恒定不变(最差),1 表示各数字等频出现(最好)。
     *
     * @param position 位号,0 = 个位
     * @return 均匀度,取值 [0,1]
     */
    public double uniformity(int position) {
        checkPosition(position);
        return uniformity[position];
    }

    /**
     * @return 样本关键字个数
     */
    public int sampleSize() {
        return sampleSize;
    }

    /**
     * @return 样本中映射到同一地址的重复次数(越大说明选位效果越差)
     */
    public int sampleCollisionCount() {
        return sampleCollisions;
    }

    /**
     * @return 样本中不同地址的个数 = 样本量 − 冲突数
     */
    public int distinctAddressCount() {
        return sampleSize - sampleCollisions;
    }

    /**
     * @return 装填因子 alpha = n / addressSpace
     */
    public double loadFactor() {
        return (double) n / addressSpace;
    }

    /**
     * @return 最长链的长度(0 表示空表),拉链法下即最坏查找长度
     */
    public int maxChainLength() {
        int max = 0;
        for (Node<Value> bucket : buckets) {
            int len = 0;
            for (Node<Value> node = bucket; node != null; node = node.next) {
                len++;
            }
            if (len > max) {
                max = len;
            }
        }
        return max;
    }

    /**
     * 依次返回所有关键字(按地址升序,同桶内为最近插入优先)。
     *
     * @return 关键字集合
     */
    public Iterable<Long> keys() {
        List<Long> result = new ArrayList<Long>(n);
        for (Node<Value> bucket : buckets) {
            for (Node<Value> node = bucket; node != null; node = node.next) {
                result.add(node.key);
            }
        }
        return result;
    }

    /**
     * 按地址升序输出所有键值对:{key value, key value}。
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        String sep = "";
        for (Node<Value> bucket : buckets) {
            for (Node<Value> node = bucket; node != null; node = node.next) {
                sb.append(sep).append(node.key).append(' ').append(node.value);
                sep = ", ";
            }
        }
        return sb.append('}').toString();
    }

    // ---------------- 内部实现 ----------------

    /**
     * 按均匀度从高到低贪心选位:每位都会把地址空间乘以基数,一旦再选一位就超过上限就停止。
     * 至少选 1 位;均匀度相同时位号小者优先,保证结果可复现。
     */
    private int[] selectPositions(int maxCapacity) {
        Integer[] order = new Integer[digitCount];
        for (int i = 0; i < digitCount; i++) {
            order[i] = i;
        }
        Arrays.sort(order, new Comparator<Integer>() {
            public int compare(Integer a, Integer b) {
                int cmp = Double.compare(uniformity[b], uniformity[a]);
                return cmp != 0 ? cmp : Integer.compare(a, b);
            }
        });

        List<Integer> chosen = new ArrayList<Integer>();
        long space = 1;
        for (Integer pos : order) {
            if (!chosen.isEmpty() && space * radix > maxCapacity) {
                break;
            }
            chosen.add(pos);
            space *= radix;
        }

        int[] result = new int[chosen.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = chosen.get(i);
        }
        Arrays.sort(result);
        return result;
    }

    /**
     * 归一化熵(以基数为底):恒定不变位得 0,各数字等频出现得 1。
     */
    private double normalizedEntropy(int[] distribution) {
        double entropy = 0.0;
        for (int count : distribution) {
            if (count > 0) {
                double p = (double) count / sampleSize;
                entropy -= p * (Math.log(p) / Math.log(radix));
            }
        }
        return entropy;
    }

    /**
     * 统计样本映射后有多少个关键字与前面的关键字落在同一地址。
     */
    private int countSampleCollisions(long[] sample) {
        boolean[] used = new boolean[addressSpace];
        int collisions = 0;
        for (long key : sample) {
            int address = hashUnchecked(key);
            if (used[address]) {
                collisions++;
            } else {
                used[address] = true;
            }
        }
        return collisions;
    }

    /**
     * 不做合法性校验的地址计算(样本已在构造时校验过)。
     */
    private int hashUnchecked(long key) {
        int address = 0;
        for (int i = positions.length - 1; i >= 0; i--) {
            address = address * radix + digitOf(key, positions[i]);
        }
        return address;
    }

    /**
     * 取 key 第 position 位上的数字(0 = 个位)。
     */
    private int digitOf(long key, int position) {
        long value = key;
        for (int i = 0; i < position; i++) {
            value /= radix;
        }
        return (int) (value % radix);
    }

    private void checkKey(long key) {
        if (key < 0) {
            throw new IllegalArgumentException("数字分析法要求非负的数字编码,当前 key = " + key);
        }
        if (key >= keyLimit) {
            throw new IllegalArgumentException("key = " + key + " 超出 " + digitCount + " 位 " + radix
                    + " 进制可表示范围 [0, " + (keyLimit - 1) + "]");
        }
    }

    private void checkPosition(int position) {
        if (position < 0 || position >= digitCount) {
            throw new IllegalArgumentException("位号必须在 [0, " + (digitCount - 1) + "] 内,当前为 " + position);
        }
    }

    private static long power(int radix, int exponent) {
        long result = 1;
        for (int i = 0; i < exponent; i++) {
            result *= radix;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <Value> Node<Value>[] newNodeArray(int size) {
        return (Node<Value>[]) new Node[size];
    }

    /**
     * 演示用:直接取最高两位作地址时的实际分布(全部 8 位样本均为 "20xxxxxx")。
     */
    private static String naiveTopTwoDigits(long[] sample) {
        boolean[] used = new boolean[100];
        int collisions = 0;
        int distinct = 0;
        for (long key : sample) {
            int address = (int) ((key / 1000000L) % 100);
            if (used[address]) {
                collisions++;
            } else {
                used[address] = true;
                distinct++;
            }
        }
        return "地址空间 = 100,实际只用到 " + distinct + " 个地址,冲突 " + collisions + " 次";
    }

    /**
     * 演示:用 60 个 8 位学号做样本,观察各位分布、选位结果、地址空间与冲突,
     * 并与"直接取最高两位"的错误做法对照。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        System.out.println("数字分析法散列函数:从关键字各位中挑出分布均匀的位组成地址");
        System.out.println();

        // 样本:8 位学号 = "20" + 年级(21~24) + 专业(01~03,分布不均) + 序号(00~99,近似均匀)
        int[] majors = {1, 1, 2, 2, 2, 3};
        long[] sample = new long[60];
        for (int i = 0; i < sample.length; i++) {
            int grade = 21 + (i % 4);
            int major = majors[i % majors.length];
            int seq = (i * 7) % 100;
            sample[i] = Long.parseLong(String.format("20%02d%02d%02d", grade, major, seq));
        }

        DigitAnalysisHashST<String> st = new DigitAnalysisHashST<String>(sample, 8, 1000);
        System.out.println("样本:" + sample.length + " 个 8 位学号,例如 "
                + sample[0] + ", " + sample[1] + ", " + sample[29] + ";地址空间上限 1000");
        System.out.println();

        int[] chosen = st.chosenPositions();
        System.out.println("数位分布(位号 0 = 个位);分布为各数字 0~9 出现的次数");
        System.out.println("  位号   分布                                归一化熵   入选");
        for (int pos = st.digitCount() - 1; pos >= 0; pos--) {
            int[] dist = st.distribution(pos);
            StringBuilder sb = new StringBuilder();
            for (int d = 0; d < dist.length; d++) {
                sb.append(d == 0 ? "" : " ").append(dist[d]);
            }
            boolean picked = false;
            for (int p : chosen) {
                if (p == pos) {
                    picked = true;
                }
            }
            System.out.printf("  %-6d %-36s %-9.4f %s%n", pos, sb.toString(), st.uniformity(pos), picked ? "是" : "");
        }
        System.out.println();

        System.out.println("选位结果:" + Arrays.toString(chosen) + " → 地址空间 = " + st.addressSpace());
        System.out.println("样本映射:" + st.sampleSize() + " 个关键字落在 " + st.distinctAddressCount()
                + " 个不同地址,冲突 " + st.sampleCollisionCount() + " 次");
        System.out.println("对照:若不做分析、直接取最高的两位(第 7、6 位),"
                + naiveTopTwoDigits(sample) + "(这就是数字分析法要解决的问题)");
        System.out.println();

        System.out.println("散列地址示例:");
        System.out.println("  关键字        地址");
        for (int i = 0; i < 5; i++) {
            System.out.printf("  %-12d  %d%n", sample[i], st.hash(sample[i]));
        }
        System.out.println();

        for (long key : sample) {
            st.put(key, "学生" + key);
        }
        System.out.println("建表后:size = " + st.size() + ",装填因子 = " + String.format("%.4f", st.loadFactor())
                + ",最长链长 = " + st.maxChainLength());
        long probe = sample[3];
        System.out.println("查询演示:get(" + probe + ") = " + st.get(probe)
                + ",contains(" + sample[0] + ") = " + st.contains(sample[0])
                + ",delete(" + probe + ") = " + st.delete(probe)
                + ",删除后 size = " + st.size());
    }
}
