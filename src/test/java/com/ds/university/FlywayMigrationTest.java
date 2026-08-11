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
 * 版本化脚本（V1 业务表 / V2 账号权限 / V3 审计日志 / V4 收编 instructor.phone_number），验证：
 * 1. flyway_schema_history 中 V1-V4 均成功应用；
 * 2. 各脚本对应的关键表真实存在。
 * 线上升级不再依赖手工 SQL，schema 漂移可被 validate-on-migrate 拦截。
 */
@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** V1-V4 迁移都记录在案且 success=1 */
    @Test
    void allMigrationsAppliedSuccessfully() {
        for (String version : new String[]{"1", "2", "3", "4"}) {
            Integer success = jdbcTemplate.queryForObject(
                    "SELECT success FROM flyway_schema_history WHERE version = ? AND type = 'SQL'",
                    Integer.class, version);
            assertEquals(1, success, "V" + version + " 迁移应成功应用");
        }
    }

    /** 迁移产物真实存在：业务表、RBAC 表、审计表、Flyway 历史表 */
    @Test
    void migratedTablesExist() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
                String.class);
        for (String expected : new String[]{"department", "sys_user", "audit_log", "flyway_schema_history"}) {
            assertTrue(tables.contains(expected), "表 " + expected + " 应由 Flyway 迁移创建/保留");
        }
    }

    /** 幂等性兜底：当前版本应为 4，且没有失败记录 */
    @Test
    void noFailedMigrationAndVersionIsLatest() {
        Integer failedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0",
                Integer.class);
        assertEquals(0, failedCount, "不应存在失败的迁移记录");

        String latest = jdbcTemplate.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE type = 'SQL' ORDER BY installed_rank DESC LIMIT 1",
                String.class);
        assertEquals("4", latest, "最新已应用迁移版本应为 4");
    }
}
