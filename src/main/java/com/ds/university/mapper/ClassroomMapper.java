package com.ds.university.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 教室 */
@Mapper
public interface ClassroomMapper {

    @Select("SELECT COUNT(*) FROM classroom")
    int count();
}