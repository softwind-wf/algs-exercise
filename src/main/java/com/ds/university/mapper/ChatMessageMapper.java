package com.ds.university.mapper;

import com.ds.university.entity.ChatMessage;
import com.ds.university.vo.ChatConversationVO;
import com.ds.university.vo.ChatDeptVO;
import com.ds.university.vo.ChatUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 站内聊天消息 */
@Mapper
public interface ChatMessageMapper {

    int insert(ChatMessage message);

    /** 两人之间的最近 limit 条消息（按 id 倒序取，调用方再反转为时间正序） */
    List<ChatMessage> selectHistory(@Param("userA") String userA,
                                    @Param("userB") String userB,
                                    @Param("limit") int limit);

    /** 将 fromUser → toUser 的未读消息全部标记为已读，返回影响行数 */
    int markRead(@Param("fromUser") String fromUser, @Param("toUser") String toUser);

    /** 清空两人之间的全部消息（双向删除），返回删除条数 */
    int deleteConversation(@Param("userA") String userA, @Param("userB") String userB);

    /** 某人的未读消息总数 */
    int countUnread(@Param("toUser") String toUser);

    /** 会话摘要列表（按最近消息倒序），含未读数与最近一条内容/时间 */
    List<ChatConversationVO> selectConversations(@Param("me") String me);

    /** 通讯录：有登录账号且启用的学生/教师（含显示名） */
    List<ChatUserVO> selectChatUsers();

    /** 综合查询：按关键字/院系/角色过滤（均可选），limit 为返回条数上限 */
    List<ChatUserVO> searchChatUsers(@Param("keyword") String keyword,
                                     @Param("dept") String dept,
                                     @Param("role") String role,
                                     @Param("limit") int limit);

    /** 我的同学：与我同开课班的学生（有账号） */
    List<ChatUserVO> selectClassmates(@Param("studentId") String studentId, @Param("limit") int limit);

    /** 我的老师：我选课的开课班授课教师（有账号） */
    List<ChatUserVO> selectMyTeachers(@Param("studentId") String studentId, @Param("limit") int limit);

    /** 我的学生：我授课开课班里的学生（有账号） */
    List<ChatUserVO> selectMyStudents(@Param("instructorId") String instructorId, @Param("limit") int limit);

    /** 我的同事：同院系的其他教师（有账号） */
    List<ChatUserVO> selectColleagues(@Param("instructorId") String instructorId, @Param("limit") int limit);

    /** 院系列表（含师生人数），供"按院系浏览" */
    List<ChatDeptVO> selectDepartments();
}
