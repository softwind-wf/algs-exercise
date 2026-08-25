@echo off
rem ============================================================
rem SQL quick-run tool (Windows native, works in CMD / PowerShell)
rem
rem Usage:
rem   sql.bat "SELECT * FROM university.course"
rem   sql.bat -db university "SELECT name FROM student"
rem   sql.bat -f src\main\resources\sql\university.sql
rem
rem DB connection config comes from src/main/resources/db.properties
rem ============================================================
cd /d "%~dp0"

rem First run: copy dependency jars into target\lib, then java classpath
rem uses the dir/* wildcard so we never hit cmd's 8191-char limit.
if not exist target\lib\mysql*.jar (
    call mvn -q dependency:copy-dependencies -DoutputDirectory=target\lib
)

set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8"
java -cp "target/classes;target/lib/*" com.ds.db.SqlRunner %*
