package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.entity.Announcement;
import com.ds.university.mapper.AnnouncementMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统公告：管理员发布/编辑/下线/删除，首页与公告页展示已发布公告。
 * <ul>
 *   <li>类型分类：NOTICE 通知 / NEWS 新闻动态 / ACTIVITY 活动 / OTHER 其他；</li>
 *   <li>定时发布：publish_time 为空立即发布，未来时间到时自动对外可见；</li>
 *   <li>到期下线：expire_time 到期后自动不可见（对外查询实时过滤，无需人工操作）。</li>
 * </ul>
 */
@Service
@Validated
public class AnnouncementService {

    /** 首页公告面板展示条数 */
    public static final int HOME_LIMIT = 6;
    /** 标题最大长度 */
    public static final int MAX_TITLE_LENGTH = 100;
    /** 内容最大长度 */
    public static final int MAX_CONTENT_LENGTH = 2000;

    /** 公告类型（展示顺序即下拉顺序） */
    public static final List<String> CATEGORIES = Arrays.asList("NOTICE", "NEWS", "ACTIVITY", "OTHER");

    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter TEXT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AnnouncementMapper announcementMapper;
    private final AuditService auditService;

    public AnnouncementService(AnnouncementMapper announcementMapper, AuditService auditService) {
        this.announcementMapper = announcementMapper;
        this.auditService = auditService;
    }

    /** 类型编码 → 中文名（管理端下拉与公开页筛选共用） */
    public Map<String, String> categoryLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("NOTICE", "通知");
        labels.put("NEWS", "新闻动态");
        labels.put("ACTIVITY", "活动");
        labels.put("OTHER", "其他");
        return labels;
    }

    /** 全部公告（管理端列表，含已下线/定时/过期），并计算展示状态 */
    public List<Announcement> listAll() {
        List<Announcement> list = announcementMapper.selectAll();
        list.forEach(this::decorate);
        return list;
    }

    /** 对外可见公告（可按时效自动过滤），limit 为 null 表示全部 */
    public List<Announcement> listPublished(String category, Integer limit) {
        return announcementMapper.selectPublished(category, limit);
    }

    /** 按 ID 取对外可见公告（详情页），不可见返回 null */
    public Announcement getPublishedById(Integer id) {
        return announcementMapper.selectPublishedById(id);
    }

    public Announcement getById(Integer id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement != null) {
            decorate(announcement);
        }
        return announcement;
    }

    /** 发布新公告（默认已发布；可指定类型/定时/到期） */
    public void create(String title, String content, String category, Integer pinned,
                       String publishTime, String expireTime) {
        Announcement announcement = new Announcement();
        announcement.setTitle(validateTitle(title));
        announcement.setContent(validateContent(content));
        announcement.setCategory(validateCategory(category));
        announcement.setPinned(normalizeFlag(pinned));
        LocalDateTime publish = parseTime(publishTime);
        LocalDateTime expire = parseTime(expireTime);
        validateTimes(publish, expire);
        announcement.setPublishTime(publish);
        announcement.setExpireTime(expire);
        announcementMapper.insert(announcement);
        auditService.record(AuditService.ACTION_CREATE, AuditService.TARGET_ANNOUNCEMENT,
                String.valueOf(announcement.getId()), "发布公告：" + announcement.getTitle());
    }

    /** 编辑公告（标题/内容/类型/置顶/定时/到期），不存在则报错 */
    public void update(Integer id, String title, String content, String category, Integer pinned,
                       String publishTime, String expireTime) {
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公告不存在或已被删除");
        }
        LocalDateTime publish = parseTime(publishTime);
        LocalDateTime expire = parseTime(expireTime);
        validateTimes(publish, expire);
        Announcement update = new Announcement();
        update.setId(id);
        update.setTitle(validateTitle(title));
        update.setContent(validateContent(content));
        update.setCategory(validateCategory(category));
        update.setPinned(normalizeFlag(pinned));
        update.setPublishTime(publish);
        update.setExpireTime(expire);
        announcementMapper.update(update);
        auditService.record(AuditService.ACTION_UPDATE, AuditService.TARGET_ANNOUNCEMENT,
                String.valueOf(id), "编辑公告：" + update.getTitle());
    }

    /**
     * 发布/下线切换，返回操作后是否对外可见。
     * 已到期但仍处于发布状态的公告：一次操作直接清除到期时间重新发布；
     * 重新发布（由下线恢复）时同样兜底清除已到期的到期时间，避免"发布"后仍不可见。
     */
    public boolean toggleEnabled(Integer id) {
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公告不存在或已被删除");
        }
        // 已到期但仍在发布状态：直接清除到期时间重新发布（无需先下线再发布）
        if (existing.getEnabled() != null && existing.getEnabled() == 1 && isExpiredNow(existing)) {
            announcementMapper.clearExpireTime(id);
            auditService.record(AuditService.ACTION_UPDATE, AuditService.TARGET_ANNOUNCEMENT,
                    String.valueOf(id), "重新发布（清除到期时间）：" + existing.getTitle());
            return true;
        }
        int enabled = (existing.getEnabled() != null && existing.getEnabled() == 1) ? 0 : 1;
        if (enabled == 1) {
            announcementMapper.clearExpireTime(id);
        }
        announcementMapper.updateEnabled(id, enabled);
        auditService.record(AuditService.ACTION_UPDATE, AuditService.TARGET_ANNOUNCEMENT,
                String.valueOf(id), (enabled == 1 ? "发布公告：" : "下线公告：") + existing.getTitle());
        return enabled == 1;
    }

    /** 删除公告，不存在则忽略 */
    public void delete(Integer id) {
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            return;
        }
        announcementMapper.delete(id);
        auditService.record(AuditService.ACTION_DELETE, AuditService.TARGET_ANNOUNCEMENT,
                String.valueOf(id), "删除公告：" + existing.getTitle());
    }

    // ==================== 内部方法 ====================

    /** 计算展示状态（已发布/定时发布/已过期/已下线）并填充表单输入值 */
    private void decorate(Announcement a) {
        if (a.getEnabled() == null || a.getEnabled() != 1) {
            a.setStatusText(Announcement.STATUS_OFFLINE);
        } else if (a.getPublishTime() != null && a.getPublishTime().isAfter(LocalDateTime.now())) {
            a.setStatusText(Announcement.STATUS_SCHEDULED);
        } else if (isExpiredNow(a)) {
            a.setStatusText(Announcement.STATUS_EXPIRED);
        } else {
            a.setStatusText(Announcement.STATUS_PUBLISHED);
        }
        if (a.getPublishTime() != null) {
            a.setPublishTimeInput(a.getPublishTime().format(INPUT_FMT));
            a.setPublishTimeText(a.getPublishTime().format(TEXT_FMT));
        }
        if (a.getExpireTime() != null) {
            a.setExpireTimeInput(a.getExpireTime().format(INPUT_FMT));
            a.setExpireTimeText(a.getExpireTime().format(TEXT_FMT));
        }
    }

    private boolean isExpiredNow(Announcement a) {
        return a.getExpireTime() != null && !a.getExpireTime().isAfter(LocalDateTime.now());
    }

    private String validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公告标题不能为空");
        }
        String trimmed = title.trim();
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公告标题不能超过 " + MAX_TITLE_LENGTH + " 字");
        }
        return trimmed;
    }

    private String validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公告内容不能为空");
        }
        String trimmed = content.trim();
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公告内容不能超过 " + MAX_CONTENT_LENGTH + " 字");
        }
        return trimmed;
    }

    private String validateCategory(String category) {
        if (category == null || !CATEGORIES.contains(category)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公告类型不正确");
        }
        return category;
    }

    private int normalizeFlag(Integer flag) {
        return flag != null && flag == 1 ? 1 : 0;
    }

    /** 解析表单时间（datetime-local 的 ISO 格式或空格格式），空值返回 null */
    private LocalDateTime parseTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String t = text.trim();
        try {
            return LocalDateTime.parse(t, INPUT_FMT);
        } catch (DateTimeParseException ignored) {
            // 尝试空格分隔格式
        }
        try {
            return LocalDateTime.parse(t, TEXT_FMT);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "时间格式不正确，请使用日期时间选择器选择");
        }
    }

    private void validateTimes(LocalDateTime publish, LocalDateTime expire) {
        if (publish != null && expire != null && !expire.isAfter(publish)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "到期时间必须晚于发布时间");
        }
        if (expire != null && !expire.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "到期时间不能早于当前时间");
        }
    }
}
