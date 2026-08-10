package com.ds.university.mapper;

import com.ds.university.vo.RosterRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 教师中心：班级名单、成绩录入、授课统计 */
@Mapper
public interface TeacherMapper {

    /** 班级名单：某开课班的学生选课记录（含成绩） */
    List<RosterRowVO> selectRoster(@Param("courseId") String courseId,
                                   @Param("secId") String secId,
                                   @Param("semester") String semester,
                                   @Param("year") Integer year);

    /** 教师是否讲授该开课班（权限校验） */
    int countTeaches(@Param("instructorId") String instructorId,
                     @Param("courseId") String courseId,
                     @Param("secId") String secId,
                     @Param("semester") String semester,
                     @Param("year") Integer year);

    /** 录入/修改成绩（grade 为空表示清除成绩） */
    int updateGrade(@Param("studentId") String studentId,
                    @Param("courseId") String courseId,
                    @Param("secId") String secId,
                    @Param("semester") String semester,
                    @Param("year") Integer year,
                    @Param("grade") String grade);

    /** 教师名下所有开课班的在修学生总数 */
    long countTeachingStudents(@Param("instructorId") String instructorId);

    /** 教师名下已录成绩的学生数 */
    long countGradedStudents(@Param("instructorId") String instructorId);
}