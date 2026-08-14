package com.ds.university.mapper;

import com.ds.university.entity.ForumPost;
import com.ds.university.entity.ForumReply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 学习论坛 */
@Mapper
public interface ForumMapper {

    // ---- 帖子 ----

    int insertPost(ForumPost post);

    /** 更新标题/正文/分类（作者编辑） */
    int updatePost(ForumPost post);

    /** 帖子总数（keyword/category 为空表示不限制） */
    long countPosts(@Param("keyword") String keyword, @Param("category") String category);

    /** 帖子分页（置顶优先，其次按活跃度倒序）；me 非空时计算"我是否已点赞" */
    List<ForumPost> selectPostPage(@Param("keyword") String keyword,
                                   @Param("category") String category,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit,
                                   @Param("me") String me);

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

    // ---- 回复 ----

    int insertReply(ForumReply reply);

    /** 某帖子的全部回复（时间正序），limit 为上限 */
    List<ForumReply> selectRepliesByPost(@Param("postId") Long postId, @Param("limit") int limit);

    ForumReply selectReplyById(@Param("id") Long id);

    int deleteReply(@Param("id") Long id);
}
