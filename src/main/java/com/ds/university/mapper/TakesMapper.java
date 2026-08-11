package com.ds.university.mapper;

import com.ds.university.vo.SectionLockVO;
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

    /** 学生某学期已选某课程的班数（不限 sec_id，用于禁止同课程选多个班） */
    @Select("SELECT COUNT(*) FROM takes WHERE ID = #{id} AND course_id = #{courseId} " +
            "AND semester = #{semester} AND year = #{year}")
    int countCourseEnrollment(@Param("id") String id,
                              @Param("courseId") String courseId,
                              @Param("semester") String semester,
                              @Param("year") Integer year);

    @Select("SELECT grade FROM takes WHERE ID = #{id} AND course_id = #{courseId} " +
            "AND sec_id = #{secId} AND semester = #{semester} AND year = #{year}")
    String selectGrade(@Param("id") String id,
                       @Param("courseId") String courseId,
                       @Param("secId") String secId,
                       @Param("semester") String semester,
                       @Param("year") Integer year);

    /**
     * 锁定开课班行并返回容量/时段信息（一条 SELECT ... FOR UPDATE 完成
     * 存在性校验 + 行锁 + 字段读取），用于选课容量校验与插入保持同一事务。
     * 容量取自教室表（标量子查询，不对 classroom 加锁）。开课班不存在时返回 null。
     */
    @Select("SELECT s.course_id, s.sec_id, s.semester, s.year, s.time_slot_id, " +
            "(SELECT cl.capacity FROM classroom cl " +
            " WHERE cl.building = s.building AND cl.room_number = s.room_number) AS capacity " +
            "FROM section s WHERE s.course_id = #{courseId} AND s.sec_id = #{secId} " +
            "AND s.semester = #{semester} AND s.year = #{year} FOR UPDATE")
    SectionLockVO lockAndGetSection(@Param("courseId") String courseId,
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