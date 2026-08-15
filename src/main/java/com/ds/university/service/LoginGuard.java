package com.ds.university.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * 登录防爆破守卫：按账号、IP 两个维度记录失败次数，窗口内失败达到阈值后临时锁定。
 * <p>
 * 账号维度：{@link #WINDOW_MILLIS} 时间窗内失败 {@link #MAX_ATTEMPTS} 次，锁定 {@link #LOCK_MILLIS}；
 * 登录成功立即清除记录。IP 维度：窗口内失败 {@link #IP_MAX_ATTEMPTS} 次锁定该 IP，
 * 阈值高于账号维度以容忍 NAT 共享出口；IP 记录只随窗口/锁定到期自然过期。
 * <p>
 * 存储由 {@link LoginRateStore} 提供：多实例模式走 Redis（跨实例共享），
 * 否则单机内存实现。
 */
@Component
public class LoginGuard {

    /** 统计窗口：5 分钟（账号与 IP 维度共用） */
    static final long WINDOW_MILLIS = 5 * 60 * 1000L;
    /** 账号维度：窗口内最大失败次数，达到即锁定 */
    static final int MAX_ATTEMPTS = 5;
    /** 锁定时长：15 分钟 */
    static final long LOCK_MILLIS = 15 * 60 * 1000L;
    /** IP 维度：窗口内最大失败次数，达到即锁定该 IP */
    public static final int IP_MAX_ATTEMPTS = 30;
    /** IP 维度锁定时长：15 分钟 */
    static final long IP_LOCK_MILLIS = 15 * 60 * 1000L;

    private final LoginRateStore store;
    private final Clock clock;

    @Autowired
    public LoginGuard(LoginRateStore store) {
        this(store, Clock.systemDefaultZone());
    }

    /** 供测试注入可控时钟与指定存储 */
    LoginGuard(LoginRateStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    /** 账号维度剩余锁定秒数，0 表示未锁定 */
    public long lockRemainingSeconds(String userId) {
        return lockRemainingSecondsByKey(accountKey(userId));
    }

    /** 记录一次账号维度登录失败；窗口内达到阈值则锁定 */
    public void recordFailure(String userId) {
        recordFailure(accountKey(userId), MAX_ATTEMPTS, LOCK_MILLIS);
    }

    /** 登录成功：清除该账号全部失败记录与锁定 */
    public void recordSuccess(String userId) {
        String key = accountKey(userId);
        store.clearFailures(key);
        store.clearLock(key);
    }

    /** IP 维度剩余锁定秒数，0 表示未锁定 */
    public long ipLockRemainingSeconds(String ip) {
        return lockRemainingSecondsByKey(ipKey(ip));
    }

    /** 记录一次 IP 维度登录失败；窗口内达到阈值则锁定该 IP */
    public void recordIpFailure(String ip) {
        recordFailure(ipKey(ip), IP_MAX_ATTEMPTS, IP_LOCK_MILLIS);
    }

    /** 测试辅助：清空指定 IP 的失败记录与锁定 */
    public void clearIp(String ip) {
        String key = ipKey(ip);
        store.clearFailures(key);
        store.clearLock(key);
    }

    private void recordFailure(String key, int maxAttempts, long lockMillis) {
        store.recordFailure(key, clock.millis(), WINDOW_MILLIS);
        if (store.failureCount(key, clock.millis(), WINDOW_MILLIS) >= maxAttempts) {
            store.lock(key, clock.millis() + lockMillis);
            store.clearFailures(key);
        }
    }

    private long lockRemainingSecondsByKey(String key) {
        long until = store.lockUntilMillis(key);
        long remainingMillis = until - clock.millis();
        return remainingMillis <= 0 ? 0 : (remainingMillis + 999) / 1000;
    }

    private static String accountKey(String userId) {
        return "ACCOUNT:" + userId;
    }

    private static String ipKey(String ip) {
        return "IP:" + ip;
    }
}
