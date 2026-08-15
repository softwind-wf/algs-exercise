/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 聊天事件发布器：Redis 实现（多实例模式）。
 * 在线集合存 Redis SET（chat:online），消息经 Pub/Sub 频道广播：
 * 发送方实例只负责落库与回执，接收方所在实例收到频道消息后向本地连接投递。
 */
@Component
@ConditionalOnProperty(name = "app.multi-instance.enabled", havingValue = "true")
public class RedisChatEventPublisher implements ChatEventPublisher {

    private static final String ONLINE_SET = "chat:online";

    private final StringRedisTemplate redis;

    public RedisChatEventPublisher(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean isRemoteEnabled() {
        return true;
    }

    @Override
    public void publishMessage(String json) {
        redis.convertAndSend(ChatWebSocketHandler.CHANNEL_MESSAGE, json);
    }

    @Override
    public void publishPresenceChange() {
        redis.convertAndSend(ChatWebSocketHandler.CHANNEL_PRESENCE, "");
    }

    @Override
    public void registerOnline(String userId) {
        redis.opsForSet().add(ONLINE_SET, userId);
    }

    @Override
    public void unregisterOnline(String userId) {
        redis.opsForSet().remove(ONLINE_SET, userId);
    }

    @Override
    public Set<String> onlineUserIds() {
        Set<String> users = redis.opsForSet().members(ONLINE_SET);
        return users == null ? new HashSet<>() : users;
    }
}
