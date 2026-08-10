package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.common.PageResult;
import com.ds.university.entity.Course;
import com.ds.university.mapper.CourseMapper;
import com.ds.university.mapper.SectionMapper;
import com.ds.university.vo.CourseDetailVO;
import org.springframework.stereotype.Service;

import java.util.List;

/** 课程业务 */
@Service
public class CourseService {

    private final CourseMapper courseMapper;
    private final SectionMapper sectionMapper;

    public CourseService(CourseMapper courseMapper, SectionMapper sectionMapper) {
        this.courseMapper = courseMapper;
        this.sectionMapper = sectionMapper;
    }

    public List<Course> list(String deptName, String keyword) {
        return courseMapper.selectAll(deptName, keyword);
    }
    /** 课程名称（用于选课/退课提示消息） */
    public String title(String courseId) {
        Course course = courseMapper.selectById(courseId);
        return course == null ? null : course.getTitle();
    }

    /** 课程分页列表 */
    public PageResult<Course> page(String deptName, String keyword, int page, int size) {
        size = PageResult.normalizeSize(size);
        long total = courseMapper.countByFilter(deptName, keyword);
        int safePage = PageResult.clampPage(page, size, total);
        List<Course> records = courseMapper.selectPage(deptName, keyword, (safePage - 1) * size, size);
        return new PageResult<>(records, safePage, size, total);
    }

    public CourseDetailVO detail(String courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        CourseDetailVO vo = new CourseDetailVO();
        vo.setCourse(course);
        vo.setPrereqs(courseMapper.selectPrereqs(courseId));
        vo.setSections(sectionMapper.selectAll(null, null, courseId));
        return vo;
    }
}