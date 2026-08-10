package com.ds.university.mapper;

import com.ds.university.vo.SectionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 开课班 */
@Mapper
public interface SectionMapper {

    /** 开课班列表（学期/年份/课程号筛选，均为可选参数） */
    List<SectionVO> selectAll(@Param("semester") String semester,
                              @Param("year") Integer year,
                              @Param("courseId") String courseId);

    int count();
}