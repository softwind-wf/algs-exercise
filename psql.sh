#!/bin/bash
# ============================================================
# PostgreSQL 快捷执行工具 (Linux / macOS / Git Bash)
#
# 优先调用 PostgreSQL 自带的 psql;找不到才退回 JDBC 版 PgSqlRunner。
#
# 用法(与以前一致):
#   ./psql.sh "SELECT * FROM course"
#   ./psql.sh -d university "SELECT name FROM student"
#   ./psql.sh -f src/main/resources/sql/university_pg.sql
#   echo "SELECT 1" | ./psql.sh
#
# 连接配置来自 src/main/resources/pg.properties(user / password / port);
# 默认库 university, 用 -d 切换。
# ============================================================
cd "$(dirname "$0")" || exit 1

# ---------- 1) 读连接配置 ----------
DBUSER="postgres"
DBPASS=""
DBPORT="5432"
DEFAULTDB="university"
if [ -f src/main/resources/pg.properties ]; then
  while IFS='=' read -r k v; do
    k="${k%$'\r'}"; v="${v%$'\r'}"
    case "$k" in
      ''|\#*) continue ;;
      user)     DBUSER="$v" ;;
      password) DBPASS="$v" ;;
      port)     DBPORT="$v" ;;
    esac
  done < src/main/resources/pg.properties
fi
export PGUSER="$DBUSER" PGPASSWORD="$DBPASS" PGPORT="$DBPORT" PGDATABASE="$DEFAULTDB"

# ---------- 2) 优先走官方 psql ----------
if command -v psql >/dev/null 2>&1; then
  args=()
  while [ $# -gt 0 ]; do
    case "$1" in
      -d|-f) args+=("$1" "$2"); shift 2 ;;
      -*)    args+=("$1"); shift ;;
      *)     args+=(-c "$1"); shift ;;   # 裸 SQL → -c "SQL"
    esac
  done
  exec psql "${args[@]}"
fi

# ---------- 3) 兜底: JDBC 版 ----------
echo "[psql.sh] 未找到 psql, 退回 JDBC 版 PgSqlRunner(不支持 psql 元命令)" >&2
if [ ! -f target/cp.txt ]; then
  mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
fi
CP="target/classes;$(cat target/cp.txt)"
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8"
exec java -cp "$CP" com.ds.db.PgSqlRunner "$@"
