@echo off
rem ============================================================
rem PostgreSQL quick-run tool (Windows native, works in CMD / PowerShell)
rem
rem Usage:
rem   psql.bat "SELECT * FROM course"
rem   psql.bat -d university "SELECT name FROM student"
rem   psql.bat -f src\main\resources\sql\university_pg.sql
rem
rem Supports stdin pipe and multiple -d / -f combos.
rem Default DB is university, use -d to switch.
rem DB connection config comes from src/main/resources/pg.properties
rem ============================================================
cd /d "%~dp0"

rem First run: copy dependency jars into target\lib, then java classpath
rem uses the dir/* wildcard so we never hit cmd's 8191-char limit.
if not exist target\lib\postgresql*.jar (
    call mvn -q dependency:copy-dependencies -DoutputDirectory=target\lib
)

set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8"
java -cp "target/classes;target/lib/*" com.ds.db.PgSqlRunner %*
