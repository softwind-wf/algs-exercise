package com.ds.university.service;

/**
 * 登录失败限流存储：跨实例共享（Redis）或单机内存实现。
 * 语义：记录失败进入时间窗、窗口内计数、锁定（带 TTL）。
 */
public interface LoginRateStore {

    /** 记录一次失败（进入时间窗），并清理窗口外的旧记录 */
    void recordFailure(String key, long nowMillis, long windowMillis);

    /** 时间窗内失败次数（同时清理窗口外旧记录） */
    int failureCount(String key, long nowMillis, long windowMillis);

    /** 清除失败记录（登录成功时调用） */
    void clearFailures(String key);

    /** 设置锁定截止时间戳（毫秒），由调用方用其时钟计算；存储负责过期清理 */
    void lock(String key, long untilMillis);

    /** 锁定截止时间戳（毫秒），0 表示未锁定 */
    long lockUntilMillis(String key);

    /** 清除锁定 */
    void clearLock(String key);
}
