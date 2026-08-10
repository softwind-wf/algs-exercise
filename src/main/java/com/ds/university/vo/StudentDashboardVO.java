package com.ds.university.vo;

import com.ds.university.entity.Student;

/** 学生中心首页数据 */
public class StudentDashboardVO {

    private Student student;
    private String advisorName;
    private int enrollmentCount;
    private int earnedCredits;
    private Double gpa;

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getAdvisorName() { return advisorName; }
    public void setAdvisorName(String advisorName) { this.advisorName = advisorName; }
    public int getEnrollmentCount() { return enrollmentCount; }
    public void setEnrollmentCount(int enrollmentCount) { this.enrollmentCount = enrollmentCount; }
    public int getEarnedCredits() { return earnedCredits; }
    public void setEarnedCredits(int earnedCredits) { this.earnedCredits = earnedCredits; }
    public Double getGpa() { return gpa; }
    public void setGpa(Double gpa) { this.gpa = gpa; }
}