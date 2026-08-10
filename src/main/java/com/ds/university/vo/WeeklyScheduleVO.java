package com.ds.university.vo;

import com.ds.university.entity.Classroom;
import com.ds.university.entity.Instructor;

import java.util.List;
import java.util.Map;

/** 周课表视图：按 星期 x 时段 展示某学期（可筛选教室/教师）的排课 */
public class WeeklyScheduleVO {

    private String semester;
    private Integer year;
    /** all / room / instructor */
    private String filterType;
    private String filterKey;
    /** 筛选结果的可读描述（如 全部 / Packard 101 / 45565 - Katz） */
    private String filterLabel;
    /** 星期列表（如 M、W、F） */
    private List<String> days;
    /** 星期显示名（如 M -> 周一） */
    private Map<String, String> dayLabels;
    /** 时段列表（如 08:00-08:50） */
    private List<String> periods;
    /** key = day|period，value = 该格内的开课班列表 */
    private Map<String, List<SectionVO>> cells;
    private List<Classroom> classrooms;
    private List<Instructor> instructors;

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getFilterType() { return filterType; }
    public void setFilterType(String filterType) { this.filterType = filterType; }
    public String getFilterKey() { return filterKey; }
    public void setFilterKey(String filterKey) { this.filterKey = filterKey; }
    public String getFilterLabel() { return filterLabel; }
    public void setFilterLabel(String filterLabel) { this.filterLabel = filterLabel; }
    public List<String> getDays() { return days; }
    public void setDays(List<String> days) { this.days = days; }
    public Map<String, String> getDayLabels() { return dayLabels; }
    public void setDayLabels(Map<String, String> dayLabels) { this.dayLabels = dayLabels; }
    public List<String> getPeriods() { return periods; }
    public void setPeriods(List<String> periods) { this.periods = periods; }
    public Map<String, List<SectionVO>> getCells() { return cells; }
    public void setCells(Map<String, List<SectionVO>> cells) { this.cells = cells; }
    public List<Classroom> getClassrooms() { return classrooms; }
    public void setClassrooms(List<Classroom> classrooms) { this.classrooms = classrooms; }
    public List<Instructor> getInstructors() { return instructors; }
    public void setInstructors(List<Instructor> instructors) { this.instructors = instructors; }
}