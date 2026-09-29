package cn.exercise.algs4.datastructure.heap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * 左式堆 meld vs 二叉堆逐条插入 —— N = 100,000 实测 demo
 *
 * 目的：把"合并两个优先队列"这一步在两种实现上的真实耗时差距跑出来。
 *
 * 同一批数据、同一台机器、同一批已装箱的 Integer 对象上对比三种做法：
 *   1) 左式堆        两个各 m 条的堆，调用一次 merge()            —— 理论 O(log n)
 *   2) 二叉堆·逐条插入  把 B 的 m 条逐条 insert 进 A 的堆          —— 理论 O(m log n)
 *   3) 二叉堆·拼数组+heapify  A、B 拼成一个数组后整体重建          —— 理论 O(n)，但要复制全部元素
 *
 * 计时纪律：
 *   - 先跑 2 轮预热让 JIT 稳定，再取 5 轮的最小值与中位数；
 *   - 每段计时前调用 System.gc()，并把二叉堆容量预留为 N，排除扩容复制带来的噪声；
 *   - 合并阶段不再新建元素对象（左式堆复用已有节点），保证三条路径在"已装箱数据"上对等；
 *   - 预热轮顺带做正确性校验：三种做法出堆都必须是非递增序列且元素总和一致。
 *
 * 运行方式（JDK 17，不需要 Maven、不联网；out/ 已被 .gitignore 覆盖）：
 *   javac -encoding UTF-8 -d out/leftist-benchmark-classes src/main/java/cn/exercise/algs4/datastructure/heap/LeftistHeap.java src/main/java/cn/exercise/algs4/datastructure/heap/MaxHeap.java src/test/java/cn/exercise/algs4/datastructure/heap/LeftistHeapMeldBenchmark.java
 *   java "-Dfile.encoding=UTF-8" "-Dsun.stdout.encoding=UTF-8" -cp out/leftist-benchmark-classes cn.exercise.algs4.datastructure.heap.LeftistHeapMeldBenchmark
 * （Windows 终端先 chcp 65001；PowerShell 下 -D 开头的参数要加引号，否则会被拆开）
 */
public class LeftistHeapMeldBenchmark {

    /** 总元素规模 */
    private static final int N = 100_000;
    /** 场景 2 的分片数 */
    private static final int SHARDS = 100;
    /** 正式计时轮数 */
    private static final int ROUNDS = 7;
    /** 预热轮数 */
    private static final int WARMUP = 2;
    /** 固定随机种子，保证结果可复现 */
    private static final long SEED = 20260929L;

    // ==================== 入口 ====================

    public static void main(String[] args) {
        title("左式堆 meld vs 二叉堆 —— N = " + N + " 实测");
        System.out.println("JVM        : " + System.getProperty("java.vm.name") + " "
                + System.getProperty("java.version") + " ("
                + System.getProperty("os.name") + "/" + System.getProperty("os.arch") + ", "
                + Runtime.getRuntime().availableProcessors() + " cores)");
        System.out.println("数据       : " + N + " 个 [0, 1000000) 的随机 Integer，固定种子 " + SEED);
        System.out.println("计时       : 每轮 gc 后分别计时，" + WARMUP + " 轮预热 + " + ROUNDS + " 轮取最小/中位");
        System.out.println();

        Integer[] data = new Integer[N];
        Random random = new Random(SEED);
        for (int i = 0; i < N; i++) {
            data[i] = random.nextInt(1_000_000);
        }

        scenarioTwoHeaps(data);
        scenarioShards(data);

        System.out.println();
        System.out.println("说明       : 左式堆 meld 走指针 + 递归、常数因子大；二叉堆数组连续、缓存友好。");
        System.out.println("             即便如此，合并这一步的差距仍是数量级级别 —— 因为数组二叉堆没有 merge，");
        System.out.println("             只能逐个插入 O(m log n)，或拼数组整体重建 O(n)（要额外复制全部元素）。");
    }

    // ==================== 场景 1：两个大堆合成一个 ====================

    private static void scenarioTwoHeaps(Integer[] data) {
        int m = N / 2;
        Integer[] a = Arrays.copyOfRange(data, 0, m);
        Integer[] b = Arrays.copyOfRange(data, m, N);

        section("场景 1：两个各 " + m + " 条的堆，合并成一个 " + N + " 条的堆");

        long[] meld = new long[ROUNDS];
        long[] insert = new long[ROUNDS];
        long[] heapify = new long[ROUNDS];

        for (int r = -WARMUP; r < ROUNDS; r++) {
            boolean record = r >= 0;

            // ---- 1) 左式堆：两个堆各自建好后，一次 meld ----
            LeftistHeap<Integer> la = new LeftistHeap<>(a);
            LeftistHeap<Integer> lb = new LeftistHeap<>(b);
            gc();
            long t0 = System.nanoTime();
            la.merge(lb);
            long t1 = System.nanoTime();
            require(la.size() == N, "左式堆 meld 后 size = " + la.size());
            require(lb.size() == 0, "被并入的左式堆应被清空，实际 size = " + lb.size());

            // ---- 2) 二叉堆：A 建堆（预留 N 容量，避免扩容复制干扰计时），再逐条插入 B ----
            MaxHeap<Integer> bin = new MaxHeap<>(N);
            for (Integer x : a) {
                bin.insert(x);
            }
            gc();
            long t2 = System.nanoTime();
            for (Integer x : b) {
                bin.insert(x);
            }
            long t3 = System.nanoTime();
            require(bin.size() == N, "二叉堆逐条插入后 size = " + bin.size());

            // ---- 3) 二叉堆：拼数组 + heapify 一次性重建 ----
            Integer[] merged = new Integer[N];
            System.arraycopy(a, 0, merged, 0, m);
            System.arraycopy(b, 0, merged, m, m);
            gc();
            long t4 = System.nanoTime();
            MaxHeap<Integer> rebuilt = new MaxHeap<>(merged);
            long t5 = System.nanoTime();
            require(rebuilt.size() == N, "heapify 重建后 size = " + rebuilt.size());

            if (record) {
                meld[r] = t1 - t0;
                insert[r] = t3 - t2;
                heapify[r] = t5 - t4;
            } else if (r == -WARMUP) {
                long expected = sum(a) + sum(b);
                checkDrain("左式堆 meld 结果", la, expected);
                checkDrain("二叉堆逐条插入结果", bin, expected);
                checkDrain("二叉堆拼数组+heapify 结果", rebuilt, expected);
                System.out.println("正确性校验 : 三种做法出堆均为非递增序列，元素总和一致 = " + expected + "  [OK]");
                System.out.println();
            }
        }

        long meldMin = min(meld);
        long insertMin = min(insert);
        long heapifyMin = min(heapify);

        System.out.println("方法" + pad("", 26) + "| 最小" + pad("", 12) + "| 中位");
        System.out.println("-----------------------------------|----------------|----------------");
        line("左式堆  一次 merge()", meld);
        line("二叉堆  逐条插入 " + fmtInt(m) + " 条", insert);
        line("二叉堆  拼数组 + heapify", heapify);
        System.out.println();
        System.out.println("操作数对比 : 一次 meld 沿最右路径 O(log2 n) ≈ " + round1(log2(N)) + " 步；"
                + "逐条插入 ≈ m x log2 n ≈ " + fmtInt(Math.round(m * log2(N))) + " 次比较");
        System.out.println("加速比     : 逐条插入 / 一次 meld = " + ratio(insertMin, meldMin) + " (最小) / "
                + ratio(median(insert), median(meld)) + " (中位)"
                + "；逐条插入 / 拼数组heapify = " + ratio(insertMin, heapifyMin) + " (最小)");
        System.out.println();
    }

    // ==================== 场景 2：分片陆续并进总堆 ====================

    private static void scenarioShards(Integer[] data) {
        int per = N / SHARDS;

        section("场景 2：" + SHARDS + " 个分片各 " + per + " 条，合并进同一个总堆");

        long[] meldOnly = new long[ROUNDS];
        long[] meldTotal = new long[ROUNDS];
        long[] insert = new long[ROUNDS];

        for (int r = -WARMUP; r < ROUNDS; r++) {
            boolean record = r >= 0;

            // 分片堆先行建好（左式堆侧），其建造成本单独计一列
            List<LeftistHeap<Integer>> shards = new ArrayList<>(SHARDS);
            for (int s = 0; s < SHARDS; s++) {
                shards.add(new LeftistHeap<>(Arrays.copyOfRange(data, s * per, (s + 1) * per)));
            }

            // ---- 1) 只测合并阶段：100 个分片堆已经在手，做 100 次 meld ----
            gc();
            LeftistHeap<Integer> total = new LeftistHeap<>();
            long t0 = System.nanoTime();
            for (int s = 0; s < SHARDS; s++) {
                total.merge(shards.get(s));
            }
            long t1 = System.nanoTime();
            require(total.size() == N, "分片 meld 后 size = " + total.size());

            // ---- 2) 含建堆的口径：重新建 100 个分片堆 + 100 次 meld ----
            gc();
            long t2 = System.nanoTime();
            LeftistHeap<Integer> total2 = new LeftistHeap<>();
            for (int s = 0; s < SHARDS; s++) {
                total2.merge(new LeftistHeap<>(Arrays.copyOfRange(data, s * per, (s + 1) * per)));
            }
            long t3 = System.nanoTime();
            require(total2.size() == N, "分片 meld 后 size = " + total2.size());

            // ---- 3) 二叉堆：没有 merge，除了一次性重建就只能把 N 条重新 insert ----
            MaxHeap<Integer> bin = new MaxHeap<>(N);
            gc();
            long t4 = System.nanoTime();
            for (int i = 0; i < N; i++) {
                bin.insert(data[i]);
            }
            long t5 = System.nanoTime();
            require(bin.size() == N, "二叉堆插入后 size = " + bin.size());

            if (record) {
                meldOnly[r] = t1 - t0;
                meldTotal[r] = t3 - t2;
                insert[r] = t5 - t4;
            } else if (r == -WARMUP) {
                long expected = sum(data);
                checkDrain("左式堆合并阶段结果", total, expected);
                checkDrain("左式堆含建堆结果", total2, expected);
                checkDrain("二叉堆逐条插入结果", bin, expected);
                System.out.println("正确性校验 : 各做法出堆均为非递增序列，元素总和一致 = " + expected + "  [OK]");
                System.out.println();
            }
        }

        long meldMin = min(meldOnly);
        long insertMin = min(insert);

        System.out.println("方法" + pad("", 26) + "| 最小" + pad("", 12) + "| 中位");
        System.out.println("-----------------------------------|----------------|----------------");
        line("左式堆  合并阶段(100 次 meld)", meldOnly);
        line("左式堆  含建堆(建分片 + 合并)", meldTotal);
        line("二叉堆  重新插入 " + fmtInt(N) + " 条", insert);
        System.out.println();
        System.out.println("加速比     : 重新插入 / 合并阶段 = " + ratio(insertMin, meldMin) + " (最小) / "
                + ratio(median(insert), median(meldOnly)) + " (中位)");
        System.out.println("参考       : 把建各分片堆也算进来后，" + fmt(min(meldTotal)) + " (左式堆) vs "
                + fmt(insertMin) + " (二叉堆) ——");
        System.out.println("             这一量级上，构造的常数因子(节点分配 + 指针跳转)会把合并收益吃掉；");
        System.out.println("             左式堆的优势只在\"堆已存在、合并本身占主导\"时兑现，即场景 1 那类用法。");
        System.out.println();
    }

    // ==================== 输出工具 ====================

    private static void title(String s) {
        System.out.println("================ " + s + " ================");
    }

    private static void section(String s) {
        System.out.println("---- " + s + " ----");
    }

    private static void line(String label, long[] times) {
        System.out.println(pad(label, 35) + "| " + lpad(fmt(min(times)), 14) + " | " + lpad(fmt(median(times)), 14));
    }

    /** 计时结果格式化：小于 1ms 用 us，否则用 ms */
    private static String fmt(long nanos) {
        return nanos < 1_000_000L
                ? String.format("%.3f us", nanos / 1_000.0)
                : String.format("%.3f ms", nanos / 1_000_000.0);
    }

    private static String fmtInt(long v) {
        return String.format("%,d", v);
    }

    private static String ratio(long base, long other) {
        return String.format("%.1fx", (double) base / other);
    }

    private static double log2(int n) {
        return Math.log(n) / Math.log(2);
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    /** 按终端显示宽度补齐（CJK 记 2 列） */
    private static int width(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += s.charAt(i) > 0x2E7F ? 2 : 1;
        }
        return w;
    }

    private static String pad(String s, int w) {
        StringBuilder sb = new StringBuilder(s);
        for (int i = width(s); i < w; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String lpad(String s, int w) {
        StringBuilder sb = new StringBuilder();
        for (int i = width(s); i < w; i++) {
            sb.append(' ');
        }
        return sb.append(s).toString();
    }

    // ==================== 统计与校验 ====================

    private static long min(long[] a) {
        long m = Long.MAX_VALUE;
        for (long v : a) {
            m = Math.min(m, v);
        }
        return m;
    }

    private static long median(long[] a) {
        long[] copy = a.clone();
        Arrays.sort(copy);
        return copy[copy.length / 2];
    }

    private static long sum(Integer[] a) {
        long s = 0;
        for (int v : a) {
            s += v;
        }
        return s;
    }

    /** 一路出堆，校验非递增，并返回总和 */
    private static long drainAndCheck(String label, Supplier<Integer> poll, BooleanSupplier empty) {
        long sum = 0;
        int count = 0;
        int prev = Integer.MAX_VALUE;
        while (!empty.getAsBoolean()) {
            int v = poll.get();
            require(v <= prev, label + " 出堆顺序不是非递增：" + prev + " -> " + v);
            prev = v;
            sum += v;
            count++;
        }
        require(count == N, label + " 出堆元素个数 = " + count + "，应为 " + N);
        return sum;
    }

    private static void checkDrain(String label, LeftistHeap<Integer> heap, long expected) {
        long sum = drainAndCheck(label, heap::delMax, heap::isEmpty);
        require(sum == expected, label + " 元素总和 = " + sum + "，应为 " + expected);
    }

    private static void checkDrain(String label, MaxHeap<Integer> heap, long expected) {
        long sum = drainAndCheck(label, heap::delMax, heap::isEmpty);
        require(sum == expected, label + " 元素总和 = " + sum + "，应为 " + expected);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("断言失败: " + message);
        }
    }

    private static void gc() {
        System.gc();
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
