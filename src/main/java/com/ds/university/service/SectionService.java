package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.mapper.SectionMapper;
import com.ds.university.vo.SectionVO;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/** 开课班业务 */
@Service
public class SectionService {

    private static final List<String> SEMESTERS = Arrays.asList("Fall", "Spring", "Summer");

    private final SectionMapper sectionMapper;

    public SectionService(SectionMapper sectionMapper) {
        this.sectionMapper = sectionMapper;
    }

    public List<SectionVO> list(String semester, Integer year, String courseId) {
        if (semester != null && !semester.isEmpty() && !SEMESTERS.contains(semester)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "学期取值仅支持 Fall / Spring / Summer");
        }
        if (year != null && year <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "年份不合法");
        }
        return sectionMapper.selectAll(semester, year, courseId);
    }
}