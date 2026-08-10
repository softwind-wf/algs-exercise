package com.ds.university.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 选课 */
@Mapper
public interface TakesMapper {

    @Select("SELECT COUNT(*) FROM takes")
    int count();
}