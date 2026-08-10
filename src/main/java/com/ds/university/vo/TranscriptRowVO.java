package com.ds.university.vo;

/** 成绩单行 */
public class TranscriptRowVO {

    private String courseId;
    private String title;
    private Integer credits;
    private String semester;
    private Integer year;
    private String grade;
    private Double gradePoint;

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public Double getGradePoint() { return gradePoint; }
    public void setGradePoint(Double gradePoint) { this.gradePoint = gradePoint; }
}