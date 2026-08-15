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
import com.ds.university.entity.ChatMessage;
import com.ds.university.mapper.ChatMessageMapper;
import com.ds.university.mapper.SysUserMapper;
import com.ds.university.vo.ChatConversationVO;
import com.ds.university.vo.ChatDeptVO;
import com.ds.university.vo.ChatGroupVO;
import com.ds.university.vo.ChatUserVO;
import com.ds.university.util.SearchText;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 站内聊天服务：一对一消息的发送、历史、未读与会话摘要。
 * 消息持久化到 chat_message 表；实时投递由 ChatWebSocketHandler 负责（在线即推，不在线留作未读）。
 * 联系人：关键字/院系/角色综合查询 + 基于选课关系的智能分组（我的同学/我的老师/我的学生/我的同事）。
 */
@Service
public class ChatService {

    /** 历史消息默认条数 */
    public static final int HISTORY_LIMIT = 100;
    /** 智能分组每组最多返回人数 */
    private static final int GROUP_LIMIT = 100;
    /** 综合查询最多返回人数 */
    private static final int SEARCH_LIMIT = 20;

    private final ChatMessageMapper chatMessageMapper;
    private final SysUserMapper sysUserMapper;

    public ChatService(ChatMessageMapper chatMessageMapper, SysUserMapper sysUserMapper) {
        this.chatMessageMapper = chatMessageMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /** 发送一条消息（仅落库与校验，实时投递由 WebSocket 处理器完成） */
    public ChatMessage send(String fromUserId, String fromName, String toUserId, String content) {
        if (fromUserId == null || fromUserId.isEmpty() || fromName == null || fromName.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "发送方信息不完整");
        }
        if (toUserId == null || toUserId.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择聊天对象");
        }
        if (toUserId.equals(fromUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能给自己发消息");
        }
        if (sysUserMapper.selectByUserId(toUserId) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "聊天对象不存在");
        }
        String safeContent = validateContent(content);
        ChatMessage message = new ChatMessage();
        message.setFromUser(fromUserId);
        message.setFromName(fromName);
        message.setToUser(toUserId);
        message.setToName(displayName(toUserId));
        message.setContent(safeContent);
        chatMessageMapper.insert(message);
        return message;
    }

    /** 两人之间的历史消息（时间正序，最近 limit 条），并顺手把对方发来的未读标记为已读 */
    public List<ChatMessage> history(String me, String other, int limit) {
        List<ChatMessage> messages = chatMessageMapper.selectHistory(me, other,
                limit <= 0 || limit > 500 ? HISTORY_LIMIT : limit);
        Collections.reverse(messages);
        chatMessageMapper.markRead(other, me);
        return messages;
    }

    /** 把 other → me 的未读消息标记为已读 */
    public void markRead(String me, String other) {
        chatMessageMapper.markRead(other, me);
    }

    /** 清空与某人的全部聊天记录（双向删除），返回删除条数 */
    public int clearConversation(String me, String other) {
        if (other == null || other.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "缺少聊天对象");
        }
        if (other.equals(me)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数错误");
        }
        return chatMessageMapper.deleteConversation(me, other);
    }

    /** 会话摘要（按最近消息倒序），填充对方显示名 */
    public List<ChatConversationVO> conversations(String me) {
        List<ChatConversationVO> list = chatMessageMapper.selectConversations(me);
        for (ChatConversationVO vo : list) {
            String partner = vo.getPartnerId();
            vo.setPartnerName(displayName(partner));
            // 若通讯录查不到（如管理员），退化为消息中冗余的显示名
            if (vo.getPartnerName() == null) {
                vo.setPartnerName(partner);
            }
        }
        return list;
    }

    /** 通讯录（学生+教师，排除自己） */
    public List<ChatUserVO> chatUsers(String me) {
        List<ChatUserVO> users = chatMessageMapper.selectChatUsers();
        users.removeIf(u -> u.getUserId().equals(me));
        return users;
    }

    /** 综合查询：关键字（姓名/账号）/院系/角色均可选，排除自己，服务端限流；关键字 >= 2 字走全文索引 */
    public List<ChatUserVO> searchUsers(String me, String keyword, String dept, String role, int limit) {
        String safeKeyword = SearchText.sanitizeForBoolean(keyword);
        boolean fulltext = SearchText.useFulltext(safeKeyword);
        String safeDept = dept == null ? "" : dept.trim();
        if (safeDept.length() > 30) {
            safeDept = safeDept.substring(0, 30);
        }
        String safeRole = role == null ? "" : role.trim();
        if (!"STUDENT".equals(safeRole) && !"INSTRUCTOR".equals(safeRole)) {
            safeRole = "";
        }
        int safeLimit = limit <= 0 ? SEARCH_LIMIT : Math.min(limit, 50);
        List<ChatUserVO> users = chatMessageMapper.searchChatUsers(safeKeyword, safeDept, safeRole, safeLimit, fulltext);
        users.removeIf(u -> u.getUserId().equals(me));
        return users;
    }

    /**
     * 智能联系人分组（基于选课/授课关系的真实社交圈）：
     * 学生 → 我的同学 / 我的老师；教师 → 我的学生 / 我的同事。
     */
    public List<ChatGroupVO> myContacts(String me, String userType, String refId) {
        List<ChatGroupVO> groups = new ArrayList<>();
        if (refId == null || refId.isEmpty()) {
            return groups;
        }
        if ("STUDENT".equals(userType)) {
            groups.add(new ChatGroupVO("我的同学",
                    chatMessageMapper.selectClassmates(refId, GROUP_LIMIT)));
            groups.add(new ChatGroupVO("我的老师",
                    chatMessageMapper.selectMyTeachers(refId, GROUP_LIMIT)));
        } else if ("INSTRUCTOR".equals(userType)) {
            groups.add(new ChatGroupVO("我的学生",
                    chatMessageMapper.selectMyStudents(refId, GROUP_LIMIT)));
            groups.add(new ChatGroupVO("我的同事",
                    chatMessageMapper.selectColleagues(refId, GROUP_LIMIT)));
        }
        // 自我过滤（理论上不会出现，防御性处理）
        for (ChatGroupVO group : groups) {
            group.getUsers().removeIf(u -> u.getUserId().equals(me));
        }
        return groups;
    }

    /** 院系列表（含师生人数），供"按院系浏览" */
    public List<ChatDeptVO> departments() {
        return chatMessageMapper.selectDepartments();
    }

    /** 显示名：优先按账号类型取学生/教师姓名，未知账号退回登录名（10 分钟缓存，避免每条消息全表扫） */
    @Cacheable(cacheNames = "displayName", key = "#userId")
    public String displayName(String userId) {
        if (userId == null) {
            return null;
        }
        for (ChatUserVO u : chatMessageMapper.selectChatUsers()) {
            if (u.getUserId().equals(userId)) {
                return u.getUserName() == null ? userId : u.getUserName();
            }
        }
        return "管理员";
    }

    private String validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "消息内容不能为空");
        }
        String trimmed = content.trim();
        if (trimmed.length() > ChatMessage.MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "消息内容不能超过 " + ChatMessage.MAX_CONTENT_LENGTH + " 字");
        }
        return trimmed;
    }
}
