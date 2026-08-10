package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.common.PageResult;
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
        validateFilters(semester, year);
        return sectionMapper.selectAll(semester, year, courseId);
    }

    /** 开课班分页列表 */
    public PageResult<SectionVO> page(String semester, Integer year, String courseId, int page, int size) {
        validateFilters(semester, year);
        size = PageResult.normalizeSize(size);
        long total = sectionMapper.countByFilter(semester, year, courseId);
        int safePage = PageResult.clampPage(page, size, total);
        List<SectionVO> records = sectionMapper.selectPage(semester, year, courseId, (safePage - 1) * size, size);
        return new PageResult<>(records, safePage, size, total);
    }

    private void validateFilters(String semester, Integer year) {
        if (semester != null && !semester.isEmpty() && !SEMESTERS.contains(semester)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "学期取值仅支持 Fall / Spring / Summer");
        }
        if (year != null && year <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "年份不合法");
        }
    }
}