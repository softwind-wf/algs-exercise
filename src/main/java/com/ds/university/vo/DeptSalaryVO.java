package com.ds.university.vo;

import java.math.BigDecimal;

/** 各系教师工资统计：人数、平均/最高/最低工资（avgPercent 供图表展示） */
public class DeptSalaryVO {

    private String deptName;
    private Integer instructorCount;
    private BigDecimal avgSalary;
    private BigDecimal maxSalary;
    private BigDecimal minSalary;
    private Integer avgPercent;

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public Integer getInstructorCount() { return instructorCount; }
    public void setInstructorCount(Integer instructorCount) { this.instructorCount = instructorCount; }
    public BigDecimal getAvgSalary() { return avgSalary; }
    public void setAvgSalary(BigDecimal avgSalary) { this.avgSalary = avgSalary; }
    public BigDecimal getMaxSalary() { return maxSalary; }
    public void setMaxSalary(BigDecimal maxSalary) { this.maxSalary = maxSalary; }
    public BigDecimal getMinSalary() { return minSalary; }
    public void setMinSalary(BigDecimal minSalary) { this.minSalary = minSalary; }
    public Integer getAvgPercent() { return avgPercent; }
    public void setAvgPercent(Integer avgPercent) { this.avgPercent = avgPercent; }
}