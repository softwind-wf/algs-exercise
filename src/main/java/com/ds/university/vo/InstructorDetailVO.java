package com.ds.university.vo;

import com.ds.university.entity.Instructor;

import java.util.List;

/** 教师详情视图：教师 + 授课开课班 */
public class InstructorDetailVO {

    private Instructor instructor;
    private List<SectionVO> sections;

    public Instructor getInstructor() {
        return instructor;
    }

    public void setInstructor(Instructor instructor) {
        this.instructor = instructor;
    }

    public List<SectionVO> getSections() {
        return sections;
    }

    public void setSections(List<SectionVO> sections) {
        this.sections = sections;
    }
}