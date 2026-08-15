package com.ds.university.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 在线用户存储：Redis 实现（多实例共享）。
 * 每个登录会话一个 key（online:session:{sessionId} = 用户 JSON），带 TTL：
 * 正常退出/超时由本实例 SessionListener 删除；实例宕机时由 Redis TTL 自动清理，不残留僵尸在线。
 * 枚举用 SCAN（避免 KEYS 阻塞）。
 */
@Component
@ConditionalOnProperty(name = "app.multi-instance.enabled", havingValue = "true")
public class RedisOnlineUserStore implements OnlineUserStore {

    private static final String SESSION_KEY = "online:session:";

    private final StringRedisTemplate redis;

    public RedisOnlineUserStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void put(String sessionId, String userJson, long ttlSeconds) {
        redis.opsForValue().set(SESSION_KEY + sessionId, userJson, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public void remove(String sessionId) {
        redis.delete(SESSION_KEY + sessionId);
    }

    @Override
    public int count() {
        int count = 0;
        try (Cursor<String> cursor = redis.scan(ScanOptions.scanOptions().match(SESSION_KEY + "*").count(200).build())) {
            while (cursor.hasNext()) {
                cursor.next();
                count++;
            }
        }
        return count;
    }

    @Override
    public List<String> allValues() {
        List<String> values = new ArrayList<>();
        try (Cursor<String> cursor = redis.scan(ScanOptions.scanOptions().match(SESSION_KEY + "*").count(200).build())) {
            while (cursor.hasNext()) {
                String value = redis.opsForValue().get(cursor.next());
                if (value != null) {
                    values.add(value);
                }
            }
        }
        return values;
    }
}
