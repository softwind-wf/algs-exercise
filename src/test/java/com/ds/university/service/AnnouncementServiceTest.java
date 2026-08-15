package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.entity.Announcement;
import com.ds.university.mapper.AnnouncementMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 系统公告单元测试：校验、定时/到期逻辑、状态计算、置顶/发布切换。
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementMapper announcementMapper;
    @Mock
    private AuditService auditService;

    private AnnouncementService service;

    @BeforeEach
    void setUp() {
        service = new AnnouncementService(announcementMapper, auditService);
    }

    private Announcement announcement(int enabled, LocalDateTime publish, LocalDateTime expire) {
        Announcement a = new Announcement();
        a.setId(1);
        a.setTitle("公告");
        a.setContent("内容");
        a.setCategory("NOTICE");
        a.setEnabled(enabled);
        a.setPublishTime(publish);
        a.setExpireTime(expire);
        return a;
    }

    // ========== 创建校验 ==========

    @Test
    void createValidatesFieldsAndCategory() {
        assertThrows(BusinessException.class, () -> service.create(" ", "内容", "NOTICE", 0, "", ""));
        assertThrows(BusinessException.class, () -> service.create("标题", " ", "NOTICE", 0, "", ""));
        assertThrows(BusinessException.class, () -> service.create("标题", "内容", "BAD", 0, "", ""));
        verify(announcementMapper, never()).insert(any());
    }

    @Test
    void createValidatesTimeWindow() {
        // 到期早于发布时间 → 拒绝
        assertThrows(BusinessException.class, () -> service.create(
                "标题", "内容", "NOTICE", 0, "2026-09-01T10:00", "2026-09-01T09:00"));
        // 到期早于当前时间 → 拒绝
        assertThrows(BusinessException.class, () -> service.create(
                "标题", "内容", "NOTICE", 0, "", "2020-01-01T10:00"));
        verify(announcementMapper, never()).insert(any());
    }

    @Test
    void createSchedulesWithFuturePublish() {
        service.create("标题", "内容", "NEWS", 1, "2999-01-01T10:00", "");
        verify(announcementMapper).insert(any(Announcement.class));
        verify(auditService).record(anyString(), eq(AuditService.TARGET_ANNOUNCEMENT), anyString(), anyString());
    }

    // ========== 状态计算 ==========

    @Test
    void listAllComputesStatuses() {
        LocalDateTime now = LocalDateTime.now();
        when(announcementMapper.selectAll()).thenReturn(java.util.Arrays.asList(
                announcement(1, null, null),                    // 已发布
                announcement(1, now.plusDays(1), null),          // 定时发布
                announcement(1, null, now.minusDays(1)),         // 已过期
                announcement(0, null, null)                      // 已下线
        ));
        java.util.List<Announcement> list = service.listAll();
        assertEquals(Announcement.STATUS_PUBLISHED, list.get(0).getStatusText());
        assertEquals(Announcement.STATUS_SCHEDULED, list.get(1).getStatusText());
        assertEquals(Announcement.STATUS_EXPIRED, list.get(2).getStatusText());
        assertEquals(Announcement.STATUS_OFFLINE, list.get(3).getStatusText());
    }

    // ========== 发布/下线切换 ==========

    @Test
    void toggleRepublishesExpiredByClearingExpireTime() {
        when(announcementMapper.selectById(1)).thenReturn(announcement(1, null, LocalDateTime.now().minusMinutes(1)));
        boolean published = service.toggleEnabled(1);
        assertTrue(published);
        verify(announcementMapper).clearExpireTime(1);
        verify(announcementMapper, never()).updateEnabled(eq(1), any());
    }

    @Test
    void toggleOfflineForPublished() {
        when(announcementMapper.selectById(1)).thenReturn(announcement(1, null, null));
        boolean published = service.toggleEnabled(1);
        assertEquals(false, published);
        verify(announcementMapper).updateEnabled(1, 0);
    }

    @Test
    void toggleThrowsForMissing() {
        when(announcementMapper.selectById(1)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.toggleEnabled(1));
    }

    // ========== 编辑 ==========

    @Test
    void updateRequiresExisting() {
        when(announcementMapper.selectById(1)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.update(1, "t", "c", "NOTICE", 0, "", ""));
    }

    @Test
    void deleteIgnoresMissing() {
        when(announcementMapper.selectById(1)).thenReturn(null);
        service.delete(1);
        verify(announcementMapper, never()).delete(any());
    }

    @Test
    void deleteAudits() {
        when(announcementMapper.selectById(1)).thenReturn(announcement(1, null, null));
        service.delete(1);
        verify(announcementMapper).delete(1);
        verify(auditService).record(anyString(), eq(AuditService.TARGET_ANNOUNCEMENT), anyString(), anyString());
    }

    @Test
    void categoriesExposedInOrder() {
        java.util.Map<String, String> labels = service.categoryLabels();
        assertEquals(4, labels.size());
        assertEquals("通知", labels.get("NOTICE"));
        assertTrue(Collections.unmodifiableSet(labels.keySet()).containsAll(java.util.Arrays.asList(
                "NOTICE", "NEWS", "ACTIVITY", "OTHER")));
    }
}
