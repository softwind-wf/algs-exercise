#!/bin/bash
# ============================================================
# MySQL 快捷执行工具 (Linux / macOS / Git Bash)
#
# 与 psql.sh 同理: 优先调用 MySQL 自带的 mysql 客户端;
# 找不到才退回 JDBC 版 SqlRunner。
#
# 用法(与以前一致):
#   ./sql.sh "SELECT * FROM university.course"
#   ./sql.sh -db university "SELECT name FROM student"
#   ./sql.sh -f src/main/resources/sql/university.sql
#   echo "SELECT 1" | ./sql.sh
#
# 连接配置来自 src/main/resources/db.properties(user / password / port,
# 可选 database);默认库 university, 用 -db 切换。
#
# 环境开关: SQL_BAT_FORCE_JDBC=1 强制走 JDBC;SQL_BAT_STRICT=1 遇错即止;
#           MYSQL_HOST=<host> 覆盖主机(默认 127.0.0.1)。
# ============================================================
cd "$(dirname "$0")" || exit 1

# ---------- 1) 读连接配置 ----------
DBHOST="${MYSQL_HOST:-127.0.0.1}"
DBUSER="root"
DBPASS=""
DBPORT="3306"
DEFAULTDB="university"
if [ -f src/main/resources/db.properties ]; then
  while IFS='=' read -r k v; do
    k="${k%$'\r'}"; v="${v%$'\r'}"
    case "$k" in
      ''|\#*) continue ;;
      user)     DBUSER="$v" ;;
      password) DBPASS="$v" ;;
      port)     DBPORT="$v" ;;
      database) DEFAULTDB="$v" ;;
    esac
  done < src/main/resources/db.properties
fi

# ---------- 2) 优先走官方 mysql 客户端 ----------
if [ "${SQL_BAT_FORCE_JDBC:-}" != "1" ] && command -v mysql >/dev/null 2>&1; then
  args=(-h "$DBHOST" -P "$DBPORT" -u "$DBUSER" --default-character-set=utf8mb4 -t)
  [ "${SQL_BAT_STRICT:-}" = "1" ] || args+=(--force)
  file=""
  dbname="$DEFAULTDB"
  while [ $# -gt 0 ]; do
    case "$1" in
      -db) dbname="$2"; shift 2 ;;
      -f)  file="$2";   shift 2 ;;
      -*)  args+=("$1"); shift ;;
      *)   args+=(-e "$1"); shift ;;
    esac
  done
  [ -n "$dbname" ] && args+=(-D "$dbname")
  export MYSQL_PWD="$DBPASS"
  if [ -n "$file" ]; then
    exec mysql "${args[@]}" < "$file"
  fi
  exec mysql "${args[@]}"
fi

# ---------- 3) 兜底: JDBC 版 ----------
echo "[sql.sh] 未找到 mysql, 退回 JDBC 版 SqlRunner" >&2
if [ ! -f target/cp.txt ]; then
  mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
fi
CP="target/classes;$(cat target/cp.txt)"
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8"
exec java -cp "$CP" com.ds.db.SqlRunner "$@"
