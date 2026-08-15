/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.config;

import com.ds.university.service.ChatEventListener;
import com.ds.university.service.ChatWebSocketHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Arrays;

/**
 * Redis 配置（多实例模式启用时生效，见 application.yml 的 app.multi-instance.enabled）：
 * 消息监听容器订阅聊天消息与在线状态变更频道，实现聊天跨实例路由。
 * 存储操作用 Spring Boot 自动配置的 StringRedisTemplate。
 */
@Configuration
@ConditionalOnProperty(name = "app.multi-instance.enabled", havingValue = "true")
public class RedisConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory factory, ChatEventListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(listener, Arrays.asList(
                new ChannelTopic(ChatWebSocketHandler.CHANNEL_MESSAGE),
                new ChannelTopic(ChatWebSocketHandler.CHANNEL_PRESENCE)));
        return container;
    }
}
