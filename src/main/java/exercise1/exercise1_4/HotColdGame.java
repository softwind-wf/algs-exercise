package exercise1.exercise1_4;

import java.util.Scanner;

public class HotColdGame {
    private static int secret; // 秘密数
    private static int count; // 猜测次数

    // 模拟反馈：返回热/冷/相等
    private static String feedback(int guess, int lastGuess) {
        count++;
        if (guess == secret) return "equal";
        int currDist = Math.abs(guess - secret);
        int lastDist = Math.abs(lastGuess - secret);
        if (currDist < lastDist) return "hot";
        else return "cold";
    }

    // 算法1：2lgN 双向二分
    public static int solve2logN(int N) {
        count = 0;
        int lo = 1, hi = N;
        int g1 = lo, g2 = hi;

        while (true) {
            String f1 = feedback(g1, g2);
            if (f1.equals("equal")) return g1;

            String f2 = feedback(g2, g1);
            if (f2.equals("equal")) return g2;

            int mid = (lo + hi) / 2;
            if (f1.equals("hot")) hi = mid;
            else if (f2.equals("hot")) lo = mid;
            else return mid; // 冷热相同，直接是中点

            g1 = lo;
            g2 = hi;
        }
    }

    // 算法2：lgN 对称点二分
    public static int solveLogN(int N) {
        count = 0;
        int lo = 1, hi = N;
        // 第一次猜测任意值，这里选 lo
        int guess = lo; 
        // 需要一个“上次猜测”来配合 feedback 计算距离变化。
        // 对于第一次猜测，我们可以构造一个虚拟的 lastGuess，使得逻辑成立，
        // 或者先进行一次特殊处理。更优雅的方式是：
        // 既然 feedback 需要 (current, last)，我们在循环外先做第一次猜测。
        
        // 初始猜测
        String f = feedback(guess, guess + (lo == 1 ? 1 : -1)); // 虚构一个 lastGuess，只要不相等即可，因为第一次无法比较冷热，只能看是否相等
        if (f.equals("equal")) return guess;

        while (true) {

            // 核心逻辑：根据上一次反馈 f 决定下一次猜哪里。
            // 如果上次是 "hot" (guess 比 lastGuess 更接近 secret)，说明 secret 在 guess 和 lastGuess 之间吗？
            // 不完全是。Hot/Cold 游戏的标准 lgN 解法是利用对称点。
            // 假设上次猜测是 g_prev，当前猜测是 g_curr。
            // 如果 feedback(g_curr, g_prev) == "hot"，说明 |g_curr - secret| < |g_prev - secret|。
            // 我们想要利用这个信息缩小区间。
            
            // 修正后的逻辑：
            // 维护区间 [lo, hi]。
            // 每次猜测 mid = (lo + hi) / 2 ? 不，标准的 lgN 算法是这样的：
            // 1. 猜 x。
            // 2. 猜 y = 2*mid - x (关于 mid 的对称点)。
            // 3. 比较 feedback(y, x)。
            //    - 如果 y 是 "hot" (比 x 热)，说明 secret 离 y 更近。由于 y 和 x 关于 mid 对称，
            //      这意味着 secret 和 y 在 mid 的同侧（或者说 secret 不在 x 那一侧的远处）。
            //      实际上，如果 y 比 x 热，说明 secret 在 (mid, hi] (假设 y > x) 或 [lo, mid) (假设 y < x)?
            //      让我们推导一下：
            //      设 secret = S, mid = M, x, y = 2M - x.
            //      |y - S| < |x - S|
            //      |2M - x - S| < |x - S|
            //      平方：(2M - x - S)^2 < (x - S)^2
            //      令 A = x - S. 则 (2M - 2S - A)^2 < A^2 ... 这种推导太复杂。
            
            // 简单直观的理解：
            // 如果 guess (新) 比 lastGuess (旧) 热，说明秘密数字离 guess 更近。
            // 在二分策略中，我们通常猜测 mid。然后根据反馈决定去左半区还是右半区。
            // 但这里的 feedback 机制比较特殊。
            
            // 重新实现标准的 lgN 策略：
            // 保持一个候选区间 [lo, hi]。
            // 每次猜测 mid = (lo + hi) / 2。
            // 为了判断方向，我们需要另一个点。
            // 上一次的猜测是 `guess`。现在的候选中点是 `mid`。
            // 我们猜测 `newGuess` = 2 * mid - guess (关于 mid 对称)。
            // 调用 feedback(newGuess, guess)。
            // 如果结果是 "hot" (newGuess 更近):
            //    说明 secret 离 newGuess 更近。因为 newGuess 和 guess 关于 mid 对称，
            //    这暗示 secret 位于包含 newGuess 的那一半区间。
            //    如果 newGuess > mid (即 guess < mid)，则 secret 在 (mid, hi]。
            //    如果 newGuess < mid (即 guess > mid)，则 secret 在 [lo, mid)。
            //    总结：如果 "hot", 区间缩小为包含 newGuess 的那一半。
            // 如果结果是 "cold" (newGuess 更远):
            //    说明 secret 离 guess 更近。
            //    区间缩小为包含 guess 的那一半。
            // 如果结果是 "equal": 找到。
            
            // 让我们用变量追踪：
            // 当前猜测是 `guess` (这是上一轮确定的点，或者是初始点)。
            // 我们想测试中点 `mid` 附近的区域。
            // 实际上，算法应该是：
            // 1. 初始 guess = lo (或任意值)。
            // 2. Loop:
            //    mid = (lo + hi) / 2
            //    nextGuess = 2 * mid - guess
            //    res = feedback(nextGuess, guess)
            //    if res == "equal" return nextGuess
            //    if res == "hot": 
            //       // nextGuess 更近。
            //       // 如果 nextGuess > mid, 说明 secret 在右边 (mid, hi] -> lo = mid + 1? 或者 lo = mid?
            //       // 如果 nextGuess < mid, 说明 secret 在左边 [lo, mid) -> hi = mid?
            //       // 注意：nextGuess 和 guess 关于 mid 对称。
            //       // 若 nextGuess > mid, 则 guess < mid. "hot" 意味着 secret 靠近 nextGuess (右侧)。
            //       // 所以 secret > mid. 新区间 [mid+1, hi]? 或者 (mid, hi]. 由于是整数，lo = mid + 1.
            //       // 若 nextGuess < mid, 则 guess > mid. "hot" 意味着 secret 靠近 nextGuess (左侧)。
            //       // 所以 secret < mid. 新区间 [lo, mid-1].
            //    if res == "cold":
            //       // guess 更近。
            //       // 若 nextGuess > mid (guess < mid). "cold" 意味着 secret 靠近 guess (左侧)。
            //       // secret < mid. hi = mid - 1.
            //       // 若 nextGuess < mid (guess > mid). "cold" 意味着 secret 靠近 guess (右侧)。
            //       // secret > mid. lo = mid + 1.
            
            // 等等，上面的逻辑有一个问题：feedback 的定义。
            // feedback(curr, last): curr 更近返回 "hot"。
            // 我们的调用是 feedback(nextGuess, guess)。
            // 所以 "hot" = nextGuess 更近。
            
            // 让我们简化区间更新逻辑：
            // 无论 guess 在哪，nextGuess 总是关于 mid 对称。
            // 情况 A: guess < mid => nextGuess > mid.
            //    "hot" (nextGuess 近) => secret 在右侧 (mid, hi]. lo = mid + 1? 
            //       考虑到边界，如果 secret == mid 呢？
            //       如果 secret == mid, 那么 |nextGuess - mid| = |guess - mid|. 距离相等。
            //       此时 feedback 应该返回 "cold" (因为 currDist < lastDist 才是 hot, 相等走 else -> cold).
            //       代码中：if (currDist < lastDist) return "hot"; else return "cold";
            //       所以如果距离相等，返回 "cold"。
            //       如果 secret == mid, dist(next) == dist(guess). 返回 "cold"。
            //       按 "cold" 逻辑处理：guess 更近 (或相等)。guess < mid. 我们认为 secret 在左侧 [lo, mid].
            //       这样 mid 就被保留了。
            //    所以：
            //    如果 guess < mid:
            //       "hot" -> secret 在 (mid, hi] -> lo = mid + 1 (或者 lo = mid? 不，mid 已经被排除了吗？如果不相等，mid 肯定不是 secret，因为距离相等会报 cold)。
            //          等一下，如果 secret == mid, dist 相等 -> cold.
            //          如果 secret != mid, 且 nextGuess 更近，说明 secret 偏向 nextGuess 一侧，即 > mid.
            //          所以 lo = mid + 1.
            //       "cold" -> secret 在 [lo, mid] -> hi = mid.
            //    如果 guess > mid:
            //       nextGuess < mid.
            //       "hot" -> secret 在 [lo, mid) -> hi = mid - 1? 或者 hi = mid.
            //          同理，如果 secret == mid -> dist 相等 -> cold.
            //          所以 "hot" 意味着 secret < mid. hi = mid - 1.
            //       "cold" -> secret 在 [mid, hi] -> lo = mid.
            
            // 这个逻辑有点复杂，而且依赖于 guess 和 mid 的关系。
            // 有没有更统一的写法？
            // 其实可以这样：
            // 如果 "hot" (nextGuess 更近): 秘密在 nextGuess 这一侧。
            //    如果 nextGuess > mid, 秘密在 (mid, hi]. lo = mid + 1.
            //    如果 nextGuess < mid, 秘密在 [lo, mid). hi = mid - 1.
            // 如果 "cold" (guess 更近或相等): 秘密在 guess 这一侧。
            //    如果 nextGuess > mid (即 guess < mid), 秘密在 [lo, mid]. hi = mid.
            //    如果 nextGuess < mid (即 guess > mid), 秘密在 [mid, hi]. lo = mid.
            
            // 还需要处理一种情况：如果 feedback 返回 "equal"，直接返回。
            
            // 让我们重写循环体：
            
            int mid = (lo + hi) / 2;
            if (lo > hi) return -1; // 不应该发生，除非出错
            
            // 防止死循环：如果 lo == hi, 直接猜 lo
            if (lo == hi) {
                String fFinal = feedback(lo, guess);
                if (fFinal.equals("equal")) return lo;
                // 理论上不会走到这，因为一定有解
                return lo; 
            }

            int nextGuess = 2 * mid - guess;
            
            // 确保 nextGuess 在合理范围内？不一定，对称点可能越界，但数学上没问题。
            // 不过为了安全，如果 nextGuess 越界，可能需要调整策略，但在二分逻辑中，只要区间正确，对称点应该能工作。
            // 实际上，如果 guess 在 [lo, hi] 内，mid 也在，nextGuess 可能在外面。
            // 但 feedback 函数只关心数值差，不关心范围。所以没关系。
            
            String res = feedback(nextGuess, guess);
            
            if (res.equals("equal")) return nextGuess;
            
            if (res.equals("hot")) {
                // nextGuess 更近
                if (nextGuess > mid) {
                    // secret 在右侧
                    lo = mid + 1;
                } else {
                    // secret 在左侧
                    hi = mid - 1;
                }
            } else {
                // cold: guess 更近 (或相等)
                if (nextGuess > mid) {
                    // 意味着 guess < mid. secret 在左侧 (包含 mid)
                    hi = mid;
                } else {
                    // 意味着 guess > mid. secret 在右侧 (包含 mid)
                    lo = mid;
                }
            }
            
            guess = nextGuess;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("输入N (范围1~N): ");
        int N = sc.nextInt();
        secret = (int) (Math.random() * N) + 1;
        System.out.println("秘密数已生成 (测试用，实际隐藏)");

        // 测试算法1
        int res1 = solve2logN(N);
        System.out.printf("算法1 (2lgN) 结果: %d, 猜测次数: %d\n", res1, count);

        // 重置并测试算法2
        secret = (int) (Math.random() * N) + 1;
        int res2 = solveLogN(N);
        System.out.printf("算法2 (lgN) 结果: %d, 猜测次数: %d\n", res2, count);

        sc.close();
    }
}