package com.ds.university.vo;

import com.ds.university.common.PageResult;
import java.util.List;

/** 成绩单：课程明细 + 汇总 */
public class TranscriptVO {

    private String studentId;
    private String studentName;
    private String deptName;
    private List<TranscriptRowVO> rows;
    private int courseCount;
    private int earnedCredits;
    private Double gpa;
    private PageResult<TranscriptRowVO> pageResult;

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public List<TranscriptRowVO> getRows() { return rows; }
    public void setRows(List<TranscriptRowVO> rows) { this.rows = rows; }
    public int getCourseCount() { return courseCount; }
    public void setCourseCount(int courseCount) { this.courseCount = courseCount; }
    public int getEarnedCredits() { return earnedCredits; }
    public void setEarnedCredits(int earnedCredits) { this.earnedCredits = earnedCredits; }
    public Double getGpa() { return gpa; }
    public void setGpa(Double gpa) { this.gpa = gpa; }

    public PageResult<TranscriptRowVO> getPageResult() { return pageResult; }
    public void setPageResult(PageResult<TranscriptRowVO> pageResult) { this.pageResult = pageResult; }
}