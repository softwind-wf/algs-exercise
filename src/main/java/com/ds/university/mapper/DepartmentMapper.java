package com.ds.university.mapper;

import com.ds.university.entity.Department;
import com.ds.university.vo.DepartmentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 系 */
@Mapper
public interface DepartmentMapper {

    /** 系列表（含教师数/课程数） */
    List<DepartmentVO> selectAllWithStats();

    /** 全部系（简单列表，用于下拉框） */
    List<Department> selectAllSimple();

    Department selectById(@Param("deptName") String deptName);

    int count();

    /** 系下的教师列表（按系名） */
    List<com.ds.university.entity.Instructor> selectInstructorsByDept(@Param("deptName") String deptName);

    /** 系下的课程列表（按系名） */
    List<com.ds.university.entity.Course> selectCoursesByDept(@Param("deptName") String deptName);
}