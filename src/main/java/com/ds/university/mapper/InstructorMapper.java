package com.ds.university.mapper;

import com.ds.university.entity.Instructor;
import com.ds.university.vo.SectionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 教师 */
@Mapper
public interface InstructorMapper {

    /** 教师列表（按系筛选，可选参数） */
    List<Instructor> selectAll(@Param("deptName") String deptName);

    Instructor selectById(@Param("id") String id);

    /** 教师授课的开课班 */
    List<SectionVO> selectSectionsByInstructor(@Param("id") String id);

    int count();
}