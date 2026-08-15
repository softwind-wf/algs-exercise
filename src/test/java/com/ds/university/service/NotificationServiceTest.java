package com.ds.university.service;

import com.ds.university.entity.UserNotification;
import com.ds.university.mapper.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 站内通知单元测试：发送、未读数、已读管理。 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationMapper notificationMapper;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationMapper);
    }

    @Test
    void notifyInsertsTruncatedSummary() {
        String longSummary = new String(new char[300]).replace('\0', 'x');
        service.notify("10101", UserNotification.TYPE_FORUM_MENTION, "/forum/1", longSummary);
        ArgumentCaptor<UserNotification> captor = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationMapper).insert(captor.capture());
        assertEquals("10101", captor.getValue().getUserId());
        assertEquals(200, captor.getValue().getSummary().length());
    }

    @Test
    void notifyIgnoresEmptySummary() {
        service.notify("10101", UserNotification.TYPE_FORUM_MENTION, "/forum/1", "");
        verify(notificationMapper, org.mockito.Mockito.never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unreadCountDelegates() {
        when(notificationMapper.countUnread("zhang")).thenReturn(3);
        assertEquals(3, service.unreadCount("zhang"));
    }

    @Test
    void markReadDelegates() {
        service.markRead(1L, "zhang");
        verify(notificationMapper).markRead(1L, "zhang");
    }

    @Test
    void markAllReadDelegates() {
        service.markAllRead("zhang");
        verify(notificationMapper).markAllRead("zhang");
    }
}
