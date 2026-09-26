# 三元联系集 proj_guide：建立，以及怎么拆成二元联系

教材：《数据库系统概念》原书第 6 版·本科教学版（机械工业出版社）第 7 章 数据库设计与 E-R 模型
· 7.7.3 二元还是 n 元联系集 · 图 7-19（instructor / student / project 通过 proj_guide 关联）。

配套动手脚本（同一组数据建两种模型，逐步验证拆分无损，再看拆分多出/丢掉了什么）：

- `src/main/resources/sql/proj_guide_ternary.sql`（PostgreSQL 17）
- `src/main/resources/sql/proj_guide_ternary_mysql.sql`（MySQL 8.0）

运行：

```bat
.\psql.bat -f src\main\resources\sql\proj_guide_ternary.sql
.\sql.bat -db university -f src\main\resources\sql\proj_guide_ternary_mysql.sql
```

---

## 1. 一句话概括

- 三元联系集 `proj_guide(instructor, student, project)` 记录的是「某教师带着某学生参与某项目」这一**三方联合事实**。
- 它可以按 7.7.3 节的办法**无损**拆成「一个新建实体集 E + 三个二元联系」。
- 但代价有三样：**要新造标识属性**、**联系表从 1 张变 4 张**、**三元联系上的约束搬不过去**。
- 把三个多对一的外键并回 E 之后，结构又等价于「三元表 + 一个代理键」—— 也就是教材那句话：*这种限制并不总是令人满意的*。

---

## 2. 原模型：三元联系集（图 7-19 a）

```
instructor ──┐
student    ──┼──< proj_guide >
project    ──┘
```

**关系模式**：`proj_guide(iid, sid, pid)`，三个键合起来做主码 —— 三个实体缺一不可。

**示例数据**（脚本步骤 1）：

| iid | sid | pid | 含义 |
|---|---|---|---|
| i1 | s1 | p1 | Katz 同 Shankar 一起参与项目A |
| i1 | s2 | p2 | Katz 同 Zhang 一起参与项目B |
| i2 | s3 | p3 | Srinivasan 同 Umar 一起参与项目C |

---

## 3. 拆分模型：新实体 E + 三个二元联系（图 7-19 b）

```
                        instructor
                            │  R_A  (E侧多 : 教师侧一)
                            │
student ── R_B ────────[  E  ]──────── R_C ──── project
        (E侧多:学生侧一)  (guide_event)   (E侧多:项目侧一)
```

拆分四步：

1. 为原三元联系集建一个新实体集 **E**，并给它一个**新的标识属性** `guide_id`；三元联系集自带的属性（如日期）一并归到 E。
   *不能拿 `(iid, sid, pid)` 当 E 的标识属性* —— 那样 E 就等于原三元表，拆分没有意义。
2. 原三元组的每一行，在 E 里生成一个实体 `e`。
3. 建三个二元联系集 `R_A(E, instructor)`、`R_B(E, student)`、`R_C(E, project)`，分别插入 `(e, 教师)`、`(e, 学生)`、`(e, 项目)`。
4. 三个联系都是「多对一」（E 侧多、实体侧一），所以 `guide_id` 是 R_A / R_B / R_C 各自的主码；进一步可以把三个外键并回 E。

**迁移时必须人工建立「三元组 ↔ guide_id」的对应关系**（脚本步骤 3 用一张 `pg_guide_map` 表把它显式存下来）：

| guide_id | iid | sid | pid |
|---|---|---|---|
| 1 | i1 | s1 | p1 |
| 2 | i1 | s2 | p2 |
| 3 | i2 | s3 | p3 |

这笔人工键的开销，就是拆分付出的第一笔代价。

---

## 4. 实测：拆分是无损的

把三张二元表按 `guide_id` 连接回来（视图 `pg_guide_rebuilt`），与原三元表做**双向差集**：

| 方向 | 差异行数 |
|---|---|
| 原三元表有、还原结果没有 | 0 |
| 还原结果有、原三元表没有 | 0 |

PostgreSQL 17.11 与 MySQL 8.0.44 实跑结果一致：就「记录了哪些三方事实」而言，拆分模型与原模型完全等价。

---

## 5. 实测：拆分会多出/丢掉的三样东西

### 反例一 · 跨对拼装（没有任何二元约束被违反）

原始数据里 Katz 只和 Shankar 一起做过项目A。绕过三元表，把「Katz 带 Shankar 做项目B」分别写进三个二元联系：

```sql
INSERT INTO pg_guide_event (guide_id, guide_date) VALUES (999, DATE '2026-09-26');
INSERT INTO pg_guide_r_a (guide_id, iid) VALUES (999, 'i1');
INSERT INTO pg_guide_r_b (guide_id, sid) VALUES (999, 's1');
INSERT INTO pg_guide_r_c (guide_id, pid) VALUES (999, 'p2');
```

| 检查项 | 实测结果 |
|---|---|
| 三个二元联系里被违反的多对一约束数 | **0**（谁都没被违反） |
| 还原后 `Katz(i1) + Shankar(s1)` 参与的项目 | `p1`、`p2` —— 多出了 p2 |

### 反例二 · 三元联系上的约束无处安放

约束：一对 `(instructor, student)` 最多参与一个项目（即 `(iid, sid)` 是超键）。三元模型里就是一个 `UNIQUE(iid, sid)`；拆开后这条跨三表的约束**用任何单表唯一约束都表达不出来**。

| 插入 `(i1, s2, p1)` | 实测结果 |
|---|---|
| 三元模型 `pg_guide_m1`（带 UNIQUE） | PostgreSQL：`duplicate key value violates unique constraint "uk_pg_guide_m1"`；MySQL：`Duplicate entry 'i1-s2' for key 'my_guide_m1.uk_my_guide_m1'`。表仍是 **3 行** |
| 拆分模型（E + R_A/R_B/R_C） | `guide_id = 1000` 的四行**全部写入成功**，还原后 `(i1,s1)`、`(i1,s2)` 各对应 **2 个项目** |
| 补救写法：三个外键并回 E，并在 E 上加 UNIQUE | 又被挡住，表仍是 **2 行** |

注意最后一行：约束确实回来了，但此时 E 的结构**已经等于「三元表 + 一个代理键」**，绕一圈回到原模型。

### 反例三 · 不建 E、直接拆成两个二元联系（有损）

把 proj_guide 直接投影成 `advises(iid, sid)` 与 `works_on(iid, pid)`，再按教师连接回来：

| 数据源 | 实测行数 |
|---|---|
| 原三元表 | 3 |
| 两个二元联系连接回来 | **5** |

多出来的两条是原始数据中并不存在的三方事实（连接伪造的假元组）：

| iid | sid | pid |
|---|---|---|
| i1 | s1 | p2 |
| i1 | s2 | p1 |

这正是教材用 Katz / Shankar / Zhang 举例要说明的：三元联系集表达的是「三者共同参与同一项目」这一**联合事实**，拆成两两的二元联系后，「绑在一起」的信息没有了，连接只能做笛卡尔式重组。

---

## 6. 三种写法对比

| 写法 | 联系表数 | 标识属性 | 约束可表达性 |
|---|---|---|---|
| 三元模型 `proj_guide(iid, sid, pid)` | 1 | 三个实体集的键，天然唯一 | 三元联系上的约束一句话就能写 |
| 拆分模型（图 7-19 b）：E + R_A + R_B + R_C | 4 | 必须新造 `guide_id`，并人工维护三元组到 E 的对应关系 | 跨三表的约束无法表达，只能靠触发器 / 应用层兜住 |
| 外键并回 E 的补救写法 `E(guide_id, iid, sid, pid)` | 1 | `guide_id` 是纯代理键 | 约束能写，但结构已退回三元表 |

---

## 7. 结论

- **能拆，但要付代价**：拆分过程是机械的（造 E → 生成 e → 写三个二元联系），结果无损，三方事实一条不少。
- **只在必要时才拆**：教材给的正面理由只有一条 —— 某些约束（如「A、B 到 C 多对一」）无法用二元联系上的基数表达，此时用三元联系建模更清楚、更省空间。
- **工程上的等价写法**：三个二元联系都是多对一，直接把三个外键放进 E 即可，这在关系模式上就是「三元联系集 + 代理主键」。

---

## 8. 附：脚本实测输出节选

PostgreSQL 17.11（`university` 库）：

```
| section                                        | 差异行数 |
| 方向一：原三元表有、还原结果没有的行数(期望 0)      | 0      |
| 方向二：还原结果有、原三元表没有的行数(期望 0)      | 0      |

| section                                            | 违反数 |
| 三个二元联系里被违反的多对一约束数(期望 0 —— 谁都没被违反) | 0    |

❌ SQL 错误: 错误: 重复键违反唯一约束"uk_pg_guide_m1"
| 三元模型写入 (i1, s2, p1) 后的行数(期望仍是 3 行,违规行被拒绝) | 3 |

| iid | sid | 项目数 |
| i1  | s1  | 2   |
| i1  | s2  | 2   |

| section                              | 原三元表行数 | 连接后行数 |
| 原三元表行数 / 两个二元联系连接回来的行数(期望 3 / 5) | 3      | 5     |

| iid | sid | pid |
| i1  | s2  | p1  |
| i1  | s1  | p2  |
```

MySQL 8.0.44 逐项一致，两处刻意演示的拒绝分别是
`Duplicate entry 'i1-s2' for key 'my_guide_m1.uk_my_guide_m1'` 与
`Duplicate entry 'i1-s2' for key 'my_guide_event_v2.uk_my_guide_event_v2'`。

示例表在两个库中都留库，可用 `pg_guide_*` / `my_guide_*` 前缀直接查询对比。
