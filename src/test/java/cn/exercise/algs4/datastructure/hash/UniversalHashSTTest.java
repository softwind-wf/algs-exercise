package cn.exercise.algs4.datastructure.hash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全域散列法单元测试。
 * 重点验证:散列函数公式、种子决定的确定性选取、**全域性 Pr ≤ 1/m**(穷举全族 + 闭式公式两条独立路径)、
 * 计数与关键字取值无关、以及"全域性只保证概率"的反例(某些随机函数仍会全部聚集)。
 */
@DisplayName("UniversalHashST 全域散列法散列表测试")
class UniversalHashSTTest {

    @Nested
    @DisplayName("散列函数 h(a,b)(key) = ((a*key+b) mod p) mod m")
    class HashFunctionTest {

        @Test
        @DisplayName("显式给定 (a,b) 时与手算一致")
        void explicitFormula() {
            UniversalHashST<String> st = new UniversalHashST<String>(4, 13, 3, 5);
            assertEquals(3L, st.a());
            assertEquals(5L, st.b());
            assertEquals(1, st.hash(0));   // (5) mod 13 = 5 → 5 mod 4 = 1
            assertEquals(0, st.hash(1));   // (8) → 8 mod 4 = 0
            assertEquals(3, st.hash(2));   // (11) → 11 mod 4 = 3
            assertEquals(1, st.hash(3));   // (14 mod 13)=1 → 1
            assertEquals(0, st.hash(4));   // (17 mod 13)=4 → 0
            assertEquals(2, st.hash(12));  // (41 mod 13)=2 → 2
        }

        @Test
        @DisplayName("a=1、b=0 时退化为 (key mod p) mod m")
        void identityLikeFamilyMember() {
            UniversalHashST<String> st = new UniversalHashST<String>(4, 13, 1, 0);
            assertEquals(1, st.hash(1));
            assertEquals(1, st.hash(5));
            assertEquals(0, st.hash(4));
            assertEquals(0, st.hash(12));
        }

        @Test
        @DisplayName("地址始终落在 [0, tableSize)")
        void addressInRange() {
            UniversalHashST<String> st = new UniversalHashST<String>(7, 97, 13, 29);
            for (long key = 0; key <= 96; key++) {
                int address = st.hash(key);
                assertTrue(address >= 0 && address < 7, "key = " + key + " 地址越界: " + address);
            }
        }

        @Test
        @DisplayName("关键字越界抛异常")
        void keyOutOfRange() {
            UniversalHashST<String> st = new UniversalHashST<String>(4, 13, 3, 5);
            assertThrows(IllegalArgumentException.class, () -> st.hash(-1));
            assertThrows(IllegalArgumentException.class, () -> st.hash(13));
            assertThrows(IllegalArgumentException.class, () -> st.put(13, "x"));
            assertThrows(IllegalArgumentException.class, () -> st.get(-1));
        }
    }

    @Nested
    @DisplayName("随机选取 (a,b):确定性、取值范围")
    class SelectionTest {

        @Test
        @DisplayName("同一种子选出同一函数,不同种子一般不同")
        void deterministicBySeed() {
            UniversalHashST<String> a = new UniversalHashST<String>(16, 97, 20261002L);
            UniversalHashST<String> b = new UniversalHashST<String>(16, 97, 20261002L);
            UniversalHashST<String> c = new UniversalHashST<String>(16, 97, 20261003L);

            assertEquals(a.a(), b.a());
            assertEquals(a.b(), b.b());
            for (long key = 0; key < 97; key++) {
                assertEquals(a.hash(key), b.hash(key));
            }
            assertNotEquals(a.a() * 97 + a.b(), c.a() * 97 + c.b(), "不同种子应选出不同的函数");
        }

        @Test
        @DisplayName("默认种子的选取结果可复现(回归锁定)")
        void defaultSeedIsStable() {
            UniversalHashST<String> st = new UniversalHashST<String>(16, 97);
            assertEquals(78L, st.a());
            assertEquals(70L, st.b());
            assertEquals(25214903917L, st.seed());
        }

        @Test
        @DisplayName("任意种子下 a ∈ [1,p-1]、b ∈ [0,p-1]")
        void parametersStayInFamily() {
            long p = 97;
            for (long seed = -50; seed <= 50; seed++) {
                UniversalHashST<String> st = new UniversalHashST<String>(16, p, seed);
                assertTrue(st.a() >= 1 && st.a() <= p - 1, "a 越界: " + st.a());
                assertTrue(st.b() >= 0 && st.b() <= p - 1, "b 越界: " + st.b());
            }
        }
    }

    @Nested
    @DisplayName("全域性:Pr[h(k1)=h(k2)] ≤ 1/m")
    class UniversalityTest {

        @Test
        @DisplayName("穷举全族与闭式公式结果一致(两条独立路径)")
        void exhaustiveMatchesClosedForm() {
            int m = 16;
            long p = 97;
            long hits = UniversalHashST.collisionCountInFamily(13, 42, m, p);
            assertEquals(492L, hits);
            assertEquals(UniversalHashST.predictedCollisionCount(m, p), hits);
            assertEquals((p - 1) * p, UniversalHashST.familySize(p));
            assertEquals(492.0 / 9312, UniversalHashST.collisionProbabilityInFamily(13, 42, m, p), 1e-12);
        }

        @Test
        @DisplayName("碰撞计数与关键字的具体取值无关(只要求两者不同)")
        void countIndependentOfKeys() {
            int m = 16;
            long p = 97;
            long reference = UniversalHashST.collisionCountInFamily(13, 42, m, p);
            assertEquals(reference, UniversalHashST.collisionCountInFamily(1, 2, m, p));
            assertEquals(reference, UniversalHashST.collisionCountInFamily(0, 96, m, p));
            assertEquals(reference, UniversalHashST.collisionCountInFamily(95, 96, m, p));
        }

        @Test
        @DisplayName("小参数下逐一验证 ≤ 1/m,且 m=1 时必然全碰撞")
        void boundHoldsForManyParameters() {
            long[] primes = {2, 3, 5, 7, 11, 13, 97};
            int[] sizes = {1, 2, 3, 4, 5, 8, 16};
            for (long p : primes) {
                long family = UniversalHashST.familySize(p);
                for (int m : sizes) {
                    long hits = UniversalHashST.collisionCountInFamily(0, 1, m, p);
                    assertTrue(hits * m <= family,
                            "p=" + p + ", m=" + m + " 时违反 Pr <= 1/m: hits=" + hits + ", |H|=" + family);
                    if (m == 1) {
                        assertEquals(family, hits, "m=1 时全部函数都碰撞");
                    }
                    assertEquals(UniversalHashST.predictedCollisionCount(m, p), hits);
                }
            }
        }

        @Test
        @DisplayName("闭式给出的概率 = (p+1-m)/(p*m),严格小于 1/m(仅 m=1 取等)")
        void probabilityStrictlyBelowBound() {
            // p = 17, m = 4:计数应为 56,概率 56/272 = 0.2059 < 0.25
            long hits = UniversalHashST.collisionCountInFamily(0, 1, 4, 17);
            assertEquals(56L, hits);
            assertEquals(UniversalHashST.predictedCollisionCount(4, 17), hits);
            assertEquals(56.0 / 272, (double) hits / UniversalHashST.familySize(17), 1e-12);
            assertTrue((double) hits / UniversalHashST.familySize(17) < 0.25);
            assertEquals((17.0 + 1 - 4) / (17.0 * 4), (double) hits / UniversalHashST.familySize(17), 1e-12);
        }
    }

    @Nested
    @DisplayName("全域性只保证概率:某些随机函数仍会全部聚集")
    class AdversaryTest {

        /** 对手知道 p 和 m,针对固定函数 h(key)=key mod 64 精心构造的集合 */
        private long[] adversary() {
            long[] keys = new long[15];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = 64L * (i + 1);
            }
            return keys;
        }

        @Test
        @DisplayName("a=1、b=47 时这批等距关键字全部落进同一个桶(反例,说明不保证每个函数都好)")
        void someFamilyMemberCollapses() {
            UniversalHashST<String> st = new UniversalHashST<String>(64, 1009, 1L, 47L);
            for (long key : adversary()) {
                st.put(key, "v");
            }
            assertEquals(15, st.maxChainLength());
        }

        @Test
        @DisplayName("500 个种子下平均最长链很小,但最差一次仍可能等于固定函数的情形")
        void averageIsSmallButWorstCanBeBad() {
            long total = 0;
            int worst = 0;
            long worstSeed = -1;
            for (long seed = 0; seed < 500; seed++) {
                UniversalHashST<String> st = new UniversalHashST<String>(64, 1009, seed);
                for (long key : adversary()) {
                    st.put(key, "v");
                }
                int max = st.maxChainLength();
                total += max;
                if (max > worst) {
                    worst = max;
                    worstSeed = seed;
                }
            }
            assertTrue((double) total / 500 < 3.0, "平均最长链应很小,实际 = " + (double) total / 500);
            assertEquals(15, worst, "最差的一次仍可能与固定函数一样糟(这正是概率保证的含义)");

            UniversalHashST<String> worstCase = new UniversalHashST<String>(64, 1009, worstSeed);
            assertEquals(1L, worstCase.a());
            assertEquals(47L, worstCase.b());
        }
    }

    @Nested
    @DisplayName("构造参数校验")
    class ConstructorTest {

        @Test
        @DisplayName("p 必须是素数,且落在 [2, 2^31-1]")
        void primeValidation() {
            assertThrows(IllegalArgumentException.class, () -> new UniversalHashST<String>(16, 15));   // 合数
            assertThrows(IllegalArgumentException.class, () -> new UniversalHashST<String>(16, 1));
            assertThrows(IllegalArgumentException.class, () -> new UniversalHashST<String>(16, 0));
            assertThrows(IllegalArgumentException.class,
                    () -> new UniversalHashST<String>(16, 4294967291L));                              // > 2^31-1
            assertThrows(IllegalArgumentException.class, () -> new UniversalHashST<String>(0, 13));
            assertThrows(IllegalArgumentException.class, () -> new UniversalHashST<String>((1 << 26) + 1, 13));
        }

        @Test
        @DisplayName("a、b 必须在族内")
        void abValidation() {
            assertThrows(IllegalArgumentException.class, () -> new UniversalHashST<String>(4, 13, 0, 5));
            assertThrows(IllegalArgumentException.class, () -> new UniversalHashST<String>(4, 13, 13, 5));
            assertThrows(IllegalArgumentException.class, () -> new UniversalHashST<String>(4, 13, 3, -1));
            assertThrows(IllegalArgumentException.class, () -> new UniversalHashST<String>(4, 13, 3, 13));
        }

        @Test
        @DisplayName("族统计方法的参数校验")
        void familyStatValidation() {
            assertThrows(IllegalArgumentException.class, () -> UniversalHashST.collisionCountInFamily(5, 5, 4, 13));
            assertThrows(IllegalArgumentException.class, () -> UniversalHashST.collisionCountInFamily(0, 1, 4, 12));
            assertThrows(IllegalArgumentException.class, () -> UniversalHashST.predictedCollisionCount(0, 13));
        }
    }

    @Nested
    @DisplayName("符号表操作(拉链法)")
    class SymbolTableTest {

        @Test
        @DisplayName("put/get/delete/contains/size/isEmpty/loadFactor/keys/toString")
        void basicApi() {
            UniversalHashST<String> st = new UniversalHashST<String>(4, 13, 3, 5);
            assertTrue(st.isEmpty());
            assertEquals(0.0, st.loadFactor(), 1e-12);

            st.put(1, "v1"); // 地址 0
            st.put(0, "v0"); // 地址 1
            st.put(2, "v2"); // 地址 3
            assertEquals(3, st.size());
            assertEquals(0.75, st.loadFactor(), 1e-12);
            assertEquals("v0", st.get(0));
            assertTrue(st.contains(2));
            assertFalse(st.contains(3));
            assertNull(st.get(3));

            List<Long> keys = new ArrayList<Long>();
            st.keys().forEach(keys::add);
            assertEquals(Arrays.asList(1L, 0L, 2L), keys); // 地址升序:0 < 1 < 3
            assertEquals("{1 v1, 0 v0, 2 v2}", st.toString());

            assertEquals("v0", st.delete(0));
            assertFalse(st.contains(0));
            assertEquals(2, st.size());
            assertNull(st.delete(0));
        }

        @Test
        @DisplayName("同地址的关键字挂链,删除链中元素不影响其它键")
        void chaining() {
            UniversalHashST<String> st = new UniversalHashST<String>(4, 13, 3, 5);
            assertEquals(st.hash(0), st.hash(3)); // 都是地址 1
            st.put(0, "a");
            st.put(3, "b");
            assertEquals(2, st.maxChainLength());
            assertEquals("a", st.get(0));
            assertEquals("b", st.get(3));
            assertEquals("a", st.delete(0));
            assertEquals("b", st.get(3));
            assertTrue(st.contains(3));
            assertEquals(1, st.size());
        }

        @Test
        @DisplayName("重复插入只覆盖值;null 值抛异常")
        void overwriteAndNull() {
            UniversalHashST<String> st = new UniversalHashST<String>(4, 13, 3, 5);
            st.put(1, "旧");
            st.put(1, "新");
            assertEquals(1, st.size());
            assertEquals("新", st.get(1));
            assertThrows(NullPointerException.class, () -> st.put(1, null));
        }
    }
}
