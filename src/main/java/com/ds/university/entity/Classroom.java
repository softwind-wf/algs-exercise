package com.ds.university.entity;

/** 教室（联合主键 building + roomNumber） */
public class Classroom {

    private String building;
    private String roomNumber;
    private Integer capacity;


    public Classroom() {
    }

    public Classroom(String building, String roomNumber, Integer capacity) {
        this.building = building;
        this.roomNumber = roomNumber;
        this.capacity = capacity;
    }    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}