# ============================================================
# PL/Java 演示工程 —— 编译 & 打包
# 用法：在项目根目录执行  ./build.ps1
# 依赖：JDK 17 / 18+（与你的 PostgreSQL 匹配），以及 pljava 的 pljava.jar
#      （PL/Java 安装后通常在 <pg>/share/pljava/ 或 __LIB__/ 下）
# ============================================================
$ErrorActionPreference = 'Stop'

# 1) 定位 pljava.jar（按需改成你机器上的实际路径）
$pljavaJar = $env:PLJAVA_JAR
if (-not $pljavaJar) {
    $candidates = @(
        'C:\Program Files\PostgreSQL\17\share\pljava\pljava.jar',
        'C:\Program Files\PostgreSQL\17\lib\pljava.jar'
    )
    foreach ($c in $candidates) { if (Test-Path $c) { $pljavaJar = $c; break } }
}
if (-not $pljavaJar) {
    Write-Host "找不到 pljava.jar，请设置环境变量 PLJAVA_JAR 指向它。" -ForegroundColor Yellow
    exit 1
}
Write-Host "使用 pljava.jar: $pljavaJar"

# 2) 编译
New-Item -ItemType Directory -Force -Path 'out' | Out-Null
javac -cp $pljavaJar -d out src\main\java\dsh\demo\UniversityFuncs.java
Write-Host "编译完成"

# 3) 打包
jar cf univ.jar -C out .
Write-Host "打包完成: univ.jar"
