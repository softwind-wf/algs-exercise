/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.common.PageResult;
import com.ds.university.entity.Instructor;
import com.ds.university.mapper.InstructorMapper;
import com.ds.university.vo.InstructorDetailVO;
import org.springframework.stereotype.Service;

import java.util.List;

/** 教师业务 */
@Service
public class InstructorService {

    private final InstructorMapper instructorMapper;

    public InstructorService(InstructorMapper instructorMapper) {
        this.instructorMapper = instructorMapper;
    }

    public List<Instructor> list(String deptName) {
        return instructorMapper.selectAll(deptName);
    }

    /** 教师分页列表 */
    public PageResult<Instructor> page(String deptName, int page, int size) {
        size = PageResult.normalizeSize(size);
        long total = instructorMapper.countByFilter(deptName);
        int safePage = PageResult.clampPage(page, size, total);
        List<Instructor> records = instructorMapper.selectPage(deptName, (safePage - 1) * size, size);
        return new PageResult<>(records, safePage, size, total);
    }

    public InstructorDetailVO detail(String id) {
        Instructor instructor = instructorMapper.selectById(id);
        if (instructor == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        InstructorDetailVO vo = new InstructorDetailVO();
        vo.setInstructor(instructor);
        vo.setSections(instructorMapper.selectSectionsByInstructor(id));
        return vo;
    }
}