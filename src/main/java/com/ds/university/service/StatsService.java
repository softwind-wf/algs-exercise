/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.service;

import com.ds.university.mapper.CourseMapper;
import com.ds.university.mapper.DepartmentMapper;
import com.ds.university.mapper.InstructorMapper;
import com.ds.university.mapper.SectionMapper;
import com.ds.university.vo.StatsVO;
import org.springframework.stereotype.Service;

/** 首页统计 */
@Service
public class StatsService {

    private final DepartmentMapper departmentMapper;
    private final CourseMapper courseMapper;
    private final InstructorMapper instructorMapper;
    private final SectionMapper sectionMapper;

    public StatsService(DepartmentMapper departmentMapper, CourseMapper courseMapper,
                        InstructorMapper instructorMapper, SectionMapper sectionMapper) {
        this.departmentMapper = departmentMapper;
        this.courseMapper = courseMapper;
        this.instructorMapper = instructorMapper;
        this.sectionMapper = sectionMapper;
    }

    public StatsVO summary() {
        StatsVO vo = new StatsVO();
        vo.setDepartmentCount(departmentMapper.count());
        vo.setCourseCount(courseMapper.count());
        vo.setInstructorCount(instructorMapper.count());
        vo.setSectionCount(sectionMapper.count());
        return vo;
    }
}