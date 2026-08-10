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

    /** 教师分页列表（筛选条件与 selectAll 一致） */
    List<Instructor> selectPage(@Param("deptName") String deptName,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    /** 教师总数（按系筛选） */
    long countByFilter(@Param("deptName") String deptName);

    Instructor selectById(@Param("id") String id);

    /** 教师授课的开课班 */
    List<SectionVO> selectSectionsByInstructor(@Param("id") String id);

    int count();


    /** 新增教师 */
    int insert(Instructor instructor);

    /** 修改教师 */
    int update(Instructor instructor);

    /** 删除教师 */
    int delete(@Param("id") String id);}