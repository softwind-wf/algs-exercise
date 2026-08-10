package com.ds.university.vo;

import com.ds.university.entity.Classroom;

import java.util.List;
import java.util.Map;

/** 排课看板数据：学期、教室列表、时间段列表、待排课班与已排课格位索引 */
public class SchedulingBoardVO {

    private String semester;
    private Integer year;
    private List<String> semesters;
    private List<Integer> years;
    private List<Classroom> classrooms;
    private List<TimeSlotVO> timeSlots;
    private List<SectionVO> pendingSections;
    /** key = building|roomNumber|timeSlotId，value = 已排入该格的开课班 */
    private Map<String, SectionVO> cellMap;
    /** 时间段标识 -> 具体时段列表（如 A -> ["M 08:00-08:50", "W 08:00-08:50", "F 09:00-09:50"]），供前端做冲突预判 */
    private Map<String, List<String>> timeSlotDays;
    /** 教师编号 -> 本学期授课列表（courseId|secId|timeSlotId），供前端做教师冲突预判 */
    private Map<String, List<String>> teacherLoad;

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public List<String> getSemesters() { return semesters; }
    public void setSemesters(List<String> semesters) { this.semesters = semesters; }
    public List<Integer> getYears() { return years; }
    public void setYears(List<Integer> years) { this.years = years; }
    public List<Classroom> getClassrooms() { return classrooms; }
    public void setClassrooms(List<Classroom> classrooms) { this.classrooms = classrooms; }
    public List<TimeSlotVO> getTimeSlots() { return timeSlots; }
    public void setTimeSlots(List<TimeSlotVO> timeSlots) { this.timeSlots = timeSlots; }
    public List<SectionVO> getPendingSections() { return pendingSections; }
    public void setPendingSections(List<SectionVO> pendingSections) { this.pendingSections = pendingSections; }
    public Map<String, SectionVO> getCellMap() { return cellMap; }
    public void setCellMap(Map<String, SectionVO> cellMap) { this.cellMap = cellMap; }
    public Map<String, List<String>> getTimeSlotDays() { return timeSlotDays; }
    public void setTimeSlotDays(Map<String, List<String>> timeSlotDays) { this.timeSlotDays = timeSlotDays; }
    public Map<String, List<String>> getTeacherLoad() { return teacherLoad; }
    public void setTeacherLoad(Map<String, List<String>> teacherLoad) { this.teacherLoad = teacherLoad; }
}