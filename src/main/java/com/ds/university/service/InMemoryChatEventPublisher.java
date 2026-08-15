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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天事件发布器：单机实现（多实例模式关闭时使用）。
 * 在线集合为本实例本地维护；发布方法为空操作（消息由 Handler 本地直发）。
 */
@Component
@ConditionalOnMissingBean(ChatEventPublisher.class)
public class InMemoryChatEventPublisher implements ChatEventPublisher {

    private final Set<String> online = ConcurrentHashMap.newKeySet();

    @Override
    public boolean isRemoteEnabled() {
        return false;
    }

    @Override
    public void publishMessage(String json) {
        // 单机模式：消息由 Handler 直接投递给本机接收方
    }

    @Override
    public void publishPresenceChange() {
        // 单机模式：在线状态由 Handler 直接广播
    }

    @Override
    public void registerOnline(String userId) {
        online.add(userId);
    }

    @Override
    public void unregisterOnline(String userId) {
        online.remove(userId);
    }

    @Override
    public Set<String> onlineUserIds() {
        return online;
    }
}
