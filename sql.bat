@echo off
rem ============================================================
rem MySQL quick-run tool (Windows)
rem
rem 与 psql.bat 同理: 本文件不再自己解析 SQL —— 优先转发给 MySQL 自带的
rem mysql.exe;找不到才退回 JDBC 版 com.ds.db.SqlRunner。
rem
rem 用法(与以前一致):
rem   sql.bat "SELECT * FROM university.course"
rem   sql.bat -db university "SELECT name FROM student"
rem   sql.bat -f src\main\resources\sql\university.sql
rem   echo "SELECT 1" | sql.bat
rem
rem 连接配置来自 src/main/resources/db.properties(user / password / port,
rem 可选 database);默认库 university,用 -db 切换。
rem
rem 环境开关:
rem   SQL_BAT_FORCE_JDBC=1  强制走 JDBC 兜底分支(验证用)
rem   SQL_BAT_STRICT=1      遇错即止(不加 --force),退出码非 0
rem   MYSQL_HOST=<host>     覆盖主机(默认 127.0.0.1)
rem ============================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"

rem ---------- 1) 读连接配置 ----------
set "DBHOST=127.0.0.1"
if defined MYSQL_HOST set "DBHOST=%MYSQL_HOST%"
set "DBUSER=root"
set "DBPASS="
set "DBPORT=3306"
set "DEFAULTDB=university"
if exist "src\main\resources\db.properties" (
    for /f "usebackq tokens=1* delims==" %%a in ("src\main\resources\db.properties") do (
        set "CFGK=%%a"
        set "CFGV=%%b"
        if not "!CFGK!"=="" if not "!CFGK:~0,1!"=="#" (
            if /i "!CFGK!"=="user"     set "DBUSER=!CFGV!"
            if /i "!CFGK!"=="password" set "DBPASS=!CFGV!"
            if /i "!CFGK!"=="port"     set "DBPORT=!CFGV!"
            if /i "!CFGK!"=="database" set "DEFAULTDB=!CFGV!"
        )
    )
)

rem ---------- 2) 找 mysql.exe ----------
if defined SQL_BAT_FORCE_JDBC goto jdbc_fallback
set "MYSQL="
for %%p in (mysql.exe) do if not defined MYSQL if not "%%~$PATH:p"=="" set "MYSQL=%%~$PATH:p"
if not defined MYSQL (
    for %%v in ("MySQL Server 9.0" "MySQL Server 8.4" "MySQL Server 8.0" "MySQL Server 5.7") do (
        if not defined MYSQL if exist "%ProgramFiles%\MySQL\%%~v\bin\mysql.exe" (
            set "MYSQL=%ProgramFiles%\MySQL\%%~v\bin\mysql.exe"
        )
    )
)
if not defined MYSQL goto jdbc_fallback

rem ---------- 3) 组装 mysql 参数 ----------
rem  固定项: 主机/端口/用户/字符集/表格输出;默认 --force(与旧 JDBC 版"遇错继续"一致)
set "ARGS=-h %DBHOST% -P %DBPORT% -u %DBUSER% --default-character-set=utf8mb4 -t"
if not defined SQL_BAT_STRICT set "ARGS=!ARGS! --force"
set "FILE="
set "DBNAME=%DEFAULTDB%"

:parse
if "%~1"=="" goto run_mysql
if /i "%~1"=="-db" goto arg_db
if /i "%~1"=="-f"  goto arg_file
set "TOK=%~1"
if "!TOK:~0,1!"=="-" goto arg_flag
goto arg_sql

:arg_db
set "DBNAME=%~2"
shift
shift
goto parse

:arg_file
set "FILE=%~2"
shift
shift
goto parse

:arg_flag
set "ARGS=!ARGS! %~1"
shift
goto parse

:arg_sql
set "ARGS=!ARGS! -e "%~1""
shift
goto parse

:run_mysql
if not "%DBNAME%"=="" set "ARGS=!ARGS! -D %DBNAME%"
rem 用 MYSQL_PWD 传口令, 避免 mysql 在命令行口令时打印 "insecure" 告警
set "MYSQL_PWD=%DBPASS%"
if defined FILE (
    "%MYSQL%" !ARGS! < "!FILE!"
) else (
    "%MYSQL%" !ARGS!
)
exit /b %ERRORLEVEL%

rem ---------- 4) 兜底: 没有 mysql.exe 时用 JDBC 版 ----------
:jdbc_fallback
echo [sql.bat] 未找到 mysql.exe, 退回 JDBC 版 SqlRunner 1>&2
if not exist target\lib\mysql*.jar (
    call mvn -q dependency:copy-dependencies -DoutputDirectory=target\lib
)
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8"
java -cp "target/classes;target/lib/*" com.ds.db.SqlRunner %*
exit /b %ERRORLEVEL%
