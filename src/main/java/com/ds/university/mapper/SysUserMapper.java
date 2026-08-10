package com.ds.university.mapper;

import com.ds.university.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 系统账号（RBAC） */
@Mapper
public interface SysUserMapper {

    SysUser selectByUserId(@Param("userId") String userId);

    /** 账号的角色集合 */
    List<String> selectRoleIds(@Param("userId") String userId);

    /** 账号的权限集合 */
    List<String> selectPermissions(@Param("userId") String userId);
}