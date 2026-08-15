/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.entity.Instructor;
import com.ds.university.mapper.InstructorMapper;
import com.ds.university.mapper.StudentMapper;
import com.ds.university.mapper.TakesMapper;
import com.ds.university.mapper.TeacherMapper;
import com.ds.university.vo.RosterRowVO;
import com.ds.university.vo.RosterVO;
import com.ds.university.vo.SectionVO;
import com.ds.university.vo.TeacherDashboardVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

/** 教师中心业务：授课列表、班级名单、成绩录入 */
@Service
@Validated
public class TeacherService {

    /** 合法成绩取值（空串表示未出成绩/清除成绩） */
    private static final List<String> VALID_GRADES = Arrays.asList(
            "A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "F");

    private final InstructorMapper instructorMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final TakesMapper takesMapper;
    private final AuditService auditService;

    public TeacherService(InstructorMapper instructorMapper, StudentMapper studentMapper,
                          TeacherMapper teacherMapper, TakesMapper takesMapper, AuditService auditService) {
        this.instructorMapper = instructorMapper;
        this.studentMapper = studentMapper;
        this.teacherMapper = teacherMapper;
        this.takesMapper = takesMapper;
        this.auditService = auditService;
    }

    /** 教师中心首页：个人信息 + 授课列表 + 统计 */
    public TeacherDashboardVO dashboard(String instructorId) {
        Instructor instructor = instructorMapper.selectById(instructorId);
        if (instructor == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        List<SectionVO> sections = instructorMapper.selectSectionsByInstructor(instructorId);

        TeacherDashboardVO vo = new TeacherDashboardVO();
        vo.setInstructor(instructor);
        vo.setSections(sections);
        vo.setSectionCount(sections.size());
        vo.setStudentCount(teacherMapper.countTeachingStudents(instructorId));
        vo.setGradedCount(teacherMapper.countGradedStudents(instructorId));
        return vo;
    }

    /** 班级名单：校验教师确实讲授该开课班 */
    public RosterVO roster(String instructorId,
                           @NotBlank(message = "开课班参数不完整") String courseId,
                           @NotBlank(message = "开课班参数不完整") String secId,
                           @NotBlank(message = "开课班参数不完整") String semester,
                           @NotNull(message = "开课班参数不完整") Integer year) {
        requireTeaching(instructorId, courseId, secId, semester, year);
        RosterVO vo = new RosterVO();
        vo.setSection(findSection(instructorId, courseId, secId, semester, year));
        vo.setRows(teacherMapper.selectRoster(courseId, secId, semester, year));
        return vo;
    }

    /** 录入/修改成绩（grade 为空串表示清除成绩；成绩变化后重算学生已修学分） */
    @Transactional
    public void updateGrade(String instructorId,
                            @NotBlank(message = "学生编号不能为空") String studentId,
                            @NotBlank(message = "开课班参数不完整") String courseId,
                            @NotBlank(message = "开课班参数不完整") String secId,
                            @NotBlank(message = "开课班参数不完整") String semester,
                            @NotNull(message = "开课班参数不完整") Integer year,
                            String grade) {
        requireTeaching(instructorId, courseId, secId, semester, year);
        String normalized = grade == null ? null : grade.trim();
        if (normalized != null && normalized.isEmpty()) {
            normalized = null;
        }
        if (normalized != null && !VALID_GRADES.contains(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_GRADE);
        }
        String oldGrade = takesMapper.selectGrade(studentId, courseId, secId, semester, year);
        teacherMapper.updateGrade(studentId, courseId, secId, semester, year, normalized);
        studentMapper.recomputeTotCred(studentId);
        auditService.record(AuditService.ACTION_GRADE_UPDATE, AuditService.TARGET_GRADE,
                studentId + "/" + courseId + "/" + secId + "/" + semester + "/" + year,
                "教师 " + instructorId + " 修改成绩：学生 " + studentId + "，课程 " + courseId
                        + "（班 " + secId + "，" + semester + " " + year + "）："
                        + display(oldGrade) + " -> " + display(normalized));
    }

    /** 成绩展示：null 显示为「无」，便于审计详情阅读 */
    private static String display(String grade) {
        return grade == null ? "无" : grade;
    }

    /** 校验该开课班确实是当前教师的授课（格式类约束已由方法参数注解接管） */
    private void requireTeaching(String instructorId, String courseId, String secId,
                                 String semester, Integer year) {
        if (teacherMapper.countTeaches(instructorId, courseId, secId, semester, year) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该开课班不是当前教师的授课");
        }
    }

    private SectionVO findSection(String instructorId, String courseId, String secId,
                                  String semester, Integer year) {
        return instructorMapper.selectSectionsByInstructor(instructorId).stream()
                .filter(s -> courseId.equals(s.getCourseId())
                        && secId.equals(s.getSecId())
                        && semester.equals(s.getSemester())
                        && year.equals(s.getYear()))
                .findFirst()
                .orElse(null);
    }
}