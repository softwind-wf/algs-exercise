package com.ds.university.vo;

/** 导师信息 */
public class AdvisorVO {

    private String iId;
    private String name;
    private String deptName;
    private int adviseeCount;

    public String getIId() { return iId; }
    public void setIId(String iId) { this.iId = iId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public int getAdviseeCount() { return adviseeCount; }
    public void setAdviseeCount(int adviseeCount) { this.adviseeCount = adviseeCount; }
}