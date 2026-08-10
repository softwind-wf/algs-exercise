package com.ds.university.mapper;

import com.ds.university.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 课程 */
@Mapper
public interface CourseMapper {

    /** 课程列表（按系/关键字筛选，均为可选参数） */
    List<Course> selectAll(@Param("deptName") String deptName, @Param("keyword") String keyword);

    Course selectById(@Param("courseId") String courseId);

    /** 某课程的先修课程 */
    List<Course> selectPrereqs(@Param("courseId") String courseId);

    /** 某课程的后续课程（被作为先修） */
    List<Course> selectDependents(@Param("courseId") String courseId);

    int count();
}