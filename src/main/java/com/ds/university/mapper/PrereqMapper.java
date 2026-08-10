package com.ds.university.mapper;

import com.ds.university.vo.PrereqVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 先修课 */
@Mapper
public interface PrereqMapper {

    /** 全部先修关系（含课程名） */
    List<PrereqVO> selectAll();

    /** 先修关系是否已存在 */
    int exists(@Param("courseId") String courseId, @Param("prereqId") String prereqId);

    int insert(@Param("courseId") String courseId, @Param("prereqId") String prereqId);

    int delete(@Param("courseId") String courseId, @Param("prereqId") String prereqId);

    @Select("SELECT COUNT(*) FROM prereq")
    int count();
}