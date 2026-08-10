package com.ds.university.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 角色 */
@Mapper
public interface SysRoleMapper {

    @Select("SELECT COUNT(*) FROM sys_role")
    int count();
}