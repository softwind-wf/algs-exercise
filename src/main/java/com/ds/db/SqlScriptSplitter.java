package com.ds.db;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL 脚本语句切分器 —— 供 {@link PgSqlRunner} / {@link SqlRunner}(JDBC 路径)使用。
 *
 * <p>比"见分号就切"的朴素实现多处理四件事:</p>
 * <ul>
 *   <li>单引号字符串({@code ''} 转义)、双引号标识符({@code ""} 转义);</li>
 *   <li>PostgreSQL 美元引用 {@code $tag$ ... $tag$} —— 函数体、触发器体里的分号不再被误切
 *       (旧实现在这里必挂: 函数定义报 Unterminated dollar quote);</li>
 *   <li>行注释 {@code -- ...} 与块注释 {@code /* ... *&#47;}(可嵌套)—— 注释里的分号不参与切分,
 *       纯注释片段会被丢弃,不再产生一堆"影响行数: 0"噪声;</li>
 *   <li>psql 元命令(行首 {@code \set} 等)—— 单独成句交给调用方识别,不会再被拼进下一条 SQL。</li>
 * </ul>
 *
 * <p>已知边界: 单引号内只认 {@code ''} 转义,不处理反斜杠转义 —— PostgreSQL 的
 * {@code standard_conforming_strings = on}(默认)下这是正确的;MySQL 路径(
 * {@code backslashEscapes = true})额外把 {@code \x} 当两字符整体。</p>
 */
public final class SqlScriptSplitter {

    private SqlScriptSplitter() {
    }

    /** PostgreSQL 语义(反斜杠不是转义符)。 */
    public static List<String> split(String sql) {
        return split(sql, false);
    }

    /**
     * @param backslashEscapes 单引号内是否把 {@code \} 当转义符(MySQL 为 true,PostgreSQL 为 false)
     */
    public static List<String> split(String sql, boolean backslashEscapes) {
        List<String> out = new ArrayList<String>();
        if (sql == null) {
            return out;
        }
        StringBuilder cur = new StringBuilder();
        boolean hasCode = false;      // 当前片段里有没有"真代码"(纯注释片段丢弃)
        boolean lineHasContent = false; // 本行是否已经有非空白字符(用于识别行首元命令)
        int i = 0;
        int n = sql.length();
        while (i < n) {
            char c = sql.charAt(i);

            // ---- psql 元命令: 行首(前面只有空白)的反斜杠, 读到行尾 ----
            if (c == '\\' && !lineHasContent) {
                int eol = sql.indexOf('\n', i);
                if (eol < 0) {
                    eol = n;
                }
                if (hasCode) {           // 元命令自成一"句", 先收掉前面的 SQL
                    out.add(cur.toString());
                    cur.setLength(0);
                    hasCode = false;
                }
                out.add(sql.substring(i, eol).trim());
                i = eol;
                continue;
            }

            // ---- 行注释 ----
            if (c == '-' && i + 1 < n && sql.charAt(i + 1) == '-') {
                int eol = sql.indexOf('\n', i);
                if (eol < 0) {
                    eol = n;
                }
                cur.append(sql, i, eol);
                lineHasContent = true;
                i = eol;
                continue;
            }

            // ---- 块注释(可嵌套) ----
            if (c == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                int j = i;
                int depth = 0;
                while (j < n) {
                    if (sql.startsWith("/*", j)) {
                        depth++;
                        j += 2;
                    } else if (sql.startsWith("*/", j)) {
                        depth--;
                        j += 2;
                        if (depth == 0) {
                            break;
                        }
                    } else {
                        j++;
                    }
                }
                int end = Math.min(j, n);
                cur.append(sql, i, end);
                lineHasContent = true;
                i = end;
                continue;
            }

            // ---- 单引号 / 双引号 ----
            if (c == '\'' || c == '"') {
                i = copyQuoted(sql, i, c, cur, backslashEscapes && c == '\'');
                hasCode = true;
                lineHasContent = true;
                continue;
            }

            // ---- 美元引用 $tag$ ... $tag$ ----
            if (c == '$') {
                int tagEnd = dollarTagEnd(sql, i);
                if (tagEnd > 0) {
                    String tag = sql.substring(i, tagEnd);
                    int close = sql.indexOf(tag, tagEnd);
                    int end = (close < 0) ? n : close + tag.length();
                    cur.append(sql, i, end);
                    hasCode = true;
                    lineHasContent = true;
                    i = end;
                    continue;
                }
            }

            // ---- 语句分隔 ----
            if (c == ';') {
                if (hasCode) {
                    out.add(cur.toString());
                }
                cur.setLength(0);
                hasCode = false;
                i++;
                continue;
            }

            if (c == '\n') {
                lineHasContent = false;
            } else if (!Character.isWhitespace(c)) {
                lineHasContent = true;
                hasCode = true;
            }
            cur.append(c);
            i++;
        }
        if (hasCode) {
            out.add(cur.toString());
        }
        return out;
    }

    /** 是否是 psql 元命令(JDBC 路径执行不了, 需要官方 psql 客户端)。 */
    public static boolean isMetaCommand(String statement) {
        return statement != null && statement.trim().startsWith("\\");
    }

    /** 从 start 处的引号开始整段拷贝(含引号),返回下一个未处理下标。 */
    private static int copyQuoted(String s, int start, char q, StringBuilder cur, boolean backslashEscapes) {
        int i = start;
        int n = s.length();
        cur.append(s.charAt(i));
        i++;
        while (i < n) {
            char c = s.charAt(i);
            if (backslashEscapes && c == '\\' && i + 1 < n) {
                cur.append(c).append(s.charAt(i + 1));
                i += 2;
                continue;
            }
            cur.append(c);
            i++;
            if (c == q) {
                if (i < n && s.charAt(i) == q) {   // '' 或 "" 转义
                    cur.append(s.charAt(i));
                    i++;
                } else {
                    break;
                }
            }
        }
        return i;
    }

    /** {@code $tag$} / {@code $$} 的结束下标(即开标签之后),不是美元引用返回 -1。 */
    private static int dollarTagEnd(String s, int i) {
        int j = i + 1;
        while (j < s.length() && (Character.isLetterOrDigit(s.charAt(j)) || s.charAt(j) == '_')) {
            j++;
        }
        if (j < s.length() && s.charAt(j) == '$') {
            return j + 1;
        }
        return -1;
    }
}
