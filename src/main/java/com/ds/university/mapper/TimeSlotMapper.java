package com.ds.university.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 时间段 */
@Mapper
public interface TimeSlotMapper {

    @Select("SELECT COUNT(*) FROM time_slot")
    int count();
}