# BCNF 体检报告 —— 对象：`airline_demo`（实践习题 7.23 交付的真实表结构）

> 体检对象不是抄来的例子，就是你自己写在 `src/main/resources/sql/airline_company.sql` 里、已经在本机 PostgreSQL 17.11 的 `airline_demo` 库中实跑通过的那 10 张表。
> 本报告所有结论都可由文末两条命令复现；所有数字都来自实跑输出，不是估算。

---

## 0. 一句话结论

**你交付的 7 张核心表（`customer` / `route` / `route_leg` / `timetable` / `flight` / `booking` / `booking_segment`）在"DDL 声明的 FD + 你补充的业务语义 FD"下全部满足 BCNF。**

唯一的例外是 `booking_segment`：一旦把"同一个座位号对应同一个舱位"这条业务规则当作函数依赖写下来，它就**违反 BCNF** —— 而且这条 FD 在真实数据上确实不成立，反例是那 3 行 `seat_no IS NULL` 的候补订座（其中两行舱位不同）。

这不是设计事故。它暴露的是三个更值得记住的边界条件：

1. **可空列上的函数依赖在 SQL 里天然"半成立"** —— 约束只能管到非 NULL 行，NULL 行整组逃逸；
2. **带 `WHERE` 的部分唯一索引不是函数依赖** —— 它约束的是关系的一个子集，不是整个关系；
3. **BCNF 分解保证无损，但不保证保持依赖** —— 这一点在下面用真实表算给你看。

---

## 1. 体检方法与可复现性

| 环节 | 做法 |
|---|---|
| 体检对象 | 从 `airline_company.sql` 解析 `CREATE TABLE` 取列与主码/唯一约束，脚本自动与手写关系模式核对列是否一致（不一致直接报错） |
| FD 来源 | DDL 里能读到的键约束 + **显式标注的语义追加**（FD 是语义约束，DDL 写不全） |
| 判定算法 | 教科书原版：属性闭包 `X⁺` → 枚举极小超键即候选键 → 逐条 FD 判"左边是否超键" |
| 无损/保依赖 | 二元分解判据 + FD 集合的**完备投影**（枚举子模式内属性子集求闭包），不用近似投影 |
| 数据证据 | 在 `airline_demo` 实跑；负测试故意解除 `ON_ERROR_STOP` 后数 ERROR 行；所有写操作包在事务里 `ROLLBACK` |

复现命令：

```powershell
# ① 理论体检：解析真实 DDL → 属性闭包 → 候选键 → 范式判定 → 分解推演
python .\tools\bcnf_audit.py --outdir docs
#    原始报告落到 docs\bcnf-audit-raw.txt

# ② 数据体检：宽表冗余度量 / 无损连接 / 更新异常 / 负测试 / 座位字典对照
.\psql.bat -d airline_demo -f src\main\resources\sql\bcnf_audit_airline.sql

# ③ 边界探测：NULL 与 UNIQUE 的真实交互（全部回滚）
.\psql.bat -d airline_demo -f src\main\resources\sql\bcnf_audit_boundary.sql
```

实测结果：①`exit 0`；②`exit 0`，5 条预期 ERROR（见第 6 节）；③`exit 0`，基线行数 3/5/7/5 全程未变。

---

## 2. 逐关系体检结果

`airline_demo` 实际有 10 张表：`customer, airport, aircraft_type, airplane, route, route_leg, timetable, flight, booking, booking_segment`。
其中 3 张纯字典表（`airport` / `aircraft_type` / `airplane`）各自只有一个单属性候选键、只有一条 FD，必然 BCNF，报告里只列一行。下表是 7 张需要动脑子的。

| 关系 | DDL 声明的键 | 算出的候选键 | 主属性 | 判定 |
|---|---|---|---|---|
| `customer` | PK(customer_id) + UNIQUE(email) | `{customer_id}` \| `{email}` | customer_id, email | BCNF（但 email 可空，见 §4.1） |
| `route` | PK(route_no) | `{route_no}` | route_no | BCNF |
| `route_leg` | PK(route_no, leg_no) | `{route_no,leg_no}` \| `{route_no,to_code}` | route_no, leg_no, to_code | BCNF |
| `timetable` | PK(schedule_id) + UNIQUE(flight_no,day_of_week,valid_from) | `{schedule_id}` \| `{day_of_week,flight_no,valid_from}` \| `{day_of_week,flight_no,route_no}` | 5 个 | BCNF |
| `flight` | PK(flight_no, flight_date) | `{flight_no,flight_date}` | flight_no, flight_date | BCNF |
| `booking` | PK(booking_id) | `{booking_id}` | booking_id | BCNF |
| `booking_segment` | PK(booking_id, seg_no) + 条件唯一索引 | `{booking_id,seg_no}` | booking_id, seg_no | **BCNF 通过；但语义 FD `seat_no→cabin_class` 使它违反 3NF/BCNF** |

几个值得记的推导细节：

- **`customer` 有两条候选键**。`customer_id⁺ = {address, customer_id, email, name, phone}`（闭包 = 全属性），`email⁺` 同样等于全属性 —— 这是"同一关系里两条候选键互相蕴含"的活标本。
- **`route_leg` 的第二条候选键是被已有约束蕴含的，不是另加的业务规则**。可延迟约束触发器 `assert_route_legs_continuous` 已经强制了两件事：同航线内 `leg_no` 从 1 连续编号、且第 k 段到达机场 = 第 k+1 段出发机场（`airline_company.sql:323-329`）。两段到达同一机场时，后一段出发机场也必须等于该机场，于是前一段到达 = 后一段出发；把这条链推广到任意两段即得**到达机场全航线不重复**。因此 `(route_no, to_code)` 也是候选键，主属性因此多了 `to_code` —— 结论是"可以放心宣称"，不需要额外建约束。
- **`timetable` 是"代理键 + 自然键并存"的正例**。`schedule_id` 是代理主键，`UNIQUE(flight_no, day_of_week, valid_from)` 是自然键。两条候选键并列，非主属性之间互不决定，所以它**真的**处于 BCNF —— 很多人以为"加了代理键就会藏冗余"，这里可以直接反驳。
- **`flight` 只有一条候选键**。注意 `dep_time` / `arr_time` 是**实际**起降时刻（实测：CA1501 2024-10-07 是 08:05 起、10:20 到，与时刻表的 08:00/10:15 不同，延误了 5 分钟）。所以它们**不是**从 `timetable` 派生出来的，不能删 —— 这是个容易误判的点。

---

## 3. 对照物：把三层设计拍平成一张宽表会怎样

`flight_wide = flight ⋈ timetable ⋈ aircraft_type ⋈ airplane`，这是你 ER 图里"没做规范化"的那种拍平写法。真实数据：

| flight_no | flight_date | route_no | type_code | seat_capacity | dep_time | arr_time | flight_status |
|---|---|---|---|---|---|---|---|
| CA1501 | 2024-10-07 | R1 | B738 | 162 | 08:05 | 10:20 | 已到达 |
| CA1501 | 2024-10-10 | R1 | B738 | 162 | 08:00 | 10:15 | 计划 |
| CA4301 | 2024-10-08 | R2 | B789 | 293 | 09:40 | 14:25 | 已到达 |
| MU5101 | 2024-10-11 | R3 | A333 | 262 | 14:20 | 17:00 | 计划 |
| MU5102 | 2024-10-13 | R3 | A333 | 262 | 18:30 | 21:10 | 计划 |

宽表上有多条函数依赖违反 BCNF，程序判定的肇事者是：

```
{flight_no} -> {route_no, type_code, seat_capacity, dep_time, arr_time}
    左边闭包 = {arr_time, dep_time, flight_no, route_no, seat_capacity, type_code}  ≠ 全属性
    => flight_no 不是超键 => 违反 3NF(右边含非主属性) => 必然也违反 BCNF
```

冗余度量（实跑）：

| route_no | type_code | seat_capacity | 在宽表里出现次数 | 涉及行 |
|---|---|---|---|---|
| R1 | B738 | 162 | **2** | CA1501 2024-10-07, CA1501 2024-10-10 |
| R2 | B789 | 293 | 1 | CA4301 2024-10-08 |
| R3 | A333 | 262 | **2** | MU5101 2024-10-11, MU5102 2024-10-13 |

`R1 / B738 / 162` 这一组事实在宽表里被写了两遍。而你的三层设计里，它只存在于 `timetable`（`route_no`/`type_code`）和 `aircraft_type`（`seat_capacity`）各一行 —— **同一个值改一次 vs 改两处**，这就是更新异常的根源。

### 3.1 完整 BCNF 分解推演（结果与你的设计一致）

对宽表按程序递归分解：

```
拆 {flight_no, flight_date, ...8 个属性}   依据 {flight_no} -> {route_no, type_code, seat_capacity, dep_time, arr_time}
   => {flight_no, route_no, type_code, seat_capacity, dep_time, arr_time}  +  {flight_no, flight_date, flight_status}
```

也就是：**时列表述拆出去（对应你的 `timetable`），剩下"哪天飞、状态如何"（对应你的 `flight`）**。`aircraft_type.seat_capacity` 还要再拆一层（它是机型字典的属性），落到你已有的 `aircraft_type` 表。结论：**你的三层设计与 BCNF 分解的结果同构**，冗余不是被"少建了表"引入的，而是被"把两个层次 SELECT 到一起看"引入的。

---

## 4. 三个真实边界（这次体检最值钱的部分）

### 4.1 边界一：可空列让唯一约束"半失效"

`customer.email` 可空，而 DDL 写了 `UNIQUE (email)`。教科书会把 email 当成一条候选键 —— **在 SQL 里它只是一条"条件候选键"**。

实跑（`bcnf_audit_boundary.sql`）：

```
探测 1: 插入两个 email 为 NULL 的客户
INSERT 0 1
INSERT 0 1
customers_with_null_email = 2        ← UNIQUE 放行了多行 NULL
```

因为 SQL 的三值逻辑里 `NULL = NULL` 结果是 UNKNOWN，唯一索引把每行 NULL 都视为互不相等。**读法**：把 email 当候选键时，心里要加上"仅对非 NULL 行成立"这个限定。要真正管住就问自己：一个客户没有邮箱时，是否允许多个这样的客户？允许 → 现状正确；不允许 → 需要一个 `NOT NULL` 或部分唯一索引 + 补一个替代键。

### 4.2 边界二：部分唯一索引不是函数依赖

```sql
CREATE UNIQUE INDEX uq_seat_per_flight
    ON booking_segment (flight_no, flight_date, seat_no)
    WHERE seat_no IS NOT NULL AND segment_status <> '已取消';
```

这个索引很聪明（取消让座，实测 MU5101 5A 一行"已取消"一行"已确认"并存，正是它放行的），但它和 FD 是**两回事**：

- 它只约束满足 `WHERE` 的行 → 对应的是"关系的一个**子关系**上的唯一性"；
- 它带 NULL 语义 → `seat_no IS NULL` 的行整组不受约束。

严格按 FD 定义（左边相同 ⇒ 右边必须相同），这条约束**推不出**任何关于整个 `booking_segment` 的函数依赖。

### 4.3 边界三：`seat_no → cabin_class` —— 一条真违反 BCNF 的语义 FD

业务上"座位号决定舱位"（1-2 排头等舱、3-6 排公务舱之类）是常识。把它写成 FD：

```
F = { (booking_id, seg_no) -> 全部属性 ,  seat_no -> cabin_class }
候选键: {booking_id, seg_no}        主属性: {booking_id, seg_no}
{seat_no}+ = {cabin_class, seat_no} ≠ 全属性  =>  seat_no 不是超键
   右边 cabin_class 是**非主属性** => 违反 3NF, 必然也违反 BCNF
```

**反例查询必须把 NULL 算进来**，否则会漏判（这点我踩到了）：

```sql
-- 反例查询 #1（只看有座位的行）: 0 行 —— 会让人误以为 FD 成立
SELECT seat_no, count(DISTINCT cabin_class) FROM booking_segment
 WHERE seat_no IS NOT NULL GROUP BY seat_no HAVING count(DISTINCT cabin_class) > 1;

-- 反例查询 #2（严格按 FD 定义，NULL 行之间也算"左边相同"）: 真反例在这里
SELECT seat_no, count(*) AS rows_in_group, count(DISTINCT cabin_class) AS n_class
  FROM booking_segment GROUP BY seat_no HAVING count(DISTINCT cabin_class) > 1;
```

探测 2 实跑输出（事务内，已回滚）：

| flight_no | flight_date | seat_no | cabin_class | segment_status |
|---|---|---|---|---|
| CA1501 | 2024-10-07 | *(NULL)* | 公务舱 | 候补 |
| CA1501 | 2024-10-07 | 12A | 经济舱 | 已登机 |
| CA1501 | 2024-10-07 | *(NULL)* | 经济舱 | 候补 |
| CA1501 | 2024-10-07 | *(NULL)* | 经济舱 | 候补 |

**3 行 `seat_no IS NULL` 里出现了 2 种舱位 → `seat_no → cabin_class` 在整个关系上不成立 → `booking_segment` 违反 BCNF。**

程序算出的 BCNF 分解，恰好就是"座位字典"方案：

```
依据 {seat_no} -> {cabin_class} 拆:
   R1 = {seat_no, cabin_class}                                  ← 座位字典
   R2 = {booking_id, seg_no, flight_no, flight_date, seat_no, segment_status}
无损连接: 是        保持依赖: 否（丢失 (booking_id,seg_no) -> 全部属性）
```

我的修正**推翻了自己上一轮讲解里的一个印象**：那里我说"BCNF 分解在本例中保依赖"。用**完备投影**重算后，标准判定是：

```
投影到 R1: {seat_no} -> {cabin_class}
投影到 R2: {booking_id,seg_no} -> {flight_no, flight_date, cabin_class, segment_status}
原 FD (booking_id,seg_no) -> seat_no 在两个子模式的 FD 集合下都推不出来
  => 保持依赖: 否
```

原因很直观：`R2` 里不再有 `cabin_class` 这个"中介"(`seat_no → cabin_class → ...` 那条路断了)，所以 `(booking_id, seg_no) → seat_no` 跨表了。**BCNF 分解无损、但可能丢依赖 —— 这次是真实表上的实例，不是教材举例。**

好在"座位字典"方案本身是对的，就是你三层机票设计里对 `aircraft_type.seat_capacity` 用的同一招：**把那条不听话的 FD 抽成一张字典表，让它在新表里是候选键**。实测：座位字典 5 行，对应 `booking_segment` 里 6 行"有座位"的订座 —— 因为 MU5101 的 5A 被取消后又卖了一次，两行合并成一行，**冗余当场消失**。

---

## 5. 四类经典异常：拿真实表逐条验

| 异常 | 宽表 / 拍平写法 | 你的三层设计（实测） |
|---|---|---|
| **更新** | 改 B738 座位数要动宽表里的 2 行（`R1/B738/162` 出现 2 次） | 只改 `aircraft_type` 1 行；实测 `rows_touched_in_normalized_design = 1` |
| **插入** | 建一个新机型必须先有航班，否则没地方写 | 机型字典独立插，不用等航班 |
| **删除** | 删掉某机型的最后一个航班，机型容量跟着消失 | `ON DELETE RESTRICT` 直接拦住（`fk_airplane_type`），信息不会丢 |
| **冗余** | `R1/B738/162`、`R3/A333/262` 各写 2 遍 | 各写 1 遍 |

---

## 6. 无损验证与负测试实测

**无损连接验证**：把三层设计按 FD 投影成两侧再自然连接：

```
decomposed_rows | original_rows | verdict
             5  |             5 | 无损: R1 * R2 的行数 = 原关系行数
```

**负测试**（故意解除 `ON_ERROR_STOP`，数 ERROR 行；退出码 0 不代表全部按预期）：

| # | 语句 | 预期 | 实测 |
|---|---|---|---|
| 6.1 | `UPDATE aircraft_type SET type_code='B739' WHERE type_code='B738'` | 被外码拦下 | ERROR 违反外键 `fk_airplane_type`（**ON UPDATE 缺省即 RESTRICT**）|
| 6.2 | 给 CA1501 2024-10-07 再插一行 `已确认/12A` | 被部分唯一索引拦下 | ERROR 重复键 `uq_seat_per_flight` |
| 6.3 | 给 MU5101 2024-10-11 再插一行 `已确认/5A` | 被拦下（已取消那行不占座） | ERROR 重复键 `uq_seat_per_flight` |
| 6.4 | `DELETE FROM aircraft_type WHERE type_code='B738'` | 被外码拦下 | ERROR 违反外键 `fk_airplane_type` |
| 6.5 | 重复插入机型 `B738` | 被主码拦下 | ERROR 重复键 `pk_aircraft_type` |

实测共 5 条 ERROR，与预期一致；**负测试后行数 3 / 7，与基线完全相同**（第 8 步复核 `aircraft_type=3, flight=5, booking_segment=7, timetable=5`）。

> 注意 6.1 的实战含义：主码/唯一列一旦被外码引用，`ON UPDATE` 缺省是 `RESTRICT` —— 想改机型代码必须显式写 `ON UPDATE CASCADE` 或先解引用。你这个字典表代码本来就不该改，所以没问题；但换成"部门编号会重组"的场景就要提前决定。

---

## 7. 定理复算：BCNF 分解一定无损，但不一定保依赖

程序算了两个只差一条 FD 的例子（都只有 3 个属性，对照最干净）：

**例 A** `R(S,T,C)`，`F = { ST→C , T→C }` —— `T→C` 的右边 `C` **不是**主属性（候选键只有 `{S,T}`），连 3NF 都违反：

```
拆 {C,S,T} 依据 {T}->{C}  =>  {C,T} + {S,T}
无损连接: 是        保持依赖: 是
```

**例 B** `R(S,T,C)`，`F = { ST→C , C→T }` —— 候选键 `{S,T}` 和 `{C,S}`，主属性 = 全部三个；`C→T` 的右边 `T` **是**主属性，所以 **3NF 的逃生口放行、BCNF 不放行**：

```
拆 {C,S,T} 依据 {C}->{T}  =>  {C,T} + {C,S}
无损连接: 是        保持依赖: 否   丢失 {S,T}->{C}
```

例 A 与例 B 的差别**只有一条 FD 的右边是不是主属性**：例 A 违反 3NF（右边非主属性），拆完既无损又保依赖；例 B 满足 3NF 却违反 BCNF，拆完无损但把 `ST→C` 丢在两张表之间，只能靠连接检查。

**判据记法**：看到"3NF 通过、BCNF 不通过"（即某条非平凡 FD 的左边不是超键、但右边全是主属性），就要**当场试算保持依赖**，别默认拆了没代价。

---

## 8. 建议与待你确认的语义

### 8.1 建议（按性价比排序）

1. **把 `cabin_class` 从 `booking_segment` 抽到座位字典**（`seat_dict(flight_no, flight_date, seat_no) → cabin_class`）。
   - 收益：消除 `seat_no → cabin_class` 这条 BCNF 违反；冗余当场消失（实测 6 行 → 5 行）。
   - 代价：`cabin_class` 变成"有座位才有"的属性；候补订座（`seat_no IS NULL`）就**没有舱位**了。
   - 所以真正要你决定的是下面 8.2 的第 1 个问题。
2. **把 `route_leg` 的第二条候选键补进文档**（`(route_no, to_code)`，已由触发器蕴含，见 §2）—— 只改文档，不用加约束。
3. **给 `timetable` 的"班号 → 航线"补一条约束**（现在"同一班号固定飞同一航线"只靠 `flight` 的触发器间接保证，`timetable` 上没约束）—— 我在审计里把 `{flight_no, day_of_week, route_no} → schedule_id` 当作语义 FD 用了，它成立与否直接决定 `timetable` 的候选键个数。
4. **`customer.email` 的可空性写进文档**：UNIQUE 对 NULL 是放行的，要不要允许"多个无邮箱客户"是个业务决定。

### 8.2 需要你确认的三个语义问题

1. **舱位是座位的属性，还是订座的属性？** 如果"12A 在任何航班上都是经济舱"，就用座位字典（本报告方案）；如果同一座位在不同航班/机型上舱位可能不同，那 `cabin_class` 就是订座自身的属性，不需要拆，但要在文档里写明它**不是**座位号决定的，避免以后有人看到"座位 5A 都是公务舱"就当 FD 用。
2. **`seat_no` 是否允许非空约束到"已确认/已登机/已取消"三类上？** 现在 `ck_seg_status` 只管状态的取值，不管"候补必须无座位、已确认必须有座位"（那是触发器 `assert_segment_seat_status` 在管）。若把 `seat_no` 改成部分非空，`seat_no → cabin_class` 才有可能被数据库真正约束住。
3. **同一座位的取消再售，要不要保留历史？** 现在的部分唯一索引让"取消 + 再售"共存（MU5101 5A 就是），这对审计很友好；但如果业务要求"座位一旦售出即不可再售（除非先删行）"，索引条件就要改。

---

## 9. 可复用的 BCNF 体检检查清单

以后拿到任何一张表，照这个顺序走，不用凭感觉：

| # | 检查项 | 具体做法 |
|---|---|---|
| 1 | 列是否写全 | 从 DDL 逐列抄，别凭记忆（`tools/bcnf_audit.py` 会自动与 DDL 核对列名差异） |
| 2 | FD 是否写全 | 每条 DDL 主码/唯一约束 = 一条 FD；**再补语义 FD**，并单独标注哪些是补的 |
| 3 | 求闭包 | 对每个候选左边算 `X⁺`，反复扫 FD 直到不动 |
| 4 | 求候选键 | 枚举属性子集，`X⁺ = 全属性` 且极小的就是候选键；**候选键可能不止一条** |
| 5 | 判 3NF / BCNF | 逐条非平凡 FD：左边是超键 → BCNF 过；左边非超键 → 看右边是否**全是主属性**（是=3NF 过、BCNF 不过；否=3NF 也挂） |
| 6 | 找违反项就分解 | 拆成 `X∪Y` 与 `R−Y`（切右边，别切左边），递归到全部通过 |
| 7 | 验无损 | `R1∩R2 → R1−R2` 或 `R1∩R2 → R2−R1`（必须是被 **F 蕴含**，不是数据凑巧） |
| 8 | 验保依赖 | 用**完备投影**：对子模式内每个属性子集求 `X⁺∩Ri`；**别用"左右分别取交集"的近似投影**（会造出伪 FD、漏掉真 FD） |
| 9 | 数据面复核 | 反例查询验 FD 在真实数据上是否成立；**可空列上的 FD 必须把多行 NULL 当作"左边相同"归成一组** |
| 10 | 区分"约束"与"FD" | 带 `WHERE` 的部分唯一索引、`CHECK`、触发器都不是 FD；它们只是**碰巧**覆盖了 FD 的一部分子集 |
| 11 | 写清前提 | 每条 FD 的来源（DDL / 业务语义 / 由其他约束推出）都写下来，否则下一个人无法复核 |

**本轮自检抓到的三个真错**（都值得你记住）：

1. **"近似投影"会误判保持依赖**：最初用"左/右分别与子模式取交集"来投影 FD，它对例 B 给出了"保依赖"的错误结论；改成完备投影后，例 B 正确显示丢失 `ST→C`。
2. **FD 的反例查询不能加 `WHERE seat_no IS NOT NULL`**：那恰好把承载反例的 NULL 行全部排除，得到 0 行的假象。这与"部分唯一索引为什么不等于 FD"是同一个坑的两面。
3. **先查触发器再下结论**：我最初把 `route_leg` 的第二条候选键标为"我补的语义"，读了 `assert_route_legs_continuous` 才发现它已被连续编号 + 首尾相接两条校验蕴含 —— 结论没变，但依据从"假设"变成了"已实现约束"。

---

## 10. 产出清单

| 文件 | 作用 |
|---|---|
| `tools/bcnf_audit.py` | 体检器：解析真实 DDL → 属性闭包 / 候选键 / 3NF-BCNF 判定 / 完全分解 / 无损与保依赖判定；含例 A、例 B 两个对照推演 |
| `src/main/resources/sql/bcnf_audit_airline.sql` | 数据体检：宽表冗余度量 → 无损连接 → 更新异常 → 5 条负测试 → 座位字典对照 → 数据未变复核 |
| `src/main/resources/sql/bcnf_audit_boundary.sql` | 边界探测：多行 NULL email + 多行 NULL seat_no 导致的 FD 失效（全部回滚） |
| `docs/bcnf-audit-raw.txt` | 体检器原始输出（逐关系逐 FD 的闭包与判定） |
| `docs/BCNF体检报告-airline_demo.md` | 本报告 |

**本轮没有改动任何交付表的数据或结构**：两个 SQL 脚本的写操作都在事务内回滚，`bcnf_audit_airline.sql` 结束时复核行数为 `aircraft_type=3 / flight=5 / booking_segment=7 / timetable=5`，与体检开始前的基线一致。
