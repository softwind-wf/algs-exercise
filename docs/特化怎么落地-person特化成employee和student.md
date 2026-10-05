# 7.8.1 特化怎么落地：person 特化成 employee 与 student

教材：《数据库系统概念》原书第 6 版·本科教学版（机械工业出版社，Silberschatz / Korth / Sudarshan）
第 7 章 7.8 扩展的 E-R 特性 · 7.8.1 特化。

配套脚本（本机 PostgreSQL 17.11 `university` 库实跑通过，可重复执行）：

- `src/main/resources/sql/er_person_specialization.sql`（PostgreSQL 17）
- E-R 图（Chen 记号）：`docs/er/person-specialization.html` / `.svg`

跑法（两条通道等价 —— `psql.bat` 已改为转发官方 `psql.exe`）：

```
.\psql.bat -f src\main\resources\sql\er_person_specialization.sql

:: 或直接调用 psql（全路径 C:\Program Files\PostgreSQL\17\bin\psql.exe，已在 PATH）
set PGPASSWORD=postgres
psql -U postgres -d university -f src\main\resources\sql\er_person_specialization.sql
```

脚本里有 7 处**故意报错**的演示，所以文件开头显式写了 `\set ON_ERROR_STOP off`，让 psql 报错后继续往下跑；判断成败要数错误条数，不能只看退出码（退出码仍是 0）。

---

## 1. 一句话结论

教材里那棵 ISA 树落到 PostgreSQL 有三种写法，**约束强度不一样**：

| 方案 | 表结构 | 子类⊆超类 | 重叠特化 | 不相交 | 全特化 |
| --- | --- | --- | --- | --- | --- |
| **A 标准映射**（超类表 + 子类表） | `person` + `employee` + `student` | 外键保证 | 天然支持 | 需触发器 | 需可延迟约束触发器 |
| **B 表继承** `INHERITS` | 父表 + 两个子表 | 无（约束不继承） | 天然支持 | 需触发器 | 需可延迟约束触发器 |
| **C 单表 + 判别列** | 一张 `person_all` | CHECK 保证 | **表达不了** | CHECK 一条搞定 | `NOT NULL` 一条搞定 |

教材第 7 章"把 E-R 图转化为关系模式"讲的是 **方案 A**；方案 B 是 PostgreSQL 方言，方便但约束弱；方案 C 只适合"全特化 + 不相交"这种最严格也最简单的场景。

---

## 2. 方案 A：ISA 树 → 三张表

| E-R 图里的东西 | 落到哪里 | 依据 |
| --- | --- | --- |
| `person(ID, name, address)` | `er_person`（超类表，共同属性） | 高层实体集一张表 |
| `employee.salary` | `er_employee.salary`（+ 主键 ID） | 低层实体集一张表，只放**附加**属性 |
| `student.tot_cred` | `er_student.tot_cred`（+ 主键 ID） | 同上 |
| person 的 ID/name/address | 子类表里**不重复存** | 靠主键继承，避免冗余与更新异常 |
| ISA 三角本身 | `er_employee.ID` / `er_student.ID` 的**主键 = 外键** | 子类实体必然"是一个 person" |

```sql
CREATE TABLE er_person (
    ID       CHAR(5)     NOT NULL,
    name     VARCHAR(30) NOT NULL,
    address  VARCHAR(40),
    CONSTRAINT pk_er_person PRIMARY KEY (ID)
);

CREATE TABLE er_employee (
    ID      CHAR(5)      NOT NULL,
    salary  NUMERIC(8,2) NOT NULL,
    CONSTRAINT pk_er_employee     PRIMARY KEY (ID),
    CONSTRAINT fk_er_employee_sub FOREIGN KEY (ID)
        REFERENCES er_person (ID) ON DELETE CASCADE,
    CONSTRAINT ck_er_employee_sal CHECK (salary > 0)
);

CREATE TABLE er_student (
    ID        CHAR(5) NOT NULL,
    tot_cred  INT     NOT NULL,
    CONSTRAINT pk_er_student      PRIMARY KEY (ID),
    CONSTRAINT fk_er_student_sub  FOREIGN KEY (ID)
        REFERENCES er_person (ID) ON DELETE CASCADE
);
```

三条要点：

1. 子类表的 `ID` 既是主键又是外键 —— 这是 ISA 的**结构保证**，不是靠应用层自觉；
2. `ON DELETE CASCADE`：删掉 person，它的 employee/student 行一并消失（子类行依附于超类行）；
3. 子类表只有"自己的属性 + 主键"，所以**不会出现**同一份 name/address 存两遍。

### 用集合运算验证 ISA 语义

```sql
-- ① 子集：employee ∪ student ⊆ person        → 0 行
SELECT ID FROM (SELECT ID FROM er_employee UNION SELECT ID FROM er_student) AS sub
EXCEPT
SELECT ID FROM er_person;

-- ② 差集：person − (employee ∪ student) = 未特化的人 → 83821（部分特化的证据）
SELECT ID, name FROM er_person
EXCEPT
SELECT p.ID, p.name FROM er_person p
WHERE EXISTS (SELECT 1 FROM er_employee e WHERE e.ID = p.ID)
   OR EXISTS (SELECT 1 FROM er_student  s WHERE s.ID = p.ID);

-- ③ 交集：employee ∩ student = 既是雇员又是学生 → 45565（重叠特化的证据）
SELECT ID FROM er_employee INTERSECT SELECT ID FROM er_student;
```

再拼回 E-R 图里"一个实体一行"的样子（**必须用 LEFT JOIN**，否则 83821 会消失）：

| id | name | salary | tot_cred | 角色 |
| --- | --- | --- | --- | --- |
| 10101 | Ravi Srinivasan | 65000.00 | NULL | 仅雇员 |
| 22222 | Albert Einstein | NULL | 90 | 仅学生 |
| 45565 | Sara Katz | 72000.00 | 32 | 既是雇员又是学生 |
| 76543 | Ming Zhao | 58000.00 | NULL | 仅雇员 |
| 83821 | Lena Brandt | NULL | NULL | 都不是 |
| 98345 | Ana Silva | NULL | 45 | 仅学生 |

---

## 3. 四种特化约束怎么落 SQL

方案 A 的默认（不加任何约束）就是**部分 + 重叠**：允许有"都不是"的 person，也允许同一个人既是雇员又是学生。另外两种要自己加：

### 3.1 不相交 disjoint —— 立刻拒绝的触发器

```sql
CREATE OR REPLACE FUNCTION er_fn_forbid_disjoint() RETURNS trigger AS
'BEGIN IF EXISTS (SELECT 1 FROM er_employee WHERE ID = NEW.ID) THEN RAISE EXCEPTION ''违反不相交特化(disjoint): person % 已经是 employee, 不能再是 student'', NEW.ID; END IF; RETURN NEW; END'
LANGUAGE plpgsql;

CREATE TRIGGER trg_er3_disjoint_guard
    BEFORE INSERT ON er_student
    FOR EACH ROW EXECUTE FUNCTION er_fn_forbid_disjoint();
```

实跑：把已是雇员的 `76543` 插进 `er_student` → 被拒：

```
❌ SQL 错误: 错误: 违反不相交特化(disjoint): person 76543 已经是 employee, 不能再是 student
```

### 3.2 全特化 total —— 提交时才检查的可延迟约束触发器

关键在于**不能**用普通触发器：正常顺序是"先插 person、再插子类行"，语句级检查会把中间状态误杀。所以用 `DEFERRABLE INITIALLY DEFERRED` 的约束触发器，等 `COMMIT` 时统一查：

```sql
CREATE OR REPLACE FUNCTION er_fn_require_subclass() RETURNS trigger AS
'BEGIN IF NOT EXISTS (SELECT 1 FROM er_employee WHERE ID = NEW.ID) AND NOT EXISTS (SELECT 1 FROM er_student WHERE ID = NEW.ID) THEN RAISE EXCEPTION ''违反全特化(total): person % 既不是 employee 也不是 student'', NEW.ID; END IF; RETURN NULL; END'
LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_er_total
    AFTER INSERT ON er_person
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION er_fn_require_subclass();
```

实跑：

```
BEGIN;
INSERT INTO er_person (ID, name, address) VALUES ('99999', 'Test Person', 'Nowhere');
COMMIT;
-- ❌ SQL 错误: 错误: 违反全特化(total): person 99999 既不是 employee 也不是 student
```

整笔事务回滚，`99999` 没进库；而"先 person 后 employee"的正常顺序在同一笔事务里提交成功 —— 这就是可延迟的意义。

> 教材里这两个约束是画在 ISA 三角旁边的（双线表示全特化、标 d 表示不相交）。它们**表达的是约束，不是表结构**，所以落到关系模型后必须靠触发器/CHECK 补上，标准映射本身给不了。

---

## 4. 方案 B：PostgreSQL 表继承 `INHERITS` 的坑

```sql
CREATE TABLE er2_person (ID CHAR(5) NOT NULL, name VARCHAR(30) NOT NULL,
                         address VARCHAR(40), CONSTRAINT pk_er2_person PRIMARY KEY (ID));
CREATE TABLE er2_employee (salary NUMERIC(8,2) NOT NULL) INHERITS (er2_person);
CREATE TABLE er2_student  (tot_cred INT NOT NULL)        INHERITS (er2_person);
```

好处很直观：`SELECT * FROM er2_person` 就是"查所有 person"（子表行自动带出来），`tableoid::regclass` 还能看出每行实际来自哪张表：

| 来自 | id | name |
| --- | --- | --- |
| er2_employee | 10101 | Ravi Srinivasan |
| er2_student | 22222 | Albert Einstein |
| er2_employee | 45565 | Sara Katz |
| er2_student | 45565 | Sara Katz |
| er2_employee | 76543 | Ming Zhao |
| er2_person | 83821 | Lena Brandt |
| er2_student | 98345 | Ana Silva |

注意两点：

1. **7 行不是 6 行**：重叠的 45565 在两棵子树里各占一行，继承模型里"一个人"没有单一表示；只想要"真正直接插进父表的那一行"，得写 `ONLY er2_person`（本脚本实测 1 行）。
2. **主键/唯一/外键不会继承到子表**：`er2_person` 上的 `PRIMARY KEY (ID)` 管不住子表，所以 `er2_employee` 里同一个 `10101` 能插两次（实测 `COUNT(*) = 2`）。外键也无法指向"整棵子树"。→ 作为 IS-A 建模，方案 B 的约束强度明显弱于方案 A。

---

## 5. 方案 C：单表 + 判别列 + CHECK

```sql
CREATE TABLE er3_person_all (
    ID      CHAR(5)     NOT NULL,
    name    VARCHAR(30) NOT NULL,
    address VARCHAR(40),
    ptype   VARCHAR(2)  NOT NULL,          -- 'E' 雇员 / 'S' 学生
    salary  NUMERIC(8,2),
    tot_cred INT,
    CONSTRAINT pk_er3_person_all PRIMARY KEY (ID),
    CONSTRAINT ck_er3_type     CHECK (ptype IN ('E','S')),
    CONSTRAINT ck_er3_salary   CHECK (ptype <> 'E' OR salary   IS NOT NULL),
    CONSTRAINT ck_er3_totcred  CHECK (ptype <> 'S' OR tot_cred IS NOT NULL),
    CONSTRAINT ck_er3_disjoint CHECK ((ptype = 'E' AND tot_cred IS NULL)
                                   OR (ptype = 'S' AND salary   IS NULL))
);
```

判别列单值 ⇒ **不相交**天然成立；`ptype NOT NULL` + CHECK ⇒ **全特化**天然成立。一个 `SELECT` 就能列出所有 person 加各自角色。

代价是**重叠特化表达不了**：`45565`（既是雇员又是学生）想插进来，`ptype` 得写 `'ES'`，实测被 CHECK 拒绝；"都不是"的 `83821` 在全特化下也无处安放（本表只放了 4 行，而 person 有 6 个）。真要放进单表，得把判别列改成组合值（`'E','S','ES'`），CHECK 也随之改写 —— 那时它就不再"天然不相交"了。

---

## 6. 怎么选

- 教材练习、要求"关系模式 + 完整性约束"可验证 → **方案 A**（子类主键=外键），再用触发器补全特化/不相交；
- 只想"一个查询捞全部 person"、能接受弱约束（报表、分析型临时结构）→ 方案 B；
- 特化是**全特化且不相交**、子类属性少 → 方案 C 最省事（一张表 + 几条 CHECK）。

---

## 7. 实跑结论（psql 17.11 客户端 + PostgreSQL 17.11 服务端 / `university` 库）

- 每遍恰好 **7 条预期报错**、**0 条警告**：5.1 外键拒绝、6.1 不相交拒绝、6.2 全特化拒绝、8.2/8.3/8.4/8.5 CHECK 拒绝。
- 最终行数：`er_person` 6、`er_employee` 3、`er_student` 3、`er2_person`（含子表）7、`er3_person_all` 4。
- 集合运算验证：`employee ∪ student ⊆ person` 0 行；未特化的人 = 83821；重叠的人 = 45565。
- 级联删除：删 `er_person` 里的 98345 → `er_student` 里的 98345 一并消失。
- 连跑两遍结果完全一致（报错 7 条 / 警告 0 条 / 行数逐项相同），脚本可重复执行（DROP IF EXISTS + 重建）。
- 两条执行通道都跑过且结果一致：`.\psql.bat -f <脚本>`（内部转发官方 psql）7 报错 / 0 警告；`PSQL_BAT_FORCE_JDBC=1` 走 JDBC 兜底版同样 7 报错，另有一行"跳过 `\set` 元命令"的提示。
- 脚本执行完毕后示例表保留在库里，需要清理见脚本步骤 10 的注释。

---

## 8. `psql.bat` 已经修好（附：为什么一度要绕开它）

旧版 `psql.bat` 封装的是 `com.ds.db.PgSqlRunner`——一个**按分号切分语句**的简化 JDBC 执行器：不认 `$$ ... $$`，也不区分"分号出现在注释里"。于是脚本被迫迁就它：注释里的半角分号要写成全角，plpgsql 函数体要塞进单引号并用 `''` 转义（`callable_demo_pg.sql` 当时干脆注明"必须用真实 psql 客户端"）。

现在这个工具本身修好了，分两层：

1. **`psql.bat` / `psql.sh` 改为优先转发 PostgreSQL 自带的 `psql.exe`**
   （PATH 里找不到时再探测 `%ProgramFiles%\PostgreSQL\{17,16,…,12}\bin\psql.exe`），命令行接口完全不变：裸 SQL、`-d`、`-f`、stdin 管道都照旧。只有连 `psql.exe` 都没有的机器才退回 JDBC 版；`set PSQL_BAT_FORCE_JDBC=1` 可以强制走兜底分支（便于验证）。
2. **兜底版也不再是坏的**：新增 `com.ds.db.SqlScriptSplitter`，正确处理
   单/双引号（含 `''`、`""` 转义）、`$$ ... $$` 与 `$tag$ ... $tag$` 美元引用、行注释与**可嵌套**块注释；纯注释片段不再刷"影响行数: 0"；psql 元命令（`\set` 等）单独成句并明确提示"JDBC 版不支持"，而不是被拼进下一条 SQL 里报语法错。

因此以前那条"注释里别写分号、函数体别用 `$$`"的规矩作废。回归证据：`SqlScriptSplitterTest`（8 个用例，含用真实脚本 `er_person_specialization.sql` 切分的用例 —— 必须切出 2 个完整 plpgsql 函数 + 1 条 `\set`），以及下面这几条端到端命令。

```
:: 官方 psql 通道（psql.bat 内部转发）
.\psql.bat "SELECT version()"                        :: 裸 SQL → -c
.\psql.bat -d university "SELECT count(*) FROM er_person"
echo "SELECT 1" | .\psql.bat                         :: stdin
.\psql.bat -f src\main\resources\sql\er_person_specialization.sql   :: 7 报错 / 0 警告

:: JDBC 兜底通道
set PSQL_BAT_FORCE_JDBC=1
.\psql.bat -f src\main\resources\sql\er_fig711_create.sql           :: 0 报错
```

MySQL 版同理：`sql.bat` / `sql.sh`（`com.ds.db.SqlRunner`）也改成了**优先转发 MySQL 自带的 `mysql.exe`**（接口不变：裸 SQL → `-e`、`-db <库>`、`-f <脚本>`、stdin；默认加 `--force` 与旧 JDBC 版"遇错继续"一致，`SQL_BAT_STRICT=1` 改为遇错即止、退出码非 0；`SQL_BAT_FORCE_JDBC=1` 验证兜底分支）。JDBC 兜底与 MySQL 脚本共用同一个 `SqlScriptSplitter`（按 MySQL 语义开了反斜杠转义）。

