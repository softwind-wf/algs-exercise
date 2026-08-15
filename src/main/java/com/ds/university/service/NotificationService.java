/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.service;

import com.ds.university.entity.UserNotification;
import com.ds.university.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 站内通知（@提及等）：写入、查询、已读管理。
 * 通知只增不改，导航栏角标轮询未读数。
 */
@Service
public class NotificationService {

    /** 通知列表上限 */
    public static final int LIST_LIMIT = 100;

    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /** 发送一条通知给指定用户 */
    public void notify(String userId, String type, String sourceUrl, String summary) {
        if (userId == null || userId.isEmpty() || summary == null || summary.isEmpty()) {
            return;
        }
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setSourceUrl(sourceUrl);
        notification.setSummary(summary.length() > 200 ? summary.substring(0, 200) : summary);
        notificationMapper.insert(notification);
    }

    /** 某用户的通知（倒序） */
    public List<UserNotification> list(String userId) {
        return notificationMapper.selectByUser(userId, LIST_LIMIT);
    }

    /** 未读数（导航栏角标） */
    public int unreadCount(String userId) {
        return notificationMapper.countUnread(userId);
    }

    /** 全部标记已读 */
    public void markAllRead(String userId) {
        notificationMapper.markAllRead(userId);
    }

    /** 单条标记已读（仅本人） */
    public void markRead(Long id, String userId) {
        notificationMapper.markRead(id, userId);
    }
}
