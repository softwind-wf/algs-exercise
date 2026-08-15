/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** 在线用户存储：单机内存实现（多实例模式关闭时使用） */
@Component
@ConditionalOnMissingBean(OnlineUserStore.class)
public class InMemoryOnlineUserStore implements OnlineUserStore {

    private final ConcurrentHashMap<String, String> sessions = new ConcurrentHashMap<>();

    @Override
    public void put(String sessionId, String userJson, long ttlSeconds) {
        sessions.put(sessionId, userJson);
    }

    @Override
    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    @Override
    public int count() {
        return sessions.size();
    }

    @Override
    public List<String> allValues() {
        return new ArrayList<>(sessions.values());
    }
}
