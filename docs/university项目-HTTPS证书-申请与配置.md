# 给 university 项目拿一张"合法"的证书：三条路，按你的部署场景选

> 项目现状：`keystore/university.p12` 是 `scripts/gen-keystore.ps1` 生成的自签证书（SAN 只有 localhost/127.0.0.1），
> `application-tls.yml` 有现成的 `tls` profile，Docker 部署只映射了 8080。
> 本文解决的就是"怎么把它换成一张浏览器不报警的证书"，以及"换完之后怎么验"。
>
> 相关：[部署说明.md](部署说明.md) §5.1、[数字证书-讲解与演示.md](数字证书-讲解与演示.md)（原理与验证的六道检查）。

---

## 1. 先分清两种"合法"

证书"合法"与否，客户端只看一件事：**签发者能不能连到它信任库里预置的根证书**。

| | 公开 CA 签发（Let's Encrypt / 阿里云 / 腾讯云…） | 自签 / 内部 CA |
|---|---|---|
| 浏览器表现 | 绿锁，无提示 | `NET::ERR_CERT_AUTHORITY_INVALID`，必须点"继续访问" |
| 谁信 | 全世界装了该根证书的系统（操作系统/浏览器/JDK 自带） | 只有手动装了你根证书的机器 |
| 适合 | 公网访问的站点 | 本地开发、校园内网、内网系统 |
| 费用 | 免费（Let's Encrypt / 云厂商 DV） | 0 |
| 续期 | Let's Encrypt 90 天自动续；云厂商免费证书通常 3 个月~1 年，**不自动续** | 自定 |

一句话：**自签证书在技术上完全合法（签名、链、扩展都对），只是没人给它背书**——上一课讲的"链走到自己就断了"，说的就是它。所以"拿一张合法证书"= 找一家公开 CA 给你的域名签一张。

---

## 2. 三条路怎么选

| 你的情况 | 走哪条 | 需要的准备 |
|---|---|---|
| 有公网域名 + 公网服务器（云主机/VPS） | **A. Let's Encrypt + Nginx**（推荐）或 **B. 云厂商免费 DV** | 域名解析权、80/443 放行、（中国内地）ICP 备案 |
| 只在内网/校园网访问 | **D. 校级 CA 或自建内部 CA** | 学校信息中心配合，或自己维护 CA + 分发根证书 |
| 只在本机开发调试 | 自签 + 把根装进本机信任库（见第 7 节） | 无 |
| 用 Cloudflare 代理 | Cloudflare Origin 证书（15 年，只被 CF 信任） | 域名托管在 CF；回源必须是 CF |

**公开 CA 的四个前置条件**（缺一个就签不下来，先自查）：

1. 你**有权改这个域名的 DNS 或放文件到网站根目录**（域名验证要用）；`xxx.edu.cn` 一般由学校信息中心统一管理，先申请子域名和解析权。
2. 服务器 **80 或 443 能被公网访问**（HTTP-01 走 80，TLS-ALPN-01 走 443）。
3. 中国内地机房：域名必须 **ICP 备案**，否则云厂商会阻断 80/443，验证永远失败。
4. 证书只能签给**域名**，签不了公网 IP（Let's Encrypt 至今不为 IP 地址签发）。内网 IP 只能用内部 CA。

> 申请时**只提交 CSR（含公钥）**，任何情况下都不要把私钥发给 CA 或第三方网站。

---

## 3. 路线 A：Let's Encrypt + Nginx（推荐）

**为什么不直接给 Spring Boot 配证书**：续期、HTTP→HTTPS 跳转、静态资源、上传大小、WebSocket 透传、多实例负载，全都堆在 Nginx 一层解决，应用代码一行不改。证书换了只 reload Nginx，不用重启 Java。

### 3.1 装 certbot 并先申请到证书

```bash
# Debian / Ubuntu
sudo apt update && sudo apt install -y certbot
# 先确认 80 端口空闲（Nginx 还没配 443 时，用 standalone 申请最省事）
sudo certbot certonly --standalone -d university.example.edu.cn

# 如果 Nginx 已经在跑 80，用 webroot（证书续期也走这条路）
sudo mkdir -p /var/www/certbot
sudo certbot certonly --webroot -w /var/www/certbot -d university.example.edu.cn
```

成功的标志：`/etc/letsencrypt/live/university.example.edu.cn/` 下出现

| 文件 | 用途 |
|---|---|
| `fullchain.pem` | **叶子 + 中间证书**，Nginx 的 `ssl_certificate` 必须用它 |
| `privkey.pem` | 私钥，`ssl_certificate_key` |
| `chain.pem` / `cert.pem` | 中间链 / 单独叶子，一般用不到 |

### 3.2 套用本仓库的 Nginx 配置

把 [deploy/nginx/university.conf](../deploy/nginx/university.conf) 复制到 `/etc/nginx/conf.d/university.conf`，改三处：

1. `server_name` 与证书路径里的域名；
2. 后端地址（默认 `127.0.0.1:8080`；Docker 部署改成 `app:8080`）；
3. 确认 `location /.well-known/acme-challenge/` 的 root 与 certbot 的 `-w` 一致。

```bash
sudo nginx -t && sudo systemctl reload nginx
```

这份配置已经处理了本项目几个具体点：`client_max_body_size 6m`（对应 `application.yml` 的 5MB 文件 / 6MB 请求上限）、WebSocket 的 `Upgrade/Connection` 透传与 1 小时读超时（聊天功能）、`X-Forwarded-Proto`（应用才能知道自己是 HTTPS）、Actuator 只允许本机访问。

### 3.3 自动续期

Let's Encrypt 证书 90 天有效，certbot 安装时会带 `certbot.timer`（snap 安装）或 cron。加一个续期后重载 Nginx 的钩子：

```bash
sudo tee /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh >/dev/null <<'EOF'
#!/bin/sh
nginx -t && systemctl reload nginx
EOF
sudo chmod +x /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh

sudo certbot renew --dry-run      # 演练：能跑通才算续期配好
```

### 3.4 Docker 部署（不用在宿主机装 certbot）

两种做法，任选：

- **最省事：Caddy 换掉 certbot。** 用本仓库的 [deploy/Caddyfile](../deploy/Caddyfile) + [deploy/docker-compose.tls.yml](../deploy/docker-compose.tls.yml)：

  ```bash
  docker compose -f docker-compose.yml -f deploy/docker-compose.tls.yml up -d --build
  ```

  Caddy 启动即自动申请、到期前自动续，不需要 cron 也不需要 reload。想只暴露 443（8080 只对本机可见）：

  ```bash
  APP_PORT=127.0.0.1:8080 docker compose -f docker-compose.yml -f deploy/docker-compose.tls.yml up -d
  ```

- **继续用 Nginx**：在 compose 里加一个 certbot 服务 + 证书卷，续期后 `docker exec university-nginx nginx -s reload`（需要把 docker socket 或定时任务接进去，比 Caddy 多一步）。没有特殊理由建议选 Caddy。

---

## 4. 路线 B：云厂商免费 DV 证书（不想在服务器装 ACME 工具）

以阿里云为例（腾讯云流程几乎一样）：

1. 控制台 → **数字证书管理服务（SSL 证书）** → 免费证书 → 创建证书，填域名；
2. 域名验证：按提示加一条 DNS TXT 记录（或上传验证文件到网站根目录）；
3. 签发后**下载 → Nginx 格式**，得到 `<域名>.pem`（证书链）+ `<域名>.key`（私钥）；
4. 把两个文件传到服务器，改 Nginx 配置的 `ssl_certificate` / `ssl_certificate_key` 路径，`nginx -t && systemctl reload nginx`。

注意三点：

- 免费 DV 有效期通常 3 个月~1 年，**到期不会自动续**，最稳妥的做法是在日历上记提前两周手动续；
- 下载的 `.pem` 一般已经是全链（叶子+中间），用 `openssl crl2pkcs7 -nocrl -certfile xxx.pem | openssl pkcs7 -print_certs -noout` 数一下有几张，**只有 1 张就说明缺中间证书**，需要把 CA 提供的 `chain.pem` 拼进去；
- 云厂商证书的信任度和 Let's Encrypt 完全一样（都是 DV），区别只在支持与有效期。

---

## 5. 路线 C：不装 Nginx，让 Spring Boot 直接跑 HTTPS

项目已经有 `application-tls.yml`（`tls` profile，8443 端口，读 `KEYSTORE_PATH` / `KEYSTORE_PASSWORD` / `KEYSTORE_ALIAS`），只缺一个"真证书的 PKCS12"。

### 5.1 把 PEM 转成 PKCS12

Linux 服务器上有 openssl，一条命令：

```bash
sudo openssl pkcs12 -export \
  -in  /etc/letsencrypt/live/university.example.edu.cn/fullchain.pem \
  -inkey /etc/letsencrypt/live/university.example.edu.cn/privkey.pem \
  -name university -out /etc/university/university.p12
```

**Windows 默认不带 openssl，但可以直接装一份**（本机在 `D:\OpenSSL-Win64\bin\openssl.exe`，OpenSSL 3.2.0）。仓库里的脚本会自动找它，找不到再退回纯 JDK 实现：

```powershell
.\scripts\cert-to-pkcs12.ps1 `
  -FullChain C:\certs\fullchain.pem `
  -PrivateKey C:\certs\privkey.pem `
  -Out keystore\university.p12 -Alias university
```

两条路都会做同一件事：解析 PEM → 用私钥找到配对的那张证书当叶子 → 按 issuer/subject 把链排成 叶子→中间→根 → 打包成 PKCS12，最后用 keytool 打印链长与 SAN 供你复核。没有 openssl 时用 [PemToPkcs12.java](../src/main/java/com/ds/crypto/PemToPkcs12.java)（纯 JDK）兜底。

实测输出（本次用 openssl 分支，3 张证书的链，产物经 keytool 复核）：

```text
[openssl] D:\OpenSSL-Win64\bin\openssl.exe
=== keytool 复核 ===
  Entry type: PrivateKeyEntry
  Certificate chain length: 3
  Certificate[1]:
  Owner: CN=localhost, O=Alg4 Demo, OU=Web, C=CN
  Issuer: CN=Alg4 Demo Intermediate CA, O=Alg4 Demo, C=CN
  DNSName: localhost
  IPAddress: 127.0.0.1
```

> 小坑：这个 Windows 版 OpenSSL 编译进去的默认配置文件路径是 `C:\Program Files\Common Files\SSL\openssl.cnf`，该文件不存在时 `pkcs12 -export` 会直接失败。脚本里用 `OPENSSL_CONF=''`（不加载配置）规避；手工执行时也可以先 `$env:OPENSSL_CONF=''`。

### 5.2 用环境变量指向新证书启动

```powershell
$env:KEYSTORE_PATH = 'keystore\university.p12'
$env:KEYSTORE_PASSWORD = '你的口令'
$env:KEYSTORE_ALIAS = 'university'
java -jar target\algs4-1.0.0.0.jar --spring.profiles.active=prod,tls
```

访问 `https://localhost:8443`（或把 `server.port` 改成 443，但那要求 root 权限，且 80→443 跳转还得再写一个连接器）。**所以生产环境仍然推荐路线 A/Caddy 在前面挡一层**，Spring Boot 保持 HTTP。

### 5.3 应用侧还要补三行配置

放在 `application-prod.yml`（HTTPS 才生效；本地 HTTP 调试加上会把登录搞坏）：

```yaml
server:
  port: 8443
  forward-headers-strategy: framework   # 让 request.isSecure() 认 X-Forwarded-Proto
  servlet:
    session:
      cookie:
        secure: true                    # 只在 HTTPS 下回传 JSESSIONID
```

`forward-headers-strategy` 在"Nginx 终结 TLS、应用跑 HTTP"时尤其重要：不配它，应用以为自己是 HTTP，重定向可能退回 http:// 造成循环，`secure` cookie 也不会生效。

---

## 6. 路线 D：校园内网（最常见的情况）

1. **先问学校信息中心**：多数高校有校级 CA 和统一证书服务，能给 `*.xxx.edu.cn` 签证书，或直接给各二级系统发证书。这条路比自己搭 CA 省事得多，而且根证书已经在全校电脑里（域策略/终端管理推送）。
2. 没有校级 CA 时，才考虑自建内部 CA：一张离线的根 + 一台签发中间 CA，把根证书通过组策略/终端管理推到所有客户端。原理就是上一课演示的那套（root → intermediate → 叶子，[CertificateDemo.java](../src/main/java/com/ds/crypto/CertificateDemo.java) 里 9 段自检可以直接拿来当模板）。
3. **内网 IP 拿不到公开 CA 证书**，只能是内部 CA 或自签；装根证书这一步躲不掉。
4. 如果学校对外有统一 HTTPS 网关/WAF，证书可能由网关统一管理，你的应用只需要保证内网 HTTP 可用。

---

## 7. 本地开发：让自己电脑别报警

| 做法 | 命令/操作 | 适用 |
|---|---|---|
| mkcert（推荐） | `mkcert -install` + `mkcert localhost 127.0.0.1`，得到 PEM，再用 `scripts/cert-to-pkcs12.ps1` 转 p12 | 本机开发，浏览器当它是合法证书 |
| 项目自带自签 | `scripts/gen-keystore.ps1` → `keystore/university.p12`（SAN=localhost/127.0.0.1） | 只要 HTTPS 能用，不介意点一次"继续访问" |
| 手动信任 | `certlm.msc` → 受信任的根证书颁发机构 → 导入根证书 | 需要给同事/测试机也消掉警告 |

mkcert 的原理也是自建 CA，只是它顺手把根装进了系统信任库——和"公开 CA"在技术路径上完全一样。

---

## 8. 配好之后怎么验（对应上一课的六道检查）

```powershell
# 1) 看服务器到底发了什么证书：链、SAN、有效期（JDK 自带，Windows 可用）
keytool -printcert -sslserver university.example.edu.cn:443

# 2) 看是否发了完整链：数一下 -----BEGIN CERTIFICATE----- 的个数，应该是 ≥2
curl.exe -vI https://university.example.edu.cn 2>&1 | Select-String 'subject:|issuer:|SSL certificate verify'

# 3) Linux 上更直接
openssl s_client -connect university.example.edu.cn:443 -servername university.example.edu.cn -showcerts </dev/null 2>/dev/null | grep -c 'BEGIN CERTIFICATE'

# 4) 应用自身是否健康（Nginx 转发正常）
curl.exe -sI https://university.example.edu.cn/actuator/health
```

判读：

| 现象 | 含义 |
|---|---|
| `SSL certificate verify ok` | 链、域名、有效期都过了 |
| `unable to get local issuer certificate` / 浏览器"证书链不完整" | 服务器只发了叶子证书，`ssl_certificate` 没用 `fullchain.pem` |
| `No subject alternative DNS name matching ... found` | 证书 SAN 里没有你访问的域名（常见：证书签的是 `www.`，你访问裸域） |
| `certificate has expired` | 续期没生效，或客户端系统时间不对 |
| 浏览器绿锁但应用重定向到 http | 缺 `forward-headers-strategy` 或 Nginx 没传 `X-Forwarded-Proto` |

浏览器里再看一眼：开发者工具 → Security → 证书链，应能看到 叶子 → 中间 → 根 三层，且显示"证书有效"。

---

## 9. 六个最容易踩的坑

1. **只配叶子证书** → `PKIX path building failed`。Nginx 用 `fullchain.pem`，不要用 `cert.pem`。
2. **域名与 SAN 不符** → 裸域和 `www.` 是两个名字，要么各签一张，要么用 `-d a.com -d www.a.com` 一张签两个。
3. **80 端口不通 / 未备案** → HTTP-01 验证失败。先 `curl http://你的域名/.well-known/acme-challenge/test` 确认能通；内地机房先备案。
4. **免费证书忘续期** → 云厂商 DV 不会自动续。用 Let's Encrypt 或设日历提醒。
5. **私钥权限过宽或被提交** → `chmod 600 privkey.pem`；本项目 `.gitignore` 已忽略 `keystore/`，别用 `git add -f` 绕过去。
6. **HSTS 开得太早** → 一旦全站强制 HTTPS 出错，回滚很麻烦（浏览器会记住 max-age）。先 `max-age=3600` 试，稳定后再拉到一年。

---

## 10. 费用与时间（粗略）

| 项 | 成本 | 时间 |
|---|---|---|
| 域名 | 30~100 元/年 | 当天 |
| ICP 备案（内地机房必需） | 免费 | 1~3 周 |
| Let's Encrypt / 云厂商 DV 证书 | 免费 | 10 分钟（DNS 验证可能要等解析生效） |
| 配置 + 验证 | — | 半天 |

---

## 11. 上线前检查清单

- [ ] 域名解析指向服务器，`ping`/`curl` 能通；
- [ ] 80 端口能访问（用于验证与跳转），443 已放行；
- [ ] `nginx -t` 通过，`ssl_certificate` 指向 `fullchain.pem`；
- [ ] `keytool -printcert -sslserver` 显示的 SAN 含你实际访问的域名；
- [ ] 浏览器绿锁，证书链三层齐全；
- [ ] HTTP 访问返回 301 到 HTTPS；
- [ ] 登录/上传/聊天（WebSocket）在 HTTPS 下都正常；
- [ ] `certbot renew --dry-run` 通过，或 Caddy 日志显示自动续期正常；
- [ ] 应用侧 `forward-headers-strategy: framework`、`cookie.secure: true` 已配（纯 HTTPS 部署时）；
- [ ] 旧证书保留一份，出问题能 5 分钟内切回。

---

## 12. 本次新增的文件

| 文件 | 说明 |
|---|---|
| [deploy/nginx/university.conf](../deploy/nginx/university.conf) | Nginx 站点配置：80→443、全链证书、反代 8080、WebSocket、上传上限、Actuator 限源 |
| [deploy/Caddyfile](../deploy/Caddyfile) | Caddy 配置：自动证书 + 反代，五行搞定 |
| [deploy/docker-compose.tls.yml](../deploy/docker-compose.tls.yml) | 在现有 compose 上叠加 Caddy（自动证书、自动续期） |
| [scripts/cert-to-pkcs12.ps1](../scripts/cert-to-pkcs12.ps1) | PEM → PKCS12（优先 openssl，没有就用纯 JDK） |
| [PemToPkcs12.java](../src/main/java/com/ds/crypto/PemToPkcs12.java) | 纯 JDK 实现：找配对叶子、按 issuer/subject 排链、打包 PKCS12、打印 SAN 与链长 |

与现有文件的衔接：`application-tls.yml` 的 `KEYSTORE_PATH/PASSWORD/ALIAS` 不用改；`keystore/` 已被 `.gitignore` 忽略；Docker 部署继续用 `docker-compose.yml`，要 HTTPS 时叠加 `deploy/docker-compose.tls.yml`。

