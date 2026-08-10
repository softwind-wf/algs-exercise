package com.ds.university.vo;

/** 先修关系视图：课程名 + 先修课程名 */
public class PrereqVO {

    private String courseId;
    private String courseTitle;
    private String prereqId;
    private String prereqTitle;

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public String getPrereqId() { return prereqId; }
    public void setPrereqId(String prereqId) { this.prereqId = prereqId; }
    public String getPrereqTitle() { return prereqTitle; }
    public void setPrereqTitle(String prereqTitle) { this.prereqTitle = prereqTitle; }
}