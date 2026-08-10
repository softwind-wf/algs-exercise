package com.ds.university.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 授课 */
@Mapper
public interface TeachesMapper {

    @Select("SELECT COUNT(*) FROM teaches")
    int count();
}