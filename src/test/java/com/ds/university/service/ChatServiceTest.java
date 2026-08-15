package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.entity.ChatMessage;
import com.ds.university.mapper.ChatMessageMapper;
import com.ds.university.mapper.SysUserMapper;
import com.ds.university.vo.ChatUserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 站内聊天服务单元测试：发送校验、历史、清空会话、联系人搜索、显示名。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceTest {

    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private SysUserMapper sysUserMapper;

    private ChatService service;

    @BeforeEach
    void setUp() {
        service = new ChatService(chatMessageMapper, sysUserMapper);
        when(chatMessageMapper.selectChatUsers()).thenReturn(Collections.emptyList());
    }

    // ========== 发送 ==========

    @Test
    void sendRejectsEmptyContent() {
        assertThrows(BusinessException.class, () -> service.send("zhang", "Zhang", "10101", "  "));
        verify(chatMessageMapper, never()).insert(any());
    }

    @Test
    void sendRejectsSelfChat() {
        assertThrows(BusinessException.class, () -> service.send("zhang", "Zhang", "zhang", "你好"));
    }

    @Test
    void sendRejectsUnknownRecipient() {
        when(sysUserMapper.selectByUserId("nobody")).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.send("zhang", "Zhang", "nobody", "你好"));
    }

    @Test
    void sendRejectsOverlongContent() {
        when(sysUserMapper.selectByUserId("10101")).thenReturn(new com.ds.university.entity.SysUser());
        String longContent = new String(new char[ChatMessage.MAX_CONTENT_LENGTH + 1]).replace('\0', '啊');
        assertThrows(BusinessException.class, () -> service.send("zhang", "Zhang", "10101", longContent));
    }

    @Test
    void sendPersistsMessage() {
        when(sysUserMapper.selectByUserId("10101")).thenReturn(new com.ds.university.entity.SysUser());
        service.send("zhang", "Zhang", "10101", "你好老师");
        verify(chatMessageMapper).insert(any(ChatMessage.class));
    }

    // ========== 历史 / 清空 ==========

    @Test
    void historyReversesAndMarksRead() {
        ChatMessage m1 = new ChatMessage();
        ChatMessage m2 = new ChatMessage();
        when(chatMessageMapper.selectHistory("zhang", "10101", 100))
                .thenReturn(Arrays.asList(m2, m1));   // 倒序返回
        java.util.List<ChatMessage> history = service.history("zhang", "10101", 100);        assertEquals(m1, history.get(0), "应反转为时间正序");
        verify(chatMessageMapper).markRead("10101", "zhang");
    }

    @Test
    void clearConversationValidates() {
        assertThrows(BusinessException.class, () -> service.clearConversation("zhang", null));
        assertThrows(BusinessException.class, () -> service.clearConversation("zhang", "zhang"));
        service.clearConversation("zhang", "10101");
        verify(chatMessageMapper).deleteConversation("zhang", "10101");
    }

    // ========== 联系人搜索 ==========

    @Test
    void searchUsersSanitizesKeywordAndPassesFulltextFlag() {
        when(chatMessageMapper.searchChatUsers("Srinivasan", "", "", 20, true))
                .thenReturn(Collections.emptyList());
        service.searchUsers("zhang", "Srinivasan", null, null, 20);
        verify(chatMessageMapper).searchChatUsers("Srinivasan", "", "", 20, true);
    }

    @Test
    void searchUsersExcludesSelf() {
        ChatUserVO me = new ChatUserVO();
        me.setUserId("zhang");
        ChatUserVO other = new ChatUserVO();
        other.setUserId("10101");
        when(chatMessageMapper.searchChatUsers("zh", "", "", 20, true))
                .thenReturn(new java.util.ArrayList<>(Arrays.asList(me, other)));
        java.util.List<ChatUserVO> users = service.searchUsers("zhang", "zh", null, null, 20);
        assertEquals(1, users.size());
        assertEquals("10101", users.get(0).getUserId());
    }

    @Test
    void searchUsersUsesLikeFallbackForSingleChar() {
        when(chatMessageMapper.searchChatUsers("张", "", "", 20, false))
                .thenReturn(Collections.emptyList());
        service.searchUsers("zhang", "张", null, null, 20);
        verify(chatMessageMapper).searchChatUsers("张", "", "", 20, false);
    }

    // ========== 显示名 ==========

    @Test
    void displayNameResolvesFromChatUsers() {
        ChatUserVO vo = new ChatUserVO();
        vo.setUserId("10101");
        vo.setUserName("Srinivasan");
        when(chatMessageMapper.selectChatUsers()).thenReturn(Collections.singletonList(vo));
        assertEquals("Srinivasan", service.displayName("10101"));
        assertEquals("管理员", service.displayName("admin"));
    }
}
