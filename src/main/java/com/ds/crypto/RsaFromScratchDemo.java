package com.ds.crypto;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

/**
 * RSA 原理演示：从 61×53 的手算，到真实 2048 位密钥。
 *
 * <p>每一段都打印中间结果，方便对照手算：
 * <ol>
 *   <li>小素数全流程：n、φ(n)、e、d、加密、解密，全部数字打印出来；</li>
 *   <li>为什么 d 能把 m 还原：欧拉定理 + e·d ≡ 1 (mod φ(n)) 的推导，并实测 m^φ(n) ≡ 1；</li>
 *   <li>进阶：m 与 n 不互素时欧拉定理不成立，但 RSA 仍然正确（中国剩余定理）；</li>
 *   <li>扩展欧几里得：怎么从 e 和 φ(n) 算出 d；</li>
 *   <li>真实 2048 位：密钥生成耗时、CRT 加速约 4 倍的实测；</li>
 *   <li>φ(n) 就是秘密：知道 φ 就能算出 d，所以安全性最终落在"分解 n 难"；</li>
 *   <li>加解密往返 + 签名只是把指数换方向。</li>
 * </ol>
 *
 * <p>运行：{@code java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp target\rsa-classes
 * com.ds.crypto.RsaFromScratchDemo}
 */
public final class RsaFromScratchDemo {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static int passed;
    private static int failed;

    private RsaFromScratchDemo() {
    }

    public static void main(String[] args) throws Exception {
        section1SmallRsa();
        section2WhyItWorks();
        section3NotCoprime();
        section4Bezout();
        section5RealKey();
        section6PhiIsSecret();
        section7RoundTripAndSign();

        System.out.println();
        System.out.println("自检：通过 " + passed + " 项，失败 " + failed + " 项");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ==================================================================
    // 1. 小素数全流程
    // ==================================================================
    private static void section1SmallRsa() {
        title(1, "小素数全流程：p=61, q=53（所有数字都能手算）");
        BigInteger p = BigInteger.valueOf(61);
        BigInteger q = BigInteger.valueOf(53);
        BigInteger n = p.multiply(q);
        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        BigInteger e = BigInteger.valueOf(17);
        BigInteger d = e.modInverse(phi);

        System.out.println("第 1 步 选两个素数：p = 61，q = 53");
        System.out.println("第 2 步 模数 n = p*q = " + n + "（这是公开的，2048 位密钥就是 2048 位的 n）");
        System.out.println("第 3 步 欧拉函数 φ(n) = (p-1)(q-1) = " + phi + "（这是秘密，等价于 p、q 本身）");
        System.out.println("第 4 步 公钥指数 e = " + e + "（通常取 65537，这里为了手算取小的）");
        System.out.println("第 5 步 私钥指数 d = e^-1 mod φ(n) = " + d + "（e 在模 φ 下的乘法逆元）");
        System.out.println("       验证 e*d = " + e.multiply(d) + " = " + e.multiply(d).divide(phi)
                + " * 3120 + 1，即 e*d ≡ 1 (mod φ(n))");
        check(e.multiply(d).mod(phi).equals(BigInteger.ONE), "e*d ≡ 1 (mod φ(n))");
        System.out.println("公钥 (n, e) = (" + n + ", " + e + ")");
        System.out.println("私钥 (n, d) = (" + n + ", " + d + ")");

        BigInteger m = BigInteger.valueOf(65);            // 'A'
        System.out.println();
        System.out.println("第 6 步 加密：c = m^e mod n，m = " + m + "（'A'）");
        BigInteger c = powTrace(m, e, n, "  m^e");
        System.out.println("  => 密文 c = " + c);
        check(c.equals(BigInteger.valueOf(2790)), "65^17 mod 3233 = 2790");

        System.out.println();
        System.out.println("第 7 步 解密：m' = c^d mod n，c = " + c);
        BigInteger recovered = powTrace(c, d, n, "  c^d");
        System.out.println("  => 明文 m' = " + recovered + "（'A'）");
        check(recovered.equals(m), "c^d mod n 还原出原始 m");
        System.out.println();
        System.out.println("整条链路：m -> m^e -> c -> c^d -> m。中间没有任何\"查表\"，全靠模幂运算。");
    }

    // ==================================================================
    // 2. 为什么 d 能把 m 还原
    // ==================================================================
    private static void section2WhyItWorks() {
        title(2, "为什么 d 能把 m 还原：欧拉定理 + e·d ≡ 1");
        BigInteger p = BigInteger.valueOf(61);
        BigInteger q = BigInteger.valueOf(53);
        BigInteger n = p.multiply(q);
        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        BigInteger e = BigInteger.valueOf(17);
        BigInteger d = e.modInverse(phi);

        System.out.println("要证：c^d ≡ m (mod n)，其中 c = m^e。");
        System.out.println("c^d = (m^e)^d = m^(e*d)");
        BigInteger ed = e.multiply(d);
        BigInteger k = ed.subtract(BigInteger.ONE).divide(phi);
        System.out.println("e*d ≡ 1 (mod φ(n))  ==>  e*d = 1 + k*φ(n)，这里 e*d = " + ed
                + " = 1 + " + k + "*" + phi);
        check(ed.equals(BigInteger.ONE.add(k.multiply(phi))), "e*d 确实等于 1 + k*φ(n)（k = " + k + "）");
        System.out.println("所以 m^(e*d) = m^(1 + k*φ(n)) = m * (m^φ(n))^k");
        System.out.println("  这一步只是指数法则：m^(A+B) = m^A * m^B，m^(k*φ) = (m^φ)^k。");
        System.out.println();
        System.out.println("欧拉定理：gcd(m, n) = 1 时，m^φ(n) ≡ 1 (mod n)。");
        System.out.println("代入：(m^φ(n))^k ≡ 1^k = 1，所以 m * (m^φ(n))^k ≡ m (mod n)。证毕。");
        System.out.println();
        System.out.println("—— 用小到能手算的数字走一遍（n=7, φ(7)=6, m=2, k=2）——");
        BigInteger n7 = BigInteger.valueOf(7);
        BigInteger phi7 = BigInteger.valueOf(6);
        BigInteger m2 = BigInteger.valueOf(2);
        System.out.println("  2^6 mod 7 = " + m2.modPow(phi7, n7) + "（欧拉定理）");
        System.out.println("  2^(1+2*6) = 2^13 = " + m2.pow(13) + " = 2*(2^6)^2 = 2*" + m2.pow(6) + "^2");
        System.out.println("  按 m*(m^φ)^k 算：2 * 1^2 = 2");
        System.out.println("  直接算 2^13 mod 7 = " + m2.modPow(BigInteger.valueOf(13), n7));
        check(m2.modPow(BigInteger.valueOf(13), n7).equals(BigInteger.valueOf(2)),
                "2^13 mod 7 = 2，两种算法一致");
        System.out.println();
        System.out.println("—— 换成 RSA 的数字（m=65, n=3233, φ=3120, e*d=46801, k=15）——");
        BigInteger m65 = BigInteger.valueOf(65);
        System.out.println("  m^φ mod n = " + m65.modPow(phi, n) + "（欧拉定理）");
        System.out.println("  m·(m^φ)^k = 65 * 1^15 = 65");
        System.out.println("  直接算 65^46801 mod 3233 = " + m65.modPow(ed, n));
        check(m65.modPow(ed, n).equals(m65), "65^46801 mod 3233 = 65，与推导一致");
        System.out.println();
        System.out.println("实测 m^φ(n) mod n：" );
        BigInteger[] samples = {BigInteger.valueOf(2), BigInteger.valueOf(3),
                BigInteger.valueOf(65), BigInteger.valueOf(1000)};
        boolean allOne = true;
        for (BigInteger m : samples) {
            BigInteger value = m.modPow(phi, n);
            allOne &= value.equals(BigInteger.ONE);
            System.out.println("  m = " + m + " -> m^3120 mod 3233 = " + value);
        }
        check(allOne, "gcd(m,n)=1 的样本都满足 m^φ(n) ≡ 1 (mod n)");
        System.out.println();
        System.out.println("注意：这个推导默认了 m 与 n 互素。RSA 实际上对所有 m 都成立，见第 3 段。");
    }

    // ==================================================================
    // 3. m 与 n 不互素时
    // ==================================================================
    private static void section3NotCoprime() {
        title(3, "进阶：m 与 n 不互素时欧拉定理失效，但 RSA 仍然正确");
        BigInteger p = BigInteger.valueOf(61);
        BigInteger q = BigInteger.valueOf(53);
        BigInteger n = p.multiply(q);
        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        BigInteger e = BigInteger.valueOf(17);
        BigInteger d = e.modInverse(phi);

        BigInteger m = p;   // 故意选 m = p，gcd(m, n) = 61 ≠ 1
        BigInteger mPhi = m.modPow(phi, n);
        BigInteger c = m.modPow(e, n);
        BigInteger recovered = c.modPow(d, n);
        System.out.println("取 m = p = 61（与 n 不互素，欧拉定理不适用）：");
        System.out.println("  m^φ(n) mod n = " + mPhi + " ≠ 1（欧拉定理确实不成立）");
        System.out.println("  但 m^(e*d) mod n = " + recovered + "，依然还原");
        check(recovered.equals(m), "不互素时 RSA 依然正确（靠中国剩余定理）");
        System.out.println();
        System.out.println("原因：分别看模 p 和模 q。");
        System.out.println("  mod p：m ≡ 0，两边都是 0；");
        System.out.println("  mod q：m 与 q 互素，欧拉定理成立，两边都是 m；");
        System.out.println("  两个同余式合起来（CRT），就对所有 m 成立。");

        System.out.println();
        System.out.println("—— CRT 最小例子：mod 3 和 mod 5 拼出 mod 15 ——");
        System.out.println("已知 x mod 3 = 2，x mod 5 = 4。x 只可能在 0..14（3*5=15 一循环）：");
        StringBuilder candidates = new StringBuilder();
        for (int x = 0; x < 15; x++) {
            if (x % 3 == 2) {
                candidates.append(x).append(" ");
            }
        }
        System.out.println("  满足 x mod 3 = 2 的候选：" + candidates.toString().trim());
        System.out.println("  其中 x mod 5 = 4 的只有 14  =>  x = 14 (mod 15)");
        boolean[] seenPairs = new boolean[15];
        int distinct = 0;
        for (int x = 0; x < 15; x++) {
            int pair = (x % 3) * 5 + (x % 5);
            if (!seenPairs[pair]) {
                seenPairs[pair] = true;
                distinct++;
            }
        }
        check(distinct == 15, "0..14 的 15 个数给出 15 组互不相同的 (mod 3, mod 5)（一一对应）");

        BigInteger bigM = BigInteger.valueOf(15);
        BigInteger m1 = BigInteger.valueOf(5);                 // M / 3
        BigInteger y1 = m1.modInverse(BigInteger.valueOf(3));  // 5^-1 mod 3 = 2
        BigInteger m2 = BigInteger.valueOf(3);                 // M / 5
        BigInteger y2 = m2.modInverse(BigInteger.valueOf(5));  // 3^-1 mod 5 = 2
        BigInteger combined = BigInteger.valueOf(2).multiply(m1).multiply(y1)
                .add(BigInteger.valueOf(4).multiply(m2).multiply(y2))
                .mod(bigM);
        System.out.println("  构造法：零件1 = 5 * 5^-1 mod 3 = 5*" + y1 + " = " + m1.multiply(y1)
                + "（mod 3 为 1，mod 5 为 0）");
        System.out.println("          零件2 = 3 * 3^-1 mod 5 = 3*" + y2 + " = " + m2.multiply(y2)
                + "（mod 3 为 0，mod 5 为 1）");
        System.out.println("          x = 2*" + m1.multiply(y1) + " + 4*" + m2.multiply(y2)
                + " = " + combined + " (mod 15)");
        check(combined.equals(BigInteger.valueOf(14)), "构造法同样得到 14");

        System.out.println();
        System.out.println("—— 用同样的办法拼出 RSA 里的 1220 ——");
        BigInteger partP = q.multiply(q.modInverse(p));   // 模 p 为 1，模 q 为 0
        BigInteger partQ = p.multiply(p.modInverse(q));   // 模 q 为 1，模 p 为 0
        System.out.println("  零件1（管 mod 61）：53 * 53^-1 mod 61 = 53 * " + q.modInverse(p)
                + " = " + partP + "，它 mod 61 = " + partP.mod(p) + "，mod 53 = " + partP.mod(q));
        System.out.println("  零件2（管 mod 53）：61 * 61^-1 mod 53 = 61 * " + p.modInverse(q)
                + " = " + partQ + "，它 mod 61 = " + partQ.mod(p) + "，mod 53 = " + partQ.mod(q));
        System.out.println("  x ≡ 0 (mod 61), x ≡ 1 (mod 53)  =>  x = 0*" + partP + " + 1*" + partQ
                + " = " + partQ.mod(n));
        check(partQ.mod(n).equals(BigInteger.valueOf(1220)), "拼出的 x = 1220");

        System.out.println();
        System.out.println("—— CRT 加速私钥：在 p、q 上各算一半，再拼回去 ——");
        BigInteger cipher = BigInteger.valueOf(2790);
        BigInteger half1 = cipher.modPow(d, p);   // = 65 mod 61 = 4
        BigInteger half2 = cipher.modPow(d, q);   // = 65 mod 53 = 12
        BigInteger qInv = q.modInverse(p);
        BigInteger h = qInv.multiply(half1.subtract(half2)).mod(p);
        BigInteger glued = half2.add(h.multiply(q));
        System.out.println("  m1 = 2790^2753 mod 61 = " + half1 + "（= 65 mod 61）");
        System.out.println("  m2 = 2790^2753 mod 53 = " + half2 + "（= 65 mod 53）");
        System.out.println("  qInv = 53^-1 mod 61 = " + qInv
                + "，h = qInv*(m1-m2) mod 61 = " + qInv + "*(" + half1 + "-" + half2 + ") mod 61 = " + h);
        System.out.println("  m = m2 + h*q = " + half2 + " + " + h + "*53 = " + glued);
        check(glued.equals(BigInteger.valueOf(65)), "两个半长运算拼回同一个 m=65");
        check(glued.equals(cipher.modPow(d, n)), "与直接算 c^d mod n 的结果一致");
        System.out.println("实现上，CRT 也正是私钥运算加速的原理（第 5 段）。");
    }

    // ==================================================================
    // 4. 扩展欧几里得求 d
    // ==================================================================
    private static void section4Bezout() {
        title(4, "从 e 和 φ(n) 算出 d：扩展欧几里得（-367 是怎么来的）");
        BigInteger phi = BigInteger.valueOf(3120);
        BigInteger e = BigInteger.valueOf(17);

        System.out.println("目标：找 d 使 17*d ≡ 1 (mod 3120)。");
        System.out.println();
        System.out.println("第 1 步 欧几里得除法链（只做带余除法，就是长除法）：");
        BigInteger a = phi;
        BigInteger b = e;
        while (!b.equals(BigInteger.ZERO)) {
            BigInteger quotient = a.divide(b);
            BigInteger remainder = a.subtract(quotient.multiply(b));
            System.out.println("  " + a + " = " + quotient + " * " + b + " + " + remainder);
            a = b;
            b = remainder;
        }
        System.out.println("  余数为 0 时停，上一行的余数 1 就是最大公约数。");

        System.out.println();
        System.out.println("第 2 步 回代：把 1 一路换成 17 和 3120 的组合（从最后一条非零余数往上代）：");
        System.out.println("  1 = 9 - 1*8");
        System.out.println("    = 9 - (17 - 1*9)              = 2*9 - 17");
        System.out.println("    = 2*(3120 - 183*17) - 17      = 2*3120 - 367*17");
        System.out.println("  所以 1 = 17*(-367) + 3120*2，即 17*(-367) ≡ 1 (mod 3120)。");
        System.out.println("  这里的 -367 就是 17 的系数（3120 的系数是 2）。");

        System.out.println();
        System.out.println("第 3 步 表格法（实际计算用这个，不用做代数回代）：");
        System.out.println("  每行满足 r = s*17 + t*3120，只记 17 的系数 s；");
        System.out.println("  规则：下一行的 s = 上一行的 s - 商 * 当前行的 s。");
        System.out.println();
        System.out.println("  " + pad("r", 8) + "  " + pad("商", 6) + "  s（17 的系数）");
        BigInteger r0 = phi;
        BigInteger r1 = e;
        BigInteger s0 = BigInteger.ZERO;
        BigInteger s1 = BigInteger.ONE;
        System.out.println("  " + pad(r0.toString(), 8) + "  " + pad("-", 6) + "  " + s0);
        System.out.println("  " + pad(r1.toString(), 8) + "  " + pad("-", 6) + "  " + s1);
        while (!r1.equals(BigInteger.ZERO)) {
            BigInteger quotient = r0.divide(r1);
            BigInteger r2 = r0.subtract(quotient.multiply(r1));
            BigInteger s2 = s0.subtract(quotient.multiply(s1));
            if (r2.equals(BigInteger.ZERO)) {
                System.out.println("  " + pad("0", 8) + "  " + pad(quotient.toString(), 6) + "  （余数 0，停）");
            } else {
                System.out.println("  " + pad(r2.toString(), 8) + "  " + pad(quotient.toString(), 6) + "  "
                        + s2 + "  = " + s0 + " - " + quotient + "*" + s1);
            }
            r0 = r1;
            r1 = r2;
            s0 = s1;
            s1 = s2;
        }
        BigInteger d = s0.mod(phi);
        System.out.println();
        System.out.println("  最后一行非零余数 1 对应系数 " + s0 + "，即 1 = 17*(" + s0 + ") + 3120*"
                + BigInteger.ONE.subtract(s0.multiply(e)).divide(phi) + "。");
        System.out.println("  负数换成 0..3119 的正数：" + s0 + " + 3120 = " + d);
        System.out.println("  验证：17 * " + d + " = " + e.multiply(d) + " = "
                + e.multiply(d).divide(phi) + " * 3120 + 1");
        check(e.multiply(d).mod(phi).equals(BigInteger.ONE), "17 * d ≡ 1 (mod 3120)");
        check(d.equals(BigInteger.valueOf(2753)), "d = 2753");
        check(d.equals(e.modInverse(phi)), "与 BigInteger.modInverse 结果一致");
    }

    // ==================================================================
    // 5. 真实 2048 位密钥 + CRT 加速
    // ==================================================================
    private static void section5RealKey() throws Exception {
        title(5, "真实 2048 位：密钥生成、CRT 加速");
        long begin = System.nanoTime();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, RANDOM);
        KeyPair keyPair = generator.generateKeyPair();
        long keygenMs = (System.nanoTime() - begin) / 1_000_000;

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyPair.getPrivate();
        BigInteger n = publicKey.getModulus();
        BigInteger e = publicKey.getPublicExponent();
        BigInteger d = privateKey.getPrivateExponent();

        System.out.println("生成 2048 位密钥耗时: " + keygenMs + " ms");
        System.out.println("n 位数: " + n.bitLength() + " 位（十进制约 " + n.toString().length() + " 位）");
        System.out.println("e = " + e + "（固定 65537 = 2^16+1，二进制 1 0000 0000 0000 0001，"
                + "只有 2 个 1 → 加密/验签只要 17 次模乘）");
        System.out.println("d 位数: " + d.bitLength() + " 位（几乎和 n 一样长，私钥运算慢就慢在这里）");
        System.out.println("CRT 参数: p " + privateKey.getPrimeP().bitLength() + " 位, q "
                + privateKey.getPrimeQ().bitLength() + " 位, dP " + privateKey.getPrimeExponentP().bitLength()
                + " 位, dQ " + privateKey.getPrimeExponentQ().bitLength() + " 位");

        BigInteger message = BigInteger.valueOf(123456789);
        BigInteger cipher = message.modPow(e, n);

        // 普通做法：直接用 d
        int rounds = 10;
        long start = System.nanoTime();
        BigInteger plain1 = null;
        for (int i = 0; i < rounds; i++) {
            plain1 = cipher.modPow(d, n);
        }
        double plainMs = (System.nanoTime() - start) / 1e6 / rounds;

        // CRT 做法：先在 p、q 上分别算，再拼起来
        start = System.nanoTime();
        BigInteger plain2 = null;
        for (int i = 0; i < rounds; i++) {
            plain2 = crtDecrypt(cipher, privateKey);
        }
        double crtMs = (System.nanoTime() - start) / 1e6 / rounds;

        System.out.println();
        System.out.println("解密同一条密文（" + rounds + " 次平均）：");
        System.out.println("  直接算 c^d mod n      : " + String.format("%.3f", plainMs) + " ms");
        System.out.println("  CRT: 模 p、模 q 再合并 : " + String.format("%.3f", crtMs) + " ms");
        System.out.println("  加速比约 " + String.format("%.1f", plainMs / crtMs) + " 倍");
        check(plain1.equals(message) && plain2.equals(message), "两种解密结果都等于原文");
        check(crtMs < plainMs, "CRT 加速确实更快（JDK 的私钥内部就用它）");
        System.out.println();
        System.out.println("密钥生成的本质：找两个 1024 位素数（Miller-Rabin 反复试），"
                + "算 φ、选 e=65537、用扩展欧几里得求 d。");
    }

    // ==================================================================
    // 6. φ(n) 就是秘密
    // ==================================================================
    private static void section6PhiIsSecret() {
        title(6, "为什么安全性落在\"分解 n 难\"：知道 φ 就等于知道 d");
        BigInteger p = BigInteger.valueOf(61);
        BigInteger q = BigInteger.valueOf(53);
        BigInteger n = p.multiply(q);
        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        BigInteger e = BigInteger.valueOf(17);

        System.out.println("攻击者知道公钥 (n=3233, e=17)，但不知道 p、q。");
        System.out.println("假设攻击者暴力枚举出 φ(n) = 3120（真实世界做不到，见下）：");
        BigInteger cracked = e.modInverse(phi);
        System.out.println("  他立刻能算 d' = e^-1 mod φ(n) = " + cracked + " —— 就是私钥 d");
        check(cracked.equals(BigInteger.valueOf(2753)), "知道 φ(n) 即可算出私钥 d");
        BigInteger cipher = BigInteger.valueOf(65).modPow(e, n);
        System.out.println("  用 d' 解密 c = " + cipher + " -> " + cipher.modPow(cracked, n) + "（原文 65）");
        check(cipher.modPow(cracked, n).equals(BigInteger.valueOf(65)), "φ 泄露 = 私钥泄露");

        System.out.println();
        System.out.println("而 φ(n) = (p-1)(q-1) 只依赖 p、q。所以：");
        System.out.println("  会分解 n  -> 得到 p、q -> 得到 φ -> 得到 d -> 能解密/伪造签名");
        System.out.println("  反过来，已知 d 也能分解 n（概率多项式时间，教材里通常不讲）。");
        System.out.println();
        System.out.println("规模感（公开记录）：");
        System.out.println("  RSA-250（829 位）2020 年被分解，约 2700 核·年；");
        System.out.println("  RSA-2048（2048 位）至今没有被分解，是目前公认的安全底线；");
        System.out.println("  RSA-1024 已不安全，RSA-512 早就被业余算力攻破。");
        System.out.println("所以 RSA 的安全边界不是\"算法保密\"，而是\"分解 n 的计算量\"。");
    }

    // ==================================================================
    // 7. 往返 + 签名
    // ==================================================================
    private static void section7RoundTripAndSign() throws Exception {
        title(7, "加解密往返，以及签名只是把指数换个方向");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, RANDOM);
        KeyPair keyPair = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyPair.getPrivate();
        BigInteger n = publicKey.getModulus();
        BigInteger e = publicKey.getPublicExponent();
        BigInteger d = privateKey.getPrivateExponent();

        String text = "RSA 原理演示：你好";
        BigInteger m = new BigInteger(1, text.getBytes(StandardCharsets.UTF_8));
        BigInteger c = m.modPow(e, n);
        BigInteger recovered = crtDecrypt(c, privateKey);
        System.out.println("消息: " + text + "（" + text.getBytes(StandardCharsets.UTF_8).length + " 字节）");
        System.out.println("c = m^e mod n（" + c.bitLength() + " 位）");
        System.out.println("m' = c^d mod n，与原文一致: "
                + Arrays.equals(m.toByteArray(), recovered.toByteArray()));
        check(Arrays.equals(m.toByteArray(), recovered.toByteArray()), "真实 2048 位加解密往返成功");

        BigInteger signature = crtDecrypt(m, privateKey);       // 用私钥算 m^d
        BigInteger checked = signature.modPow(e, n);            // 用公钥算回去
        System.out.println();
        System.out.println("签名：s = m^d mod n（用私钥），验证：s^e mod n == m（用公钥）");
        System.out.println("  验证结果: " + checked.equals(m));
        check(checked.equals(m), "签名/验证就是把 e 和 d 换个方向，数学上是同一套运算");
        System.out.println();
        System.out.println("但真实签名不是这样直接用：必须先 hash 再填充（见 docs/数字签名-讲解与演示.md），");
        System.out.println("真实加密也要用 OAEP 填充，裸 RSA 只能签\"比 n 小的整数\"，不能直接用。");
    }

    // ==================================================================
    // 工具
    // ==================================================================

    /** 平方-乘（从最高位开始）：逐步打印，展示 O(log e) 次模乘。 */
    private static BigInteger powTrace(BigInteger base, BigInteger exponent, BigInteger modulus, String label) {
        String bits = exponent.toString(2);
        System.out.println(label + "：底数 " + base + "，指数 " + exponent + " = 0b" + bits
                + "（" + bits.length() + " 位）");
        BigInteger result = BigInteger.ONE;
        int squares = 0;
        int multiplies = 0;
        for (int i = 0; i < bits.length(); i++) {
            result = result.multiply(result).mod(modulus);
            squares++;
            String action = "平方";
            if (bits.charAt(i) == '1') {
                result = result.multiply(base).mod(modulus);
                multiplies++;
                action = "平方 + 乘底数";
            }
            System.out.println("  bit " + (i + 1) + "/" + bits.length() + " = " + bits.charAt(i)
                    + " -> " + action + " -> " + result);
        }
        System.out.println("  共 " + squares + " 次平方、" + multiplies + " 次乘底数（naive 要 "
                + exponent + " 次乘法）");
        return result;
    }

    /** 中国剩余定理加速的私钥运算：先在 p、q 上算，再用 qInv 拼回模 n 的结果。 */
    private static BigInteger crtDecrypt(BigInteger value, RSAPrivateCrtKey key) {
        BigInteger m1 = value.modPow(key.getPrimeExponentP(), key.getPrimeP());
        BigInteger m2 = value.modPow(key.getPrimeExponentQ(), key.getPrimeQ());
        BigInteger h = key.getCrtCoefficient().multiply(m1.subtract(m2)).mod(key.getPrimeP());
        return m2.add(h.multiply(key.getPrimeQ()));
    }

    private static String pad(String text, int width) {
        StringBuilder builder = new StringBuilder(text);
        while (builder.length() < width) {
            builder.append(' ');
        }
        return builder.toString();
    }

    private static void title(int step, String text) {
        System.out.println();
        System.out.println("==================================================================");
        System.out.println("【" + step + "】" + text);
        System.out.println("==================================================================");
    }

    private static void check(boolean condition, String what) {
        if (condition) {
            passed++;
            System.out.println("  [通过] " + what);
        } else {
            failed++;
            System.out.println("  [失败] " + what);
        }
    }
}
