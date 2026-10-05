@echo off
rem ============================================================
rem PostgreSQL quick-run tool (Windows)
rem
rem 本文件不再自己解析 SQL —— 优先转发给 PostgreSQL 自带的 psql.exe;
rem 只有在机器上找不到 psql.exe 时, 才退回 JDBC 版 com.ds.db.PgSqlRunner。
rem
rem 用法(与以前完全一致):
rem   psql.bat "SELECT * FROM course"
rem   psql.bat -d university "SELECT name FROM student"
rem   psql.bat -f src\main\resources\sql\university_pg.sql
rem   echo "SELECT 1" | psql.bat
rem
rem 走官方 psql 后, 脚本里可以放心使用:
rem   · $$ ... $$ 函数体          (旧 JDBC 切分器在这里必挂)
rem   · 注释里的半角分号
rem   · \set / \i / \echo 等元命令
rem
rem 连接配置来自 src/main/resources/pg.properties(user / password / port);
rem 默认库 university, 用 -d 切换。
rem ============================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"

rem ---------- 1) 读连接配置 ----------
set "DBUSER=postgres"
set "DBPASS="
set "DBPORT=5432"
set "DEFAULTDB=university"
if exist "src\main\resources\pg.properties" (
    for /f "usebackq tokens=1* delims==" %%a in ("src\main\resources\pg.properties") do (
        set "CFGK=%%a"
        set "CFGV=%%b"
        if not "!CFGK!"=="" if not "!CFGK:~0,1!"=="#" (
            if /i "!CFGK!"=="user"     set "DBUSER=!CFGV!"
            if /i "!CFGK!"=="password" set "DBPASS=!CFGV!"
            if /i "!CFGK!"=="port"     set "DBPORT=!CFGV!"
        )
    )
)
set "PGUSER=%DBUSER%"
set "PGPASSWORD=%DBPASS%"
set "PGPORT=%DBPORT%"
set "PGDATABASE=%DEFAULTDB%"

rem ---------- 2) 找 psql.exe ----------
rem 想强制走 JDBC 兜底路径(例如验证兜底是否可用): set PSQL_BAT_FORCE_JDBC=1
if defined PSQL_BAT_FORCE_JDBC goto jdbc_fallback
set "PSQL="
for %%p in (psql.exe) do if not defined PSQL if not "%%~$PATH:p"=="" set "PSQL=%%~$PATH:p"
if not defined PSQL (
    for %%v in (17 16 15 14 13 12) do (
        if not defined PSQL if exist "%ProgramFiles%\PostgreSQL\%%v\bin\psql.exe" (
            set "PSQL=%ProgramFiles%\PostgreSQL\%%v\bin\psql.exe"
        )
    )
)
if not defined PSQL goto jdbc_fallback

rem ---------- 3) 组装 psql 参数 ----------
rem  -d / -f 原样转发;其它 -xxx 选项原样转发;裸 SQL 转成 -c "SQL"
set "ARGS="
:parse
if "%~1"=="" goto run_psql
if /i "%~1"=="-d" (
    set "ARGS=!ARGS! -d "%~2""
    shift
    shift
    goto parse
)
if /i "%~1"=="-f" (
    set "ARGS=!ARGS! -f "%~2""
    shift
    shift
    goto parse
)
set "TOK=%~1"
if "!TOK:~0,1!"=="-" (
    set "ARGS=!ARGS! %~1"
    shift
    goto parse
)
set "ARGS=!ARGS! -c "%~1""
shift
goto parse

:run_psql
rem 无参数时 psql 自己从 stdin 读(支持 echo "SELECT 1" | psql.bat)
"%PSQL%" !ARGS!
exit /b %ERRORLEVEL%

rem ---------- 4) 兜底: 没有 psql.exe 时用 JDBC 版 ----------
:jdbc_fallback
echo [psql.bat] 未找到 psql.exe, 退回 JDBC 版 PgSqlRunner(不支持 psql 元命令) 1>&2
if not exist target\lib\postgresql*.jar (
    call mvn -q dependency:copy-dependencies -DoutputDirectory=target\lib
)
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8"
java -cp "target/classes;target/lib/*" com.ds.db.PgSqlRunner %*
exit /b %ERRORLEVEL%
