package com.ds.university.vo;

/** 选课人数统计：开课班选课人数 vs 教室容量（utilization 为百分比，容量为空时为 null） */
public class EnrollmentReportVO {

    private String courseId;
    private String secId;
    private String semester;
    private Integer year;
    private String courseTitle;
    private String building;
    private String roomNumber;
    private String timeSlotId;
    private Integer capacity;
    private Integer enrolled;
    private Integer utilization;

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getSecId() { return secId; }
    public void setSecId(String secId) { this.secId = secId; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getTimeSlotId() { return timeSlotId; }
    public void setTimeSlotId(String timeSlotId) { this.timeSlotId = timeSlotId; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public Integer getEnrolled() { return enrolled; }
    public void setEnrolled(Integer enrolled) { this.enrolled = enrolled; }
    public Integer getUtilization() { return utilization; }
    public void setUtilization(Integer utilization) { this.utilization = utilization; }
}