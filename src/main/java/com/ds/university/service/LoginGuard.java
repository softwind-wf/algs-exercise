package com.ds.university.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录防爆破守卫：按账号、IP 两个维度记录失败次数，窗口内失败达到阈值后临时锁定。
 * <p>
 * 账号维度：{@link #WINDOW_MILLIS} 时间窗内失败 {@link #MAX_ATTEMPTS} 次，
 * 锁定 {@link #LOCK_MILLIS}；登录成功立即清除记录。
 * <p>
 * IP 维度：同一 IP 在窗口内失败 {@link #IP_MAX_ATTEMPTS} 次则锁定该 IP
 * {@link #IP_LOCK_MILLIS}，防止攻击者对全站账号各试几次绕过账号维度锁定；
 * 阈值高于账号维度以容忍 NAT 共享出口；IP 记录只随窗口/锁定到期自然过期，
 * 不随单个账号登录成功而清除。
 * <p>
 * 内存实现，重启后清零；多实例部署需替换为 Redis 等共享存储。
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

    private final Clock clock;
    private final Map<String, Record> accountRecords = new ConcurrentHashMap<>();
    private final Map<String, Record> ipRecords = new ConcurrentHashMap<>();

    public LoginGuard() {
        this(Clock.systemDefaultZone());
    }

    /** 供测试注入可控时钟 */
    LoginGuard(Clock clock) {
        this.clock = clock;
    }

    /** 账号维度剩余锁定秒数，0 表示未锁定 */
    public long lockRemainingSeconds(String userId) {
        return lockRemainingSeconds(accountRecords, userId);
    }

    /** 记录一次账号维度登录失败；窗口内达到阈值则锁定 */
    public void recordFailure(String userId) {
        recordFailure(accountRecords, userId, MAX_ATTEMPTS, LOCK_MILLIS);
    }

    /** 登录成功：清除该账号全部失败记录与锁定 */
    public void recordSuccess(String userId) {
        accountRecords.remove(userId);
    }

    /** IP 维度剩余锁定秒数，0 表示未锁定 */
    public long ipLockRemainingSeconds(String ip) {
        return lockRemainingSeconds(ipRecords, ip);
    }

    /** 记录一次 IP 维度登录失败；窗口内达到阈值则锁定该 IP */
    public void recordIpFailure(String ip) {
        recordFailure(ipRecords, ip, IP_MAX_ATTEMPTS, IP_LOCK_MILLIS);
    }

    /** 测试辅助：清空指定 IP 的失败记录与锁定 */
    public void clearIp(String ip) {
        ipRecords.remove(ip);
    }

    private long lockRemainingSeconds(Map<String, Record> map, String key) {
        Record record = map.get(key);
        if (record == null) {
            return 0;
        }
        synchronized (record) {
            if (record.lockUntil <= 0) {
                // 从未锁定：不能删除记录，否则会清空失败计数历史
                return 0;
            }
            long remaining = record.lockUntil - clock.millis();
            if (remaining <= 0) {
                // 锁定已到期：连同失败记录一起清除，重新开始计数
                map.remove(key, record);
                return 0;
            }
            return (remaining + 999) / 1000;
        }
    }

    private void recordFailure(Map<String, Record> map, String key, int maxAttempts, long lockMillis) {
        long now = clock.millis();
        Record record = map.computeIfAbsent(key, k -> new Record());
        synchronized (record) {
            record.failures.addLast(now);
            purgeExpired(record, now);
            if (record.failures.size() >= maxAttempts) {
                record.lockUntil = now + lockMillis;
                record.failures.clear();
            }
        }
    }

    private void purgeExpired(Record record, long now) {
        Iterator<Long> it = record.failures.iterator();
        while (it.hasNext()) {
            if (now - it.next() > WINDOW_MILLIS) {
                it.remove();
            } else {
                break;
            }
        }
    }

    private static final class Record {
        /** 窗口内的失败时间戳（毫秒），按时间升序 */
        final Deque<Long> failures = new ArrayDeque<>();
        /** 锁定截止时间戳（毫秒），<= 当前时间表示未锁定 */
        volatile long lockUntil;
    }
}
