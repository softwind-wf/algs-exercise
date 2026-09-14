# PL/Java 演示工程（外部语言过程 · Java）

PostgreSQL 用 **PL/Java** 写外部函数：Java 静态方法经 `LANGUAGE java` 注册成 SQL 函数，
像普通函数一样被 SQL 调用，逻辑跑在数据库后端进程内的嵌入式 JVM 里。

> **本工程是即开即用的完整方案**。当前这台 Windows + PostgreSQL 17(MSVC) 没有 PL/Java 可安装路径
> （官方只发 Linux 包 + 源码构建，Windows 非受支持构建目标），所以"在本机跑通"受环境限制。
> 请在不限于以下的任一 PL/Java 可用环境运行：

## 运行前提

- PostgreSQL 9.5+（建议 12+）上已安装 **PL/Java 1.6.x**
  - Linux：`apt install postgresql-<ver>-pljava`（或从 `tada/pljava` 源码 `mvn package`）
  - Docker/WSL：见下文
- JDK 17+，且 PL/Java 用同一 JVM 版本启动
- `pljava.jar` 可被构建脚本找到（设 `PLJAVA_JAR` 环境变量指到它）

## 步骤

```bash
# 1) 编译 + 打包
./build.ps1            # Windows；Linux 用  ./build 里的 javac/jar 等价命令

# 2) 建库并执行安装脚本（install.sql 里 univ.jar 路径按需改）
psql -U postgres -d extlang_demo -f install.sql
```

install.sql 会：启用 `java` 语言 → `sqlj.install_jar` 装入 jar → `set_classpath`
→ `CREATE FUNCTION ... LANGUAGE java` 注册 → 调用并展示结果。

## 预期输出

```
              salutation
-------------------------------------------
 Dear Dr. Zhang, welcome to the university!

   avg_grade
---------------
 81.6666666667

  top_word
-----------
 the

 count_students
----------------
               3
```

## 用 Docker 跑（最省事）

PL/Java 有官方镜像，起一个 Linux PostgreSQL + PL/Java 的容器即可：

```bash
docker run -d --name pljava -e POSTGRES_PASSWORD=postgres -p 5433:5432 \
  ghcr.io/tada/pljava:16        # 或 :17，视 PG 大版本
# 把工程 jar 拷进容器并执行 install.sql
```

## 说明

- 类型自动映射：`text↔String`、`int4↔int`、`float8↔double`、`int4[]↔int[]`、`boolean↔Boolean` 等。
- 入参为 `java.sql.Connection` 时，PL/Java 自动传入当前事务连接，可回查数据库。
- 外部语言函数默认**不受信**，只有超级用户能创建（安全红线）。
