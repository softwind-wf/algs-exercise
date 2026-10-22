package com.ds.temporal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 演示：用一张双时态工资表走完 7 个真实场景，回答 4 类查询。
 *
 * <p>场景（与 {@code src/main/resources/sql/temporal_modeling_demo.sql} 完全一致）：
 * 研发部员工 E1001 从 2021 年入职起涨过几次薪，其中一次是 HR 拖到 2024 年才补录的追溯涨薪，
 * 还有一次是 2022 年那次涨薪的金额录入错误、2024 年才更正。员工 E1002 的入职日期也录错过一次。
 *
 * <p>运行：
 * <pre>
 * javac -encoding UTF-8 -d target/temporal-classes src/main/java/com/ds/temporal/*.java
 * java -Dfile.encoding=UTF-8 -cp target/temporal-classes com.ds.temporal.TemporalDemo
 * </pre>
 * 或直接跑 {@code scripts/run-temporal-demo.ps1}（含 JUnit 测试）。
 */
public final class TemporalDemo {

    private static final String E1 = "E1001"; // 张伟
    private static final String E2 = "E1002"; // 李娜
    private static final String LINE = "========================================================================";

    private TemporalDemo() {
    }

    public static void main(String[] args) {
        TemporalTable<Integer> table = new TemporalTable<Integer>();

        System.out.println(LINE);
        System.out.println("时态数据建模 · 一个具体例子");
        System.out.println("双时态 = 有效时间(valid time) + 事务时间(transaction time)");
        System.out.println("场景：研发部工资表。同一个问题，换一个“提问时刻”，答案不一样。");
        System.out.println(LINE);

        printTimeAxes();
        printEventPlan();

        // ---------------- 7 个事件 ----------------
        System.out.println();
        System.out.println("------------------------------------------------------------------------");
        System.out.println("【写入】7 次写入，每次都同时带“事务时间”和“有效区间”");
        System.out.println("------------------------------------------------------------------------");

        System.out.println();
        System.out.println("事件 1：张伟入职（2021-01-01 录入，现实从 2021-01-01 起生效）");
        System.out.println("  写: append(tx=2021-01-01 00:00, E1001, 12000, valid=[2021-01-01, ∞))");
        table.append(tx("2021-01-01T00:00"), E1, 12000, valid("2021-01-01", null));
        report(table);

        System.out.println();
        System.out.println("事件 2：李娜入职（2021-07-01 录入，现实从 2021-07-01 起生效）");
        System.out.println("  写: append(tx=2021-07-01 00:00, E1002, 10000, valid=[2021-07-01, ∞))");
        table.append(tx("2021-07-01T00:00"), E2, 10000, valid("2021-07-01", null));
        report(table);

        System.out.println();
        System.out.println("事件 3：张伟涨薪（2022-04-01 录入并生效）");
        System.out.println("  写: setValueFrom(tx=2022-04-01 00:00, E1001, 14000, from=2022-04-01)");
        System.out.println("      内部把 [2021-01-01, ∞) 切成 [2021-01-01, 2022-04-01) 12000 + [2022-04-01, ∞) 14000");
        table.setValueFrom(tx("2022-04-01T00:00"), E1, 14000, date("2022-04-01"));
        report(table);

        System.out.println();
        System.out.println("事件 4：张伟再涨薪（2024-01-05 录入，2024-01-01 起生效）");
        System.out.println("  写: setValueFrom(tx=2024-01-05 00:00, E1001, 18000, from=2024-01-01)");
        table.setValueFrom(tx("2024-01-05T00:00"), E1, 18000, date("2024-01-01"));
        report(table);

        System.out.println();
        System.out.println("事件 5：HR 补录一次“追溯涨薪”（2024-05-10 才录，现实从 2023-03-01 起生效）");
        System.out.println("  写: setValueFrom(tx=2024-05-10 00:00, E1001, 16000, from=2023-03-01)");
        System.out.println("      注意：系统自动把这刀截止到“下一次变化”2024-01-01，只改 [2023-03-01, 2024-01-01)");
        table.setValueFrom(tx("2024-05-10T00:00"), E1, 16000, date("2023-03-01"));
        report(table);

        System.out.println();
        System.out.println("事件 6：更正 2022 年那次涨薪的金额（2024-06-01 才发现应为 14500，不是 14000）");
        System.out.println("  写: updateForPortion(tx=2024-06-01 00:00, E1001, 14500, valid=[2022-04-01, 2023-03-01))");
        table.updateForPortion(tx("2024-06-01T00:00"), E1, 14500,
                valid("2022-04-01", "2023-03-01"));
        report(table);

        System.out.println();
        System.out.println("事件 7：更正李娜的入职日期（2024-07-01 发现 2021-07-01 是错的，应为 2021-08-01）");
        System.out.println("  写: deleteForPortion(tx=2024-07-01 00:00, E1002, valid=[2021-07-01, 2021-08-01))");
        System.out.println("      逻辑删除：只截掉错录的那一段，旧版本仍留在版本史里");
        table.deleteForPortion(tx("2024-07-01T00:00"), E2, valid("2021-07-01", "2021-08-01"));
        report(table);

        // ---------------- 四类查询 ----------------
        System.out.println();
        System.out.println("------------------------------------------------------------------------");
        System.out.println("【查询】同样的数据，四类问题");
        System.out.println("------------------------------------------------------------------------");

        System.out.println();
        System.out.println("查询 1（当前信念）：现在这张表认为每个人、每个时段拿多少钱");
        System.out.println("  SQL 对应: SELECT emp_id, salary, valid FROM temporal_salary WHERE upper_inf(tx)");
        printRows(table.current());

        System.out.println();
        System.out.println("查询 2（单时态 · 按有效日期）：现在认为某天拿多少钱");
        for (String d : new String[]{"2022-06-01", "2023-06-01", "2024-06-01"}) {
            System.out.println("  " + d + " -> " + show(table.valueAt(E1, date(d))));
        }

        System.out.println();
        System.out.println("查询 3（单时态 · 按事务时刻）：那天的系统“当时”相信什么");
        for (String t : new String[]{"2023-07-15T00:00", "2024-05-10T00:00", "2024-06-05T00:00"}) {
            System.out.println();
            System.out.println("  事务时刻 " + t.replace('T', ' ') + " 的系统状态：");
            printRows(table.asOf(tx(t)));
        }

        System.out.println();
        System.out.println("查询 4（双时态）：在事务时刻 tx，系统认为有效日期 valid 当天拿多少钱");
        printMatrix(table);

        System.out.println();
        System.out.println("  >> 同一个“2023-06-01 的工资”，三个答案：");
        System.out.println("     现在问          -> " + show(table.valueAt(E1, date("2023-06-01")))
                + "   (补录与更正都已入库)");
        System.out.println("     2023-07-15 问   -> " + show(table.valueAsOf(E1, date("2023-06-01"), tx("2023-07-15T00:00")))
                + "   (当时 2023-03 的涨薪还没录)");
        System.out.println("     2024-05-10 问   -> " + show(table.valueAsOf(E1, date("2023-06-01"), tx("2024-05-10T00:00")))
                + "   (补录已入库，但 2022 年的更正还没发生)");

        // ---------------- 版本史 ----------------
        System.out.println();
        System.out.println("------------------------------------------------------------------------");
        System.out.println("【版本史】E1001 的 8 个物理版本（旧版本只是被封上事务时间上界，没有删除）");
        System.out.println("------------------------------------------------------------------------");
        printVersionHistory(table.versions(E1));

        // ---------------- 碰撞示范 ----------------
        System.out.println();
        System.out.println("------------------------------------------------------------------------");
        System.out.println("【约束示范】想插一条与已有信念重叠的记录，必须被拒绝");
        System.out.println("------------------------------------------------------------------------");
        System.out.println("  尝试: append(tx=2024-08-01 00:00, E1001, 15000, valid=[2023-01-01, 2023-06-01))");
        try {
            table.append(tx("2024-08-01T00:00"), E1, 15000, valid("2023-01-01", "2023-06-01"));
            System.out.println("  !! 居然成功了 —— 说明时态键没有生效，这是 bug");
        } catch (IllegalArgumentException e) {
            System.out.println("  被拒绝: " + e.getMessage());
        }

        // ---------------- 结论 ----------------
        System.out.println();
        System.out.println(LINE);
        System.out.println("三句话总结：");
        System.out.println("  1. 只存当前值：答不出任何历史（2022 年拿多少？不知道）。");
        System.out.println("  2. 只加有效期：答得出“哪天拿多少”，但答不出“当时系统怎么想”，一改就覆盖旧认知。");
        System.out.println("  3. 双时态：两个时间维度都在，四类问题全答得出；代价是行数是版本数，不是人数。");
        System.out.println();
        System.out.println("  “涨薪”和“更正错误”在机制上是同一个操作：封掉旧版本 + 写一个新版本。");
        System.out.println("  区别只在故事里；数据库不需要知道，也最好不要让数据库去猜。");
        System.out.println();
        System.out.println("  约束（同一员工同一时刻只能有一份工资）在 PostgreSQL 里交给排除约束，");
        System.out.println("  在 Java 里交给 append 的检查与 verify()；两边都不靠“记得写 WHERE”。");
        System.out.println(LINE);
    }

    // ------------------------------------------------------------------
    // 输出辅助
    // ------------------------------------------------------------------

    private static void printTimeAxes() {
        System.out.println();
        System.out.println("【0.1】两条时间线");
        System.out.println("  有效时间 valid time  ：这份工资在现实世界里从哪天生效、到哪天失效。");
        System.out.println("  事务时间 tx time     ：数据库从什么时候开始相信这条记录。");
        System.out.println("  一次写入 = (事务时间, 员工, 月薪, 有效区间)。两个时间都要记，缺一个就少一类答案。");
    }

    private static void printEventPlan() {
        System.out.println();
        System.out.println("【0.2】7 次写入的计划（先看全貌，再看每一步的表）");
        System.out.println("  #  事务时间(tx)        员工    月薪    有效区间(valid)             现实里发生了什么");
        System.out.println("  1  2021-01-01 00:00   E1001   12000   [2021-01-01, ∞)           张伟入职");
        System.out.println("  2  2021-07-01 00:00   E1002   10000   [2021-07-01, ∞)           李娜入职");
        System.out.println("  3  2022-04-01 00:00   E1001   14000   [2022-04-01, ∞)           张伟涨薪");
        System.out.println("  4  2024-01-05 00:00   E1001   18000   [2024-01-01, ∞)           张伟再涨薪");
        System.out.println("  5  2024-05-10 00:00   E1001   16000   [2023-03-01, 2024-01-01)  追溯涨薪（补录）");
        System.out.println("  6  2024-06-01 00:00   E1001   14500   [2022-04-01, 2023-03-01)  更正 2022 年金额");
        System.out.println("  7  2024-07-01 00:00   E1002   10000   [2021-08-01, ∞)           更正入职日期");
        System.out.println("  注：第 5 条实际写入的是 [2023-03-01, ∞)，由 setValueFrom 自动截止到下一次变化；");
        System.out.println("      第 7 条是 deleteForPortion 截掉 [2021-07-01, 2021-08-01) 后剩下的部分。");
    }

    private static void report(TemporalTable<Integer> table) {
        System.out.println("  当前信念（事务时间无上界的行）：");
        printRows(table.current());
        List<String> problems = table.verify();
        System.out.println("  物理行数: " + table.rowCount() + "    不变量检查: "
                + (problems.isEmpty() ? "0 处违反" : problems.size() + " 处违反 " + problems));
    }

    private static void printRows(List<TemporalRow<Integer>> rows) {
        System.out.println("      "
                + String.format("%-5s | %6s | %-28s | %s", "emp", "salary", "valid interval", "tx interval"));
        for (TemporalRow<Integer> row : rows) {
            System.out.println("      " + formatRow(row));
        }
    }

    private static void printMatrix(TemporalTable<Integer> table) {
        String[] validDates = {"2022-06-01", "2023-06-01", "2024-06-01"};
        String[] txTimes = {"2023-07-15T00:00", "2024-05-10T00:00", "2024-06-05T00:00"};
        System.out.println("      valid \\ tx    | 2023-07-15    | 2024-05-10    | 2024-06-05");
        for (String v : validDates) {
            StringBuilder line = new StringBuilder("      " + v + "    |");
            for (String t : txTimes) {
                Optional<TemporalRow<Integer>> hit = table.valueAsOf(E1, date(v), tx(t));
                String cell = hit.isPresent() ? String.valueOf(hit.get().value()) : "-";
                line.append(String.format(" %-13s |", cell));
            }
            System.out.println(line);
        }
    }

    private static void printVersionHistory(List<TemporalRow<Integer>> rows) {
        System.out.println("      #   "
                + String.format("%-5s | %6s | %-28s | %s", "emp", "salary", "valid interval", "tx interval"));
        int i = 1;
        for (TemporalRow<Integer> row : rows) {
            System.out.println(String.format("      %-3d ", i) + formatRow(row));
            i++;
        }
    }

    private static String formatRow(TemporalRow<Integer> row) {
        return String.format("%-5s | %6d | %-28s | %s",
                row.key(), row.value(),
                TemporalRow.formatValid(row.valid()),
                TemporalRow.formatTx(row.tx()));
    }

    private static String show(Optional<TemporalRow<Integer>> row) {
        if (!row.isPresent()) {
            return "（无记录）";
        }
        TemporalRow<Integer> hit = row.get();
        return hit.value() + "（有效区间 " + TemporalRow.formatValid(hit.valid()) + "）";
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
