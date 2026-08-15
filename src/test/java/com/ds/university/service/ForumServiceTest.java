package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.common.PageResult;
import com.ds.university.entity.ForumCategory;
import com.ds.university.entity.ForumPost;
import com.ds.university.entity.ForumPostHistory;
import com.ds.university.entity.ForumReply;
import com.ds.university.entity.SysUser;
import com.ds.university.mapper.ForumMapper;
import com.ds.university.mapper.SysUserMapper;
import com.ds.university.vo.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 论坛业务单元测试：发帖/编辑/回复/点赞/置顶/加精/删除/板块管理/@提及渲染/权限校验。
 * 使用 Mockito 隔离 Mapper，不依赖数据库。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ForumServiceTest {

    @Mock
    private ForumMapper forumMapper;
    @Mock
    private ChatService chatService;
    @Mock
    private AuditService auditService;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private NotificationService notificationService;

    private ForumService service;

    private final LoginUser zhang = user("zhang", "STUDENT");
    private final LoginUser admin = user("admin", "ADMIN");

    private LoginUser user(String id, String type) {
        LoginUser u = new LoginUser();
        u.setUserId(id);
        u.setUserType(type);
        u.setRoles(new ArrayList<>(Arrays.asList(type)));
        return u;
    }

    private ForumCategory category(int id, String name) {
        ForumCategory c = new ForumCategory();
        c.setId(id);
        c.setName(name);
        c.setEnabled(1);
        return c;
    }

    private ForumPost post(long id, String title, String content, String author) {
        ForumPost p = new ForumPost();
        p.setId(id);
        p.setTitle(title);
        p.setContent(content);
        p.setCategoryId(1);
        p.setAuthorUser(author);
        p.setAuthorName(author);
        p.setReplyCount(0);
        p.setPinned(0);
        p.setFeatured(0);
        p.setLikeCount(0);
        return p;
    }

    @BeforeEach
    void setUp() {
        service = new ForumService(forumMapper, chatService, auditService, sysUserMapper, notificationService);
        when(chatService.displayName(anyString())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ========== 板块 ==========

    @Test
    void categoriesReturnsEnabledOnly() {
        when(forumMapper.selectEnabledCategories()).thenReturn(Arrays.asList(category(1, "学习交流")));
        assertEquals(1, service.categories().size());
        assertEquals("学习交流", service.categories().get(0).getName());
    }

    @Test
    void createCategoryRejectsDuplicateName() {
        when(forumMapper.insertCategory(any())).thenThrow(new DuplicateKeyException("dup"));
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.createCategory(admin, "学习交流", 1));
        assertEquals("板块名称已存在", e.getMessage());
    }

    @Test
    void createCategoryRequiresAdminAndAudits() {
        assertThrows(BusinessException.class, () -> service.createCategory(zhang, "新板块", 1));
        when(forumMapper.insertCategory(any())).thenReturn(1);
        service.createCategory(admin, "新板块", 1);
        verify(auditService).record(anyString(), eq(AuditService.TARGET_FORUM), anyString(), anyString());
    }

    @Test
    void renameCategoryRequiresAdmin() {
        assertThrows(BusinessException.class, () -> service.renameCategory(zhang, 1, "改名"));
        when(forumMapper.selectCategoryById(1)).thenReturn(category(1, "旧名"));
        service.renameCategory(admin, 1, "新名");
        verify(forumMapper).renameCategory(1, "新名");
    }

    @Test
    void toggleCategoryRequiresAdmin() {
        assertThrows(BusinessException.class, () -> service.toggleCategoryEnabled(zhang, 1));
        when(forumMapper.selectCategoryById(1)).thenReturn(category(1, "板块"));
        boolean enabled = service.toggleCategoryEnabled(admin, 1);
        assertFalse(enabled, "原启用应切换为停用");
        verify(forumMapper).toggleCategoryEnabled(1, 0);
    }

    // ========== 发帖 ==========

    @Test
    void createPostValidatesTitleContentCategory() {
        assertThrows(BusinessException.class, () -> service.createPost(zhang, " ", "内容", 1));
        assertThrows(BusinessException.class, () -> service.createPost(zhang, "标题", " ", 1));
        when(forumMapper.selectEnabledCategories()).thenReturn(Arrays.asList(category(1, "学习交流")));
        assertThrows(BusinessException.class, () -> service.createPost(zhang, "标题", "内容", 999));
        when(forumMapper.selectEnabledCategories()).thenReturn(Arrays.asList(category(1, "学习交流")));
        service.createPost(zhang, "标题", "内容", 1);
        verify(forumMapper).insertPost(any(ForumPost.class));
    }

    @Test
    void createPostMentionsRealUserAndNotifies() {
        when(forumMapper.selectEnabledCategories()).thenReturn(Arrays.asList(category(1, "学习交流")));
        when(forumMapper.insertPost(any(ForumPost.class))).thenAnswer(inv -> {
            ForumPost p = inv.getArgument(0);
            p.setId(42L);
            return 1;
        });
        when(sysUserMapper.selectByUserId("10101")).thenReturn(new SysUser());
        service.createPost(zhang, "求助", "@10101 老师请解答", 1);
        verify(notificationService).notify(eq("10101"), eq("FORUM_MENTION"),
                eq("/forum/42"), anyString());
    }

    @Test
    void createPostIgnoresSelfMentionAndUnknownUser() {
        when(forumMapper.selectEnabledCategories()).thenReturn(Arrays.asList(category(1, "学习交流")));
        when(forumMapper.insertPost(any(ForumPost.class))).thenReturn(1);
        when(sysUserMapper.selectByUserId("nobody")).thenReturn(null);
        service.createPost(zhang, "测试", "@zhang 自提及 @nobody 不存在", 1);
        verify(notificationService, never()).notify(anyString(), anyString(), anyString(), anyString());
    }

    // ========== 分页与搜索 ==========

    @Test
    void pageClampsAndPassesFulltextFlag() {
        when(forumMapper.countPosts("索引", 1, true)).thenReturn(0L);
        PageResult<ForumPost> result = service.page("索引+", 1, 0, 0, "zhang");
        assertEquals(1, result.getPage());
        verify(forumMapper).selectPostPage("索引", 1, 0, PageResult.normalizeSize(0), "zhang", true);
    }

    @Test
    void pageUsesLikeFallbackForSingleChar() {
        when(forumMapper.countPosts("数", 1, false)).thenReturn(0L);
        service.page("数", 1, 1, 10, null);
        verify(forumMapper).selectPostPage("数", 1, 0, 10, null, false);
    }

    // ========== 编辑 ==========

    @Test
    void updatePostRequiresAuthorAndSnapshotsHistory() {
        when(forumMapper.selectPostById(1L, null)).thenReturn(post(1, "旧标题", "旧内容", "zhang"));
        when(forumMapper.selectEnabledCategories()).thenReturn(Arrays.asList(category(1, "学习交流")));
        when(forumMapper.updatePost(any())).thenReturn(1);
        service.updatePost(zhang, 1L, "新标题", "新内容", 1);
        ArgumentCaptor<ForumPostHistory> history = ArgumentCaptor.forClass(ForumPostHistory.class);
        verify(forumMapper).insertPostHistory(history.capture());
        assertEquals("旧标题", history.getValue().getTitle());
        assertEquals("旧内容", history.getValue().getContent());
        assertEquals("zhang", history.getValue().getEditedBy());
        verify(forumMapper).updatePost(any(ForumPost.class));
    }

    @Test
    void updatePostRejectsNonAuthor() {
        when(forumMapper.selectPostById(1L, null)).thenReturn(post(1, "标题", "内容", "katz"));
        assertThrows(BusinessException.class, () -> service.updatePost(zhang, 1L, "x", "y", 1));
        verify(forumMapper, never()).updatePost(any());
    }

    // ========== 回复 ==========

    @Test
    void createReplyRejectsMissingPostAndUpdatesCounters() {
        when(forumMapper.selectPostById(1L, null)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.createReply(zhang, 1L, "回复"));

        ForumPost p = post(1, "标题", "内容", "zhang");
        p.setReplyCount(2);
        when(forumMapper.selectPostById(1L, null)).thenReturn(p);
        when(forumMapper.insertReply(any())).thenAnswer(inv -> {
            ((ForumReply) inv.getArgument(0)).setId(9L);
            return 1;
        });
        service.createReply(zhang, 1L, "新回复");
        verify(forumMapper).updatePostActivity(eq(1L), eq(3), any());
    }

    // ========== 点赞 ==========

    @Test
    void toggleLikeSwitchesOnAndOff() {
        when(forumMapper.selectPostById(1L, null)).thenReturn(post(1, "标题", "内容", "zhang"));
        when(forumMapper.countLike(1L, "zhang")).thenReturn(0);
        assertTrue(service.toggleLike(zhang, 1L));
        verify(forumMapper).insertLike(1L, "zhang");
        verify(forumMapper).updateLikeCount(1L, 1);

        when(forumMapper.countLike(1L, "zhang")).thenReturn(1);
        assertFalse(service.toggleLike(zhang, 1L));
        verify(forumMapper).deleteLike(1L, "zhang");
        verify(forumMapper).updateLikeCount(1L, -1);
    }

    // ========== 置顶 / 加精 ==========

    @Test
    void pinAndFeatureRequireAdmin() {
        assertThrows(BusinessException.class, () -> service.togglePinned(zhang, 1L));
        assertThrows(BusinessException.class, () -> service.toggleFeatured(zhang, 1L));
    }

    @Test
    void adminPinsAndFeaturesWithAudit() {
        when(forumMapper.selectPostById(1L, null)).thenReturn(post(1, "标题", "内容", "zhang"));
        assertTrue(service.togglePinned(admin, 1L));
        verify(forumMapper).updatePinned(1L, 1);
        verify(auditService).record(anyString(), eq(AuditService.TARGET_FORUM), anyString(), anyString());
        assertTrue(service.toggleFeatured(admin, 1L));
        verify(forumMapper).updateFeatured(1L, 1);
    }

    // ========== 删除 ==========

    @Test
    void deletePostRequiresAuthorOrAdmin() {
        when(forumMapper.selectPostById(1L, null)).thenReturn(post(1, "标题", "内容", "katz"));
        assertThrows(BusinessException.class, () -> service.deletePost(zhang, 1L));
        verify(forumMapper, never()).deletePost(anyLong());

        service.deletePost(admin, 1L);
        verify(forumMapper).deletePost(1L);
        verify(auditService).record(anyString(), eq(AuditService.TARGET_FORUM), anyString(), anyString());
    }

    @Test
    void deleteReplyRequiresAuthorOrAdmin() {
        ForumReply reply = new ForumReply();
        reply.setId(5L);
        reply.setPostId(1L);
        reply.setAuthorUser("katz");
        when(forumMapper.selectReplyById(5L)).thenReturn(reply);
        assertThrows(BusinessException.class, () -> service.deleteReply(zhang, 5L));
        verify(forumMapper, never()).deleteReply(anyLong());

        service.deleteReply(admin, 5L);
        verify(forumMapper).deleteReply(5L);
    }

    // ========== @提及渲染 ==========

    @Test
    void renderContentEscapesHtmlAndRendersMention() {
        when(sysUserMapper.selectByUserId("10101")).thenReturn(new SysUser());
        String rendered = service.renderContent("<script>alert(1)</script> @10101 你好");
        assertFalse(rendered.contains("<script>"), "脚本标签必须被转义");
        assertTrue(rendered.contains("&lt;script&gt;"));
        assertTrue(rendered.contains("forum-mention"), "真实账号应渲染提及徽标");
        assertTrue(rendered.contains("10101"));
    }

    @Test
    void renderContentKeepsUnknownMentionAsText() {
        when(sysUserMapper.selectByUserId("nobody")).thenReturn(null);
        String rendered = service.renderContent("@nobody 你好");
        assertFalse(rendered.contains("forum-mention"), "不存在账号不应渲染徽标");
        assertTrue(rendered.contains("@nobody"));
    }

    @Test
    void likersFillsAdminNameFallback() {
        com.ds.university.vo.ForumLikeVO vo = new com.ds.university.vo.ForumLikeVO();
        vo.setUserId("admin");
        vo.setUserName(null);
        when(forumMapper.selectLikers(1L, 50)).thenReturn(new java.util.ArrayList<>(java.util.Collections.singletonList(vo)));
        List<com.ds.university.vo.ForumLikeVO> likers = service.likers(1L);
        assertEquals("管理员", likers.get(0).getUserName());
    }
}
