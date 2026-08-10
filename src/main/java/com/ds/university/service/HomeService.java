package com.ds.university.service;

import com.ds.university.mapper.CourseMapper;
import com.ds.university.mapper.DepartmentMapper;
import com.ds.university.mapper.SectionMapper;
import com.ds.university.vo.CourseStatVO;
import com.ds.university.vo.DepartmentVO;
import com.ds.university.vo.SectionVO;
import org.springframework.stereotype.Service;

import java.util.List;

/** 首页内容聚合：热门课程、最新开课班、院系一览 */
@Service
public class HomeService {

    private static final int HOT_COURSE_LIMIT = 5;
    private static final int LATEST_SECTION_LIMIT = 6;

    private final CourseMapper courseMapper;
    private final SectionMapper sectionMapper;
    private final DepartmentMapper departmentMapper;

    public HomeService(CourseMapper courseMapper, SectionMapper sectionMapper, DepartmentMapper departmentMapper) {
        this.courseMapper = courseMapper;
        this.sectionMapper = sectionMapper;
        this.departmentMapper = departmentMapper;
    }

    /** 热门课程（按累计选课人数降序） */
    public List<CourseStatVO> hotCourses() {
        return courseMapper.selectHotCourses(HOT_COURSE_LIMIT);
    }

    /** 最新开课班（按年份/学期倒序） */
    public List<SectionVO> latestSections() {
        return sectionMapper.selectLatest(LATEST_SECTION_LIMIT);
    }

    /** 院系一览（含教师数/课程数） */
    public List<DepartmentVO> departments() {
        return departmentMapper.selectAllWithStats();
    }
}