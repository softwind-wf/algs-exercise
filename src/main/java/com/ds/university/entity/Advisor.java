package com.ds.university.entity;

/** 指导关系（一个学生一个导师） */
public class Advisor {

    private String sId;
    private String iId;

    public String getSId() {
        return sId;
    }

    public void setSId(String sId) {
        this.sId = sId;
    }

    public String getIId() {
        return iId;
    }

    public void setIId(String iId) {
        this.iId = iId;
    }
}