package com.ds.university.vo;

import com.ds.university.entity.Course;
import com.ds.university.entity.Department;
import com.ds.university.entity.Instructor;

import java.util.List;

/** 系详情视图 */
public class DepartmentDetailVO {

    private Department department;
    private List<Instructor> instructors;
    private List<Course> courses;

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<Instructor> getInstructors() {
        return instructors;
    }

    public void setInstructors(List<Instructor> instructors) {
        this.instructors = instructors;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
    }
}