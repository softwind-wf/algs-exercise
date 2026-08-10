package com.ds.university.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 指导关系 */
@Mapper
public interface AdvisorMapper {

    @Select("SELECT COUNT(*) FROM advisor")
    int count();
}