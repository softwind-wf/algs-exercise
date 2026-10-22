package com.ds.temporal;

import java.util.Objects;

/**
 * 左闭右开时间区间 {@code [start, end)}，{@code end == null} 表示 {@code +∞}（无上界）。
 *
 * <p>时态建模里有两个时间维度，两者都用这个区间表示：
 * <ul>
 *   <li><b>有效时间</b>（valid time）：事实在现实世界中成立的区间，元素类型为
 *       {@code Period<LocalDate>}（日期粒度足够，工资按天生效）；</li>
 *   <li><b>事务时间</b>（transaction time）：数据库相信这条事实的时间区间，元素类型为
 *       {@code Period<LocalDateTime>}（同一分钟内可能发生多次修改，所以用时间戳）。</li>
 * </ul>
 *
 * <p>用左闭右开区间是刻意的：{@code [2021-01-01, 2022-04-01)} 与
 * {@code [2022-04-01, 2023-01-01)} 首尾相接但<b>不重叠</b>，相邻的两段不会被误判成冲突。
 *
 * @param <T> 时间点类型（{@code LocalDate} / {@code LocalDateTime} 等）
 */
public final class Period<T extends Comparable<? super T>> {

    private final T start;
    private final T end;

    private Period(T start, T end) {
        this.start = start;
        this.end = end;
    }

    /**
     * 构造 {@code [start, end)}。
     *
     * @param end {@code null} 表示无上界（至今 / 未来）
     */
    public static <T extends Comparable<? super T>> Period<T> of(T start, T end) {
        if (start == null) {
            throw new IllegalArgumentException("区间下界不能为 null");
        }
        if (end != null && !aBeforeB(start, end)) {
            throw new IllegalArgumentException("区间下界必须早于上界: [" + start + ", " + end + ")");
        }
        return new Period<T>(start, end);
    }

    /** 构造 {@code [start, +∞)}。 */
    public static <T extends Comparable<? super T>> Period<T> from(T start) {
        return of(start, null);
    }

    public T start() {
        return start;
    }

    /** {@code null} 表示无上界。 */
    public T end() {
        return end;
    }

    public boolean isUnboundedEnd() {
        return end == null;
    }

    /** 时间点是否落在区间内（左闭右开）。 */
    public boolean contains(T point) {
        if (point == null) {
            throw new IllegalArgumentException("时间点不能为 null");
        }
        return !aBeforeB(point, start) && aBeforeB(point, end);
    }

    /** 两个区间是否有交集（首尾相接不算）。 */
    public boolean overlaps(Period<T> other) {
        if (other == null) {
            throw new IllegalArgumentException("另一个区间不能为 null");
        }
        return aBeforeB(start, other.end) && aBeforeB(other.start, end);
    }

    /** 两个区间是否首尾相接（本区间上界 == 另一区间下界，或反之）。 */
    public boolean adjacentTo(Period<T> other) {
        T otherStart = other.start;
        if (end != null && end.equals(otherStart)) {
            return true;
        }
        T otherEnd = other.end;
        return otherEnd != null && otherEnd.equals(start);
    }

    /** 换上界；{@code newEnd} 为 {@code null} 表示改成无上界。 */
    public Period<T> withEnd(T newEnd) {
        return of(start, newEnd);
    }

    /** {@code a < b}；{@code b == null} 视为 {@code +∞}，结果恒为 {@code true}。 */
    private static <T extends Comparable<? super T>> boolean aBeforeB(T a, T b) {
        return b == null || a.compareTo(b) < 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Period)) {
            return false;
        }
        Period<?> other = (Period<?>) o;
        return Objects.equals(start, other.start) && Objects.equals(end, other.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    /** 形如 {@code [2021-01-01, 2022-04-01)}；无上界打印为 {@code ∞}。 */
    @Override
    public String toString() {
        return "[" + start + ", " + (end == null ? "∞" : end) + ")";
    }
}
