package cn.exercise.algs4.datastructure.gist;

/**
 * 闭区间 {@code [low, high]},作为 GiST 示例里的键与查询条件。
 *
 * <p>选它是因为区间同时具备 GiST 需要的三件事:能求并(包围键)、能算扩张代价、
 * 能判“是否可能重叠”(consistent),而且肉眼可验证。</p>
 */
public final class Interval {

    private final long low;
    private final long high;

    private Interval(long low, long high) {
        this.low = low;
        this.high = high;
    }

    public static Interval of(long low, long high) {
        if (low > high) {
            throw new IllegalArgumentException("区间下界不能大于上界: " + low + " > " + high);
        }
        return new Interval(low, high);
    }

    public long low() {
        return low;
    }

    public long high() {
        return high;
    }

    /** 区间长度(闭区间按 high-low 计,只用于比较大小,不追求语义精度)。 */
    public long length() {
        return high - low;
    }

    /** 两个区间是否有重叠(边界相触也算重叠,与 PostgreSQL 的 {@code &&} 一致)。 */
    public boolean overlaps(Interval other) {
        return low <= other.high && other.low <= high;
    }

    /** 是否为另一个区间的包围区间。 */
    public boolean covers(Interval other) {
        return low <= other.low && other.high <= high;
    }

    /** 并集,即 GiST 的“包围键”。 */
    public Interval union(Interval other) {
        return new Interval(Math.min(low, other.low), Math.max(high, other.high));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Interval)) {
            return false;
        }
        Interval other = (Interval) o;
        return low == other.low && high == other.high;
    }

    @Override
    public int hashCode() {
        int result = (int) (low ^ (low >>> 32));
        result = 31 * result + (int) (high ^ (high >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "[" + low + ", " + high + "]";
    }
}
