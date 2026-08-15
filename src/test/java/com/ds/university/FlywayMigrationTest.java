package com.ds.university;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Flyway 迁移集成测试：应用启动时自动执行 classpath:db/migration 下的
 * 版本化脚本（V1 业务表 / V2 账号权限 / V3 审计日志 / V4 收编 instructor.phone_number /
 * V5 用户头像 sys_user.avatar / V6 系统公告 sys_announcement / V7 公告增强 /
 * V8 站内聊天 chat_message / V9 学习论坛 forum_post/forum_reply / V10 论坛增强 forum_like /
 * V11 论坛扩展：动态板块/编辑历史/站内通知 / V12 全文检索索引（ngram）），验证：
 * 1. flyway_schema_history 中 V1-V12 均成功应用；
 * 2. 各脚本对应的关键表/列/索引真实存在。
 * 线上升级不再依赖手工 SQL，schema 漂移可被 validate-on-migrate 拦截。
 */
@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** V1-V12 迁移都记录在案且 success=1 */
    @Test
    void allMigrationsAppliedSuccessfully() {
        for (String version : new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"}) {
            Integer success = jdbcTemplate.queryForObject(
                    "SELECT success FROM flyway_schema_history WHERE version = ? AND type = 'SQL'",
                    Integer.class, version);
            assertEquals(1, success, "V" + version + " 迁移应成功应用");
        }
    }

    /** 迁移产物真实存在：业务表、RBAC 表、审计表、公告表、聊天表、论坛相关表、通知表、头像列、Flyway 历史表 */
    @Test
    void migratedTablesExist() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
                String.class);
        for (String expected : new String[]{"department", "sys_user", "audit_log", "sys_announcement",
                "chat_message", "forum_post", "forum_reply", "forum_like", "forum_category",
                "forum_post_history", "user_notification", "flyway_schema_history"}) {
            assertTrue(tables.contains(expected), "表 " + expected + " 应由 Flyway 迁移创建/保留");
        }
        Integer avatarColumns = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'avatar'",
                Integer.class);
        assertEquals(1, avatarColumns, "sys_user.avatar 列应由 V5 迁移添加");
        Integer annColumns = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'sys_announcement' " +
                        "AND column_name IN ('category', 'publish_time', 'expire_time')",
                Integer.class);
        assertEquals(3, annColumns, "sys_announcement 的 category/publish_time/expire_time 列应由 V7 迁移添加");
        Integer forumColumns = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'forum_post' " +
                        "AND column_name IN ('category_id', 'pinned', 'featured', 'like_count')",
                Integer.class);
        assertEquals(4, forumColumns, "forum_post 的 category_id/pinned/featured/like_count 列应由 V10/V11 迁移添加");
        Integer fulltextIndexes = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics " +
                        "WHERE table_schema = DATABASE() AND index_name IN " +
                        "('ft_forum_post_title_content', 'ft_student_name', 'ft_instructor_name')",
                Integer.class);
        assertEquals(3, fulltextIndexes, "V12 应添加 3 个 ngram 全文索引（论坛帖子/学生姓名/教师姓名）");
    }

    /** 幂等性兜底：当前版本应为 12，且没有失败记录 */
    @Test
    void noFailedMigrationAndVersionIsLatest() {
        Integer failedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0",
                Integer.class);
        assertEquals(0, failedCount, "不应存在失败的迁移记录");

        String latest = jdbcTemplate.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE type = 'SQL' ORDER BY installed_rank DESC LIMIT 1",
                String.class);
        assertEquals("12", latest, "最新已应用迁移版本应为 12");
    }
}
