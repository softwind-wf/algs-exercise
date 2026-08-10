package com.ds.university.vo;

/** 排课时间段选项：标识 + 可读描述（如 A · M 08:00-08:50 / W 08:00-08:50 / F 09:00-09:50） */
public class TimeSlotVO {

    private String timeSlotId;
    private String label;

    public String getTimeSlotId() { return timeSlotId; }
    public void setTimeSlotId(String timeSlotId) { this.timeSlotId = timeSlotId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}