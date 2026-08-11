package com.ds.university.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LoginGuard 单元测试：用可控时钟验证失败计数、锁定、过期清理与成功清除。
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
        guard = new LoginGuard(clock);
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
}
