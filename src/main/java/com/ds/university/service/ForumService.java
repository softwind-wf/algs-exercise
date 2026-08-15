/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.common.PageResult;
import com.ds.university.entity.ForumCategory;
import com.ds.university.entity.ForumPost;
import com.ds.university.entity.ForumPostHistory;
import com.ds.university.entity.ForumReply;
import com.ds.university.entity.UserNotification;
import com.ds.university.mapper.ForumMapper;
import com.ds.university.mapper.SysUserMapper;
import com.ds.university.vo.ForumLikeVO;
import com.ds.university.vo.LoginUser;
import com.ds.university.util.SearchText;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 学习论坛：学生/教师发帖与回复；作者可编辑/删除自己的内容，管理员可置顶/加精/删除/维护板块。
 * 帖子列表按板块筛选、置顶优先、活跃度排序；点赞为登录用户可切换（支持点赞人列表）；
 * 编辑保存历史快照；正文与回复支持 @账号 提及（服务端转义渲染 + 站内通知被提及人）。
 */
@Service
public class ForumService {

    /** 回复列表上限 */
    private static final int REPLY_LIMIT = 1000;
    /** 点赞人列表上限 */
    private static final int LIKER_LIMIT = 50;
    /** 编辑历史上限 */
    private static final int HISTORY_LIMIT = 50;
    /** @提及匹配：@字母数字下划线（1~20 位），与登录账号格式一致 */
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9_]{1,20})");

    private final ForumMapper forumMapper;
    private final ChatService chatService;
    private final AuditService auditService;
    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;

    public ForumService(ForumMapper forumMapper, ChatService chatService,
                        AuditService auditService, SysUserMapper sysUserMapper,
                        NotificationService notificationService) {
        this.forumMapper = forumMapper;
        this.chatService = chatService;
        this.auditService = auditService;
        this.sysUserMapper = sysUserMapper;
        this.notificationService = notificationService;
    }

    // ==================== 板块（管理员维护） ====================

    /** 启用的板块（发帖/筛选可选；5 分钟缓存，管理员变更时主动失效） */
    @Cacheable(cacheNames = "forumCategories", key = "'enabled'")
    public List<ForumCategory> categories() {
        return forumMapper.selectEnabledCategories();
    }

    /** 全部板块（管理端，含停用；与启用列表同缓存，变更时一并失效） */
    @Cacheable(cacheNames = "forumCategories", key = "'all'")
    public List<ForumCategory> allCategories() {
        return forumMapper.selectAllCategories();
    }

    /** 新增板块（仅管理员），返回新板块 */
    @CacheEvict(cacheNames = "forumCategories", allEntries = true)
    public ForumCategory createCategory(LoginUser user, String name, Integer sortOrder) {
        requireAdmin(user);
        ForumCategory category = new ForumCategory();
        category.setName(validateCategoryName(name));
        category.setSortOrder(sortOrder == null ? 0 : sortOrder);
        try {
            forumMapper.insertCategory(category);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "板块名称已存在");
        }
        auditService.record(AuditService.ACTION_CREATE, AuditService.TARGET_FORUM,
                "CATEGORY_" + category.getId(), "新增板块：" + category.getName());
        return category;
    }

    /** 重命名板块（仅管理员） */
    @CacheEvict(cacheNames = "forumCategories", allEntries = true)
    public void renameCategory(LoginUser user, Integer id, String name) {
        requireAdmin(user);
        ForumCategory existing = forumMapper.selectCategoryById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "板块不存在");
        }
        try {
            forumMapper.renameCategory(id, validateCategoryName(name));
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "板块名称已存在");
        }
        auditService.record(AuditService.ACTION_UPDATE, AuditService.TARGET_FORUM,
                "CATEGORY_" + id, "重命名板块：" + existing.getName() + " → " + name.trim());
    }

    /** 启用/停用板块（仅管理员）；停用后新帖不可选，存量帖保留展示 */
    @CacheEvict(cacheNames = "forumCategories", allEntries = true)
    public boolean toggleCategoryEnabled(LoginUser user, Integer id) {
        requireAdmin(user);
        ForumCategory existing = forumMapper.selectCategoryById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "板块不存在");
        }
        int enabled = (existing.getEnabled() != null && existing.getEnabled() == 1) ? 0 : 1;
        forumMapper.toggleCategoryEnabled(id, enabled);
        auditService.record(AuditService.ACTION_UPDATE, AuditService.TARGET_FORUM,
                "CATEGORY_" + id, (enabled == 1 ? "启用板块：" : "停用板块：") + existing.getName());
        return enabled == 1;
    }

    // ==================== 帖子 ====================

    /** 帖子分页（关键字 + 板块筛选），me 非空时计算点赞状态；关键字 >= 2 字走全文索引 */
    public PageResult<ForumPost> page(String keyword, Integer categoryId, int page, int size, String me) {
        String safeKeyword = SearchText.sanitizeForBoolean(trimLimit(keyword, 30));
        boolean fulltext = SearchText.useFulltext(safeKeyword);
        int safeSize = PageResult.normalizeSize(size);
        long total = forumMapper.countPosts(safeKeyword, categoryId, fulltext);
        int safePage = PageResult.clampPage(page, safeSize, total);
        List<ForumPost> records = forumMapper.selectPostPage(safeKeyword, categoryId,
                (safePage - 1) * safeSize, safeSize, me, fulltext);
        return new PageResult<>(records, safePage, safeSize, total);
    }

    /** 帖子详情（含点赞状态与 @提及渲染），不存在则报错 */
    public ForumPost getPost(Long id, String me) {
        ForumPost post = forumMapper.selectPostById(id, me);
        if (post == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "帖子不存在或已被删除");
        }
        post.setRenderedContent(renderContent(post.getContent()));
        return post;
    }

    /** 某帖子的全部回复（时间正序，含 @提及渲染） */
    public List<ForumReply> replies(Long postId) {
        List<ForumReply> list = forumMapper.selectRepliesByPost(postId, REPLY_LIMIT);
        for (ForumReply reply : list) {
            reply.setRenderedContent(renderContent(reply.getContent()));
        }
        return list;
    }

    /** 发帖（含 @提及通知） */
    public void createPost(LoginUser user, String title, String content, Integer categoryId) {
        ForumPost post = new ForumPost();
        post.setTitle(validateTitle(title));
        post.setContent(validateContent(content, ForumPost.MAX_CONTENT_LENGTH, "正文"));
        post.setCategoryId(requireEnabledCategory(categoryId).getId());
        post.setAuthorUser(user.getUserId());
        post.setAuthorName(displayName(user.getUserId()));
        forumMapper.insertPost(post);
        notifyMentions(user.getUserId(), content, "/forum/" + post.getId(),
                displayName(user.getUserId()) + " 在帖子《" + post.getTitle() + "》中提到了你");
    }

    /** 编辑帖子（仅作者）：保存编辑前快照，更新标题/正文/板块 */
    public void updatePost(LoginUser user, Long id, String title, String content, Integer categoryId) {
        ForumPost post = mustGet(id);
        checkModerator(user, post.getAuthorUser());
        // 编辑前快照入历史
        ForumPostHistory history = new ForumPostHistory();
        history.setPostId(id);
        history.setTitle(post.getTitle());
        history.setContent(post.getContent());
        history.setCategoryId(post.getCategoryId());
        history.setEditedBy(user.getUserId());
        forumMapper.insertPostHistory(history);

        ForumPost update = new ForumPost();
        update.setId(id);
        update.setTitle(validateTitle(title));
        update.setContent(validateContent(content, ForumPost.MAX_CONTENT_LENGTH, "正文"));
        update.setCategoryId(requireEnabledCategory(categoryId).getId());
        forumMapper.updatePost(update);
    }

    /** 帖子编辑历史（倒序） */
    public List<ForumPostHistory> history(Long postId) {
        return forumMapper.selectPostHistory(postId, HISTORY_LIMIT);
    }

    /** 回复（并同步帖子的回复数与最后回复时间；含 @提及通知） */
    public void createReply(LoginUser user, Long postId, String content) {
        ForumPost post = forumMapper.selectPostById(postId, null);
        if (post == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "帖子不存在或已被删除");
        }
        ForumReply reply = new ForumReply();
        reply.setPostId(postId);
        reply.setContent(validateContent(content, ForumReply.MAX_CONTENT_LENGTH, "回复内容"));
        reply.setAuthorUser(user.getUserId());
        reply.setAuthorName(displayName(user.getUserId()));
        forumMapper.insertReply(reply);
        int replyCount = (post.getReplyCount() == null ? 0 : post.getReplyCount()) + 1;
        forumMapper.updatePostActivity(postId, replyCount, LocalDateTime.now());
        notifyMentions(user.getUserId(), content, "/forum/" + postId + "#reply-" + reply.getId(),
                displayName(user.getUserId()) + " 在帖子《" + post.getTitle() + "》的回复中提到了你");
    }

    /** 置顶/取消置顶（仅管理员） */
    public boolean togglePinned(LoginUser user, Long id) {
        requireAdmin(user);
        ForumPost post = mustGet(id);
        int pinned = (post.getPinned() != null && post.getPinned() == 1) ? 0 : 1;
        forumMapper.updatePinned(id, pinned);
        auditService.record(AuditService.ACTION_UPDATE, AuditService.TARGET_FORUM,
                String.valueOf(id), (pinned == 1 ? "置顶帖子：" : "取消置顶：") + post.getTitle());
        return pinned == 1;
    }

    /** 加精/取消加精（仅管理员） */
    public boolean toggleFeatured(LoginUser user, Long id) {
        requireAdmin(user);
        ForumPost post = mustGet(id);
        int featured = (post.getFeatured() != null && post.getFeatured() == 1) ? 0 : 1;
        forumMapper.updateFeatured(id, featured);
        auditService.record(AuditService.ACTION_UPDATE, AuditService.TARGET_FORUM,
                String.valueOf(id), (featured == 1 ? "加精帖子：" : "取消加精：") + post.getTitle());
        return featured == 1;
    }

    /** 点赞/取消点赞（登录用户可切换），返回操作后是否已赞 */
    public boolean toggleLike(LoginUser user, Long postId) {
        if (forumMapper.selectPostById(postId, null) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "帖子不存在或已被删除");
        }
        if (forumMapper.countLike(postId, user.getUserId()) > 0) {
            forumMapper.deleteLike(postId, user.getUserId());
            forumMapper.updateLikeCount(postId, -1);
            return false;
        }
        forumMapper.insertLike(postId, user.getUserId());
        forumMapper.updateLikeCount(postId, 1);
        return true;
    }

    /** 点赞人列表（时间正序），管理员显示名兜底 */
    public List<ForumLikeVO> likers(Long postId) {
        List<ForumLikeVO> list = forumMapper.selectLikers(postId, LIKER_LIMIT);
        for (ForumLikeVO vo : list) {
            if (vo.getUserName() == null || vo.getUserName().isEmpty()) {
                vo.setUserName("管理员");
            }
        }
        return list;
    }

    /** 删除帖子（作者或管理员），连带回复与历史（外键级联删除） */
    public void deletePost(LoginUser user, Long id) {
        ForumPost post = forumMapper.selectPostById(id, null);
        if (post == null) {
            return;
        }
        checkModerator(user, post.getAuthorUser());
        forumMapper.deletePost(id);
        auditService.record(AuditService.ACTION_DELETE, AuditService.TARGET_FORUM,
                String.valueOf(id), "删除帖子：" + post.getTitle() + "（作者 " + post.getAuthorUser() + "）");
    }

    /** 删除回复（回复作者或管理员） */
    public void deleteReply(LoginUser user, Long replyId) {
        ForumReply reply = forumMapper.selectReplyById(replyId);
        if (reply == null) {
            return;
        }
        checkModerator(user, reply.getAuthorUser());
        forumMapper.deleteReply(replyId);
        auditService.record(AuditService.ACTION_DELETE, AuditService.TARGET_FORUM,
                String.valueOf(replyId), "删除回复（帖子 " + reply.getPostId() + "，作者 " + reply.getAuthorUser() + "）");
    }

    /**
     * 渲染内容：先整体 HTML 转义（防 XSS），再把 @账号 替换为提及徽标（仅当账号存在），
     * 显示名同样转义。输出为安全 HTML，模板用 th:utext 输出。
     */
    public String renderContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        String escaped = HtmlUtils.htmlEscape(content);
        Matcher matcher = MENTION_PATTERN.matcher(escaped);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String userId = matcher.group(1);
            if (sysUserMapper.selectByUserId(userId) != null) {
                String name = HtmlUtils.htmlEscape(displayName(userId));
                matcher.appendReplacement(sb, Matcher.quoteReplacement(
                        "<span class=\"forum-mention\">@" + name + "</span>"));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    // ==================== 内部方法 ====================

    /** 扫描内容中的 @提及（去重、排除自己、仅真实账号），并发送站内通知 */
    private void notifyMentions(String authorUserId, String content, String sourceUrl, String summary) {
        if (content == null || content.isEmpty()) {
            return;
        }
        Set<String> mentioned = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String userId = matcher.group(1);
            if (!userId.equals(authorUserId) && sysUserMapper.selectByUserId(userId) != null) {
                mentioned.add(userId);
            }
        }
        for (String userId : mentioned) {
            notificationService.notify(userId, UserNotification.TYPE_FORUM_MENTION, sourceUrl, summary);
        }
    }

    private ForumCategory requireEnabledCategory(Integer categoryId) {
        if (categoryId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择板块");
        }
        for (ForumCategory category : forumMapper.selectEnabledCategories()) {
            if (category.getId().equals(categoryId)) {
                return category;
            }
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "板块不存在或已停用");
    }

    private String validateCategoryName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "板块名称不能为空");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 30) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "板块名称不能超过 30 字");
        }
        return trimmed;
    }

    private ForumPost mustGet(Long id) {
        ForumPost post = forumMapper.selectPostById(id, null);
        if (post == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "帖子不存在或已被删除");
        }
        return post;
    }

    /** 作者本人或管理员 */
    private void checkModerator(LoginUser user, String authorUser) {
        boolean isAdmin = user.getRoles() != null && user.getRoles().contains("ADMIN");
        if (!isAdmin && !user.getUserId().equals(authorUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "只能操作自己发布的内容");
        }
    }

    private void requireAdmin(LoginUser user) {
        if (user.getRoles() == null || !user.getRoles().contains("ADMIN")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "仅管理员可执行此操作");
        }
    }

    private String validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标题不能为空");
        }
        String trimmed = title.trim();
        if (trimmed.length() > ForumPost.MAX_TITLE_LENGTH) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "标题不能超过 " + ForumPost.MAX_TITLE_LENGTH + " 字");
        }
        return trimmed;
    }

    private String validateContent(String content, int maxLength, String label) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, label + "不能为空");
        }
        String trimmed = content.trim();
        if (trimmed.length() > maxLength) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, label + "不能超过 " + maxLength + " 字");
        }
        return trimmed;
    }

    private String displayName(String userId) {
        return chatService.displayName(userId);
    }

    private String trimLimit(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
