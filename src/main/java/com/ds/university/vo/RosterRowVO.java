package com.ds.university.vo;

/** 班级名单行 */
public class RosterRowVO {

    private String studentId;
    private String name;
    private String deptName;
    private Integer totCred;
    private String grade;

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public Integer getTotCred() { return totCred; }
    public void setTotCred(Integer totCred) { this.totCred = totCred; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
}