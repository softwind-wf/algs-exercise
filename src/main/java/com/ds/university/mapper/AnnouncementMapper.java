package com.ds.university.mapper;

import com.ds.university.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 系统公告 */
@Mapper
public interface AnnouncementMapper {

    /** 全部公告（管理端，含已下线/定时/过期，置顶优先、新→旧） */
    List<Announcement> selectAll();

    /**
     * 对外可见公告：enabled=1 且（无发布时间或已到发布时间）且（无到期时间或未到期）。
     * category 为空表示全部类型；limit 为空表示不限制。
     */
    List<Announcement> selectPublished(@Param("category") String category, @Param("limit") Integer limit);

    /** 按 ID 取对外可见公告（详情页用），不存在或不可见返回 null */
    Announcement selectPublishedById(@Param("id") Integer id);

    /** 按 ID 取任意状态公告（管理端用） */
    Announcement selectById(@Param("id") Integer id);

    int insert(Announcement announcement);

    /** 更新标题/内容/类型/置顶/发布时间/到期时间 */
    int update(Announcement announcement);

    /** 发布/下线（enabled：1 发布 / 0 下线） */
    int updateEnabled(@Param("id") Integer id, @Param("enabled") Integer enabled);

    /** 清除到期时间（过期公告重新发布时使用） */
    int clearExpireTime(@Param("id") Integer id);

    int delete(@Param("id") Integer id);
}
