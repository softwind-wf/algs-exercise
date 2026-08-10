package com.ds.university.vo;

import java.math.BigDecimal;

/** 系经费统计：系名、楼、预算、教师数（budgetPercent 供图表展示） */
public class DeptBudgetVO {

    private String deptName;
    private String building;
    private BigDecimal budget;
    private Integer instructorCount;
    private Integer budgetPercent;

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public Integer getInstructorCount() { return instructorCount; }
    public void setInstructorCount(Integer instructorCount) { this.instructorCount = instructorCount; }
    public Integer getBudgetPercent() { return budgetPercent; }
    public void setBudgetPercent(Integer budgetPercent) { this.budgetPercent = budgetPercent; }
}