package com.ds.university.vo;

/** 成绩分布：成绩取值 + 人数（percent 为该成绩占总出分人数的百分比） */
public class GradeCountVO {

    private String grade;
    private Integer count;
    private Integer percent;

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public Integer getPercent() { return percent; }
    public void setPercent(Integer percent) { this.percent = percent; }
}