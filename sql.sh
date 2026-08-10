#!/bin/bash
# ============================================================
# 通过 JDBC 直接操作 MySQL 的快捷工具
#
# 用法：
#   ./sql.sh "SELECT * FROM university.course"
#   ./sql.sh -db university "SELECT name FROM student WHERE tot_cred > 100"
#   ./sql.sh -f src/main/resources/sql/university.sql
#   echo "SELECT 1" | ./sql.sh
#
# 依赖 src/main/resources/db.properties 中的连接配置
# ============================================================
cd "$(dirname "$0")"

# 首次使用或依赖更新时，重新生成 classpath
if [ ! -f target/cp.txt ]; then
  mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
fi

# Windows 平台 Java 的 classpath 用分号分隔
CP="target/classes;$(cat target/cp.txt)"
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8"

exec java -cp "$CP" com.ds.db.SqlRunner "$@"
