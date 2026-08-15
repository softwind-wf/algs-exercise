package com.ds.university.service;

import com.ds.university.mapper.StatsMapper;
import com.ds.university.vo.DeptBudgetVO;
import com.ds.university.vo.DeptSalaryVO;
import com.ds.university.vo.EnrollmentReportVO;
import com.ds.university.vo.GradeCountVO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** 统计报表业务（UC-09）：系预算、教师平均工资、选课人数、成绩分布 */
@Service
public class StatsReportService {

    /** 成绩展示顺序（未出分固定排最后） */
    private static final List<String> GRADE_ORDER = Arrays.asList(
            "A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "F");

    private final StatsMapper statsMapper;

    public StatsReportService(StatsMapper statsMapper) {
        this.statsMapper = statsMapper;
    }

    /** 系经费与教师数（含相对最大值的百分比，供图表展示） */
    @Cacheable(cacheNames = "stats", key = "'deptBudget'")
    public List<DeptBudgetVO> deptBudget() {
        List<DeptBudgetVO> rows = statsMapper.selectDeptBudget();
        BigDecimal max = rows.stream()
                .map(DeptBudgetVO::getBudget)
                .filter(b -> b != null)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);
        for (DeptBudgetVO row : rows) {
            if (row.getBudget() != null) {
                row.setBudgetPercent(percent(row.getBudget(), max));
            } else {
                row.setBudgetPercent(0);
            }
        }
        return rows;
    }

    /** 各系教师平均工资（含相对最大值的百分比） */
    @Cacheable(cacheNames = "stats", key = "'salaryByDept'")
    public List<DeptSalaryVO> salaryByDept() {
        List<DeptSalaryVO> rows = statsMapper.selectSalaryByDept();
        BigDecimal max = rows.stream()
                .map(DeptSalaryVO::getAvgSalary)
                .filter(s -> s != null)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);
        for (DeptSalaryVO row : rows) {
            if (row.getAvgSalary() != null) {
                row.setAvgPercent(percent(row.getAvgSalary(), max));
            } else {
                row.setAvgPercent(0);
            }
        }
        return rows;
    }

    private int percent(BigDecimal value, BigDecimal max) {
        if (value == null || max == null || max.signum() == 0) {
            return 0;
        }
        return value.multiply(BigDecimal.valueOf(100))
                .divide(max, 0, RoundingMode.HALF_UP).intValue();
    }
    /** 选课人数 vs 容量（含利用率百分比） */
    @Cacheable(cacheNames = "stats", key = "'enrollment:' + #semester + ':' + #year + ':' + #courseId")
    public List<EnrollmentReportVO> enrollment(String semester, Integer year, String courseId) {
        List<EnrollmentReportVO> rows = statsMapper.selectEnrollment(semester, year, courseId);
        for (EnrollmentReportVO row : rows) {
            if (row.getCapacity() != null && row.getCapacity() > 0 && row.getEnrolled() != null) {
                row.setUtilization((int) Math.min(100L,
                        Math.round(row.getEnrolled() * 100.0 / row.getCapacity())));
            } else {
                row.setUtilization(null);
            }
        }
        return rows;
    }

    /** 某课程成绩分布（按成绩档位排序，未出分最后；含占比百分比） */
    @Cacheable(cacheNames = "stats", key = "'gradeDist:' + #courseId")
    public List<GradeCountVO> gradeDistribution(String courseId) {
        List<GradeCountVO> rows = statsMapper.selectGradeDistribution(courseId);
        int total = rows.stream().mapToInt(r -> r.getCount() == null ? 0 : r.getCount()).sum();
        for (GradeCountVO row : rows) {
            row.setPercent(total == 0 ? 0 : (int) Math.round(row.getCount() * 100.0 / total));
        }
        List<GradeCountVO> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparingInt((GradeCountVO r) -> {
            if ("未出分".equals(r.getGrade())) {
                return Integer.MAX_VALUE;
            }
            int idx = GRADE_ORDER.indexOf(r.getGrade());
            return idx < 0 ? GRADE_ORDER.size() : idx;
        }));
        return sorted;
    }
}