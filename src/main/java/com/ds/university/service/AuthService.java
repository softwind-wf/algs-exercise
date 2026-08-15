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
import com.ds.university.entity.SysUser;
import com.ds.university.mapper.SysUserMapper;
import com.ds.university.vo.LoginUser;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 登录鉴权服务：校验 BCrypt 密码，加载角色与权限。
 */
@Service
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final LoginGuard loginGuard;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(SysUserMapper sysUserMapper, LoginGuard loginGuard) {
        this.sysUserMapper = sysUserMapper;
        this.loginGuard = loginGuard;
    }

    public LoginUser login(String userId, String rawPassword, String clientIp) {
        // 防爆破：IP 维度优先检查（拦截对全站账号各试几次的攻击），锁定期间直接拒绝，不做任何密码运算
        throwIfIpLocked(clientIp);
        throwIfLocked(userId);

        SysUser user = sysUserMapper.selectByUserId(userId);
        if (user == null) {
            failAndThrowIfLocked(userId, clientIp);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (user.getEnabled() == null || user.getEnabled() != 1) {
            failAndThrowIfLocked(userId, clientIp);
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            failAndThrowIfLocked(userId, clientIp);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        loginGuard.recordSuccess(userId);

        List<String> roles = sysUserMapper.selectRoleIds(userId);
        List<String> permissions = sysUserMapper.selectPermissions(userId);

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setUserType(user.getUserType());
        loginUser.setRefId(user.getRefId());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setRoles(roles);
        loginUser.setPermissions(permissions);
        return loginUser;
    }

    /** 记录账号与 IP 两个维度的失败；若本次失败触发了任一维度锁定，立即报锁定而不是普通失败 */
    private void failAndThrowIfLocked(String userId, String clientIp) {
        loginGuard.recordFailure(userId);
        if (clientIp != null && !clientIp.isEmpty()) {
            loginGuard.recordIpFailure(clientIp);
        }
        throwIfLocked(userId);
        throwIfIpLocked(clientIp);
    }

    private void throwIfLocked(String userId) {
        long lockedSeconds = loginGuard.lockRemainingSeconds(userId);
        if (lockedSeconds > 0) {
            long minutes = (lockedSeconds + 59) / 60;
            throw new BusinessException(ErrorCode.LOGIN_LOCKED,
                    "登录失败次数过多，请 " + minutes + " 分钟后再试");
        }
    }

    private void throwIfIpLocked(String clientIp) {
        if (clientIp == null || clientIp.isEmpty()) {
            return;
        }
        long lockedSeconds = loginGuard.ipLockRemainingSeconds(clientIp);
        if (lockedSeconds > 0) {
            long minutes = (lockedSeconds + 59) / 60;
            throw new BusinessException(ErrorCode.LOGIN_LOCKED,
                    "当前 IP 登录失败次数过多，请 " + minutes + " 分钟后再试");
        }
    }

    /** 修改密码：校验原密码后，加密保存新密码。 */
    public void changePassword(String userId, String oldPassword, String newPassword) {
        SysUser user = sysUserMapper.selectByUserId(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (oldPassword == null || oldPassword.isEmpty() || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_WRONG);
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 32) {
            throw new BusinessException(ErrorCode.PASSWORD_INVALID);
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码不能与原密码相同");
        }
        sysUserMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
    }
}