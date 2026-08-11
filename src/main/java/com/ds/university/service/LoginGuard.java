package com.ds.university.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录防爆破守卫：按账号记录失败次数，窗口内连续失败达到阈值后临时锁定。
 * <p>
 * 策略：{@link #WINDOW_MILLIS} 时间窗内失败 {@link #MAX_ATTEMPTS} 次，
 * 锁定 {@link #LOCK_MILLIS}；登录成功立即清除记录。
 * 内存实现，重启后清零；多实例部署需替换为 Redis 等共享存储。
 */
@Component
public class LoginGuard {

    /** 统计窗口：5 分钟 */
    static final long WINDOW_MILLIS = 5 * 60 * 1000L;
    /** 窗口内最大失败次数，达到即锁定 */
    static final int MAX_ATTEMPTS = 5;
    /** 锁定时长：15 分钟 */
    static final long LOCK_MILLIS = 15 * 60 * 1000L;

    private final Clock clock;
    private final Map<String, Record> records = new ConcurrentHashMap<>();

    public LoginGuard() {
        this(Clock.systemDefaultZone());
    }

    /** 供测试注入可控时钟 */
    LoginGuard(Clock clock) {
        this.clock = clock;
    }

    /** 剩余锁定秒数，0 表示未锁定 */
    public long lockRemainingSeconds(String userId) {
        Record record = records.get(userId);
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
                records.remove(userId, record);
                return 0;
            }
            return (remaining + 999) / 1000;
        }
    }

    /** 记录一次登录失败；窗口内达到阈值则锁定 */
    public void recordFailure(String userId) {
        long now = clock.millis();
        Record record = records.computeIfAbsent(userId, k -> new Record());
        synchronized (record) {
            record.failures.addLast(now);
            purgeExpired(record, now);
            if (record.failures.size() >= MAX_ATTEMPTS) {
                record.lockUntil = now + LOCK_MILLIS;
                record.failures.clear();
            }
        }
    }

    /** 登录成功：清除该账号全部失败记录与锁定 */
    public void recordSuccess(String userId) {
        records.remove(userId);
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
