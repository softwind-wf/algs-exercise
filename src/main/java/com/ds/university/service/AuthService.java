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
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    public LoginUser login(String userId, String rawPassword) {
        SysUser user = sysUserMapper.selectByUserId(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (user.getEnabled() == null || user.getEnabled() != 1) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        List<String> roles = sysUserMapper.selectRoleIds(userId);
        List<String> permissions = sysUserMapper.selectPermissions(userId);

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setUserType(user.getUserType());
        loginUser.setRefId(user.getRefId());
        loginUser.setRoles(roles);
        loginUser.setPermissions(permissions);
        return loginUser;
    }
}