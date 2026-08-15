# 大学教务信息网站（University Web）

> 一个功能完整、可直接运行的 **Spring Boot + Spring MVC + MyBatis + MySQL + Redis** 高校教务/校园信息化系统。
> 覆盖教务管理、学生中心、教师中心、公告、实时聊天、论坛、通知、在线统计、日志审计等全流程，
> 支持 **Docker 一键部署**，内置 **17 个测试类**，代码规范、注释完善，适合**毕业设计、课程设计、Java Web 实战学习**。

---

## ✨ 功能总览

### 一、教务基础数据（公开 + 管理端 CRUD）
| 模块 | 说明 |
|---|---|
| 院系管理 | 院系增删改查、专业归属 |
| 课程管理 | 课程信息、学分、先修课程管理 |
| 开课班管理 | 开课班分配教师/教室/时间 |
| 教师管理 | 教师档案、所属院系 |
| 学生管理 | 学生档案、所属院系/专业 |
| 排课管理 | 可视化排课、周课表视图 |

### 二、三种角色门户
- **学生中心**：我的课程、成绩单、个人课表、导师信息
- **教师中心**：授课班列表、学生名单、个人课表
- **教务管理后台**：基础数据、账号、在线用户、统计报表、审计日志等 16+ 个管理页面

### 三、校园社交与协作（本项目核心亮点）
| 模块 | 功能点 |
|---|---|
| 💬 **实时聊天** | 原生 WebSocket（非 STOMP 封装）；学生↔教师一对一私聊；消息历史、清空会话；联系人按"我的同学 / 我的老师"分类，支持**按院系浏览**教师 |
| 📢 **系统公告** | 公告列表/详情；**分类**；**定时发布**、**过期自动下线**；管理端全流程管理 |
| 🗣 **论坛** | 发帖/回帖；**动态板块**（管理端自定义）；**编辑历史**留存；**@提及**自动通知；**点赞**及点赞列表；**全文检索**（MySQL ngram 全文索引，中文分词友好） |
| 🔔 **通知中心** | @提及 / 回复 / 新消息通知；未读角标实时轮询 |

### 四、账号与安全（企业级实践，毕设加分项）
- BCrypt 密码加密存储（Spring Security Crypto）
- **登录限流**：同一 IP 失败次数超限锁定，Redis 跨实例共享计数
- **会话安全**：登录后 Session ID 轮换（防会话固定攻击）、HttpOnly + SameSite Cookie
- **CSRF 防护**：自研拦截器 + 常量时间比较 + 登录后 token 轮换
- **安全响应头**：CSP、X-Frame-Options、nosniff 等统一注入
- **日志脱敏**：手机号、身份证号、密码、Token 等敏感信息自动掩码（logback 自定义转换器）
- 头像上传：格式/大小校验、目录隔离、静态资源映射

### 五、多实例与运维
- **Redis 多实例共享**：在线用户统计、登录限流、聊天跨实例路由（存储层抽象 + 条件装配，可一键切换单机/集群）
- **在线人数统计**：SessionListener + Redis 实时在线用户数与明细
- **Flyway 数据库版本管理**：12 个迁移脚本，建库建表自动完成
- **Actuator 健康检查 + Prometheus 指标**：Docker/K8s 探活就绪
- **日志体系**：logback 滚动文件、错误日志独立归档

### 六、测试（17 个测试类）
Service 层单元测试（Mockito）+ 集成测试 + MockMvc 鉴权/安全头测试 + Flyway 迁移一致性测试。

---

## 🛠 技术栈

| 层次 | 技术 |
|---|---|
| 后端 | Java 8 · Spring Boot 2.7.18 · Spring MVC · Spring Cache |
| ORM | MyBatis（XML 映射 + 驼峰映射） |
| 数据库 | MySQL 8 · Flyway 迁移管理 · ngram 全文索引 |
| 缓存/共享 | Redis（Lettuce）· 存储层抽象支持单机内存/Redis 双实现 |
| 实时通信 | 原生 WebSocket（Spring WebSocket，无 STOMP 依赖） |
| 模板引擎 | Thymeleaf（布局片段复用） |
| 前端 | Bootstrap 5 · 原生 JS · 响应式布局 |
| 构建/部署 | Maven · Docker · Docker Compose 一键编排 MySQL + Redis + 应用 |
| 监控 | Actuator · Micrometer Prometheus |

---

## 🚀 快速开始

### 方式一：Docker 一键部署（推荐，5 分钟跑起来）

需要：Docker + Docker Compose

```bash
docker compose up -d --build
```

自动完成：拉取 MySQL 8 / Redis 7 → 创建数据库并执行 12 个 Flyway 迁移（建表+初始数据）→ 启动应用。

访问：`http://localhost:8080`

### 方式二：本地 IDEA 运行

需要：JDK 8、Maven、本机 MySQL 8（库名 `university`，账号 root/root 可在 `application.yml` 修改）、Redis（可选，多实例功能用）

```bash
# 1. 建库（应用启动时 Flyway 会自动建表灌数据）
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS university DEFAULT CHARACTER SET utf8mb4;"

# 2. 启动（默认 dev 配置，模板热加载）
mvn spring-boot:run
```

访问：`http://localhost:8080`

### 演示账号（密码均为 `password`）

| 账号 | 角色 | 可体验 |
|---|---|---|
| `zhang` | 学生 | 学生中心、选课、成绩、聊天、论坛 |
| `katz` | 教师 | 教师中心、授课名单、聊天、论坛 |
| `admin` | 管理员 | 全部后台管理、在线统计、审计日志 |

---

## 📁 项目结构

```
src/main/java/com/ds/university/
├── controller/    # 15 个控制器：教务/学生/教师/公告/聊天/论坛/通知/管理后台
├── service/       # 业务层：Auth/Account/Avatar/Announcement/Chat/Forum/Notification...
│                  #   ├── ChatWebSocketHandler     WebSocket 处理器
│                  #   ├── ChatEventPublisher       跨实例消息路由（Redis Pub/Sub）
│                  #   ├── LoginGuard               登录限流（Redis 共享计数）
│                  └── ...
├── config/        # 安全/拦截器/监听器：CsrfInterceptor、AuthInterceptor、
│                  #   OnlineUserTracker、SessionListener、SecurityHeaderFilter、日志脱敏
├── entity/ vo/ util/ common/
src/main/resources/
├── templates/     # 约 50 个 Thymeleaf 页面（公开/学生/教师/管理/论坛/聊天）
├── static/        # CSS / JS / Bootstrap
├── mapper/        # MyBatis XML
└── db/migration/  # Flyway V1~V12 迁移脚本
src/test/java/com/ds/university/   # 17 个测试类
docker-compose.yml / Dockerfile    # 容器化部署
```

---

## 📸 主要页面清单

- 公开：首页、院系、课程、开课班、教师、公告列表/详情、论坛、聊天
- 学生端（6 页）：个人中心、我的课程、成绩单、课表、导师
- 教师端（5 页）：工作台、授课班、名单、课表
- 管理端（16 页）：总览、院系/课程/开课班/教师/学生/教室管理、排课、账号、公告、论坛板块、在线用户、统计报表、审计日志

---

## ❓ 常见问题

**Q1：登录后过一会儿又要重新登录？**
浏览器 Cookie 不区分端口只区分域名。如果你同时跑着本机 IDEA 实例和 Docker 实例（如 8080 与 8443），两者共用同一 JSESSIONID 会互相踢下线。
解决：Docker 实例已配置独立 Cookie 名 `JSESSIONID_DOCKER`（见 docker-compose.yml），两实例互不干扰；或只保留一个实例运行。

**Q2：8080 端口被占用？**
```bash
APP_PORT=8090 docker compose up -d
```

**Q3：如何开启 HTTPS？**
项目内置 `application-tls.yml`（8443 端口 + SSL），配合 `scripts/gen-keystore.ps1` 生成证书即可。

**Q4：不想用 Redis？**
`application.yml` 中 `app.multi-instance.enabled=false`，自动回退单机内存实现，无需 Redis 即可运行。

---

## 📄 版权说明

- 本项目的校园信息系统代码（`com.ds.university` 包）为原创代码。
- 仓库目录中还包含 Princeton 大学《Algorithms, 4th Edition》配套源码（`edu.princeton.cs.algs4` 包等），版权归 Robert Sedgewick / Kevin Wayne 所有，仅供学习参考，请勿用于商用分发。

---

## 🔒 版权水印（作者发货前必读）

源码的 71 个核心文件（controller / service / config / util / common / 启动类）已内置版权水印头，当前署名为占位符 `YOUR_NAME`。**发货前务必替换为你的实际署名**（网名 / 店铺名 / 真实姓名），防止买家转售时无法追溯。

### 一键替换署名

```powershell
# 1. 交互式（推荐）：提示输入署名 + 确认，防呆防忘
powershell -ExecutionPolicy Bypass -File scripts\apply-watermark.ps1

# 2. 非交互：直接指定署名
powershell -ExecutionPolicy Bypass -File scripts\apply-watermark.ps1 -Name 你的网名

# 3. 仅检查：发货前快速验证水印状态（0 残留才可发货）
powershell -ExecutionPolicy Bypass -File scripts\apply-watermark.ps1 -Check
```

脚本特性：空署名自动拒绝、替换后全项目校验残留、只改写含占位符的文件、UTF-8 无 BOM 写回（兼容 Java 8）。

### 发货前检查清单

1. `scripts\apply-watermark.ps1 -Check` → 输出 `[OK] 所有文件均不含占位符`；
2. 重新构建镜像让容器内代码同步水印：`docker compose up -d --build`；
3. 再打包发货。

---

## ⚠️ 交付须知（购买者）

1. 源码默认**不包含**数据库导出文件：表结构与初始数据由 Flyway 迁移脚本自动生成（V1~V12），无需手工导入。
2. 运行环境要求：JDK 8 + Maven 3.6+（或 Docker），MySQL 8，Redis（可选）。
3. 演示数据内置：院系、课程、教师、学生、示例账号、示例公告/帖子。
4. 本项目为**教学演示系统**，数据均为演示用途，适合毕业设计/课程设计/学习研究。
