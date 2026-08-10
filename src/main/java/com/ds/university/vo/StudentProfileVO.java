package com.ds.university.vo;

import com.ds.university.entity.Student;

/** 学生信息视图：学生 + 导师 */
public class StudentProfileVO {

    private Student student;
    private String advisorName;

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public String getAdvisorName() {
        return advisorName;
    }

    public void setAdvisorName(String advisorName) {
        this.advisorName = advisorName;
    }
}