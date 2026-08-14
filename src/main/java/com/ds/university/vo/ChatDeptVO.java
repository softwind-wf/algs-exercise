package com.ds.university.vo;

/** 院系（含师生人数），供"按院系浏览"联系人使用 */
public class ChatDeptVO {

    private String deptName;
    private int studentCount;
    private int instructorCount;

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(int studentCount) {
        this.studentCount = studentCount;
    }

    public int getInstructorCount() {
        return instructorCount;
    }

    public void setInstructorCount(int instructorCount) {
        this.instructorCount = instructorCount;
    }
}
