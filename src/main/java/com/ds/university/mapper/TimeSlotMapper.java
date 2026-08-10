package com.ds.university.mapper;

import com.ds.university.entity.TimeSlot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 时间段 */
@Mapper
public interface TimeSlotMapper {

    /** 某时间段标识对应的所有时段（如 A 对应 M/W/F 多行） */
    @Select("SELECT time_slot_id, day, start_time, end_time FROM time_slot " +
            "WHERE time_slot_id = #{timeSlotId} ORDER BY day, start_time")
    List<TimeSlot> selectById(@Param("timeSlotId") String timeSlotId);

    /** 全部时间段（排课下拉框展示具体时段） */
    @Select("SELECT time_slot_id, day, start_time, end_time FROM time_slot ORDER BY time_slot_id, day, start_time")
    List<TimeSlot> selectAll();

    /** 去重的时间段标识（排课下拉框） */
    @Select("SELECT DISTINCT time_slot_id FROM time_slot ORDER BY time_slot_id")
    List<String> selectDistinctIds();

    @Select("SELECT COUNT(*) FROM time_slot")
    int count();
}