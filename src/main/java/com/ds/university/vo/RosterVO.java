package com.ds.university.vo;

import java.util.List;

/** 班级名单视图：开课班信息 + 学生明细 */
public class RosterVO {

    private SectionVO section;
    private List<RosterRowVO> rows;

    public SectionVO getSection() { return section; }
    public void setSection(SectionVO section) { this.section = section; }
    public List<RosterRowVO> getRows() { return rows; }
    public void setRows(List<RosterRowVO> rows) { this.rows = rows; }
}