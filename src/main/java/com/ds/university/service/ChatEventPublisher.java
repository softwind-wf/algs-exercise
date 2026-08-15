/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.service;

import java.util.Set;

/**
 * 聊天事件发布器：消息跨实例路由与全局在线集合。
 * 多实例模式走 Redis Pub/Sub + SET；单机模式为本地直发（isRemoteEnabled()=false）。
 */
public interface ChatEventPublisher {

    /** 是否为跨实例模式（true 时消息经 Pub/Sub 路由，false 时本地直发） */
    boolean isRemoteEnabled();

    /** 发布一条聊天消息（JSON 负载） */
    void publishMessage(String json);

    /** 发布在线状态变更事件（实例收到后向本地客户端推送最新在线集合） */
    void publishPresenceChange();

    /** 注册在线（连接建立） */
    void registerOnline(String userId);

    /** 注销在线（连接关闭） */
    void unregisterOnline(String userId);

    /** 当前在线用户集合（跨实例） */
    Set<String> onlineUserIds();
}
