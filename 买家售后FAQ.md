# 买家售后 FAQ（纯文字版）

> 使用说明：遇到问题先按"症状"在本文查找对应条目，按"解决"步骤操作。
> 大部分问题都可以通过重启容器或清理缓存解决，请耐心按顺序排查。
> 文中 `代码块` 是需要输入的命令，注意区分 Windows 和 Mac/Linux 的命令差异。

---

## 一、部署环境类

### Q1. 我的电脑需要装哪些软件？

| 部署方式 | 必需软件 | 可选 |
|---|---|---|
| 方式一：Docker（推荐） | 仅需 Docker Desktop（Windows/Mac 自带 Compose） | 无 |
| 方式二：IDEA 本地 | JDK 8、Maven、MySQL 8、IDEA | Redis（没有也能跑） |

> Docker 方式最省事：不需要装 Java、Maven、MySQL、Redis 任何一个，全部由容器提供。

### Q2. 源码解压后放在哪里？

- 路径中**不要有中文和空格**，例如 `D:\university-web`（Windows）或 `~/university-web`（Mac/Linux）。
- 不要放在桌面、下载目录等权限受限的位置（macOS 尤其注意）。

### Q3. Docker Desktop 装好了但命令提示"docker 不是内部或外部命令"？

- 原因：Docker Desktop 未启动，或环境变量未生效。
- 解决：① 打开 Docker Desktop 应用，等它显示 Running；② 重新打开命令行窗口；③ 再执行 `docker --version`。

### Q4. 拉取镜像/下载依赖特别慢？

- 原因：访问国外镜像源网络慢。
- 解决：耐心等待（首次 5~15 分钟属正常）；如果长时间卡住，重试 `docker compose up -d --build` 即可断点续传。

---

## 二、Docker 部署类

### Q5. 执行 `docker compose up -d --build` 后报错怎么办？

**步骤 1：看是哪一步报错**
- 报错关键字 `pull access denied` / `not found`：镜像拉取失败，检查网络后重试。
- 报错关键字 `Ports are not available` / `address already in use`：**8080 端口被占用**，见 Q6。
- 报错关键字 `BUILD FAILURE`：Maven 编译失败，见 Q7。
- 报错关键字 `Connection refused`（连接 MySQL 失败）：等待 MySQL 初始化完成，见 Q8。

**步骤 2：查看容器日志定位问题**
```bash
docker compose ps                     # 看三个容器状态
docker logs university-app            # 看应用日志（最新错误在最后）
docker logs university-mysql          # 看数据库日志
```

### Q6. 8080 端口被占用（Ports are not available）？

**原因**：本机已有程序占用 8080（常见于其他 Java 项目、IDEA 实例）。
**解决**：换一个端口启动，例如 8090：
```bash
# Windows PowerShell
$env:APP_PORT=8090; docker compose up -d

# Mac / Linux
export APP_PORT=8090 && docker compose up -d
```
然后访问 `http://localhost:8090`。

### Q7. 构建报 BUILD FAILURE（编译失败）？

- 最常见原因：网络问题导致 Maven 依赖下载不完整。
- 解决：重试 `docker compose up -d --build`；若反复失败，先执行 `docker builder prune -f` 清理构建缓存后再重试。
- 如果持续失败，把完整的报错信息截图发给卖家（见 Q32）。

### Q8. 应用一直重启/连接数据库失败？

- 原因：MySQL 容器首次启动需要 30~60 秒初始化，应用在等待其健康后才启动。
- 解决：等待 1~2 分钟再执行 `docker compose ps`，看到三个容器都是 `Up` 且 `mysql`、`redis` 显示 `(healthy)` 即正常。
- 若长时间不健康，执行 `docker compose logs mysql | tail -50` 查看数据库日志并反馈。

### Q9. 如何彻底重置（清空所有数据重新部署）？

> ⚠️ 以下命令会**删除全部数据库数据、聊天记录、上传头像**，执行前请确认。
```bash
docker compose down -v     # 停止并删除数据卷
docker compose up -d --build
```

### Q10. 停止/重启/开机自启？

```bash
docker compose down        # 停止（保留数据，重启后数据还在）
docker compose up -d       # 再次启动
docker compose restart     # 只重启应用（快速）
```
- 三个容器都配置了 `restart: unless-stopped`，Docker 启动后会自动拉起，无需手动。

### Q11. 怎么更新代码/升级？

```bash
docker compose up -d --build   # 重新构建镜像并滚动更新
```
- 应用数据（数据库、头像、日志）都存于 Docker 卷，升级不丢失。

---

## 三、IDEA 本地运行类

### Q12. IDEA 导入项目后依赖下载不完？

- 解决：File → Settings → Maven，确认使用本地 Maven（或 IDEA 自带）；必要时删除本地仓库 `~/.m2/repository` 中对应失败目录后重新导入；或使用阿里云镜像（项目已提供 `docker/maven-settings.xml`，可复制其镜像配置到本机 Maven settings.xml）。

### Q13. 本地运行报数据库连接失败（Access denied / Communications link failure）？

**原因**：`application.yml` 里的数据库账号密码与本地 MySQL 不一致，或 MySQL 未启动、库未创建。
**解决**：
1. 确认 MySQL 8 已启动；
2. 在 MySQL 中执行建库命令：
   ```sql
   CREATE DATABASE IF NOT EXISTS university DEFAULT CHARACTER SET utf8mb4;
   ```
3. 修改 `src/main/resources/application.yml` 中 datasource 的 username/password 为本地实际账号密码；
4. 重启应用。**表结构和数据无需手动导入**，启动时 Flyway 自动创建。

### Q14. 没有 Redis 能跑吗？

- 能。默认配置了 Redis 地址，连接失败会自动重试，但**不影响启动**。
- 也可以主动关闭多实例功能（纯单机模式）：
  - 在 `application.yml` 中设置 `app.multi-instance.enabled: false`，或在启动参数加 `--app.multi-instance.enabled=false`。
- 关闭后：在线统计、登录限流、聊天路由回退为单机内存实现，功能不受影响。

### Q15. 本地改端口？

- 修改 `application.yml` 第一行 `server.port: 8080` 为其他端口（如 8088），重启即可。
- 注意 IDEA 方式与 Docker 方式同时运行时，浏览器 Cookie 不区分端口，会出现互相踢下线（详见 Q18）。

---

## 四、登录与账号类

### Q16. 登录提示"账号或密码错误"？

- 先确认账号密码无误：演示账号 `zhang` / `katz` / `admin`，密码均为 `password`（注意区分大小写）。
- 若连续输错 5 次，会被**登录限流锁定 30 分钟**（同一 IP），请稍后再试，或重启应用（限流计数在 Redis，`docker compose restart` 不清除，需等锁定时间结束或清 Redis：`docker exec university-redis redis-cli flushdb`）。

### Q17. 忘记改过的密码？

- 方法一（Docker）：进入 MySQL 容器执行 SQL 重置。
  ```bash
  docker exec -it university-mysql mysql -uroot -proot123 university -e "UPDATE sys_user SET password='\$2a\$10\$/Gu.uRug7LYoOu0PzCdVKOqo4Ayxt3fM2utBEet4jNQ5nouNqojKO' WHERE user_id='admin';"
  ```
  重置后密码为 `password`。
- 方法二（IDEA）：用 MySQL 客户端连接本地库执行同样 SQL。

### Q18. 登录后过一会儿又要重新登录？（高频问题）

**原因**：浏览器 Cookie **不区分端口、只区分域名**。如果你同时运行了本机 IDEA 实例（如 8443）和 Docker 实例（8080），两个实例共用同一个 JSESSIONID，互相覆盖登录状态，表现为"过一会儿就被踢下线"。
**解决**：
1. 推荐只保留一个实例运行（关掉另一个）；
2. 若确实需要同时跑两个：Docker 实例已配置独立 Cookie 名 `JSESSIONID_DOCKER`（见 docker-compose.yml 的 `SERVER_SERVLET_SESSION_COOKIE_NAME`），两者互不干扰；
3. 操作后**刷新浏览器并重新登录一次**。

### Q19. 换了浏览器/清了缓存后要重新登录？

- 正常现象。登录状态保存在浏览器 Cookie 中，换浏览器、清 Cookie、浏览器更新都可能导致重新登录，重新登录即可。
- 会话默认 30 分钟无操作过期（可在 `application.yml` 的 `server.servlet.session.timeout` 修改）。

### Q20. 提示"无权访问"（403）？

- 原因：当前账号角色与该页面权限不匹配。例如用学生 zhang 访问 `/admin` 管理后台会被拒绝。
- 解决：使用对应角色账号登录——管理后台用 `admin`，学生页面用 `zhang`，教师页面用 `katz`。

---

## 五、功能使用类

### Q21. 聊天发不了消息/收不到消息？

1. 确认对方账号在线（或对方重新登录后），聊天需要双方 WebSocket 连接；
2. 确认浏览器地址是 `http://localhost:8080`，且页面没被防火墙拦截 WebSocket（一般本机无此问题）；
3. 刷新页面重试；仍不行则 `docker compose restart university-app`。

### Q22. 论坛发帖/回帖报 CSRF 错误（403）？

- 原因：页面停留过久，CSRF token 过期。
- 解决：刷新页面后重新填写提交即可（每个页面会携带最新 token）。

### Q23. 公告不显示/看不到某条公告？

- 公告支持**定时发布**和**过期时间**：未到发布时间或已过期的公告不会显示在列表。
- 管理端（admin）→ 公告管理，检查该公告的发布状态和时间设置。

### Q24. 论坛搜索搜不到内容？

- 论坛帖子标题/内容、学生姓名、教师姓名支持**全文检索**（MySQL ngram 索引，对中文友好）。
- 若新发帖后搜索不到，可能索引同步延迟，稍等片刻或刷新重试。

### Q25. 头像上传失败？

- 头像要求：常见图片格式（jpg/png 等），大小不超过 5MB。
- Docker 方式下头像保存在 Docker 卷 `uploads-data` 中，容器重建不丢失。

### Q26. 在线人数统计不准？

- 在线统计基于会话，退出登录、会话超时（30 分钟）后自动下线，有最长 35 分钟 TTL 兜底，属于正常延迟。
- 单机模式下为实例本地统计；Docker 模式下为 Redis 跨实例共享统计，更准确。

---

## 六、数据与备份类

### Q27. 数据存在哪里？如何备份？

- **Docker 方式**：数据在 Docker 命名卷中（`mysql-data`、`uploads-data`、`logs-data`、`redis-data`），位于 Docker 数据目录内。备份 MySQL：
  ```bash
  docker exec university-mysql mysqldump -uroot -proot123 university > backup.sql
  ```
  恢复：
  ```bash
  docker exec -i university-mysql mysql -uroot -proot123 university < backup.sql
  ```
- **IDEA 方式**：数据在本地 MySQL 中，用 Navicat / mysqldump 备份即可。

### Q28. 头像/上传文件备份？

- Docker 方式：头像在 `uploads-data` 卷中。备份卷：`docker run --rm -v university-web_uploads-data:/data -v "$PWD":/backup alpine tar czf /backup/uploads.tar.gz -C /data .`

### Q29. 日志在哪里看？

- Docker：`docker logs -f university-app`（实时）；应用内日志文件在容器 `/app/logs/`（application.log / error.log），已挂载到 `logs-data` 卷。
- IDEA：控制台 + 项目根目录 `logs/` 文件夹（application.log / error.log）。

---

## 七、排障通用技巧

### Q30. 一键排障三步走

```bash
# 1. 看状态
docker compose ps

# 2. 看应用日志（最后 50 行）
docker logs --tail 50 university-app

# 3. 确认健康
curl http://localhost:8080/actuator/health
# 返回 {"status":"UP"} 即正常
```

### Q31. 页面样式错乱/功能点了没反应？

- 多为浏览器缓存了旧版 JS/CSS。强制刷新：Ctrl+F5（Windows）/ Cmd+Shift+R（Mac）。
- 也可以换个浏览器或无痕窗口验证。

### Q32. 遇到解决不了的问题，联系卖家时需要提供什么？

请按以下格式提供，能大幅加快处理：
1. 部署方式：Docker / IDEA；
2. 操作系统：Windows / macOS / Linux；
3. 执行哪条命令时出错；
4. 完整报错信息截图（`docker logs --tail 50 university-app` 的输出）；
5. 已尝试过的解决步骤。

---

## 八、附录：命令速查表

```bash
# ===== Docker 部署 =====
docker compose up -d --build       # 首次构建并启动（5~15 分钟）
docker compose ps                  # 查看容器状态
docker compose logs -f university-app   # 跟踪应用日志
docker compose restart             # 重启所有容器
docker compose down                # 停止（保留数据）
docker compose down -v             # 停止并删除数据（慎用！）

# ===== 换端口 =====
$env:APP_PORT=8090; docker compose up -d        # Windows PowerShell
export APP_PORT=8090 && docker compose up -d    # Mac/Linux

# ===== MySQL 维护（Docker）=====
docker exec -it university-mysql mysql -uroot -proot123 university   # 进入 MySQL
docker exec university-mysql mysqldump -uroot -proot123 university > backup.sql  # 备份

# ===== 健康检查 =====
curl http://localhost:8080/actuator/health      # {"status":"UP"} 即正常

# ===== IDEA 本地 =====
# 建库
CREATE DATABASE IF NOT EXISTS university DEFAULT CHARACTER SET utf8mb4;
# 运行
mvn spring-boot:run
```

---

> 本文档随源码交付。问题未解决时，请先按 Q32 的格式整理信息再联系卖家，谢谢配合。
