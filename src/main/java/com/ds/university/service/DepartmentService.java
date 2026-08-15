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
import com.ds.university.entity.Department;
import com.ds.university.mapper.DepartmentMapper;
import com.ds.university.vo.DepartmentDetailVO;
import com.ds.university.vo.DepartmentVO;
import org.springframework.stereotype.Service;

import java.util.List;

/** 系业务 */
@Service
public class DepartmentService {

    private final DepartmentMapper departmentMapper;

    public DepartmentService(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    public List<DepartmentVO> listWithStats() {
        return departmentMapper.selectAllWithStats();
    }

    /** 系列表分页（含教师数/课程数） */
    public PageResult<DepartmentVO> page(int page, int size) {
        size = PageResult.normalizeSize(size);
        long total = departmentMapper.count();
        int safePage = PageResult.clampPage(page, size, total);
        List<DepartmentVO> records = departmentMapper.selectPageWithStats((safePage - 1) * size, size);
        return new PageResult<>(records, safePage, size, total);
    }

    public List<Department> listAll() {
        return departmentMapper.selectAllSimple();
    }

    public DepartmentDetailVO detail(String deptName) {
        Department department = departmentMapper.selectById(deptName);
        if (department == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        DepartmentDetailVO vo = new DepartmentDetailVO();
        vo.setDepartment(department);
        vo.setInstructors(departmentMapper.selectInstructorsByDept(deptName));
        vo.setCourses(departmentMapper.selectCoursesByDept(deptName));
        return vo;
    }
}