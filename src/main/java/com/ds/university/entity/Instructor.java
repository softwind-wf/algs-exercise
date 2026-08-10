package com.ds.university.entity;

import java.math.BigDecimal;

/** 教师 */
public class Instructor {

    private String id;
    private String name;
    private String deptName;
    private BigDecimal salary;


    public Instructor() {
    }

    public Instructor(String id, String name, String deptName, BigDecimal salary) {
        this.id = id;
        this.name = name;
        this.deptName = deptName;
        this.salary = salary;
    }    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }
}