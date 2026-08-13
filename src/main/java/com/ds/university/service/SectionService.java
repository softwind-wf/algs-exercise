package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.common.PageResult;
import com.ds.university.mapper.SectionMapper;
import com.ds.university.mapper.TimeSlotMapper;
import com.ds.university.vo.SectionDetailVO;
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
    private final TimeSlotMapper timeSlotMapper;

    public SectionService(SectionMapper sectionMapper, TimeSlotMapper timeSlotMapper) {
        this.sectionMapper = sectionMapper;
        this.timeSlotMapper = timeSlotMapper;
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

    /** 开课班详情：基本信息 + 时间槽明细 + 选课学生名单 */
    public SectionDetailVO detail(String courseId, String secId, String semester, Integer year) {
        SectionVO section = sectionMapper.selectDetail(courseId, secId, semester, year);
        if (section == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        SectionDetailVO vo = new SectionDetailVO();
        vo.setSection(section);
        vo.setTimeSlots(timeSlotMapper.selectById(section.getTimeSlotId()));
        vo.setStudents(sectionMapper.selectStudents(courseId, secId, semester, year));
        return vo;
    }
}