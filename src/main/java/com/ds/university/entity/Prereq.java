package com.ds.university.entity;

/** 先修课（课程自关联） */
public class Prereq {

    private String courseId;
    private String prereqId;

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getPrereqId() {
        return prereqId;
    }

    public void setPrereqId(String prereqId) {
        this.prereqId = prereqId;
    }
}