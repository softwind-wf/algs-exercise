# ============================================================
# PL/Java 演示 —— 在 Docker 里一键跑通（在你的普通终端执行）
#   注意：DSH 沙箱 shell 禁止访问 Docker 命名管道，必须在普通终端运行本脚本。
# 用法：在 pljava-demo 目录打开 PowerShell，执行  .\run-pljava-demo.ps1
# ============================================================
$ErrorActionPreference = 'Stop'

# ---- 可改项 ----
$PG_VER    = '17'
$IMAGE     = "tada/pljava:$PG_VER"      # 若 pull 失败，换成 ghcr.io/tada/pljava:$PG_VER
$CONTAINER = 'pljava'
$HOSTPORT  = 5433
# ----------------

Write-Host "[1] 拉取镜像 $IMAGE ..." -ForegroundColor Cyan
docker pull $IMAGE

Write-Host "[2] 启动容器 $CONTAINER ..." -ForegroundColor Cyan
docker rm -f $CONTAINER 2>$null | Out-Null
docker run -d --name $CONTAINER -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=extlang_demo -p $HOSTPORT:5432 $IMAGE

Write-Host "[3] 等待数据库就绪 ..." -ForegroundColor Cyan
for ($i=1; $i -le 30; $i++) {
    if ((docker exec $CONTAINER pg_isready -U postgres 2>$null) -match 'accepting') { break }
    Start-Sleep -Seconds 2
}
Write-Host "    数据库已就绪"

Write-Host "[4] 拷贝工程源码进容器 ..." -ForegroundColor Cyan
docker cp . $CONTAINER:/tmp/pljava-demo

Write-Host "[5] 在容器内编译并打包 univ.jar ..." -ForegroundColor Cyan
docker exec $CONTAINER sh -c 'cd /tmp/pljava-demo && \
  PLJAVA_JAR=$(find / -name "pljava.jar" 2>/dev/null | head -n1) && \
  echo "pljava.jar=$PLJAVA_JAR" && \
  mkdir -p out && \
  javac -cp "$PLJAVA_JAR" -d out src/main/java/dsh/demo/UniversityFuncs.java && \
  jar cf univ.jar -C out . && \
  echo "== built ==" && ls -l univ.jar'

Write-Host "[6] 执行安装与调用脚本 ..." -ForegroundColor Cyan
docker exec $CONTAINER psql -U postgres -d extlang_demo -v ON_ERROR_STOP=1 -f /tmp/pljava-demo/install.sql

Write-Host "[7] 完成。本地访问入口: localhost:$HOSTPORT" -ForegroundColor Green
