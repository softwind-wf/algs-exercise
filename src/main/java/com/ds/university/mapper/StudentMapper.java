package com.ds.university.mapper;

import com.ds.university.entity.Student;
import com.ds.university.entity.TimeSlot;
import com.ds.university.vo.AdvisorVO;
import com.ds.university.vo.CatalogSectionVO;
import com.ds.university.vo.EnrollmentVO;
import com.ds.university.vo.TranscriptRowVO;
import com.ds.university.vo.TranscriptSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 学生 */
@Mapper
public interface StudentMapper {

    Student selectById(@Param("id") String id);

    /** 学生导师姓名 */
    String selectAdvisorName(@Param("id") String id);

    /** 导师信息 */
    AdvisorVO selectAdvisor(@Param("id") String id);

    /** 已选开课班列表 */
    List<EnrollmentVO> selectEnrollments(@Param("id") String id);

    /** 成绩单明细 */
    List<TranscriptRowVO> selectTranscript(@Param("id") String id);

    /** 成绩单明细总数（分页使用） */
    long countTranscript(@Param("id") String id);

    /** 成绩单明细分页（含绩点换算） */
    List<TranscriptRowVO> selectTranscriptPage(@Param("id") String id,
                                               @Param("offset") int offset,
                                               @Param("size") int size);

    /** 成绩单聚合统计：已修学分 / 加权总绩点 / 已出成绩学分 */
    TranscriptSummaryVO selectTranscriptSummary(@Param("id") String id);

    /** 选课目录：某学期/年份的开课班，标记当前学生是否已选 */
    List<CatalogSectionVO> selectCatalog(@Param("id") String id,
                                         @Param("semester") String semester,
                                         @Param("year") Integer year,
                                         @Param("courseId") String courseId);

    /** 选课目录数量（分页使用） */
    long countCatalog(@Param("id") String id,
                      @Param("semester") String semester,
                      @Param("year") Integer year,
                      @Param("courseId") String courseId);

    /** 选课目录分页 */
    List<CatalogSectionVO> selectCatalogPage(@Param("id") String id,
                                             @Param("semester") String semester,
                                             @Param("year") Integer year,
                                             @Param("courseId") String courseId,
                                             @Param("offset") int offset,
                                             @Param("size") int size);

    /** 时间冲突检测：查找与学生已选课程同天重叠的新时间段（至多返回一条） */
    TimeSlot selectConflictingTimeSlot(@Param("id") String id,
                                       @Param("semester") String semester,
                                       @Param("year") Integer year,
                                       @Param("timeSlotId") String timeSlotId);

    /** 某课程中该学生尚未通过的先修课数量 */
    int countPrereqNotPassed(@Param("id") String id, @Param("courseId") String courseId);

    /** 有开课班的年份（从新到旧） */
    List<Integer> selectDistinctYears();

    int count();

    /** 全部学生（简单列表，用于后台管理） */
    List<Student> selectAllSimple();


    /** 按已通过成绩重算已修总学分（成绩录入/清除、退课后调用） */
    int recomputeTotCred(@Param("id") String id);

    /** 新增学生 */
    int insert(Student student);

    /** 修改学生 */
    int update(Student student);

    /** 删除学生 */
    int delete(@Param("id") String id);}