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

if not exist target\cp.txt (
    call mvn -q dependency:build-classpath -Dmdep.outputFile=target\cp.txt
)

set "CP=target\classes"
for /f "usebackq delims=" %%i in ("target\cp.txt") do set "CP=%CP%;%%i"

set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8"
java -cp "%CP%" com.ds.db.SqlRunner %*
