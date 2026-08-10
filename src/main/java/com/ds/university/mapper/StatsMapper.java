package com.ds.university.mapper;

import com.ds.university.vo.DeptBudgetVO;
import com.ds.university.vo.DeptSalaryVO;
import com.ds.university.vo.EnrollmentReportVO;
import com.ds.university.vo.GradeCountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 统计报表（UC-09）：系预算、教师工资、选课人数、成绩分布 */
@Mapper
public interface StatsMapper {

    /** 系经费与教师数 */
    List<DeptBudgetVO> selectDeptBudget();

    /** 各系教师平均工资 */
    List<DeptSalaryVO> selectSalaryByDept();

    /** 选课人数 vs 教室容量（学期/年份/课程筛选，均可选） */
    List<EnrollmentReportVO> selectEnrollment(@Param("semester") String semester,
                                              @Param("year") Integer year,
                                              @Param("courseId") String courseId);

    /** 某课程成绩分布（跨学期汇总，含未出分） */
    List<GradeCountVO> selectGradeDistribution(@Param("courseId") String courseId);
}