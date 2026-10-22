package com.ds.temporal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 双时态内存表：一个员工（{@code key}）的取值（{@code value}，本例子是月薪）随时间变化的版本史。
 *
 * <p>每条事实带两个时间区间：
 * <ul>
 *   <li><b>有效时间 valid</b>：现实世界里"从哪天到哪天拿这份工资"；</li>
 *   <li><b>事务时间 tx</b>：数据库里"从什么时候开始相信这条记录"。</li>
 * </ul>
 * 写操作只做两件事：<b>把旧版本的事务时间上界封到"现在"</b>，再<b>以"现在"为起点插入新版本</b>。
 * 于是任何历史时刻的信念都能原样重建，整张表是 append-only 的。
 *
 * <p>写操作（对应 SQL:2011 的应用时间表语义，PostgreSQL 17 未内置这些语法，本类与
 * {@code temporal_modeling_demo.sql} 都是手写实现）：
 * <ul>
 *   <li>{@link #append}：录入一条以前没有的事实（从某天起生效）；</li>
 *   <li>{@link #setValueFrom}：从某天起改值，自动截到"下一次变化"之前；</li>
 *   <li>{@link #updateForPortion}：只改一段有效区间（等价 {@code UPDATE ... FOR PORTION OF}）；</li>
 *   <li>{@link #deleteForPortion}：只删一段有效区间（等价 {@code DELETE ... FOR PORTION OF}）。</li>
 * </ul>
 *
 * <p>读操作按"问的是哪个时间"分四类：
 * {@link #current()}（现在的信念）、{@link #valueAt}（现在的信念里某天多少钱）、
 * {@link #asOf}（某个事务时刻相信的全部行）、{@link #valueAsOf}（同时指定两个时间）。
 *
 * @param <V> 取值类型
 */
public final class TemporalTable<V> {

    private final List<TemporalRow<V>> rows = new ArrayList<TemporalRow<V>>();

    /** 上一次写入的事务时间；null 表示还没写过。事务时钟只能前进。 */
    private LocalDateTime clock;

    // ------------------------------------------------------------------
    // 写操作
    // ------------------------------------------------------------------

    /**
     * 录入一条新事实：{@code key} 在 {@code valid} 区间内取值为 {@code value}。
     *
     * <p>要求 {@code valid} 与"当前信念"里同一 key 的有效区间都不重叠——这就是<b>时态键</b>：
     * 同一员工、同一事务时刻，任何一天只能有一份工资。重叠时抛 {@link IllegalArgumentException}。
     *
     * @param tx    本次写入的事务时间（必须严格晚于上一次写入）
     * @param valid 有效时间区间
     */
    public void append(LocalDateTime tx, String key, V value, Period<LocalDate> valid) {
        requireClock(tx);
        for (TemporalRow<V> row : currentRows(key)) {
            if (row.valid().overlaps(valid)) {
                throw new IllegalArgumentException("时态键冲突：员工 " + key + " 已有有效期 "
                        + TemporalRow.formatValid(row.valid()) + "，与 " + TemporalRow.formatValid(valid)
                        + " 重叠；同一员工在同一事务时刻只能有一份工资。");
            }
        }
        rows.add(new TemporalRow<V>(key, value, valid, Period.from(tx)));
        clock = tx;
    }

    /**
     * 从 {@code from} 起把工资改成 {@code value}，直到该员工"下一次变化"之前。
     *
     * <p>这是 HR 最自然的操作："2022-04-01 涨到 14000"。实现上先找到当前信念里覆盖
     * {@code from} 的那一段，把它的上界（可能是 {@code ∞}）当作截止，再调用
     * {@link #updateForPortion}。找不到覆盖行时抛异常（新员工请先用 {@link #append}）。
     */
    public void setValueFrom(LocalDateTime tx, String key, V value, LocalDate from) {
        requireClock(tx);
        TemporalRow<V> covering = null;
        for (TemporalRow<V> row : currentRows(key)) {
            if (row.valid().contains(from)) {
                covering = row;
                break;
            }
        }
        if (covering == null) {
            throw new IllegalArgumentException("员工 " + key + " 在 " + from
                    + " 没有任何当前有效的记录，无法从这天起改薪；新记录请先 append。");
        }
        updateForPortion(tx, key, value, Period.of(from, covering.valid().end()));
    }

    /**
     * 把 {@code [portion.start, portion.end)} 这段有效区间的值改成 {@code value}
     * （其余部分保持原值），等价 SQL:2011 的 {@code UPDATE ... FOR PORTION OF valid TIME}。
     *
     * <p>对每个与 portion 相交的当前行：
     * <ol>
     *   <li>把它的 tx 上界封到 {@code tx}（旧版本从此不再被相信，但保留在版本史里）；</li>
     *   <li>左残余 {@code [行起点, portion 起点)} 以旧值重新插入；</li>
     *   <li>右残余 {@code [portion 终点, 行终点)} 以旧值重新插入；</li>
     *   <li>重叠的那段以新值插入。</li>
     * </ol>
     * 如果 portion 完全落在现有记录之外，抛异常（"改"一段不存在的区间多半是调用方算错了）。
     */
    public void updateForPortion(LocalDateTime tx, String key, V value, Period<LocalDate> portion) {
        requireClock(tx);
        List<TemporalRow<V>> touched = new ArrayList<TemporalRow<V>>();
        for (TemporalRow<V> row : currentRows(key)) {
            if (row.valid().overlaps(portion)) {
                touched.add(row);
            }
        }
        if (touched.isEmpty()) {
            throw new IllegalArgumentException("员工 " + key + " 在 " + TemporalRow.formatValid(portion)
                    + " 没有当前有效的记录，updateForPortion 无从下手（新增记录请用 append）。");
        }
        for (TemporalRow<V> row : touched) {
            replaceWithClosed(row, tx);
            LocalDate leftEnd = portion.start();
            if (row.valid().start().isBefore(leftEnd)) {
                rows.add(new TemporalRow<V>(key, row.value(),
                        Period.of(row.valid().start(), leftEnd), Period.<LocalDateTime>from(tx)));
            }
            LocalDate rightStart = portion.end();
            if (rightStart != null && (row.valid().end() == null || rightStart.isBefore(row.valid().end()))) {
                rows.add(new TemporalRow<V>(key, row.value(),
                        Period.of(rightStart, row.valid().end()), Period.<LocalDateTime>from(tx)));
            }
            LocalDate midStart = later(row.valid().start(), portion.start());
            LocalDate midEnd = earlier(row.valid().end(), portion.end());
            rows.add(new TemporalRow<V>(key, value, Period.of(midStart, midEnd), Period.<LocalDateTime>from(tx)));
        }
        clock = tx;
    }

    /**
     * 把 {@code [portion.start, portion.end)} 这段有效区间从"当前信念"里删掉，
     * 只保留左右两段残余，等价 SQL:2011 的 {@code DELETE ... FOR PORTION OF valid TIME}。
     *
     * <p>注意是<b>逻辑删除</b>：旧版本仍然留在版本史里，问"删之前系统怎么想"照样答得出。
     */
    public void deleteForPortion(LocalDateTime tx, String key, Period<LocalDate> portion) {
        requireClock(tx);
        List<TemporalRow<V>> touched = new ArrayList<TemporalRow<V>>();
        for (TemporalRow<V> row : currentRows(key)) {
            if (row.valid().overlaps(portion)) {
                touched.add(row);
            }
        }
        if (touched.isEmpty()) {
            throw new IllegalArgumentException("员工 " + key + " 在 " + TemporalRow.formatValid(portion)
                    + " 没有当前有效的记录，deleteForPortion 无从下手。");
        }
        for (TemporalRow<V> row : touched) {
            replaceWithClosed(row, tx);
            if (row.valid().start().isBefore(portion.start())) {
                rows.add(new TemporalRow<V>(key, row.value(),
                        Period.of(row.valid().start(), portion.start()), Period.<LocalDateTime>from(tx)));
            }
            LocalDate rightStart = portion.end();
            if (rightStart != null && (row.valid().end() == null || rightStart.isBefore(row.valid().end()))) {
                rows.add(new TemporalRow<V>(key, row.value(),
                        Period.of(rightStart, row.valid().end()), Period.<LocalDateTime>from(tx)));
            }
        }
        clock = tx;
    }

    // ------------------------------------------------------------------
    // 读操作
    // ------------------------------------------------------------------

    /** 当前信念（事务时间无上界的行），按 key、有效时间起点排序。 */
    public List<TemporalRow<V>> current() {
        return sorted(filterCurrent(null));
    }

    /** 当前信念里某个员工的钱，按有效时间排序。 */
    public List<TemporalRow<V>> history(String key) {
        return sorted(filterCurrent(key));
    }

    /**
     * 事务时刻 {@code txTime} 的系统状态：tx 区间包含该时刻的全部行。
     *
     * <p>回答"那一天系统里存的是什么"。
     */
    public List<TemporalRow<V>> asOf(LocalDateTime txTime) {
        if (txTime == null) {
            throw new IllegalArgumentException("事务时刻不能为 null");
        }
        List<TemporalRow<V>> result = new ArrayList<TemporalRow<V>>();
        for (TemporalRow<V> row : rows) {
            if (row.tx().contains(txTime)) {
                result.add(row);
            }
        }
        return sorted(result);
    }

    /** 当前信念里，员工 {@code key} 在 {@code date} 那天的取值；没有记录返回 {@link Optional#empty()}。 */
    public Optional<TemporalRow<V>> valueAt(String key, LocalDate date) {
        return find(filterCurrent(key), key, date);
    }

    /**
     * 双时态查询：<b>在事务时刻 {@code txTime}，系统认为</b>员工 {@code key} 在
     * {@code validDate} 那天的取值是多少。
     *
     * <p>这是"当时怎么想 + 什么时候的事"两个维度同时指定，也是双时态表存在的理由：
     * 同一个 validDate，换一个 txTime 可能得到不同答案。
     */
    public Optional<TemporalRow<V>> valueAsOf(String key, LocalDate validDate, LocalDateTime txTime) {
        if (txTime == null) {
            throw new IllegalArgumentException("事务时刻不能为 null");
        }
        List<TemporalRow<V>> rowsAtTx = new ArrayList<TemporalRow<V>>();
        for (TemporalRow<V> row : rows) {
            if (row.tx().contains(txTime)) {
                rowsAtTx.add(row);
            }
        }
        return find(rowsAtTx, key, validDate);
    }

    /** 某个员工的完整版本史（含已被封存的旧版本），按事务时间、有效时间排序。 */
    public List<TemporalRow<V>> versions(String key) {
        List<TemporalRow<V>> result = new ArrayList<TemporalRow<V>>();
        for (TemporalRow<V> row : rows) {
            if (row.key().equals(key)) {
                result.add(row);
            }
        }
        return sortedByTx(result);
    }

    /** 全表版本史（含旧版本），按事务时间、有效时间排序。 */
    public List<TemporalRow<V>> allVersions() {
        return sortedByTx(rows);
    }

    /** 表的物理行数（含已封存版本）。 */
    public int rowCount() {
        return rows.size();
    }

    // ------------------------------------------------------------------
    // 不变量自检
    // ------------------------------------------------------------------

    /**
     * 检查 5 条不变量，返回违反项描述；空列表表示一切正常。
     *
     * <ol>
     *   <li>区间非空（valid / tx 的下界都严格早于上界）；</li>
     *   <li>行的事务时间不晚于表的事务时钟；</li>
     *   <li>同一员工、<b>事务时间重叠</b>的两个版本，有效时间不允许重叠
     *       （这就是 PostgreSQL 排除约束 {@code EXCLUDE USING gist (emp_id WITH =, valid WITH &&, tx WITH &&)}
     *       在内存里的对应物）；</li>
     *   <li>没有完全重复的行；</li>
     *   <li>当前信念中同一员工的有效区间两两不重叠（由第 3 条与"当前行 tx 都是 [x, ∞)"可推出，这里再显式查一遍）。</li>
     * </ol>
     */
    public List<String> verify() {
        List<String> problems = new ArrayList<String>();
        for (int i = 0; i < rows.size(); i++) {
            TemporalRow<V> a = rows.get(i);
            if (a.valid().end() != null && !a.valid().start().isBefore(a.valid().end())) {
                problems.add("有效区间为空: " + a);
            }
            if (a.tx().end() != null && !a.tx().start().isBefore(a.tx().end())) {
                problems.add("事务区间为空: " + a);
            }
            if (clock != null && a.tx().start().isAfter(clock)) {
                problems.add("行的事务时间晚于事务时钟(" + clock + "): " + a);
            }
            for (int j = i + 1; j < rows.size(); j++) {
                TemporalRow<V> b = rows.get(j);
                if (!a.key().equals(b.key())) {
                    continue;
                }
                if (a.equals(b)) {
                    problems.add("重复行: " + a);
                }
                if (a.tx().isUnboundedEnd() && b.tx().isUnboundedEnd()) {
                    // 两条都是当前信念：其成对检查由下面的第 5 条单独负责，避免重复报错
                    continue;
                }
                if (a.valid().overlaps(b.valid()) && a.tx().overlaps(b.tx())) {
                    problems.add("同一员工、事务时间重叠的版本，有效期不允许重叠: " + a + " vs " + b);
                }
            }
        }
        // 第 5 条：当前信念中同一员工的有效期两两不重叠（显式再查一遍，便于报错定位）
        List<TemporalRow<V>> current = filterCurrent(null);
        for (int i = 0; i < current.size(); i++) {
            for (int j = i + 1; j < current.size(); j++) {
                TemporalRow<V> a = current.get(i);
                TemporalRow<V> b = current.get(j);
                if (a.key().equals(b.key()) && a.valid().overlaps(b.valid())) {
                    problems.add("当前信念中同一员工的有效期不允许重叠: " + a + " vs " + b);
                }
            }
        }
        return problems;
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /** 把 {@code row} 从事务时间上界封到 {@code tx}，替换掉原对象。 */
    private void replaceWithClosed(TemporalRow<V> row, LocalDateTime tx) {
        if (!rows.remove(row)) {
            throw new IllegalStateException("内部错误：要封存的行不在表里 " + row);
        }
        rows.add(row.withTxEnd(tx));
    }

    private List<TemporalRow<V>> filterCurrent(String key) {
        List<TemporalRow<V>> result = new ArrayList<TemporalRow<V>>();
        for (TemporalRow<V> row : rows) {
            if (row.tx().isUnboundedEnd() && (key == null || row.key().equals(key))) {
                result.add(row);
            }
        }
        return result;
    }

    private List<TemporalRow<V>> currentRows(String key) {
        return filterCurrent(key);
    }

    private Optional<TemporalRow<V>> find(List<TemporalRow<V>> candidates, String key, LocalDate date) {
        TemporalRow<V> hit = null;
        for (TemporalRow<V> row : candidates) {
            if (row.key().equals(key) && row.valid().contains(date)) {
                if (hit != null) {
                    throw new IllegalStateException("不变量被破坏：同一时刻存在两条有效记录 " + hit + " 与 " + row);
                }
                hit = row;
            }
        }
        return Optional.ofNullable(hit);
    }

    private List<TemporalRow<V>> sorted(Collection<TemporalRow<V>> source) {
        List<TemporalRow<V>> list = new ArrayList<TemporalRow<V>>(source);
        Collections.sort(list, new Comparator<TemporalRow<V>>() {
            @Override
            public int compare(TemporalRow<V> a, TemporalRow<V> b) {
                int c = a.key().compareTo(b.key());
                if (c != 0) {
                    return c;
                }
                c = a.valid().start().compareTo(b.valid().start());
                if (c != 0) {
                    return c;
                }
                return a.tx().start().compareTo(b.tx().start());
            }
        });
        return list;
    }

    /** 版本史的顺序：先事务时间（故事顺序），再有效时间——与 SQL 版 ORDER BY lower(tx), lower(valid) 一致。 */
    private List<TemporalRow<V>> sortedByTx(Collection<TemporalRow<V>> source) {
        List<TemporalRow<V>> list = new ArrayList<TemporalRow<V>>(source);
        Collections.sort(list, new Comparator<TemporalRow<V>>() {
            @Override
            public int compare(TemporalRow<V> a, TemporalRow<V> b) {
                int c = a.tx().start().compareTo(b.tx().start());
                if (c != 0) {
                    return c;
                }
                c = a.valid().start().compareTo(b.valid().start());
                if (c != 0) {
                    return c;
                }
                return a.key().compareTo(b.key());
            }
        });
        return list;
    }

    private void requireClock(LocalDateTime tx) {
        if (tx == null) {
            throw new IllegalArgumentException("事务时间不能为 null");
        }
        if (clock != null && !tx.isAfter(clock)) {
            throw new IllegalArgumentException("事务时间必须严格晚于上一次写入(" + clock
                    + ")：已经封存的历史不允许改写。");
        }
    }

    private static LocalDate later(LocalDate a, LocalDate b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.compareTo(b) >= 0 ? a : b;
    }

    private static LocalDate earlier(LocalDate a, LocalDate b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.compareTo(b) <= 0 ? a : b;
    }
}
