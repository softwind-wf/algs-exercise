package com.ds.university.config;

import java.util.List;

/**
 * 在线用户存储：跨实例共享（Redis，按会话 TTL 自动清理）或单机内存实现。
 * 存储内容为登录会话（sessionId → 用户 JSON）。
 */
public interface OnlineUserStore {

    /** 记录在线会话（带 TTL 秒数，超时自动清理） */
    void put(String sessionId, String userJson, long ttlSeconds);

    /** 移除会话（退出/超时/容器回收） */
    void remove(String sessionId);

    /** 在线会话数 */
    int count();

    /** 全部在线用户 JSON */
    List<String> allValues();
}
