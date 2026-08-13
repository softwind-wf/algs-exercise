package com.ds.university.mapper;

import com.ds.university.entity.Section;

import com.ds.university.vo.SectionStudentVO;
import com.ds.university.vo.SectionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 开课班 */
@Mapper
public interface SectionMapper {

    /** 开课班列表（学期/年份/课程号筛选，均为可选参数） */
    List<SectionVO> selectAll(@Param("semester") String semester,
                              @Param("year") Integer year,
                              @Param("courseId") String courseId);

    /** 开课班分页列表（筛选条件与 selectAll 一致） */
    List<SectionVO> selectPage(@Param("semester") String semester,
                               @Param("year") Integer year,
                               @Param("courseId") String courseId,
                               @Param("offset") int offset,
                               @Param("limit") int limit);

    /** 开课班总数（按学期/年份/课程号筛选） */
    long countByFilter(@Param("semester") String semester,
                       @Param("year") Integer year,
                       @Param("courseId") String courseId);

    /** 最新开课班（按年份/学期倒序，首页展示） */
    List<SectionVO> selectLatest(@Param("limit") int limit);

    int count();

    /** 已有开课班的年份列表（排课看板筛选） */
    @Select("SELECT DISTINCT year FROM section ORDER BY year DESC")
    List<Integer> selectYears();


    /** 开课班详情（含课程名/教师/选课人数/容量/所属系） */
    SectionVO selectDetail(@Param("courseId") String courseId,
                           @Param("secId") String secId,
                           @Param("semester") String semester,
                           @Param("year") Integer year);

    /** 开课班选课学生名单（含成绩） */
    List<SectionStudentVO> selectStudents(@Param("courseId") String courseId,
                                          @Param("secId") String secId,
                                          @Param("semester") String semester,
                                          @Param("year") Integer year);

    /** 开课班详情（含开课班实体字段） */
    Section selectById(@Param("courseId") String courseId,
                       @Param("secId") String secId,
                       @Param("semester") String semester,
                       @Param("year") Integer year);

    /** 新增开课班（排课） */
    int insert(Section section);

    /** 修改开课班（教室/时间段） */
    int update(Section section);

    /** 删除开课班 */
    int delete(@Param("courseId") String courseId,
               @Param("secId") String secId,
               @Param("semester") String semester,
               @Param("year") Integer year);

    /** 同一教室同一时间（含部分重叠）是否已被其他开课班占用 */
    int countRoomTimeConflict(@Param("courseId") String courseId,
                              @Param("secId") String secId,
                              @Param("semester") String semester,
                              @Param("year") Integer year,
                              @Param("building") String building,
                              @Param("roomNumber") String roomNumber,
                              @Param("timeSlotId") String timeSlotId);

    /** 同一教师在同一时间（含部分重叠）是否已有其他授课 */
    int countInstructorTimeConflict(@Param("instructorId") String instructorId,
                                    @Param("courseId") String courseId,
                                    @Param("secId") String secId,
                                    @Param("semester") String semester,
                                    @Param("year") Integer year,
                                    @Param("timeSlotId") String timeSlotId);

    /** 某开课班当前已选人数 */
    int countEnrolled(@Param("courseId") String courseId,
                      @Param("secId") String secId,
                      @Param("semester") String semester,
                      @Param("year") Integer year);
}