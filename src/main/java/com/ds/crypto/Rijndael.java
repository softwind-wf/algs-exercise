package com.ds.crypto;

/**
 * Rijndael 分组密码的完整实现（Rijndael 设计文档 / FIPS-197）。
 *
 * <p>与 AES 的关系：AES 是 Rijndael 在“分组固定 128 位”这一子集上的三个标准组合
 * （Nb=4，Nk∈{4,6,8}，即 AES-128/192/256）。Rijndael 本身允许分组和密钥各自取
 * 128/192/256 位，因此状态列数 Nb∈{4,6,8}、密钥字数 Nk∈{4,6,8}，轮数
 * {@code Nr = max(Nb, Nk) + 6}（共 9 种组合，均可用）。
 *
 * <p>状态按“列优先”装入 4 行 Nb 列的字节矩阵：第 i 个字节落在
 * {@code state[i mod 4][i / 4]}。加密每轮依次执行
 * SubBytes → ShiftRows → MixColumns → AddRoundKey，最后一轮省略 MixColumns；
 * 解密直接实现 FIPS-197 的“逆密码”结构（初始 AddRoundKey(Nr)，然后
 * InvShiftRows → InvSubBytes → AddRoundKey → InvMixColumns，最后补一轮不含
 * InvMixColumns 的收尾）。
 *
 * <p>本类只负责单个分组的变换；CBC 模式与 PKCS#7 填充见 {@link RijndaelCbc}，
 * 逐轮演算见 {@link RijndaelDemo}。实现以可读性与教学性为先：S 盒由
 * GF(2^8) 上的乘法求逆 + 仿射变换现场生成，而不是抄 256 字节常量表。
 * <b>这不是恒定时间实现，也不做侧信道防护，禁止用于生产环境。</b>
 *
 * <p>典型用法：
 * <pre>{@code
 * Rijndael aes = new Rijndael(key16);        // AES-128
 * byte[] ct = aes.encryptBlock(pt16);
 * byte[] pt = aes.decryptBlock(ct);
 * }</pre>
 */
public final class Rijndael {

    /** 行移位量：Nb=4 与 Nb=6 都是 0/1/2/3。 */
    private static final int[] SHIFTS_NB_4_6 = {0, 1, 2, 3};

    /** 行移位量：Nb=8 时第 3、4 行分别移 3、4 列（Rijndael 设计文档表 3）。 */
    private static final int[] SHIFTS_NB_8 = {0, 1, 3, 4};

    /** S 盒与逆 S 盒，用 GF(2^8) 求逆 + 仿射变换在类初始化时生成。 */
    private static final int[] S_BOX = new int[256];
    private static final int[] INV_S_BOX = new int[256];

    static {
        buildBoxes();
    }

    /** 状态矩阵列数 Nb（4/6/8）。 */
    private final int nb;
    /** 密钥字数 Nk（4/6/8）。 */
    private final int nk;
    /** 轮数 Nr = max(Nb, Nk) + 6。 */
    private final int nr;
    /** 扩展密钥：Nb*(Nr+1) 个字，每字 4 字节按大端序打包进 int。 */
    private final int[] w;

    /**
     * 以 128 位分组（AES 分组长度）构造，密钥长度决定 Nk：16/24/32 字节 → 4/6/8。
     *
     * @param key 16、24 或 32 字节密钥
     */
    public Rijndael(byte[] key) {
        this(key, 16);
    }

    /**
     * 按 Rijndael 参数构造。
     *
     * @param key            16/24/32 字节密钥（Nk=4/6/8）
     * @param blockSizeBytes 16/24/32 字节分组（Nb=4/6/8）
     */
    public Rijndael(byte[] key, int blockSizeBytes) {
        if (key == null) {
            throw new IllegalArgumentException("密钥不能为 null");
        }
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new IllegalArgumentException(
                    "密钥长度必须是 16/24/32 字节（Nk=4/6/8），实际 " + key.length + " 字节");
        }
        if (blockSizeBytes != 16 && blockSizeBytes != 24 && blockSizeBytes != 32) {
            throw new IllegalArgumentException(
                    "分组长度必须是 16/24/32 字节（Nb=4/6/8），实际 " + blockSizeBytes + " 字节");
        }
        this.nb = blockSizeBytes / 4;
        this.nk = key.length / 4;
        this.nr = Math.max(nb, nk) + 6;
        this.w = expandKey(key, nb, nk, nr);
    }

    /** 分组长度（字节）：16/24/32。 */
    public int blockSize() {
        return 4 * nb;
    }

    /** 密钥长度（字节）：16/24/32。 */
    public int keySize() {
        return 4 * nk;
    }

    /** 状态列数 Nb：4/6/8。 */
    public int nb() {
        return nb;
    }

    /** 密钥字数 Nk：4/6/8。 */
    public int nk() {
        return nk;
    }

    /** 轮数 Nr = max(Nb, Nk) + 6。 */
    public int nr() {
        return nr;
    }

    /** 扩展密钥副本，共 {@code Nb*(Nr+1)} 个字（大端序打包）。 */
    public int[] keySchedule() {
        return w.clone();
    }

    /** 第 {@code round} 轮的轮密钥，共 {@code 4*Nb} 字节，按状态列优先排列。 */
    public byte[] roundKey(int round) {
        if (round < 0 || round > nr) {
            throw new IllegalArgumentException("轮次必须在 0.." + nr + " 之间，实际 " + round);
        }
        byte[] out = new byte[4 * nb];
        for (int c = 0; c < nb; c++) {
            int word = w[round * nb + c];
            out[4 * c] = (byte) (word >>> 24);
            out[4 * c + 1] = (byte) (word >>> 16);
            out[4 * c + 2] = (byte) (word >>> 8);
            out[4 * c + 3] = (byte) word;
        }
        return out;
    }

    /** 加密一个分组（长度必须等于 {@link #blockSize()}）。 */
    public byte[] encryptBlock(byte[] input) {
        return encryptBlock(input, null);
    }

    /**
     * 加密一个分组，并在每一步之后回调 {@code trace}；{@code trace} 为 null 时不做记录。
     *
     * @param input 长度为 {@link #blockSize()} 的明文分组
     * @param trace 逐步状态回调，可为 null
     * @return 等长密文分组
     */
    public byte[] encryptBlock(byte[] input, Trace trace) {
        requireBlock(input);
        int[][] state = load(input);
        emit(trace, 0, "input", state);
        addRoundKey(state, 0);
        emit(trace, 0, "addRoundKey", state);

        for (int round = 1; round <= nr; round++) {
            emit(trace, round, "start", state);
            subBytes(state);
            emit(trace, round, "subBytes", state);
            shiftRows(state);
            emit(trace, round, "shiftRows", state);
            if (round < nr) {
                mixColumns(state);
                emit(trace, round, "mixColumns", state);
            }
            addRoundKey(state, round);
            emit(trace, round, "addRoundKey", state);
        }
        return store(state);
    }

    /** 解密一个分组（长度必须等于 {@link #blockSize()}）。 */
    public byte[] decryptBlock(byte[] input) {
        return decryptBlock(input, null);
    }

    /**
     * 解密一个分组，并在每一步之后回调 {@code trace}；{@code trace} 为 null 时不做记录。
     *
     * @param input 长度为 {@link #blockSize()} 的密文分组
     * @param trace 逐步状态回调，可为 null
     * @return 等长明文分组
     */
    public byte[] decryptBlock(byte[] input, Trace trace) {
        requireBlock(input);
        int[][] state = load(input);

        // FIPS-197 逆密码：先加第 Nr 轮密钥，再倒着走 Nr-1..1 的逆轮，最后补收尾轮。
        addRoundKey(state, nr);
        emit(trace, nr, "addRoundKey", state);

        for (int round = nr - 1; round >= 1; round--) {
            emit(trace, round, "start", state);
            invShiftRows(state);
            emit(trace, round, "invShiftRows", state);
            invSubBytes(state);
            emit(trace, round, "invSubBytes", state);
            addRoundKey(state, round);
            emit(trace, round, "addRoundKey", state);
            invMixColumns(state);
            emit(trace, round, "invMixColumns", state);
        }

        emit(trace, 0, "start", state);
        invShiftRows(state);
        emit(trace, 0, "invShiftRows", state);
        invSubBytes(state);
        emit(trace, 0, "invSubBytes", state);
        addRoundKey(state, 0);
        emit(trace, 0, "addRoundKey", state);

        return store(state);
    }

    // ------------------------------------------------------------------
    // 分组读写与轮变换
    // ------------------------------------------------------------------

    private void requireBlock(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("分组不能为 null");
        }
        if (input.length != blockSize()) {
            throw new IllegalArgumentException(
                    "分组长度必须是 " + blockSize() + " 字节，实际 " + input.length + " 字节");
        }
    }

    /** 列优先装入状态：state[r][c] = input[4c + r]。 */
    private int[][] load(byte[] input) {
        int[][] state = new int[4][nb];
        for (int c = 0; c < nb; c++) {
            for (int r = 0; r < 4; r++) {
                state[r][c] = input[4 * c + r] & 0xff;
            }
        }
        return state;
    }

    /** 列优先写回：output[4c + r] = state[r][c]。 */
    private byte[] store(int[][] state) {
        byte[] out = new byte[4 * nb];
        for (int c = 0; c < nb; c++) {
            for (int r = 0; r < 4; r++) {
                out[4 * c + r] = (byte) state[r][c];
            }
        }
        return out;
    }

    /** SubBytes：对每个字节查 S 盒。 */
    private void subBytes(int[][] state) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < nb; c++) {
                state[r][c] = S_BOX[state[r][c]];
            }
        }
    }

    /** InvSubBytes：对每个字节查逆 S 盒。 */
    private void invSubBytes(int[][] state) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < nb; c++) {
                state[r][c] = INV_S_BOX[state[r][c]];
            }
        }
    }

    /** ShiftRows：第 r 行循环左移 offset 列；Nb=8 时 offset 为 0/1/3/4。 */
    private void shiftRows(int[][] state) {
        int[] offsets = (nb == 8) ? SHIFTS_NB_8 : SHIFTS_NB_4_6;
        for (int r = 1; r < 4; r++) {
            int shift = offsets[r];
            int[] row = state[r].clone();
            for (int c = 0; c < nb; c++) {
                state[r][c] = row[(c + shift) % nb];
            }
        }
    }

    /** InvShiftRows：ShiftRows 的逆，第 r 行循环右移 offset 列。 */
    private void invShiftRows(int[][] state) {
        int[] offsets = (nb == 8) ? SHIFTS_NB_8 : SHIFTS_NB_4_6;
        for (int r = 1; r < 4; r++) {
            int shift = offsets[r];
            int[] row = state[r].clone();
            for (int c = 0; c < nb; c++) {
                state[r][c] = row[(c - shift + nb) % nb];
            }
        }
    }

    /**
     * MixColumns：每列左乘 Rijndael 的 MDS 矩阵
     * <pre>
     * [02 03 01 01]
     * [01 02 03 01]
     * [01 01 02 03]
     * [03 01 01 02]
     * </pre>
     * 用“整列异或 + xtime”的等价形式计算，避免逐个字节做通用 GF 乘法。
     */
    private void mixColumns(int[][] state) {
        for (int c = 0; c < nb; c++) {
            int a0 = state[0][c];
            int a1 = state[1][c];
            int a2 = state[2][c];
            int a3 = state[3][c];
            int all = a0 ^ a1 ^ a2 ^ a3;
            state[0][c] = a0 ^ all ^ xtime(a0 ^ a1);
            state[1][c] = a1 ^ all ^ xtime(a1 ^ a2);
            state[2][c] = a2 ^ all ^ xtime(a2 ^ a3);
            state[3][c] = a3 ^ all ^ xtime(a3 ^ a0);
        }
    }

    /** InvMixColumns：每列左乘逆矩阵（行系数 0e/0b/0d/09 循环移位）。 */
    private void invMixColumns(int[][] state) {
        for (int c = 0; c < nb; c++) {
            int a0 = state[0][c];
            int a1 = state[1][c];
            int a2 = state[2][c];
            int a3 = state[3][c];
            state[0][c] = multiply(a0, 0x0e) ^ multiply(a1, 0x0b) ^ multiply(a2, 0x0d) ^ multiply(a3, 0x09);
            state[1][c] = multiply(a0, 0x09) ^ multiply(a1, 0x0e) ^ multiply(a2, 0x0b) ^ multiply(a3, 0x0d);
            state[2][c] = multiply(a0, 0x0d) ^ multiply(a1, 0x09) ^ multiply(a2, 0x0e) ^ multiply(a3, 0x0b);
            state[3][c] = multiply(a0, 0x0b) ^ multiply(a1, 0x0d) ^ multiply(a2, 0x09) ^ multiply(a3, 0x0e);
        }
    }

    /** AddRoundKey：把第 {@code round} 轮的 Nb 个字按列异或进状态。 */
    private void addRoundKey(int[][] state, int round) {
        for (int c = 0; c < nb; c++) {
            int word = w[round * nb + c];
            state[0][c] ^= (word >>> 24) & 0xff;
            state[1][c] ^= (word >>> 16) & 0xff;
            state[2][c] ^= (word >>> 8) & 0xff;
            state[3][c] ^= word & 0xff;
        }
    }

    /** 每步之后回调，状态传副本，回调方可安全保存。 */
    private static void emit(Trace trace, int round, String phase, int[][] state) {
        if (trace != null) {
            trace.step(round, phase, copy(state));
        }
    }

    private static int[][] copy(int[][] state) {
        int[][] out = new int[state.length][];
        for (int r = 0; r < state.length; r++) {
            out[r] = state[r].clone();
        }
        return out;
    }

    // ------------------------------------------------------------------
    // 密钥扩展
    // ------------------------------------------------------------------

    /**
     * Rijndael 密钥扩展（设计文档第 3.6 节 / FIPS-197 第 5.2 节）。
     *
     * <p>前 Nk 个字就是密钥本身；之后每个字由“前一个字”和“往前 Nk 个字”异或得到。
     * 当 {@code i % Nk == 0} 时，前一个字先做 RotWord → SubWord → 异或 Rcon[i/Nk]；
     * 当 {@code Nk > 6 且 i % Nk == 4} 时，前一个字只做一次 SubWord（256 位密钥特有）。
     */
    private static int[] expandKey(byte[] key, int nb, int nk, int nr) {
        int totalWords = nb * (nr + 1);
        int[] expanded = new int[totalWords];
        for (int i = 0; i < nk; i++) {
            expanded[i] = wordAt(key, i);
        }
        int rcon = 0x01;
        for (int i = nk; i < totalWords; i++) {
            int temp = expanded[i - 1];
            if (i % nk == 0) {
                temp = subWord(rotWord(temp)) ^ (rcon << 24);
                rcon = xtime(rcon);
            } else if (nk > 6 && i % nk == 4) {
                temp = subWord(temp);
            }
            expanded[i] = expanded[i - nk] ^ temp;
        }
        return expanded;
    }

    /** 取密钥的第 {@code index} 个字（大端序）。 */
    private static int wordAt(byte[] key, int index) {
        int off = 4 * index;
        return ((key[off] & 0xff) << 24)
                | ((key[off + 1] & 0xff) << 16)
                | ((key[off + 2] & 0xff) << 8)
                | (key[off + 3] & 0xff);
    }

    /** RotWord：把字循环左移一个字节，如 09cf4f3c → cf4f3c09。 */
    private static int rotWord(int word) {
        return (word << 8) | (word >>> 24);
    }

    /** SubWord：对字的 4 个字节分别查 S 盒。 */
    private static int subWord(int word) {
        return (S_BOX[(word >>> 24) & 0xff] << 24)
                | (S_BOX[(word >>> 16) & 0xff] << 16)
                | (S_BOX[(word >>> 8) & 0xff] << 8)
                | S_BOX[word & 0xff];
    }

    // ------------------------------------------------------------------
    // GF(2^8) 运算、S 盒生成
    // ------------------------------------------------------------------

    /**
     * GF(2^8) 上乘 2（模不可约多项式 x^8 + x^4 + x^3 + x + 1，即 0x11b）：
     * 左移一位，若原最高位为 1 再异或 0x1b。
     */
    static int xtime(int b) {
        b &= 0xff;
        int result = (b << 1) & 0xff;
        if ((b & 0x80) != 0) {
            result ^= 0x1b;
        }
        return result;
    }

    /** GF(2^8) 上的通用乘法（俄罗斯农民乘法），S 盒求逆与 InvMixColumns 使用。 */
    static int multiply(int a, int b) {
        int product = 0;
        int x = a & 0xff;
        int y = b & 0xff;
        while (y != 0) {
            if ((y & 1) != 0) {
                product ^= x;
            }
            x = xtime(x);
            y >>>= 1;
        }
        return product;
    }

    /** S 盒查表（供演示/测试；入参自动截为低 8 位）。 */
    static int sBox(int b) {
        return S_BOX[b & 0xff];
    }

    /** 逆 S 盒查表（供演示/测试；入参自动截为低 8 位）。 */
    static int invSBox(int b) {
        return INV_S_BOX[b & 0xff];
    }

    /**
     * 生成 S 盒：先求 GF(2^8) 上的乘法逆元（0 的逆元规定为 0），
     * 再套仿射变换 {@code b' = b ^ rotl(b,1) ^ rotl(b,2) ^ rotl(b,3) ^ rotl(b,4) ^ 0x63}。
     */
    private static void buildBoxes() {
        for (int a = 1; a < 256; a++) {
            int inverse = power(a, 254);          // a^254 = a^-1（a ≠ 0）
            int s = inverse
                    ^ rotl8(inverse, 1)
                    ^ rotl8(inverse, 2)
                    ^ rotl8(inverse, 3)
                    ^ rotl8(inverse, 4)
                    ^ 0x63;
            S_BOX[a] = s & 0xff;
        }
        S_BOX[0] = 0x63;                          // 0 的逆元按定义取 0，仿射后为 0x63
        for (int a = 0; a < 256; a++) {
            INV_S_BOX[S_BOX[a]] = a;
        }
    }

    /** GF(2^8) 快速幂，用于求逆元 a^254。 */
    private static int power(int a, int exponent) {
        int result = 1;
        int base = a;
        int e = exponent;
        while (e > 0) {
            if ((e & 1) != 0) {
                result = multiply(result, base);
            }
            base = multiply(base, base);
            e >>>= 1;
        }
        return result;
    }

    /** 8 位循环左移 n 位。 */
    private static int rotl8(int b, int n) {
        return ((b << n) | (b >>> (8 - n))) & 0xff;
    }

    /**
     * 逐步状态回调。{@code state} 是 4 行 × Nb 列的字节矩阵（值 0..255），
     * 每次回调传的是副本，可安全保存或打印。
     */
    public interface Trace {

        /**
         * @param round 轮次：0 表示密钥扩展后的初始状态或最后一轮；其余为 1..Nr
         * @param phase 步骤名：input / start / subBytes / shiftRows / mixColumns /
         *              addRoundKey / invShiftRows / invSubBytes / invMixColumns
         * @param state 该步骤执行后的状态快照
         */
        void step(int round, String phase, int[][] state);
    }
}
