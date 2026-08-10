package com.ds.university.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 先修课 */
@Mapper
public interface PrereqMapper {

    @Select("SELECT COUNT(*) FROM prereq")
    int count();
}