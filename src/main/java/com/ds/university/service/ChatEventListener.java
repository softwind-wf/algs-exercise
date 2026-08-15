package com.ds.university.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 聊天事件订阅（多实例模式启用时注册到 RedisMessageListenerContainer）：
 * 收到消息频道 → 交付给接收方所在实例的本地会话；
 * 收到在线状态频道 → 向本地客户端推送最新在线集合。
 */
@Component
@ConditionalOnProperty(name = "app.multi-instance.enabled", havingValue = "true")
public class ChatEventListener implements MessageListener {

    private final ChatWebSocketHandler handler;

    public ChatEventListener(ChatWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        if (ChatWebSocketHandler.CHANNEL_MESSAGE.equals(channel)) {
            handler.handleRemoteMessage(body);
        } else if (ChatWebSocketHandler.CHANNEL_PRESENCE.equals(channel)) {
            handler.handleRemotePresence();
        }
    }
}
