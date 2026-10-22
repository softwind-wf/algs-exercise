package com.ds.temporal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 双时态表里的一行：{@code (key, value, valid, tx)}。
 *
 * <p>一行的含义是：<b>在事务时间区间 {@code tx} 内，数据库认为</b>「{@code key} 在有效时间区间
 * {@code valid} 里取值为 {@code value}」。行一旦写入就不再改写（只能把 {@code tx} 的上界封上，
 * 表示"从这一刻起不再相信这一行"），所以整张表是 append-only 的版本史。
 *
 * <p>不可变对象：修改事务时间上界通过 {@link #withTxEnd(LocalDateTime)} 产生新行，
 * 由 {@link TemporalTable} 负责把旧行换掉。
 *
 * @param <V> 取值类型（本例子用 {@code Integer} 表示月薪）
 */
public final class TemporalRow<V> {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);

    private final String key;
    private final V value;
    private final Period<LocalDate> valid;
    private final Period<LocalDateTime> tx;

    public TemporalRow(String key, V value, Period<LocalDate> valid, Period<LocalDateTime> tx) {
        if (key == null || value == null || valid == null || tx == null) {
            throw new IllegalArgumentException("行的四个字段都不能为 null");
        }
        this.key = key;
        this.value = value;
        this.valid = valid;
        this.tx = tx;
    }

    public String key() {
        return key;
    }

    public V value() {
        return value;
    }

    /** 有效时间区间（现实世界）。 */
    public Period<LocalDate> valid() {
        return valid;
    }

    /** 事务时间区间（数据库相信这一行的时段）。 */
    public Period<LocalDateTime> tx() {
        return tx;
    }

    /** 把事务时间上界封到 {@code end}，表示"从 end 起不再相信这一行"。 */
    public TemporalRow<V> withTxEnd(LocalDateTime end) {
        return new TemporalRow<V>(key, value, valid, tx.withEnd(end));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TemporalRow)) {
            return false;
        }
        TemporalRow<?> other = (TemporalRow<?>) o;
        return key.equals(other.key)
                && value.equals(other.value)
                && valid.equals(other.valid)
                && tx.equals(other.tx);
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + valid.hashCode();
        result = 31 * result + tx.hashCode();
        return result;
    }

    /** 形如 {@code E1001 | 12000 | [2021-01-01, 2022-04-01) | [2021-01-01 00:00, ∞)}。 */
    @Override
    public String toString() {
        return key + " | " + value + " | " + formatValid(valid) + " | " + formatTx(tx);
    }

    /** 有效区间的展示形式：日期、无上界为 {@code ∞}。 */
    public static String formatValid(Period<LocalDate> valid) {
        return "[" + DATE.format(valid.start()) + ", "
                + (valid.end() == null ? "∞" : DATE.format(valid.end())) + ")";
    }

    /** 事务区间的展示形式：到分钟、无上界为 {@code ∞}。 */
    public static String formatTx(Period<LocalDateTime> tx) {
        return "[" + DATE_TIME.format(tx.start()) + ", "
                + (tx.end() == null ? "∞" : DATE_TIME.format(tx.end())) + ")";
    }
}
