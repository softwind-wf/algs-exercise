package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.entity.Student;
import com.ds.university.mapper.StudentMapper;
import com.ds.university.vo.StudentProfileVO;
import org.springframework.stereotype.Service;

/** 学生业务 */
@Service
public class StudentService {

    private final StudentMapper studentMapper;

    public StudentService(StudentMapper studentMapper) {
        this.studentMapper = studentMapper;
    }

    public StudentProfileVO profile(String id) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        StudentProfileVO vo = new StudentProfileVO();
        vo.setStudent(student);
        vo.setAdvisorName(studentMapper.selectAdvisorName(id));
        return vo;
    }
}