package cn.exercise.algs4.datastructure.hash.independent;

import cn.exercise.algs4.datastructure.hash.independent.mut.DirectMutantBoundNoLower;
import cn.exercise.algs4.datastructure.hash.independent.mut.DirectMutantCapacityOff;
import cn.exercise.algs4.datastructure.hash.independent.mut.DirectMutantHashNoMinAddr;
import cn.exercise.algs4.datastructure.hash.independent.mut.DirectMutantNoNullCheck;
import cn.exercise.algs4.datastructure.hash.independent.mut.DivisionMutantFloorMod;
import cn.exercise.algs4.datastructure.hash.independent.mut.DivisionMutantLoadDenom;
import cn.exercise.algs4.datastructure.hash.independent.mut.DivisionMutantNoTombstone;
import cn.exercise.algs4.datastructure.hash.independent.mut.DivisionMutantUnsuccBound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 变异测试(自造变异体,不修改受保护实现,而是复制到 mut 子包注入缺陷)。
 * 每个 @Test 复刻对应盲测断言在变异体上的判定:断言"变异体偏离教材期望"为真,
 * 等价于证明该盲测在变异体上会变红(kill)。注释标明由哪个盲测杀死。
 */
class MutationKillingTest {

    private static final int[] KEYS = {19, 14, 23, 1, 68, 20, 84, 27, 55, 11, 10, 79};

    // M1: DivisionHash hash floorMod->%   KILLED BY: DivisionHashBlindTest.hash_isFloorMod_nonNegative
    @Test
    void kills_floorModToRemainder() {
        DivisionMutantFloorMod<String> st = new DivisionMutantFloorMod<String>(16);
        // 正确: hash(-1)=12;变异体用 % 得 -1
        assertTrue(st.hash(-1) != 12, "mutation (floorMod->%) not detected");
    }

    // M2: DivisionHash delete 不留墓碑   KILLED BY: DivisionHashBlindTest.deleteLeavesTombstone_probeChainNotBroken
    @Test
    void kills_deleteWithoutTombstone() {
        DivisionMutantNoTombstone<String> st = new DivisionMutantNoTombstone<String>(16);
        st.put(19, "a"); // slot6
        st.put(32, "b"); // slot7
        st.put(45, "c"); // slot8
        st.delete(32);   // 变异体把 slot7 直接置 null,链断裂
        assertFalse(st.contains(45), "mutation (no tombstone) not detected: chain still intact?");
    }

    // M3: DivisionHash loadFactor 分母错误   KILLED BY: DivisionHashBlindTest.loadFactor_usesTableSize
    @Test
    void kills_loadFactorWrongDenominator() {
        DivisionMutantLoadDenom<String> st = new DivisionMutantLoadDenom<String>(16, 13);
        for (int i = 0; i < 8; i++) st.put(100 + i, "v" + i);
        // 正确: 8/16=0.5;变异体 8/13≈0.615
        assertNotEquals(0.5, st.loadFactor(), 1e-9);
        assertTrue(Math.abs(st.loadFactor() - 8.0 / 16.0) > 1e-9, "mutation (loadFactor denom) not detected");
    }

    // M4: DivisionHash unsuccessfulProbeSum 起点数用 p 而非 m   KILLED BY: classicExample ASL失败
    @Test
    void kills_unsuccessfulSumBound() {
        DivisionMutantUnsuccBound<String> st = new DivisionMutantUnsuccBound<String>(16);
        for (int i = 0; i < KEYS.length; i++) st.put(KEYS[i], "v" + i);
        // 正确 ASL失败 = 94/16 = 5.875;变异体只累加 13 个起点
        assertTrue(Math.abs(st.averageUnsuccessfulProbes() - 5.875) > 1e-9,
                "mutation (unsuccessful bound p) not detected");
    }

    // M5: DirectAddress hash 不减 minAddr   KILLED BY: hash_matchesLinearIndex + generalConstructor
    @Test
    void kills_hashDropsMinAddr() {
        DirectMutantHashNoMinAddr<String> st = new DirectMutantHashNoMinAddr<String>(10, 20, 3, 100);
        // 正确: hash(10)=0;变异体返回 3*10+100=130(越界)
        assertNotEquals(0, st.hash(10));
        assertTrue(st.hash(10) != 0, "mutation (drop -minAddr) not detected");
    }

    // M6: DirectAddress put 去掉 null 校验   KILLED BY: nullValue_throwsNullPointerException
    @Test
    void kills_removedNullCheck() {
        DirectMutantNoNullCheck<String> st = new DirectMutantNoNullCheck<String>(0, 9);
        boolean threw = false;
        try {
            st.put(3, null);
        } catch (NullPointerException e) {
            threw = true;
        }
        assertFalse(threw, "mutation (no null check) not detected: still throws NPE?");
    }

    // M7: DirectAddress capacity 少 1   KILLED BY: capacity_matchesRangeLength
    @Test
    void kills_capacityOffByOne() {
        DirectMutantCapacityOff<String> st = new DirectMutantCapacityOff<String>(5, 15);
        // 正确: 11;变异体: 10
        assertNotEquals(11, st.capacity());
        assertTrue(st.capacity() != 11, "mutation (capacity hi-lo) not detected");
    }

    // M8: DirectAddress hash 去掉下界检查   KILLED BY: outOfRangeKey_throwsIllegalArgumentException
    @Test
    void kills_removedLowerBoundCheck() {
        DirectMutantBoundNoLower<String> st = new DirectMutantBoundNoLower<String>(5, 15);
        // 正确: put(4,...) 应抛 IllegalArgumentException;变异体不抛 IAE(改抛 AIOOBE 或不抛)
        boolean threwIAE = false;
        boolean threwAnything = false;
        try {
            st.put(4, "x");
        } catch (IllegalArgumentException e) {
            threwIAE = true;
        } catch (Throwable t) {
            threwAnything = true;
        }
        assertTrue(!threwIAE, "mutation (no lower-bound IAE) not detected: still throws IAE");
        // 变异体对越界关键字仍会抛异常,但类型错误(实测为 ArrayIndexOutOfBoundsException:-1),
        // 因此盲测的 assertThrows(IllegalArgumentException.class, ...) 会失败 -> 该变异体被杀。
        assertTrue(threwAnything, "mutant should throw a non-IAE error (AIOOBE) for out-of-range lower key");
    }
}
