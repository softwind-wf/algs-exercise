package com.ds.university.config;

import com.ds.university.vo.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 在线用户追踪单元测试：登录注册、会话销毁注销、计数与明细（内存存储）。
 */
class OnlineUserTrackerTest {

    private OnlineUserTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new OnlineUserTracker(new InMemoryOnlineUserStore(), new ObjectMapper());
    }

    private HttpSession session(String id) {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn(id);
        return session;
    }

    private LoginUser user(String id) {
        LoginUser u = new LoginUser();
        u.setUserId(id);
        u.setUserType("STUDENT");
        u.setRefId("00128");
        return u;
    }

    @Test
    void loginRegistersAndDestroyRemoves() {
        HttpSession s1 = session("s1");
        tracker.sessionCreated();
        tracker.login(s1, user("zhang"));
        assertEquals(1, tracker.onlineCount());
        assertEquals("zhang", tracker.onlineUsers().get(0).getUserId());

        tracker.sessionDestroyed(s1);
        assertEquals(0, tracker.onlineCount());
        assertEquals(0, tracker.totalSessionCount());
    }

    @Test
    void multipleUsersCountedIndependently() {
        tracker.login(session("a"), user("zhang"));
        tracker.login(session("b"), user("10101"));
        assertEquals(2, tracker.onlineCount());
        tracker.sessionDestroyed(session("a"));
        assertEquals(1, tracker.onlineCount());
    }

    @Test
    void anonymousSessionDoesNotInflateOnlineCount() {
        tracker.sessionCreated();
        tracker.sessionCreated();
        assertEquals(2, tracker.totalSessionCount());
        assertEquals(0, tracker.onlineCount());
    }
}
