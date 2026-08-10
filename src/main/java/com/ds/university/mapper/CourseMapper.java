package com.ds.university.mapper;

import com.ds.university.entity.Course;
import com.ds.university.vo.CourseStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 课程 */
@Mapper
public interface CourseMapper {

    /** 课程列表（按系/关键字筛选，均为可选参数） */
    List<Course> selectAll(@Param("deptName") String deptName, @Param("keyword") String keyword);

    /** 课程分页列表（筛选条件与 selectAll 一致） */
    List<Course> selectPage(@Param("deptName") String deptName,
                            @Param("keyword") String keyword,
                            @Param("offset") int offset,
                            @Param("limit") int limit);

    /** 课程总数（按系/关键字筛选） */
    long countByFilter(@Param("deptName") String deptName, @Param("keyword") String keyword);

    Course selectById(@Param("courseId") String courseId);

    /** 某课程的先修课程 */
    List<Course> selectPrereqs(@Param("courseId") String courseId);

    /** 某课程的后续课程（被作为先修） */
    List<Course> selectDependents(@Param("courseId") String courseId);

    /** 热门课程（按累计选课人数降序） */
    List<CourseStatVO> selectHotCourses(@Param("limit") int limit);

    int count();


    /** 新增课程 */
    int insert(Course course);

    /** 修改课程 */
    int update(Course course);

    /** 删除课程 */
    int delete(@Param("courseId") String courseId);
}