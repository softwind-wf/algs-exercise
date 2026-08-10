package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
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