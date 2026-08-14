package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.common.PageResult;
import com.ds.university.entity.ForumPost;
import com.ds.university.entity.ForumReply;
import com.ds.university.mapper.ForumMapper;
import com.ds.university.mapper.SysUserMapper;
import com.ds.university.vo.LoginUser;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 学习论坛：学生/教师发帖与回复；作者可编辑/删除自己的内容，管理员可置顶/加精/删除。
 * 帖子列表按分类筛选、置顶优先、活跃度排序；点赞为登录用户可切换；
 * 正文与回复支持 @账号 提及渲染（服务端转义 + 解析显示名，防 XSS）。
 */
@Service
public class ForumService {

    /** 回复列表上限 */
    private static final int REPLY_LIMIT = 1000;
    /** @提及匹配：@字母数字下划线（1~20 位），与登录账号格式一致 */
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9_]{1,20})");

    private final ForumMapper forumMapper;
    private final ChatService chatService;
    private final AuditService auditService;
    private final SysUserMapper sysUserMapper;

    public ForumService(ForumMapper forumMapper, ChatService chatService,
                        AuditService auditService, SysUserMapper sysUserMapper) {
        this.forumMapper = forumMapper;
        this.chatService = chatService;
        this.auditService = auditService;
        this.sysUserMapper = sysUserMapper;
    }

    /** 板块分类：编码 → 名称 */
    public Map<String, String> categories() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("STUDY", "学习交流");
        map.put("COURSE", "课程答疑");
        map.put("CAMPUS", "校园生活");
        map.put("SHARE", "资源共享");
        map.put("SUGGEST", "意见建议");
        return map;
    }

    /** 帖子分页（支持标题/正文关键字搜索 + 分类筛选），me 非空时计算点赞状态 */
    public PageResult<ForumPost> page(String keyword, String category, int page, int size, String me) {
        String safeKeyword = trimLimit(keyword, 30);
        String safeCategory = validateCategoryOptional(category);
        int safeSize = PageResult.normalizeSize(size);
        long total = forumMapper.countPosts(safeKeyword, safeCategory);
        int safePage = PageResult.clampPage(page, safeSize, total);
        List<ForumPost> records = forumMapper.selectPostPage(safeKeyword, safeCategory,
                (safePage - 1) * safeSize, safeSize, me);
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

    /** 发帖 */
    public void createPost(LoginUser user, String title, String content, String category) {
        ForumPost post = new ForumPost();
        post.setTitle(validateTitle(title));
        post.setContent(validateContent(content, ForumPost.MAX_CONTENT_LENGTH, "正文"));
        post.setCategory(validateCategory(category));
        post.setAuthorUser(user.getUserId());
        post.setAuthorName(displayName(user.getUserId()));
        forumMapper.insertPost(post);
    }

    /** 编辑帖子（仅作者），标题/正文/分类 */
    public void updatePost(LoginUser user, Long id, String title, String content, String category) {
        ForumPost post = mustGet(id);
        checkModerator(user, post.getAuthorUser());
        ForumPost update = new ForumPost();
        update.setId(id);
        update.setTitle(validateTitle(title));
        update.setContent(validateContent(content, ForumPost.MAX_CONTENT_LENGTH, "正文"));
        update.setCategory(validateCategory(category));
        forumMapper.updatePost(update);
    }

    /** 回复（并同步帖子的回复数与最后回复时间） */
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
    }

    /** 置顶/取消置顶（仅管理员），返回操作后是否置顶 */
    public boolean togglePinned(LoginUser user, Long id) {
        requireAdmin(user);
        ForumPost post = mustGet(id);
        int pinned = (post.getPinned() != null && post.getPinned() == 1) ? 0 : 1;
        forumMapper.updatePinned(id, pinned);
        auditService.record(AuditService.ACTION_UPDATE, AuditService.TARGET_FORUM,
                String.valueOf(id), (pinned == 1 ? "置顶帖子：" : "取消置顶：") + post.getTitle());
        return pinned == 1;
    }

    /** 加精/取消加精（仅管理员），返回操作后是否加精 */
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

    /** 删除帖子（作者或管理员），连带回复（外键级联删除） */
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

    private String validateCategory(String category) {
        if (category == null || !categories().containsKey(category)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "板块分类不正确");
        }
        return category;
    }

    private String validateCategoryOptional(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "";
        }
        return categories().containsKey(category) ? category : "";
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
