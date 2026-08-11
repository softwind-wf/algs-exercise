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

    /** 更新密码（BCrypt 哈希） */
    int updatePassword(@Param("userId") String userId, @Param("password") String password);

    /** 全部账号 */
    List<SysUser> selectAll();

    /** 某类型下已开户的业务主键集合 */
    List<String> selectRefIds(@Param("userType") String userType);

    /** 按业务主键查找账号 */
    SysUser selectByRefId(@Param("userType") String userType, @Param("refId") String refId);

    /** 新增账号 */
    int insert(SysUser user);

    /** 新增账号-角色关联 */
    int insertUserRole(@Param("userId") String userId, @Param("roleId") String roleId);

    /** 启用/禁用账号 */
    int updateEnabled(@Param("userId") String userId, @Param("enabled") Integer enabled);

    /** 账号总数 */
    long count();

    /** 删除账号-角色关联 */
    int deleteUserRoles(@Param("userId") String userId);

    /** 删除账号 */
    int deleteByUserId(@Param("userId") String userId);
}