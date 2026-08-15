package com.ds.university.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 登录限流存储：Redis 实现（多实例共享）。
 * 失败时间窗用 ZSET（score=时间戳，member 唯一），锁定用带 TTL 的 key（value=截止时间戳）。
 * key 前缀：login:fail:{维度}（ZSET）/ login:lock:{维度}（STRING）。
 */
@Component
@ConditionalOnProperty(name = "app.multi-instance.enabled", havingValue = "true")
public class RedisLoginRateStore implements LoginRateStore {

    private static final String FAIL_KEY = "login:fail:";
    private static final String LOCK_KEY = "login:lock:";

    private final StringRedisTemplate redis;

    public RedisLoginRateStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void recordFailure(String key, long nowMillis, long windowMillis) {
        String member = UUID.randomUUID().toString() + ":" + nowMillis;
        redis.opsForZSet().add(FAIL_KEY + key, member, nowMillis);
        purge(key, nowMillis, windowMillis);
    }

    @Override
    public int failureCount(String key, long nowMillis, long windowMillis) {
        purge(key, nowMillis, windowMillis);
        Long count = redis.opsForZSet().zCard(FAIL_KEY + key);
        return count == null ? 0 : count.intValue();
    }

    @Override
    public void clearFailures(String key) {
        redis.delete(FAIL_KEY + key);
    }

    @Override
    public void lock(String key, long untilMillis) {
        long ttlSeconds = Math.max(1, (untilMillis - System.currentTimeMillis() + 999) / 1000);
        redis.opsForValue().set(LOCK_KEY + key, String.valueOf(untilMillis), Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public long lockUntilMillis(String key) {
        String value = redis.opsForValue().get(LOCK_KEY + key);
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void clearLock(String key) {
        redis.delete(LOCK_KEY + key);
    }

    private void purge(String key, long nowMillis, long windowMillis) {
        redis.opsForZSet().removeRangeByScore(FAIL_KEY + key, 0, nowMillis - windowMillis);
    }
}
