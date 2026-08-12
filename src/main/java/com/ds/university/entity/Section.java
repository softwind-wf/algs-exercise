package com.ds.university.entity;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/** 开课班（联合主键 courseId + secId + semester + year）。
 *  主键字段上的约束供 service 层 @Valid 方法参数校验（Bean Validation 统一拦截）。 */
public class Section {

    @NotBlank(message = "开课班课程号不能为空")
    private String courseId;

    @NotBlank(message = "开课班班号不能为空")
    private String secId;

    @NotBlank(message = "学期不能为空")
    @Pattern(regexp = "Fall|Spring|Summer", message = "学期仅支持 Fall / Spring / Summer")
    private String semester;

    @NotNull(message = "年份不能为空")
    @Min(value = 2000, message = "年份不合法")
    @Max(value = 2100, message = "年份不合法")
    private Integer year;
    private String building;
    private String roomNumber;
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

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getTimeSlotId() {
        return timeSlotId;
    }

    public void setTimeSlotId(String timeSlotId) {
        this.timeSlotId = timeSlotId;
    }
}