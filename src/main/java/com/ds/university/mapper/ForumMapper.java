package com.ds.university.mapper;

import com.ds.university.entity.ForumCategory;
import com.ds.university.entity.ForumPost;
import com.ds.university.entity.ForumPostHistory;
import com.ds.university.entity.ForumReply;
import com.ds.university.vo.ForumLikeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 学习论坛 */
@Mapper
public interface ForumMapper {

    // ---- 帖子 ----

    int insertPost(ForumPost post);

    /** 更新标题/正文/板块（作者编辑） */
    int updatePost(ForumPost post);

    /** 帖子总数（keyword/categoryId 为空表示不限制；fulltext=true 走全文索引） */
    long countPosts(@Param("keyword") String keyword,
                    @Param("categoryId") Integer categoryId,
                    @Param("fulltext") boolean fulltext);

    /** 帖子分页（置顶优先，其次按活跃度倒序）；me 非空时计算"我是否已点赞" */
    List<ForumPost> selectPostPage(@Param("keyword") String keyword,
                                   @Param("categoryId") Integer categoryId,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit,
                                   @Param("me") String me,
                                   @Param("fulltext") boolean fulltext);

    ForumPost selectPostById(@Param("id") Long id, @Param("me") String me);

    /** 回复后更新冗余计数与最后回复时间 */
    int updatePostActivity(@Param("id") Long id,
                           @Param("replyCount") int replyCount,
                           @Param("lastReplyTime") LocalDateTime lastReplyTime);

    /** 置顶/取消置顶（管理员） */
    int updatePinned(@Param("id") Long id, @Param("pinned") Integer pinned);

    /** 加精/取消加精（管理员） */
    int updateFeatured(@Param("id") Long id, @Param("featured") Integer featured);

    int deletePost(@Param("id") Long id);

    // ---- 点赞 ----

    int insertLike(@Param("postId") Long postId, @Param("userId") String userId);

    int deleteLike(@Param("postId") Long postId, @Param("userId") String userId);

    int countLike(@Param("postId") Long postId, @Param("userId") String userId);

    /** 点赞数增减（delta = 1 / -1） */
    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);

    /** 点赞人列表（时间正序），limit 为上限 */
    List<ForumLikeVO> selectLikers(@Param("postId") Long postId, @Param("limit") int limit);

    // ---- 编辑历史 ----

    int insertPostHistory(ForumPostHistory history);

    /** 某帖子的编辑历史（时间倒序，最近在前） */
    List<ForumPostHistory> selectPostHistory(@Param("postId") Long postId, @Param("limit") int limit);

    // ---- 板块（管理员维护） ----

    /** 启用的板块（对外可选） */
    List<ForumCategory> selectEnabledCategories();

    /** 全部板块（管理端，含停用） */
    List<ForumCategory> selectAllCategories();

    ForumCategory selectCategoryById(@Param("id") Integer id);

    int insertCategory(ForumCategory category);

    int renameCategory(@Param("id") Integer id, @Param("name") String name);

    int toggleCategoryEnabled(@Param("id") Integer id, @Param("enabled") Integer enabled);

    /** 该板块下帖子数（停用/删除时提示） */
    long countPostsByCategory(@Param("categoryId") Integer categoryId);

    // ---- 回复 ----

    int insertReply(ForumReply reply);

    /** 某帖子的全部回复（时间正序），limit 为上限 */
    List<ForumReply> selectRepliesByPost(@Param("postId") Long postId, @Param("limit") int limit);

    ForumReply selectReplyById(@Param("id") Long id);

    int deleteReply(@Param("id") Long id);
}
