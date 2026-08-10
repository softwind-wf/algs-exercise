package com.ds.university.vo;

/** 已选开课班（我的选课列表） */
public class EnrollmentVO {

    private String courseId;
    private String secId;
    private String semester;
    private Integer year;
    private String title;
    private Integer credits;
    private String instructorNames;
    private String timeSlotId;
    private String building;
    private String roomNumber;
    private Integer enrolled;
    private Integer capacity;
    private String grade;

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getSecId() { return secId; }
    public void setSecId(String secId) { this.secId = secId; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }
    public String getInstructorNames() { return instructorNames; }
    public void setInstructorNames(String instructorNames) { this.instructorNames = instructorNames; }
    public String getTimeSlotId() { return timeSlotId; }
    public void setTimeSlotId(String timeSlotId) { this.timeSlotId = timeSlotId; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public Integer getEnrolled() { return enrolled; }
    public void setEnrolled(Integer enrolled) { this.enrolled = enrolled; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
}