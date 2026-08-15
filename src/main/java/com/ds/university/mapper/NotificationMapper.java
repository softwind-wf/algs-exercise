package com.ds.university.mapper;

import com.ds.university.entity.UserNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 站内通知 */
@Mapper
public interface NotificationMapper {

    int insert(UserNotification notification);

    /** 某用户的最近 limit 条通知（倒序） */
    List<UserNotification> selectByUser(@Param("userId") String userId, @Param("limit") int limit);

    /** 未读通知数（导航栏角标） */
    int countUnread(@Param("userId") String userId);

    /** 全部标记已读 */
    int markAllRead(@Param("userId") String userId);

    /** 单条标记已读（仅本人） */
    int markRead(@Param("id") Long id, @Param("userId") String userId);
}
