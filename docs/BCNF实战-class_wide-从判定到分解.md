# BCNF 实战：一张真实宽表 class_wide 的判定、分解与迁移验证

> 对象：PostgreSQL 库 `bcnf_class_demo` 中的 `class_wide`（11 个属性、11 行数据）。
> 脚本：`src/main/resources/sql/bcnf_class_wide.sql`；原始输出：`docs/bcnf-class-raw.txt`。
> 对应教材：《数据库系统概念》8.5 节 —— BCNF 判定（第 198 页）与 BCNF 分解算法（图 8-11）。

## 1. 场景：教务系统里的"宽表"

教务系统里常见一种手工维护/报表导出的宽表：一门课的课程名、开课系、学分，随每个开课班重复；
每节课所在教室的容量，随每节课重复。`class_wide` 就是这种表：

| 属性 | 含义 | 备注 |
|---|---|---|
| course_id | 课程号 | F1 的左部 |
| title | 课程名 | 随开课班重复 |
| dept_name | 开课系 | 随开课班重复 |
| credits | 学分 | 随开课班重复 |
| sec_id | 开课班号 | |
| semester / year | 学期 / 年份 | |
| building / room_number | 教室楼 / 房间号 | F2 的左部 |
| capacity | 教室容量 | 随每节课重复 |
| time_slot_id | 上课时段 | 每节课一个 |

主键：`(course_id, sec_id, semester, year)`。数据共 11 行：6 门课、4 间教室。

语义 FD（业务规则，不是从数据统计出来的）：

```
F1  course_id -> title, dept_name, credits
F2  (building, room_number) -> capacity
F3  (course_id, sec_id, semester, year) -> building, room_number, time_slot_id
```

> FD 是"业务上必须成立"的规则；数据只能**证伪**（找出反例），不能证明。
> 脚本第 2 步在数据上验证这三条 FD 没有反例（三条查询都返回 0 行），但那只是"数据目前没打脸"。

## 2. 先判 BCNF：候选键 + 每条非平凡 FD 的左部

### 2.1 候选键

`(course_id, sec_id, semester, year)` 唯一标识一行（11 行 = 11 个唯一组合），是候选键。
Java 工具 `FdCalculator.candidateKeys` 对 11 个属性枚举子集后也只得到这一个候选键。

### 2.2 判据与逐条判定

BCNF：F 中**每条非平凡 FD 的左部都必须是超键**（教材第 198 页："仅须检查给定集合 F 中的函数依赖是否违反 BCNF 就够了"）。

| 非平凡 FD | 左部 | 左部是超键吗 | 判定 |
|---|---|---|---|
| `course_id → {title, dept_name, credits}` | course_id | 否：同一门课有多个开课班（CS-101 有 sec_id 1、2） | **违反 BCNF** |
| `{building, room_number} → {capacity}` | {building, room_number} | 否：同一间教室开了多门课（Taylor 3128 有 CS-315、CS-319） | **违反 BCNF** |
| `{course_id, sec_id, semester, year} → {building, room_number, time_slot_id}` | 主键 | 是（就是候选键） | 不违反 |

数据侧证据（脚本第 3 步）：

```
3.2 course_id 不是超键：同一门课有多个开课班
 course_id | 行数 | 不同开课班
-----------+------+------------
 BIO-101   |    2 |          2
 CS-101    |    3 |          2
 CS-190    |    2 |          2
 CS-315    |    2 |          2

3.3 (building, room_number) 不是超键：同一间教室开了多门课
 building | room_number | 不同课程数
----------+-------------+------------
 Painter  | 514         |          2
 Taylor   | 3128        |          2
```

Java 工具 `BcnfCheck.checkRelation` 对同一份 schema + F 的输出：

```
先判 BCNF: 不属于 BCNF
  { course_id } -> { credits,dept_name,title } 违反 BCNF
    ({ course_id }+ = { course_id,credits,dept_name,title }, 还缺 7 个属性)
  { building,room_number } -> { capacity } 违反 BCNF
    ({ building,room_number }+ = { building,capacity,room_number }, 还缺 8 个属性)
候选键: { course_id,sec_id,semester,year }
```

**结论：`class_wide` 不满足 BCNF。**

顺带一句：这张表连 2NF 都没到 —— `course_id → title` 是非主属性对候选键一部分的依赖。
把课程信息拆出去以后，剩下的 `building, room_number → capacity` 仍然违反 BCNF；
所以分解必须循环执行，拆一次不够。

## 3. BCNF 分解（图 8-11 算法）

规则（教材第 198 页）：

```
result := {R};
while (result 中存在不属于 BCNF 的 Ri) do
    取 Ri 上成立、非平凡、α 不是 Ri 超键、且 α∩β=∅ 的 FD α→β;
    result := (result − Ri) ∪ (Ri − β) ∪ (α, β);
```

本文实现（`com.ds.fd.BcnfDecomposition.decompose`）的见证依赖由"子集闭包法"给出：
`β = (α⁺ − α) ∩ Ri`，因此天然满足 `α∩β = ∅`。执行记录：

```
第 1 步: 拆分 class_wide, 违反项 { course_id } -> { credits,dept_name,title }
    => (course_id, sec_id, semester, year, building, room_number, capacity, time_slot_id)
     + (course_id, title, dept_name, credits)
第 2 步: 拆分开课表, 违反项 { building, room_number } -> { capacity }
    => (course_id, sec_id, semester, year, building, room_number, time_slot_id)
     + (building, room_number, capacity)
最终: course(course_id, title, dept_name, credits)
    | section(course_id, sec_id, semester, year, building, room_number, time_slot_id)
    | classroom(building, room_number, capacity)
复核: BCNF? true, 无损? true, 保持依赖? true
```

## 4. 在数据库里落地

脚本第 6 步建三张表并把数据搬过去（`INSERT ... SELECT DISTINCT`）：

| 表 | 主键 | 行数 |
|---|---|---|
| course | course_id | 6 |
| classroom | (building, room_number) | 4 |
| section | (course_id, sec_id, semester, year) | 11 |
| class_wide（原表保留对照） | (course_id, sec_id, semester, year) | 11 |

三条 FD 在数据库层面变成约束：

- `course` 的 PK = course_id ⇒ F1 的左部是键；
- `classroom` 的 PK = (building, room_number) ⇒ F2 的左部是键；
- `section` 的 PK = (course_id, sec_id, semester, year) ⇒ F3 的左部是键；
- 两个外键分别指向 `course` 与 `classroom`，保证连接语义。

## 5. 无损验证

三张表自然连接回来，行数仍为 11；与原宽表**双向 EXCEPT 都返回 0 行**：

```
连接后行数: 11

7.1 原宽表 − 连接结果 (0 rows)
7.2 连接结果 − 原宽表 (0 rows)
```

（这正是教材"BCNF 分解一定无损"的实证；理论上每步拆分的公共属性 α 决定右侧 αβ，
每一步都是二元无损分解，复合起来仍无损。）

## 6. 异常消失

**更新异常**：改 CS-101 的课程名。

```
在宽表上:  UPDATE 3      -- 3 个开课班各存一份，漏改一行就自相矛盾
在 course 表上: UPDATE 1  -- 课程信息只有一份
```

**删除异常**：删掉 CS-319 唯一的开课班，宽表里该课程的信息（课程名/系/学分）随之消失；
分解后 `section` 里删掉该行，`course` 中的 CS-319 仍然保留。

## 7. 保持依赖

F1 落在 `course`，F2 落在 `classroom`，F3 落在 `section`，三条 FD 都能在单表上用主键检查，
不需要连接 ⇒ 这个分解保持依赖。若换一种拆法（例如把课程表再拆细）就可能丢依赖，
所以 BCNF 分解"无损"是保证的，"保持依赖"不是。

## 8. 运行方式

```powershell
# 1) 建独立演示库（可反复重建）
.\psql.bat "DROP DATABASE IF EXISTS bcnf_class_demo WITH (FORCE)"
.\psql.bat "CREATE DATABASE bcnf_class_demo"

# 2) 跑完整演练（建表 → 判定 → 迁移 → 验证 → 视图）
.\psql.bat -d bcnf_class_demo -f src\main\resources\sql\bcnf_class_wide.sql

# 3) 只读复查
.\psql.bat -d bcnf_class_demo "SELECT * FROM class_wide_v ORDER BY course_id, semester, year, sec_id"
```

Java 侧同一份 schema 的判定与分解输出，见 `FdClosureDemo` 的【十一】（`.\scripts\run-fd-tests.ps1` 第 4 步）。

## 9. 容易踩的坑

1. **FD 不是数据统计**：0 行反例只说明"目前没违规"，不能证明规则成立；判定 BCNF 用的是语义 FD。
2. **候选键要枚举**：主键只是候选键之一，本例恰好唯一；有多个候选键时，3NF/BCNF 的判定都必须参考全部候选键。
3. **拆一次不够**：第一步拆掉课程信息后，第二块仍违反 BCNF（教室容量），必须循环到全部模式合格。
4. **α∩β=∅ 不是形式要求**：它保证拆出的两块公共属性恰为 α，从而 α→β 能作为无损判据；否则无损证明不成立。
5. **数据迁移要 DISTINCT + 主键**：去重正确性依赖"FD 在数据上成立"；若历史数据有反例，`INSERT ... SELECT DISTINCT` 会悄悄多出组合，先用第 2 步的查询查反例。
6. **兼容视图**：老应用不用改 SQL，`class_wide_v` 是等价的连接视图（只读；更新应走三张基表）。
