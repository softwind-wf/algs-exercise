package com.ds.university.vo;

import com.ds.university.entity.TimeSlot;

import java.util.List;

/** 开课班详情：基本信息 + 时间槽明细 + 选课学生名单 */
public class SectionDetailVO {

    private SectionVO section;
    private List<TimeSlot> timeSlots;
    private List<SectionStudentVO> students;

    public SectionVO getSection() {
        return section;
    }

    public void setSection(SectionVO section) {
        this.section = section;
    }

    public List<TimeSlot> getTimeSlots() {
        return timeSlots;
    }

    public void setTimeSlots(List<TimeSlot> timeSlots) {
        this.timeSlots = timeSlots;
    }

    public List<SectionStudentVO> getStudents() {
        return students;
    }

    public void setStudents(List<SectionStudentVO> students) {
        this.students = students;
    }
}
