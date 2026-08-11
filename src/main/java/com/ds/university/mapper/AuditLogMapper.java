package com.ds.university.mapper;

import com.ds.university.entity.AuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 审计日志（audit_log）读写 */
@Mapper
public interface AuditLogMapper {

    @Insert("INSERT INTO audit_log (user_id, action, target_type, target_id, detail, client_ip) " +
            "VALUES (#{userId}, #{action}, #{targetType}, #{targetId}, #{detail}, #{clientIp})")
    int insert(@Param("userId") String userId,
               @Param("action") String action,
               @Param("targetType") String targetType,
               @Param("targetId") String targetId,
               @Param("detail") String detail,
               @Param("clientIp") String clientIp);

    /** 按操作类型/关键字倒序查询（user_id、target_id、detail 模糊匹配），最多返回 limit 条 */
    @Select("<script>" +
            "SELECT id, user_id, action, target_type, target_id, detail, client_ip, created_at " +
            "FROM audit_log " +
            "<where>" +
            "<if test=\"action != null and action != ''\"> AND action = #{action}</if>" +
            "<if test=\"keyword != null and keyword != ''\"> AND (" +
            "user_id LIKE CONCAT('%', #{keyword}, '%') OR " +
            "target_id LIKE CONCAT('%', #{keyword}, '%') OR " +
            "detail LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "</where>" +
            " ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<AuditLog> query(@Param("action") String action,
                         @Param("keyword") String keyword,
                         @Param("limit") int limit);
}
