package com.ds.university.mapper;

import com.ds.university.entity.Classroom;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 教室 */
@Mapper
public interface ClassroomMapper {

    @Select("SELECT building, room_number, capacity FROM classroom ORDER BY building, room_number")
    List<Classroom> selectAll();

    @Select("SELECT building, room_number, capacity FROM classroom " +
            "WHERE building = #{building} AND room_number = #{roomNumber}")
    Classroom selectById(@Param("building") String building, @Param("roomNumber") String roomNumber);

    @Insert("INSERT INTO classroom (building, room_number, capacity) " +
            "VALUES (#{building}, #{roomNumber}, #{capacity})")
    int insert(Classroom classroom);

    @Update("UPDATE classroom SET capacity = #{capacity} " +
            "WHERE building = #{building} AND room_number = #{roomNumber}")
    int update(Classroom classroom);

    @Delete("DELETE FROM classroom WHERE building = #{building} AND room_number = #{roomNumber}")
    int delete(@Param("building") String building, @Param("roomNumber") String roomNumber);

    @Select("SELECT COUNT(*) FROM classroom")
    int count();
}