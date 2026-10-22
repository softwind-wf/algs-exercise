package com.ds.temporal;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TemporalTable} 的行为测试：写操作、四类查询、不变量，以及和演示里 3×3 答案矩阵的对账。
 *
 * <p>测试数据与 {@code TemporalDemo} / {@code temporal_modeling_demo.sql} 是同一组 7 个事件，
 * 所以这里的断言同时充当“Java 实现与 SQL 实现结果一致”的规格。
 */
public class TemporalTableTest {

    private static final String E1 = "E1001";
    private static final String E2 = "E1002";

    // ------------------------------------------------------------------
    // 写操作
    // ------------------------------------------------------------------

    @Test
    public void appendThenCurrentState() {
        TemporalTable<Integer> table = new TemporalTable<Integer>();
        table.append(tx("2021-01-01T00:00"), E1, 12000, valid("2021-01-01", null));

        assertEquals(1, table.current().size());
        assertEquals(1, table.rowCount());
        assertEquals(12000, valueAt(table, E1, "2021-01-01"));
        assertEquals(12000, valueAt(table, E1, "2030-01-01"), "无上界的记录对未来日期也有效");
        assertFalse(table.valueAt(E1, date("2020-12-31")).isPresent());
    }

    @Test
    public void appendRejectsOverlappingValidTime() {
        TemporalTable<Integer> table = new TemporalTable<Integer>();
        table.append(tx("2021-01-01T00:00"), E1, 12000, valid("2021-01-01", null));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> table.append(tx("2022-01-01T00:00"), E1, 13000, valid("2021-06-01", "2021-12-01")));
        assertTrue(error.getMessage().contains("重叠"), "错误消息应当说明是时态键重叠: " + error.getMessage());
        assertEquals(1, table.rowCount(), "被拒绝的写入不能留下半截数据");
    }

    @Test
    public void appendAllowsDisjointIntervals() {
        TemporalTable<Integer> table = new TemporalTable<Integer>();
        table.append(tx("2021-01-01T00:00"), E1, 12000, valid("2021-01-01", "2022-01-01"));
        table.append(tx("2022-01-01T00:00"), E1, 13000, valid("2023-01-01", "2024-01-01"));

        assertEquals(2, table.current().size(), "中间可以有空档：录入的只是已知的两段");
        assertFalse(table.valueAt(E1, date("2022-06-01")).isPresent(), "空档里的日期没有记录");
    }

    @Test
    public void setValueFromSplitsTimeline() {
        TemporalTable<Integer> table = new TemporalTable<Integer>();
        table.append(tx("2021-01-01T00:00"), E1, 12000, valid("2021-01-01", null));
        table.setValueFrom(tx("2022-04-01T00:00"), E1, 14000, date("2022-04-01"));

        assertEquals(Arrays.asList(12000, 14000), salaries(table.history(E1)));
        assertEquals(3, table.rowCount(), "1 条旧版本被封存 + 2 条当前行");
        TemporalRow<Integer> old = table.versions(E1).get(0);
        assertTrue(old.tx().end() != null, "旧版本的事务时间必须被上界封住");
        assertEquals(tx("2022-04-01T00:00"), old.tx().end());
    }

    @Test
    public void setValueFromRequiresCoveringRow() {
        TemporalTable<Integer> table = new TemporalTable<Integer>();
        assertThrows(IllegalArgumentException.class,
                () -> table.setValueFrom(tx("2021-01-01T00:00"), E1, 12000, date("2021-01-01")),
                "空表上没有覆盖行，setValueFrom 必须报错（应先用 append）");
    }

    @Test
    public void updateForPortionKeepsRemainders() {
        TemporalTable<Integer> table = new TemporalTable<Integer>();
        table.append(tx("2021-01-01T00:00"), E1, 12000, valid("2021-01-01", null));
        table.setValueFrom(tx("2022-04-01T00:00"), E1, 14000, date("2022-04-01"));
        table.setValueFrom(tx("2024-01-05T00:00"), E1, 18000, date("2024-01-01"));

        table.updateForPortion(tx("2024-06-01T00:00"), E1, 16000, valid("2023-03-01", "2024-01-01"));

        assertEquals(Arrays.asList(12000, 14000, 16000, 18000), salaries(table.history(E1)));
        assertEquals(7, table.rowCount());
        assertEquals(16000, valueAt(table, E1, "2023-06-01"));
        assertEquals(18000, valueAt(table, E1, "2024-06-01"), "只改指定区间，右侧 18000 不受影响");
        assertTrue(table.verify().isEmpty());
    }

    @Test
    public void updateForPortionOnUncoveredRangeThrows() {
        TemporalTable<Integer> table = new TemporalTable<Integer>();
        table.append(tx("2021-01-01T00:00"), E1, 12000, valid("2021-01-01", "2022-01-01"));

        assertThrows(IllegalArgumentException.class,
                () -> table.updateForPortion(tx("2022-01-01T00:00"), E1, 15000, valid("2023-01-01", "2024-01-01")),
                "改一段不存在的区间应当报错");
    }

    @Test
    public void deleteForPortionTrimsLeftEdge() {
        TemporalTable<Integer> table = new TemporalTable<Integer>();
        table.append(tx("2021-07-01T00:00"), E2, 10000, valid("2021-07-01", null));

        table.deleteForPortion(tx("2024-07-01T00:00"), E2, valid("2021-07-01", "2021-08-01"));

        assertEquals(1, table.current().size());
        assertEquals("[2021-08-01, ∞)",
                TemporalRow.formatValid(table.current().get(0).valid()));
        assertEquals(2, table.rowCount(), "旧版本仍在版本史里，逻辑删除不是物理删除");
        assertFalse(table.valueAt(E2, date("2021-07-15")).isPresent());
        assertEquals(10000, valueAt(table, E2, "2021-08-01"));
    }

    @Test
    public void deleteForPortionInMiddleSplitsIntoTwo() {
        TemporalTable<Integer> table = new TemporalTable<Integer>();
        table.append(tx("2021-01-01T00:00"), E2, 10000, valid("2021-01-01", null));

        table.deleteForPortion(tx("2022-01-01T00:00"), E2, valid("2022-01-01", "2022-03-01"));

        assertEquals(2, table.current().size());
        assertFalse(table.valueAt(E2, date("2022-02-01")).isPresent(), "被删的中间段没有记录");
        assertEquals(10000, valueAt(table, E2, "2021-12-31"));
        assertEquals(10000, valueAt(table, E2, "2022-03-01"));
    }

    @Test
    public void deleteForPortionOnUncoveredRangeThrows() {
        TemporalTable<Integer> table = new TemporalTable<Integer>();
        table.append(tx("2021-01-01T00:00"), E2, 10000, valid("2021-01-01", "2022-01-01"));

        assertThrows(IllegalArgumentException.class,
                () -> table.deleteForPortion(tx("2022-01-01T00:00"), E2, valid("2023-01-01", "2024-01-01")));
    }

    @Test
    public void clockMustAdvanceStrictly() {
        TemporalTable<Integer> table = new TemporalTable<Integer>();
        table.append(tx("2021-01-01T00:00"), E1, 12000, valid("2021-01-01", null));

        assertThrows(IllegalArgumentException.class,
                () -> table.append(tx("2021-01-01T00:00"), E2, 10000, valid("2021-07-01", null)),
                "同一事务时刻重复写入会被拒绝（版本先后无法区分）");
        assertThrows(IllegalArgumentException.class,
                () -> table.append(tx("2020-12-31T00:00"), E2, 10000, valid("2021-07-01", null)),
                "事务时间不能倒流（不允许改写已封存的历史）");
        table.append(tx("2021-01-02T00:00"), E2, 10000, valid("2021-07-01", null));
        assertEquals(2, table.rowCount());
    }

    // ------------------------------------------------------------------
    // 四类查询
    // ------------------------------------------------------------------

    @Test
    public void asOfReconstructsOldBelief() {
        TemporalTable<Integer> table = new TemporalTable<Integer>();
        table.append(tx("2021-01-01T00:00"), E1, 12000, valid("2021-01-01", null));
        table.setValueFrom(tx("2022-04-01T00:00"), E1, 14000, date("2022-04-01"));
        table.setValueFrom(tx("2024-01-05T00:00"), E1, 18000, date("2024-01-01"));
        table.setValueFrom(tx("2024-05-10T00:00"), E1, 16000, date("2023-03-01"));

        assertEquals(Arrays.asList(12000, 14000, 16000, 18000), salaries(table.asOf(tx("2024-05-10T00:00"))));
        assertEquals(14000, valueAsOf(table, E1, "2023-06-01", "2023-07-15T00:00"),
                "2023-07-15 时还不知道 2023-03 的补录");
        assertEquals(16000, valueAsOf(table, E1, "2023-06-01", "2024-05-10T00:00"));
        assertEquals(16000, valueAt(table, E1, "2023-06-01"), "当前信念里 2023-06-01 是 16000");
    }

    @Test
    public void retractionChangesHireDateAsOf() {
        TemporalTable<Integer> table = new TemporalTable<Integer>();
        table.append(tx("2021-07-01T00:00"), E2, 10000, valid("2021-07-01", null));
        table.deleteForPortion(tx("2024-07-01T00:00"), E2, valid("2021-07-01", "2021-08-01"));

        assertEquals(10000, valueAsOf(table, E2, "2021-07-15", "2024-06-05T00:00"),
                "更正前：系统认为入职日是 2021-07-01");
        assertFalse(table.valueAsOf(E2, date("2021-07-15"), tx("2024-07-05T00:00")).isPresent(),
                "更正后：入职日变成 2021-08-01，2021-07-15 查不到记录");
        assertEquals(10000, valueAsOf(table, E2, "2021-08-15", "2024-07-05T00:00"));
    }

    // ------------------------------------------------------------------
    // 完整时间线（7 个事件）与 3×3 矩阵
    // ------------------------------------------------------------------

    @Test
    public void fullTimelineCountsAndInvariants() {
        TemporalTable<Integer> table = fullTimeline();

        assertEquals(10, table.rowCount(), "E1001 8 个版本 + E1002 2 个版本");
        assertEquals(8, table.versions(E1).size());
        assertEquals(2, table.versions(E2).size());
        assertEquals(5, table.current().size(), "当前信念：E1001 4 段 + E1002 1 段");
        assertTrue(table.verify().isEmpty(), "完整时间线不应有任何不变量违反");
        assertEquals(Arrays.asList(12000, 14500, 16000, 18000), salaries(table.history(E1)));
        assertEquals(Arrays.asList(10000), salaries(table.history(E2)));
    }

    @Test
    public void fullTimelineMatrixMatchesDocumentedAnswers() {
        TemporalTable<Integer> table = fullTimeline();

        // valid 2022-06-01
        assertEquals(14500, valueAt(table, E1, "2022-06-01"), "当前信念里 2022 年那次已更正为 14500");
        assertEquals(14000, valueAsOf(table, E1, "2022-06-01", "2023-07-15T00:00"));
        assertEquals(14000, valueAsOf(table, E1, "2022-06-01", "2024-05-10T00:00"));
        assertEquals(14500, valueAsOf(table, E1, "2022-06-01", "2024-06-05T00:00"));
        // valid 2023-06-01
        assertEquals(16000, valueAt(table, E1, "2023-06-01"));
        assertEquals(14000, valueAsOf(table, E1, "2023-06-01", "2023-07-15T00:00"));
        assertEquals(16000, valueAsOf(table, E1, "2023-06-01", "2024-05-10T00:00"));
        assertEquals(16000, valueAsOf(table, E1, "2023-06-01", "2024-06-05T00:00"));
        // valid 2024-06-01
        assertEquals(18000, valueAt(table, E1, "2024-06-01"));
        assertEquals(14000, valueAsOf(table, E1, "2024-06-01", "2023-07-15T00:00"),
                "旧信念里的 [2022-04-01, ∞) 会一直延伸到未来");
        assertEquals(18000, valueAsOf(table, E1, "2024-06-01", "2024-05-10T00:00"));
        assertEquals(18000, valueAsOf(table, E1, "2024-06-01", "2024-06-05T00:00"));
    }

    @Test
    public void everyWriteKeepsInvariants() {
        TemporalTable<Integer> table = new TemporalTable<Integer>();
        table.append(tx("2021-01-01T00:00"), E1, 12000, valid("2021-01-01", null));
        assertTrue(table.verify().isEmpty());
        table.append(tx("2021-07-01T00:00"), E2, 10000, valid("2021-07-01", null));
        assertTrue(table.verify().isEmpty());
        table.setValueFrom(tx("2022-04-01T00:00"), E1, 14000, date("2022-04-01"));
        assertTrue(table.verify().isEmpty());
        table.setValueFrom(tx("2024-01-05T00:00"), E1, 18000, date("2024-01-01"));
        assertTrue(table.verify().isEmpty());
        table.setValueFrom(tx("2024-05-10T00:00"), E1, 16000, date("2023-03-01"));
        assertTrue(table.verify().isEmpty());
        table.updateForPortion(tx("2024-06-01T00:00"), E1, 14500, valid("2022-04-01", "2023-03-01"));
        assertTrue(table.verify().isEmpty());
        table.deleteForPortion(tx("2024-07-01T00:00"), E2, valid("2021-07-01", "2021-08-01"));
        assertTrue(table.verify().isEmpty());
        assertTrue(table.verify().isEmpty(), "每步之后都必须没有不变量违反");
    }

    // ------------------------------------------------------------------
    // Period 本身的边界语义
    // ------------------------------------------------------------------

    @Test
    public void periodEdgeCases() {
        Period<Integer> p = Period.of(1, 5);
        assertTrue(p.contains(1));
        assertTrue(p.contains(4));
        assertFalse(p.contains(5), "右端点不属于区间");
        assertFalse(p.contains(0));

        Period<Integer> next = Period.of(5, 9);
        assertFalse(p.overlaps(next), "首尾相接不算重叠");
        assertTrue(p.adjacentTo(next));
        assertTrue(p.overlaps(Period.of(4, 6)));

        Period<Integer> open = Period.from(3);
        assertTrue(open.contains(100));
        assertTrue(open.overlaps(Period.of(50, 60)));
        assertTrue(open.toString().contains("∞"));

        assertThrows(IllegalArgumentException.class, () -> Period.of(5, 5));
        assertThrows(IllegalArgumentException.class, () -> Period.of(6, 5));
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private static TemporalTable<Integer> fullTimeline() {
        TemporalTable<Integer> table = new TemporalTable<Integer>();
        table.append(tx("2021-01-01T00:00"), E1, 12000, valid("2021-01-01", null));
        table.append(tx("2021-07-01T00:00"), E2, 10000, valid("2021-07-01", null));
        table.setValueFrom(tx("2022-04-01T00:00"), E1, 14000, date("2022-04-01"));
        table.setValueFrom(tx("2024-01-05T00:00"), E1, 18000, date("2024-01-01"));
        table.setValueFrom(tx("2024-05-10T00:00"), E1, 16000, date("2023-03-01"));
        table.updateForPortion(tx("2024-06-01T00:00"), E1, 14500, valid("2022-04-01", "2023-03-01"));
        table.deleteForPortion(tx("2024-07-01T00:00"), E2, valid("2021-07-01", "2021-08-01"));
        return table;
    }

    private static List<Integer> salaries(List<TemporalRow<Integer>> rows) {
        List<Integer> values = new ArrayList<Integer>();
        for (TemporalRow<Integer> row : rows) {
            values.add(row.value());
        }
        return values;
    }

    private static int valueAt(TemporalTable<Integer> table, String key, String date) {
        Optional<TemporalRow<Integer>> row = table.valueAt(key, date(date));
        assertTrue(row.isPresent(), key + " 在 " + date + " 应当有记录");
        return row.get().value();
    }

    private static int valueAsOf(TemporalTable<Integer> table, String key, String validDate, String txTime) {
        Optional<TemporalRow<Integer>> row = table.valueAsOf(key, date(validDate), tx(txTime));
        assertTrue(row.isPresent(), key + " 在 " + validDate + " / tx " + txTime + " 应当有记录");
        return row.get().value();
    }

    private static LocalDate date(String iso) {
        return LocalDate.parse(iso);
    }

    private static LocalDateTime tx(String iso) {
        return LocalDateTime.parse(iso);
    }

    private static Period<LocalDate> valid(String from, String toOrNull) {
        return Period.of(date(from), toOrNull == null ? null : date(toOrNull));
    }
}
