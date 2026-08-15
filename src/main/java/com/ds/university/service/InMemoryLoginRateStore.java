/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录限流存储：单机内存实现（多实例模式关闭时使用，也是测试用的默认实现）。
 * 每个 key 维护窗口内失败时间戳（升序）+ 锁定截止时间。
 */
@Component
@ConditionalOnMissingBean(LoginRateStore.class)
public class InMemoryLoginRateStore implements LoginRateStore {

    private final Map<String, Record> records = new ConcurrentHashMap<>();

    @Override
    public void recordFailure(String key, long nowMillis, long windowMillis) {
        Record record = records.computeIfAbsent(key, k -> new Record());
        synchronized (record) {
            record.failures.addLast(nowMillis);
            purgeExpired(record, nowMillis, windowMillis);
        }
    }

    @Override
    public int failureCount(String key, long nowMillis, long windowMillis) {
        Record record = records.get(key);
        if (record == null) {
            return 0;
        }
        synchronized (record) {
            purgeExpired(record, nowMillis, windowMillis);
            return record.failures.size();
        }
    }

    @Override
    public void clearFailures(String key) {
        Record record = records.get(key);
        if (record != null) {
            synchronized (record) {
                record.failures.clear();
            }
        }
    }

    @Override
    public void lock(String key, long untilMillis) {
        Record record = records.computeIfAbsent(key, k -> new Record());
        synchronized (record) {
            record.lockUntil = untilMillis;
        }
    }

    @Override
    public long lockUntilMillis(String key) {
        Record record = records.get(key);
        return record == null ? 0 : record.lockUntil;
    }

    @Override
    public void clearLock(String key) {
        Record record = records.get(key);
        if (record != null) {
            synchronized (record) {
                record.lockUntil = 0;
            }
        }
    }

    private void purgeExpired(Record record, long nowMillis, long windowMillis) {
        Iterator<Long> it = record.failures.iterator();
        while (it.hasNext()) {
            if (nowMillis - it.next() > windowMillis) {
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
