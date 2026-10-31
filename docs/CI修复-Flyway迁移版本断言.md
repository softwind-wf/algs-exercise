# CI 失败修复：Flyway 迁移版本断言写死

> 失败任务：GitHub Actions `CI / test`（push 到 main）
> 根因：`FlywayMigrationTest` 把"最新迁移版本"写死为 12，而本次新增了 `V13__transcript_signature.sql`
> 修复文件：`src/test/java/com/ds/university/FlywayMigrationTest.java`
> 验证：JDK 8 + `mvn -B clean test` 全绿（2457 个测试，0 失败）

---

## 1. 失败现象

CI 日志里的关键一行：

```text
[ERROR] FlywayMigrationTest.noFailedMigrationAndVersionIsLatest:87
        最新已应用迁移版本应为 12 ==> expected: <12> but was: <13>
[ERROR] Tests run: 2457, Failures: 1, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

## 2. 本地复现（与 CI 同环境）

本机装有 JDK 8（`C:\Program Files\Java\jdk-1.8`），用独立库（不影响开发库）复现：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-1.8'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:3306/university_ci_check?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
$env:DB_USER = 'root'; $env:DB_PASSWORD = 'root'
$env:REDIS_HOST = '127.0.0.1'; $env:REDIS_PORT = '6379'

mvn -B clean test
```

结果与 CI 一致：**2457 个测试、1 个失败**，失败项就是 `FlywayMigrationTest.noFailedMigrationAndVersionIsLatest`。

## 3. 根因

两个断言写死了旧版本：

```java
// 修复前
for (String version : new String[]{"1", ..., "12"}) { ... }   // 漏了 V13
assertEquals("12", latest, "最新已应用迁移版本应为 12");      // 新版本一加就失败
```

新增 `V13__transcript_signature.sql` 后，数据库最新版本变成 13，断言自然不成立。
这类"把版本号写死"的测试每加一个迁移就要改一次，属于设计缺陷，不只是这次漏改。

## 4. 修复

`FlywayMigrationTest` 改为**从 classpath 的迁移脚本动态推导最新版本**，并补上 V13 的表结构断言：

1. `latestScriptVersion()`：扫描 `classpath:db/migration/V*.sql`，取最大 `V{n}`；
2. `allMigrationsAppliedSuccessfully()`：按 1..latest 逐个校验 `success = 1`（新增迁移自动覆盖）；
3. `noFailedMigrationAndVersionIsLatest()`：断言数据库最新版本 == 脚本目录最大版本；
4. `migratedTablesExist()`：新增 `transcript_signature` 表及 `serial_no/payload_hash/signature/issued_at`
   四列的存在性断言（V13 的产物）。

## 5. 验证

修复后同样命令（JDK 8 + 独立库 + MySQL/Redis 服务）：

```text
[INFO] Results:
[INFO] Tests run: 2457, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  01:24 min
```

## 6. 提交注意

改动只有一个文件：

```text
 M src/test/java/com/ds/university/FlywayMigrationTest.java
```

提交并推送后，GitHub Actions 的 `CI / test` 会重跑；`docker-build` 依赖 test 通过，也会跟着恢复。
