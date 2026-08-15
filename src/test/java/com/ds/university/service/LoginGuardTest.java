package com.ds.university.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LoginGuard 单元测试：用可控时钟验证账号与 IP 两个维度的失败计数、锁定、过期清理与成功清除。
 */
class LoginGuardTest {

    /** 可手动推进的时钟 */
    private static class MutableClock extends Clock {
        private long millis = 1_700_000_000_000L;

        void advance(long millis) {
            this.millis += millis;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }

        @Override
        public long millis() {
            return millis;
        }
    }

    private MutableClock clock;
    private LoginGuard guard;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        guard = new LoginGuard(new InMemoryLoginRateStore(), clock);
    }

    @Test
    void notLockedBelowThreshold() {
        for (int i = 0; i < LoginGuard.MAX_ATTEMPTS - 1; i++) {
            guard.recordFailure("alice");
        }
        assertEquals(0, guard.lockRemainingSeconds("alice"));
    }

    @Test
    void lockedWhenReachingThreshold() {
        for (int i = 0; i < LoginGuard.MAX_ATTEMPTS; i++) {
            guard.recordFailure("alice");
        }
        long remaining = guard.lockRemainingSeconds("alice");
        assertTrue(remaining > 0, "达到阈值后应处于锁定状态");
        assertTrue(remaining <= LoginGuard.LOCK_MILLIS / 1000);
    }

    @Test
    void lockExpiresAfterLockDuration() {
        for (int i = 0; i < LoginGuard.MAX_ATTEMPTS; i++) {
            guard.recordFailure("alice");
        }
        assertTrue(guard.lockRemainingSeconds("alice") > 0);

        clock.advance(LoginGuard.LOCK_MILLIS + 1);
        assertEquals(0, guard.lockRemainingSeconds("alice"));
    }

    @Test
    void failuresOutsideWindowDoNotCount() {
        for (int i = 0; i < LoginGuard.MAX_ATTEMPTS - 1; i++) {
            guard.recordFailure("alice");
        }
        // 超出统计窗口后再失败一次，不应触发锁定
        clock.advance(LoginGuard.WINDOW_MILLIS + 1);
        guard.recordFailure("alice");
        assertEquals(0, guard.lockRemainingSeconds("alice"));
    }

    @Test
    void successClearsFailureHistory() {
        for (int i = 0; i < LoginGuard.MAX_ATTEMPTS - 1; i++) {
            guard.recordFailure("alice");
        }
        guard.recordSuccess("alice");
        // 清除后再失败 MAX_ATTEMPTS-1 次不应锁定
        for (int i = 0; i < LoginGuard.MAX_ATTEMPTS - 1; i++) {
            guard.recordFailure("alice");
        }
        assertEquals(0, guard.lockRemainingSeconds("alice"));
    }

    @Test
    void accountsAreIsolated() {
        for (int i = 0; i < LoginGuard.MAX_ATTEMPTS; i++) {
            guard.recordFailure("alice");
        }
        assertTrue(guard.lockRemainingSeconds("alice") > 0);
        assertEquals(0, guard.lockRemainingSeconds("bob"));
    }

    /**
     * 回归用例：模拟真实登录流程——每次尝试都先查锁再记失败。
     * 查询未锁定状态不得清空失败计数历史，否则永远无法触发锁定。
     */
    @Test
    void interleavedLockCheckDoesNotResetFailures() {
        for (int i = 0; i < LoginGuard.MAX_ATTEMPTS; i++) {
            assertEquals(0, guard.lockRemainingSeconds("alice"), "第 " + (i + 1) + " 次尝试前应未锁定");
            guard.recordFailure("alice");
        }
        assertTrue(guard.lockRemainingSeconds("alice") > 0, "交错查询不应重置计数，达到阈值后应锁定");
    }

    // ===== IP 维度 =====

    @Test
    void ipNotLockedBelowThreshold() {
        for (int i = 0; i < LoginGuard.IP_MAX_ATTEMPTS - 1; i++) {
            guard.recordIpFailure("10.0.0.1");
        }
        assertEquals(0, guard.ipLockRemainingSeconds("10.0.0.1"));
    }

    @Test
    void ipLockedWhenReachingThreshold() {
        for (int i = 0; i < LoginGuard.IP_MAX_ATTEMPTS; i++) {
            guard.recordIpFailure("10.0.0.1");
        }
        long remaining = guard.ipLockRemainingSeconds("10.0.0.1");
        assertTrue(remaining > 0, "同一 IP 达到阈值后应处于锁定状态");
        assertTrue(remaining <= LoginGuard.IP_LOCK_MILLIS / 1000);
    }

    @Test
    void ipLockExpiresAfterLockDuration() {
        for (int i = 0; i < LoginGuard.IP_MAX_ATTEMPTS; i++) {
            guard.recordIpFailure("10.0.0.1");
        }
        assertTrue(guard.ipLockRemainingSeconds("10.0.0.1") > 0);

        clock.advance(LoginGuard.IP_LOCK_MILLIS + 1);
        assertEquals(0, guard.ipLockRemainingSeconds("10.0.0.1"));
    }

    @Test
    void ipFailuresOutsideWindowDoNotCount() {
        for (int i = 0; i < LoginGuard.IP_MAX_ATTEMPTS - 1; i++) {
            guard.recordIpFailure("10.0.0.1");
        }
        // 超出统计窗口后再失败一次，不应触发锁定
        clock.advance(LoginGuard.WINDOW_MILLIS + 1);
        guard.recordIpFailure("10.0.0.1");
        assertEquals(0, guard.ipLockRemainingSeconds("10.0.0.1"));
    }

    @Test
    void differentIpsAreIsolated() {
        for (int i = 0; i < LoginGuard.IP_MAX_ATTEMPTS; i++) {
            guard.recordIpFailure("10.0.0.1");
        }
        assertTrue(guard.ipLockRemainingSeconds("10.0.0.1") > 0);
        assertEquals(0, guard.ipLockRemainingSeconds("10.0.0.2"));
    }

    /** IP 与账号两个维度互不影响 */
    @Test
    void ipAndAccountDimensionsAreIsolated() {
        // IP 达到阈值不应锁定任何账号
        for (int i = 0; i < LoginGuard.IP_MAX_ATTEMPTS; i++) {
            guard.recordIpFailure("10.0.0.1");
        }
        assertEquals(0, guard.lockRemainingSeconds("alice"));
        // 账号达到阈值不应锁定 IP
        for (int i = 0; i < LoginGuard.MAX_ATTEMPTS; i++) {
            guard.recordFailure("bob");
        }
        assertTrue(guard.lockRemainingSeconds("bob") > 0);
        assertEquals(0, guard.ipLockRemainingSeconds("10.0.0.2"));
    }

    /** 单个账号登录成功不得清空 IP 维度记录（防止攻击者用一个正确账号洗白 IP） */
    @Test
    void accountSuccessDoesNotClearIpRecord() {
        for (int i = 0; i < LoginGuard.IP_MAX_ATTEMPTS - 1; i++) {
            guard.recordIpFailure("10.0.0.1");
        }
        guard.recordSuccess("alice");
        guard.recordIpFailure("10.0.0.1");
        assertTrue(guard.ipLockRemainingSeconds("10.0.0.1") > 0, "账号成功不应重置 IP 计数，达到阈值后应锁定");
    }

    /** clearIp 应清除失败记录与锁定 */
    @Test
    void clearIpResetsRecord() {
        for (int i = 0; i < LoginGuard.IP_MAX_ATTEMPTS; i++) {
            guard.recordIpFailure("10.0.0.1");
        }
        assertTrue(guard.ipLockRemainingSeconds("10.0.0.1") > 0);
        guard.clearIp("10.0.0.1");
        assertEquals(0, guard.ipLockRemainingSeconds("10.0.0.1"));
    }
}
