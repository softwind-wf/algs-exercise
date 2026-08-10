package com.ds.university.vo;

import com.ds.university.entity.Instructor;

import java.util.List;

/** 教师中心首页视图：个人信息 + 授课列表 + 统计 */
public class TeacherDashboardVO {

    private Instructor instructor;
    private List<SectionVO> sections;
    private int sectionCount;
    private long studentCount;
    private long gradedCount;

    public Instructor getInstructor() { return instructor; }
    public void setInstructor(Instructor instructor) { this.instructor = instructor; }
    public List<SectionVO> getSections() { return sections; }
    public void setSections(List<SectionVO> sections) { this.sections = sections; }
    public int getSectionCount() { return sectionCount; }
    public void setSectionCount(int sectionCount) { this.sectionCount = sectionCount; }
    public long getStudentCount() { return studentCount; }
    public void setStudentCount(long studentCount) { this.studentCount = studentCount; }
    public long getGradedCount() { return gradedCount; }
    public void setGradedCount(long gradedCount) { this.gradedCount = gradedCount; }
}