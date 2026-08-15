package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.entity.SysUser;
import com.ds.university.mapper.SysUserMapper;
import com.ds.university.vo.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录鉴权单元测试：登录成功/失败/禁用/锁定、修改密码校验。
 * 演示账号 zhang 的 BCrypt 哈希来自 V2 迁移初始数据（明文 password）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    private static final String BCRYPT_PASSWORD =
            "$2a$10$/Gu.uRug7LYoOu0PzCdVKOqo4Ayxt3fM2utBEet4jNQ5nouNqojKO";

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private LoginGuard loginGuard;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(sysUserMapper, loginGuard);
        when(loginGuard.lockRemainingSeconds(anyString())).thenReturn(0L);
        when(loginGuard.ipLockRemainingSeconds(anyString())).thenReturn(0L);
    }

    private SysUser user(int enabled) {
        SysUser user = new SysUser();
        user.setUserId("zhang");
        user.setPassword(BCRYPT_PASSWORD);
        user.setUserType("STUDENT");
        user.setRefId("00128");
        user.setEnabled(enabled);
        return user;
    }

    @Test
    void loginSuccessLoadsRolesPermissionsAndAvatar() {
        SysUser user = user(1);
        user.setAvatar("abc.png");
        when(sysUserMapper.selectByUserId("zhang")).thenReturn(user);
        when(sysUserMapper.selectRoleIds("zhang")).thenReturn(Arrays.asList("STUDENT"));
        when(sysUserMapper.selectPermissions("zhang")).thenReturn(Arrays.asList("course:view", "take:enroll"));

        LoginUser loginUser = service.login("zhang", "password", "127.0.0.1");
        assertEquals("zhang", loginUser.getUserId());
        assertEquals("abc.png", loginUser.getAvatar());
        assertTrue(loginUser.hasRole("STUDENT"));
        assertTrue(loginUser.hasPermission("take:enroll"));
        verify(loginGuard).recordSuccess("zhang");
    }

    @Test
    void loginRejectsWrongPassword() {
        when(sysUserMapper.selectByUserId("zhang")).thenReturn(user(1));
        assertThrows(BusinessException.class, () -> service.login("zhang", "wrong", "127.0.0.1"));
        verify(loginGuard).recordFailure("zhang");
    }

    @Test
    void loginRejectsDisabledAccount() {
        when(sysUserMapper.selectByUserId("zhang")).thenReturn(user(0));
        assertThrows(BusinessException.class, () -> service.login("zhang", "password", "127.0.0.1"));
    }

    @Test
    void loginRejectsUnknownUser() {
        when(sysUserMapper.selectByUserId("nobody")).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.login("nobody", "password", "127.0.0.1"));
    }

    @Test
    void loginRejectsLockedAccount() {
        when(loginGuard.lockRemainingSeconds("zhang")).thenReturn(300L);
        assertThrows(BusinessException.class, () -> service.login("zhang", "password", "127.0.0.1"));
    }

    // ========== 修改密码 ==========

    @Test
    void changePasswordRejectsWrongOldPassword() {
        when(sysUserMapper.selectByUserId("zhang")).thenReturn(user(1));
        assertThrows(BusinessException.class,
                () -> service.changePassword("zhang", "wrong", "newpass123"));
    }

    @Test
    void changePasswordRejectsWeakNewPassword() {
        when(sysUserMapper.selectByUserId("zhang")).thenReturn(user(1));
        assertThrows(BusinessException.class,
                () -> service.changePassword("zhang", "password", "123"));
    }

    @Test
    void changePasswordRejectsSameAsOld() {
        when(sysUserMapper.selectByUserId("zhang")).thenReturn(user(1));
        assertThrows(BusinessException.class,
                () -> service.changePassword("zhang", "password", "password"));
    }

    @Test
    void changePasswordSucceeds() {
        when(sysUserMapper.selectByUserId("zhang")).thenReturn(user(1));
        service.changePassword("zhang", "password", "newpass123");
        verify(sysUserMapper).updatePassword(anyString(), anyString());
    }
}
