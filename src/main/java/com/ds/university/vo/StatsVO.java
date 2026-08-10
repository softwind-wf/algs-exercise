package com.ds.university.vo;

/** 首页统计数据 */
public class StatsVO {

    private Integer departmentCount;
    private Integer courseCount;
    private Integer instructorCount;
    private Integer sectionCount;

    public Integer getDepartmentCount() {
        return departmentCount;
    }

    public void setDepartmentCount(Integer departmentCount) {
        this.departmentCount = departmentCount;
    }

    public Integer getCourseCount() {
        return courseCount;
    }

    public void setCourseCount(Integer courseCount) {
        this.courseCount = courseCount;
    }

    public Integer getInstructorCount() {
        return instructorCount;
    }

    public void setInstructorCount(Integer instructorCount) {
        this.instructorCount = instructorCount;
    }

    public Integer getSectionCount() {
        return sectionCount;
    }

    public void setSectionCount(Integer sectionCount) {
        this.sectionCount = sectionCount;
    }
}