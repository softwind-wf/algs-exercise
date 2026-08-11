package com.ds.university.vo;

/**
 * 成绩单聚合统计（SQL 聚合查询返回）：
 * 已修学分（F 不计入）、加权总绩点、已出成绩学分（GPA 分母）。
 */
public class TranscriptSummaryVO {

    private Integer earnedCredits;
    private Double totalPoints;
    private Integer gradedCredits;

    public Integer getEarnedCredits() { return earnedCredits; }
    public void setEarnedCredits(Integer earnedCredits) { this.earnedCredits = earnedCredits; }
    public Double getTotalPoints() { return totalPoints; }
    public void setTotalPoints(Double totalPoints) { this.totalPoints = totalPoints; }
    public Integer getGradedCredits() { return gradedCredits; }
    public void setGradedCredits(Integer gradedCredits) { this.gradedCredits = gradedCredits; }
}
