package com.ds.university.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 授课 */
@Mapper
public interface TeachesMapper {

    /** 某开课班的授课教师编号 */
    @Select("SELECT ID FROM teaches WHERE course_id = #{courseId} AND sec_id = #{secId} " +
            "AND semester = #{semester} AND year = #{year}")
    List<String> selectInstructorIds(@Param("courseId") String courseId,
                                     @Param("secId") String secId,
                                     @Param("semester") String semester,
                                     @Param("year") Integer year);

    @Insert("INSERT INTO teaches (ID, course_id, sec_id, semester, year) " +
            "VALUES (#{instructorId}, #{courseId}, #{secId}, #{semester}, #{year})")
    int insert(@Param("instructorId") String instructorId,
               @Param("courseId") String courseId,
               @Param("secId") String secId,
               @Param("semester") String semester,
               @Param("year") Integer year);

    /** 删除某开课班的全部授课记录 */
    @Delete("DELETE FROM teaches WHERE course_id = #{courseId} AND sec_id = #{secId} " +
            "AND semester = #{semester} AND year = #{year}")
    int deleteBySection(@Param("courseId") String courseId,
                        @Param("secId") String secId,
                        @Param("semester") String semester,
                        @Param("year") Integer year);

    @Select("SELECT COUNT(*) FROM teaches")
    int count();
}