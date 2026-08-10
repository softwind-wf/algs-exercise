package com.ds.university.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 权限点 */
@Mapper
public interface SysPermissionMapper {

    @Select("SELECT COUNT(*) FROM sys_permission")
    int count();
}