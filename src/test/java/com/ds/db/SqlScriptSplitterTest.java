package com.ds.db;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@link SqlScriptSplitter} 的回归测试 —— 覆盖旧实现会切错/切挂的全部场景。
 * 旧实现(见 git 历史: 只认 ' 与 ")在下面 5 个用例上都会失败。
 */
class SqlScriptSplitterTest {

    @Test
    void 普通语句按分号切分且纯注释片段被丢弃() {
        String sql = "-- 开头注释; 里面有分号\n"
                + "SELECT 1;\n"
                + "-- 中间注释; 也有分号\n"
                + "SELECT 2;\n"
                + "-- 结尾只有注释; 不应产生语句\n";
        List<String> stmts = SqlScriptSplitter.split(sql);
        assertEquals(2, stmts.size(), "只应有 2 条真语句: " + stmts);
        assertTrue(stmts.get(0).contains("SELECT 1"));
        assertTrue(stmts.get(1).contains("SELECT 2"));
    }

    @Test
    void 单引号与双引号内的分号不切分且支持双写转义() {
        String sql = "INSERT INTO t(a,b) VALUES ('x;y', \"col;name\"); SELECT 'it''s; fine';";
        List<String> stmts = SqlScriptSplitter.split(sql);
        assertEquals(2, stmts.size(), "引号里的分号不能切: " + stmts);
        assertTrue(stmts.get(0).contains("'x;y'"));
        assertTrue(stmts.get(0).contains("\"col;name\""));
        assertTrue(stmts.get(1).contains("'it''s; fine'"));
    }

    @Test
    void 行注释与嵌套块注释里的分号不切分() {
        String sql = "SELECT 1 /* 块注释; 里 /* 嵌套; */ 还有; */ ; SELECT 2 -- 行注释;\n;";
        List<String> stmts = SqlScriptSplitter.split(sql);
        assertEquals(2, stmts.size(), "注释里的分号不能切: " + stmts);
        assertTrue(stmts.get(0).contains("SELECT 1"));
        assertTrue(stmts.get(1).contains("SELECT 2"));
    }

    @Test
    void 美元引用函数体整体成句() {
        String sql = "CREATE OR REPLACE FUNCTION f(x int) RETURNS int AS $$\n"
                + "BEGIN\n"
                + "    IF x > 0 THEN RETURN x; ELSE RETURN 0; END IF;\n"
                + "END\n"
                + "$$ LANGUAGE plpgsql;\n"
                + "SELECT f(5);";
        List<String> stmts = SqlScriptSplitter.split(sql);
        assertEquals(2, stmts.size(), "函数体必须是一整句: " + stmts);
        assertTrue(stmts.get(0).contains("CREATE OR REPLACE FUNCTION"));
        assertTrue(stmts.get(0).contains("END IF;"), "函数体内的分号要保留");
        assertTrue(stmts.get(1).contains("SELECT f(5)"));
    }

    @Test
    void 带标签的美元引用也整体成句() {
        String sql = "DO $body$ BEGIN RAISE NOTICE 'a;b'; END $body$; SELECT 1;";
        List<String> stmts = SqlScriptSplitter.split(sql);
        assertEquals(2, stmts.size(), "带标签的 $tag$ 也要整体识别: " + stmts);
        assertTrue(stmts.get(0).contains("$body$"));
        assertTrue(stmts.get(1).contains("SELECT 1"));
    }

    @Test
    void psql元命令单独成句并可识别() {
        String sql = "\\set ON_ERROR_STOP off\nDROP TABLE IF EXISTS t;\n\\echo done\n";
        List<String> stmts = SqlScriptSplitter.split(sql);
        assertEquals(3, stmts.size(), "元命令不能和下一条 SQL 粘在一起: " + stmts);
        assertTrue(SqlScriptSplitter.isMetaCommand(stmts.get(0)));
        assertFalse(SqlScriptSplitter.isMetaCommand(stmts.get(1)));
        assertTrue(stmts.get(1).contains("DROP TABLE"));
        assertTrue(SqlScriptSplitter.isMetaCommand(stmts.get(2)));
    }

    @Test
    void mysql模式识别反斜杠转义的单引号() {
        String mysql = "INSERT INTO t VALUES ('It\\'s; ok'); SELECT 1;";
        List<String> stmts = SqlScriptSplitter.split(mysql, true);
        assertEquals(2, stmts.size(), "MySQL 的 \\' 不能让切分跑偏: " + stmts);
        assertTrue(stmts.get(0).contains("It\\'s; ok"));
    }

    @Test
    void 真实脚本能切出函数体与全部语句() throws Exception {
        Path script = Paths.get("src/main/resources/sql/er_person_specialization.sql");
        assumeTrue(Files.exists(script), "脚本不存在则跳过: " + script.toAbsolutePath());
        String sql = new String(Files.readAllBytes(script), StandardCharsets.UTF_8);
        List<String> stmts = SqlScriptSplitter.split(sql);

        assertTrue(stmts.size() > 40, "语句数明显偏少, 可能被切错: " + stmts.size());
        int functions = 0;
        int meta = 0;
        for (String s : stmts) {
            if (s.contains("CREATE OR REPLACE FUNCTION")) {
                functions++;
                assertTrue(s.contains("END") && s.contains("LANGUAGE plpgsql"),
                        "函数体被切碎了: " + s);
            }
            if (SqlScriptSplitter.isMetaCommand(s)) {
                meta++;
            }
        }
        assertEquals(2, functions, "脚本里有 2 个 plpgsql 函数");
        assertEquals(1, meta, "脚本里有 1 条 \\set 元命令");
    }
}
