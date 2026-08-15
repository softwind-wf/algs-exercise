package com.ds.university.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 日志脱敏工具单元测试 */
class SensitiveDataUtilTest {

    @Test
    void masksBcryptHash() {
        String hash = "$2a$10$/Gu.uRug7LYoOu0PzCdVKOqo4Ayxt3fM2utBEet4jNQ5nouNqojKO";
        String masked = SensitiveDataUtil.mask("密码哈希 " + hash);
        assertFalse(masked.contains("$2a$"), "不应包含 BCrypt 哈希原文");
        assertTrue(masked.contains("***"));
    }

    @Test
    void masksPhone() {
        assertEquals("联系 138****5678", SensitiveDataUtil.mask("联系 13812345678"));
    }

    @Test
    void masksIdCard() {
        assertEquals("身份证 110101********1234", SensitiveDataUtil.mask("身份证 110101199001011234"));
    }

    @Test
    void masksPasswordParameter() {
        assertEquals("password=***", SensitiveDataUtil.mask("password=secret123"));
        assertEquals("{\"password\":\"***\"}", SensitiveDataUtil.mask("{\"password\":\"secret123\"}"));
        assertEquals("&password='***'&", SensitiveDataUtil.mask("&password='secret123'&"));
    }

    @Test
    void masksSessionAndTokens() {
        assertEquals("JSESSIONID=***", SensitiveDataUtil.mask("JSESSIONID=ABC123XYZ"));
        assertEquals("Cookie: jsessionid=***", SensitiveDataUtil.mask("Cookie: jsessionid=ABC123XYZ"));
        assertEquals("_csrf=***", SensitiveDataUtil.mask("_csrf=AbC123+Def456/"));
        assertEquals("csrfToken=***", SensitiveDataUtil.mask("csrfToken=abcDEF123"));
        assertEquals("Authorization: Bearer ***", SensitiveDataUtil.mask("Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.abc"));
    }

    @Test
    void leavesNormalTextUntouched() {
        String normal = "用户 zhang 登录成功，部门 Comp. Sci.，成绩 A+";
        assertEquals(normal, SensitiveDataUtil.mask(normal));
    }
}
