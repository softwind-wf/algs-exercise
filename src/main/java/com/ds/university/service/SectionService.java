package com.ds.university.service;

import com.ds.university.common.PageResult;
import com.ds.university.mapper.SectionMapper;
import com.ds.university.vo.SectionVO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import java.util.List;

/** 开课班业务 */
@Service
@Validated
public class SectionService {

    private final SectionMapper sectionMapper;

    public SectionService(SectionMapper sectionMapper) {
        this.sectionMapper = sectionMapper;
    }

    public List<SectionVO> list(@Pattern(regexp = "|Fall|Spring|Summer", message = "学期取值仅支持 Fall / Spring / Summer") String semester,
                                @Positive(message = "年份不合法") Integer year, String courseId) {
        return sectionMapper.selectAll(semester, year, courseId);
    }

    /** 开课班分页列表 */
    public PageResult<SectionVO> page(@Pattern(regexp = "|Fall|Spring|Summer", message = "学期取值仅支持 Fall / Spring / Summer") String semester,
                                      @Positive(message = "年份不合法") Integer year, String courseId, int page, int size) {
        size = PageResult.normalizeSize(size);
        long total = sectionMapper.countByFilter(semester, year, courseId);
        int safePage = PageResult.clampPage(page, size, total);
        List<SectionVO> records = sectionMapper.selectPage(semester, year, courseId, (safePage - 1) * size, size);
        return new PageResult<>(records, safePage, size, total);
    }
}