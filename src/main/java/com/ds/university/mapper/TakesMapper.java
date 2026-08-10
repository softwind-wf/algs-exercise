package com.ds.university.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 选课（takes）写操作 */
@Mapper
public interface TakesMapper {

    @Select("SELECT COUNT(*) FROM takes WHERE ID = #{id} AND course_id = #{courseId} " +
            "AND sec_id = #{secId} AND semester = #{semester} AND year = #{year}")
    int exists(@Param("id") String id,
               @Param("courseId") String courseId,
               @Param("secId") String secId,
               @Param("semester") String semester,
               @Param("year") Integer year);

    @Select("SELECT COUNT(*) FROM takes WHERE course_id = #{courseId} AND sec_id = #{secId} " +
            "AND semester = #{semester} AND year = #{year}")
    int countEnrolled(@Param("courseId") String courseId,
                      @Param("secId") String secId,
                      @Param("semester") String semester,
                      @Param("year") Integer year);

    @Select("SELECT grade FROM takes WHERE ID = #{id} AND course_id = #{courseId} " +
            "AND sec_id = #{secId} AND semester = #{semester} AND year = #{year}")
    String selectGrade(@Param("id") String id,
                       @Param("courseId") String courseId,
                       @Param("secId") String secId,
                       @Param("semester") String semester,
                       @Param("year") Integer year);

    /** 锁定开课班行（SELECT ... FOR UPDATE），用于选课容量校验与插入保持同一事务 */
    @Select("SELECT COUNT(*) FROM section WHERE course_id = #{courseId} AND sec_id = #{secId} " +
            "AND semester = #{semester} AND year = #{year} FOR UPDATE")
    int lockSection(@Param("courseId") String courseId,
                   @Param("secId") String secId,
                   @Param("semester") String semester,
                   @Param("year") Integer year);

    @Insert("INSERT INTO takes (ID, course_id, sec_id, semester, year, grade) " +
            "VALUES (#{id}, #{courseId}, #{secId}, #{semester}, #{year}, NULL)")
    int insert(@Param("id") String id,
               @Param("courseId") String courseId,
               @Param("secId") String secId,
               @Param("semester") String semester,
               @Param("year") Integer year);

    @Delete("DELETE FROM takes WHERE ID = #{id} AND course_id = #{courseId} " +
            "AND sec_id = #{secId} AND semester = #{semester} AND year = #{year}")
    int delete(@Param("id") String id,
               @Param("courseId") String courseId,
               @Param("secId") String secId,
               @Param("semester") String semester,
               @Param("year") Integer year);
}