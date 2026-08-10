package com.ds.university.vo;

import com.ds.university.entity.Course;

import java.util.List;

/** 课程详情视图：课程 + 先修课 + 开课班 */
public class CourseDetailVO {

    private Course course;
    private List<Course> prereqs;
    private List<SectionVO> sections;

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public List<Course> getPrereqs() {
        return prereqs;
    }

    public void setPrereqs(List<Course> prereqs) {
        this.prereqs = prereqs;
    }

    public List<SectionVO> getSections() {
        return sections;
    }

    public void setSections(List<SectionVO> sections) {
        this.sections = sections;
    }
}