package com.ds.university.vo;

/**
 * 选课事务中锁定的开课班行信息（SELECT ... FOR UPDATE 返回），
 * 仅包含容量校验与时间冲突检查所需字段。
 */
public class SectionLockVO {

    private String courseId;
    private String secId;
    private String semester;
    private Integer year;
    private Integer capacity;
    private String timeSlotId;

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getSecId() {
        return secId;
    }

    public void setSecId(String secId) {
        this.secId = secId;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getTimeSlotId() {
        return timeSlotId;
    }

    public void setTimeSlotId(String timeSlotId) {
        this.timeSlotId = timeSlotId;
    }
}
