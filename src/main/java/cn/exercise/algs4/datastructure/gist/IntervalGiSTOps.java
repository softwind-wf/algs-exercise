package cn.exercise.algs4.datastructure.gist;

import java.util.List;

/**
 * 区间类型的 GiST 回调实现:一棵“能查重叠的一维区间树”。
 *
 * <p>对应 PostgreSQL 里 {@code range} 类型的 GiST opclass 的简化版:</p>
 * <ul>
 *   <li>{@code union} → 区间并集(父键必须覆盖所有子键);</li>
 *   <li>{@code penalty} → 加入新键后长度增加多少;</li>
 *   <li>{@code consistent} → 是否与查询区间重叠(即 {@code &&} 谓词);</li>
 *   <li>{@code pickSplit} → R 树式的“二次分裂”:先挑一对最不该在一起的种子,
 *       再按“扩张更小”把其余条目分到两组。</li>
 * </ul>
 */
public final class IntervalGiSTOps implements GiSTOps<Interval, Interval> {

    @Override
    public Interval union(Interval left, Interval right) {
        return left.union(right);
    }

    @Override
    public double penalty(Interval current, Interval addition) {
        return current.union(addition).length() - current.length();
    }

    @Override
    public boolean consistent(Interval key, Interval query) {
        return key.overlaps(query);
    }

    @Override
    public boolean equal(Interval left, Interval right) {
        return left.equals(right);
    }

    @Override
    public boolean[] pickSplit(List<Interval> keys) {
        int n = keys.size();
        if (n < 2) {
            throw new IllegalArgumentException("分裂至少需要 2 个条目,实际 " + n);
        }
        int seedA = 0;
        int seedB = 1;
        long worstWaste = Long.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                long waste = keys.get(i).union(keys.get(j)).length()
                        - keys.get(i).length() - keys.get(j).length();
                if (waste > worstWaste) {
                    worstWaste = waste;
                    seedA = i;
                    seedB = j;
                }
            }
        }
        boolean[] group = new boolean[n];
        group[seedA] = true;
        group[seedB] = false;
        Interval coverA = keys.get(seedA);
        Interval coverB = keys.get(seedB);
        int countA = 1;
        int countB = 1;
        for (int i = 0; i < n; i++) {
            if (i == seedA || i == seedB) {
                continue;
            }
            Interval key = keys.get(i);
            long growA = coverA.union(key).length() - coverA.length();
            long growB = coverB.union(key).length() - coverB.length();
            boolean toA = growA < growB || (growA == growB && countA <= countB);
            if (toA) {
                group[i] = true;
                coverA = coverA.union(key);
                countA++;
            } else {
                group[i] = false;
                coverB = coverB.union(key);
                countB++;
            }
        }
        return group;
    }
}
