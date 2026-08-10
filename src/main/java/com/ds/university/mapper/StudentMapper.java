package com.ds.university.mapper;

import com.ds.university.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 学生 */
@Mapper
public interface StudentMapper {

    Student selectById(@Param("id") String id);

    /** 学生导师姓名 */
    String selectAdvisorName(@Param("id") String id);

    int count();
}