package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.common.PageResult;
import com.ds.university.entity.Student;
import com.ds.university.entity.TimeSlot;
import com.ds.university.mapper.StudentMapper;
import com.ds.university.mapper.TakesMapper;
import com.ds.university.mapper.TimeSlotMapper;
import com.ds.university.vo.AdvisorVO;
import com.ds.university.vo.CatalogSectionVO;
import com.ds.university.vo.EnrollmentVO;
import com.ds.university.vo.StudentDashboardVO;
import com.ds.university.vo.StudentProfileVO;
import com.ds.university.vo.TranscriptRowVO;
import com.ds.university.vo.SectionVO;
import com.ds.university.vo.TranscriptVO;
import com.ds.university.vo.WeeklyScheduleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 学生中心业务：选课/退课、成绩单、导师 */
@Service
public class StudentService {

    private static final List<String> SEMESTERS = Arrays.asList("Fall", "Spring", "Summer");

    /** 星期排序（周一至周日） */
    private static final List<String> DAY_ORDER = Arrays.asList("M", "T", "W", "TH", "F", "S", "U");

    /** 星期显示名 */
    private static final Map<String, String> DAY_LABELS = new LinkedHashMap<>();

    static {
        DAY_LABELS.put("M", "周一");
        DAY_LABELS.put("T", "周二");
        DAY_LABELS.put("W", "周三");
        DAY_LABELS.put("TH", "周四");
        DAY_LABELS.put("F", "周五");
        DAY_LABELS.put("S", "周六");
        DAY_LABELS.put("U", "周日");
    }

    /** 绩点对照表 */
    private static final Map<String, Double> GRADE_POINTS = new HashMap<>();

    static {
        GRADE_POINTS.put("A+", 4.3);
        GRADE_POINTS.put("A", 4.0);
        GRADE_POINTS.put("A-", 3.7);
        GRADE_POINTS.put("B+", 3.3);
        GRADE_POINTS.put("B", 3.0);
        GRADE_POINTS.put("B-", 2.7);
        GRADE_POINTS.put("C+", 2.3);
        GRADE_POINTS.put("C", 2.0);
        GRADE_POINTS.put("C-", 1.7);
        GRADE_POINTS.put("D+", 1.3);
        GRADE_POINTS.put("D", 1.0);
        GRADE_POINTS.put("F", 0.0);
    }

    private final StudentMapper studentMapper;
    private final TakesMapper takesMapper;
    private final TimeSlotMapper timeSlotMapper;

    public StudentService(StudentMapper studentMapper, TakesMapper takesMapper,
                          TimeSlotMapper timeSlotMapper) {
        this.studentMapper = studentMapper;
        this.takesMapper = takesMapper;
        this.timeSlotMapper = timeSlotMapper;
    }

    /** 学生中心首页 */
    public StudentDashboardVO dashboard(String studentId) {
        Student student = requireStudent(studentId);
        StudentDashboardVO vo = new StudentDashboardVO();
        vo.setStudent(student);
        vo.setAdvisorName(studentMapper.selectAdvisorName(studentId));
        vo.setEnrollmentCount(studentMapper.selectEnrollments(studentId).size());
        TranscriptVO transcript = transcript(studentId);
        vo.setEarnedCredits(transcript.getEarnedCredits());
        vo.setGpa(transcript.getGpa());
        return vo;
    }

    /** 个人基本信息（含导师名） */
    public StudentProfileVO profile(String studentId) {
        Student student = requireStudent(studentId);
        StudentProfileVO vo = new StudentProfileVO();
        vo.setStudent(student);
        vo.setAdvisorName(studentMapper.selectAdvisorName(studentId));
        return vo;
    }

    /** 可选年份（有开课班的年份） */
    public List<Integer> years() {
        return studentMapper.selectDistinctYears();
    }

    /** 我的选课列表 */
    public List<EnrollmentVO> enrollments(String studentId) {
        return studentMapper.selectEnrollments(studentId);
    }

    /** 我的课程表：按 星期 x 时间段 展示本学期已选课程 */
    public WeeklyScheduleVO weeklySchedule(String studentId, String semester, Integer year) {
        validateTerm(semester, year);
        WeeklyScheduleVO vo = new WeeklyScheduleVO();
        vo.setSemester(semester);
        vo.setYear(year);
        vo.setFilterType("all");
        vo.setFilterKey("");
        vo.setFilterLabel("我的课程");

        Map<String, List<TimeSlot>> slotRows = timeSlotMapper.selectAll().stream()
                .collect(Collectors.groupingBy(TimeSlot::getTimeSlotId,
                        LinkedHashMap::new, Collectors.toList()));

        List<EnrollmentVO> enrollments = studentMapper.selectEnrollments(studentId);
        Map<String, List<SectionVO>> cells = new HashMap<>();
        LinkedHashSet<String> daySet = new LinkedHashSet<>();
        LinkedHashSet<String> periodSet = new LinkedHashSet<>();
        for (EnrollmentVO e : enrollments) {
            if (!semester.equals(e.getSemester()) || !year.equals(e.getYear())) {
                continue;
            }
            List<TimeSlot> rows = slotRows.get(e.getTimeSlotId());
            if (rows == null) {
                continue;
            }
            SectionVO section = toSectionVO(e);
            for (TimeSlot ts : rows) {
                daySet.add(ts.getDay());
                periodSet.add(ts.getStartTime() + "-" + ts.getEndTime());
                cells.computeIfAbsent(ts.getDay() + "|" + ts.getStartTime() + "-" + ts.getEndTime(),
                        k -> new ArrayList<>()).add(section);
            }
        }

        List<String> days = new ArrayList<>(daySet);
        days.sort(Comparator.comparingInt(d -> {
            int idx = DAY_ORDER.indexOf(d);
            return idx < 0 ? Integer.MAX_VALUE : idx;
        }));
        Map<String, String> dayLabels = new LinkedHashMap<>();
        for (String d : days) {
            dayLabels.put(d, DAY_LABELS.getOrDefault(d, d));
        }
        List<String> periods = new ArrayList<>(periodSet);
        periods.sort(Comparator.comparing(p -> LocalTime.parse(p.substring(0, 5))));

        vo.setDays(days);
        vo.setDayLabels(dayLabels);
        vo.setPeriods(periods);
        vo.setCells(cells);
        return vo;
    }

    private SectionVO toSectionVO(EnrollmentVO e) {
        SectionVO s = new SectionVO();
        s.setCourseId(e.getCourseId());
        s.setSecId(e.getSecId());
        s.setSemester(e.getSemester());
        s.setYear(e.getYear());
        s.setBuilding(e.getBuilding());
        s.setRoomNumber(e.getRoomNumber());
        s.setTimeSlotId(e.getTimeSlotId());
        s.setCourseTitle(e.getTitle());
        s.setInstructorNames(e.getInstructorNames());
        return s;
    }



    /** 选课目录（某学期/年份的开课班，标记已选） */
    public List<CatalogSectionVO> catalog(String studentId, String semester, Integer year, String courseId) {
        validateTerm(semester, year);
        return studentMapper.selectCatalog(studentId, semester, year, courseId);
    }
    /** 选课目录（分页） */
    public PageResult<CatalogSectionVO> catalogPage(String studentId, String semester, Integer year,
                                                   String courseId, int page, int size) {
        validateTerm(semester, year);
        size = PageResult.normalizeSize(size);
        long total = studentMapper.countCatalog(studentId, semester, year, courseId);
        int safePage = PageResult.clampPage(page, size, total);
        List<CatalogSectionVO> records = studentMapper.selectCatalogPage(
                studentId, semester, year, courseId, (safePage - 1) * size, size);
        return new PageResult<>(records, safePage, size, total);
    }

    /** 选课（事务内锁定开课班行，防并发超选） */
    @Transactional
    public void enroll(String studentId, String courseId, String secId, String semester, Integer year) {
        validateEnrollParams(courseId, secId, semester, year);

        // 1. 开课班必须存在
        List<CatalogSectionVO> sections = studentMapper.selectCatalog(studentId, semester, year, courseId);
        CatalogSectionVO section = sections.stream()
                .filter(s -> secId.equals(s.getSecId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "开课班不存在"));

        // 2. 不能重复选课
        if (takesMapper.exists(studentId, courseId, secId, semester, year) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_ENROLL);
        }

        // 3. 锁定开课班行，容量校验与插入在同一事务中串行执行
        takesMapper.lockSection(courseId, secId, semester, year);

        // 4. 容量检查
        int enrolledCount = takesMapper.countEnrolled(courseId, secId, semester, year);
        if (section.getCapacity() != null && enrolledCount >= section.getCapacity()) {
            throw new BusinessException(ErrorCode.SECTION_FULL);
        }

        // 5. 先修课程检查
        int prereqNotPassed = studentMapper.countPrereqNotPassed(studentId, courseId);
        if (prereqNotPassed > 0) {
            throw new BusinessException(ErrorCode.PREREQ_NOT_DONE);
        }

        // 6. 时间冲突检查（同学期同天时间重叠）
        checkTimeConflict(studentId, courseId, secId, semester, year, section.getTimeSlotId());

        takesMapper.insert(studentId, courseId, secId, semester, year);
    }

    /** 退课（退课后重算已修总学分） */
    @Transactional
    public void drop(String studentId, String courseId, String secId, String semester, Integer year) {
        validateEnrollParams(courseId, secId, semester, year);

        if (takesMapper.exists(studentId, courseId, secId, semester, year) == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未选择该开课班");
        }
        String grade = takesMapper.selectGrade(studentId, courseId, secId, semester, year);
        if (grade != null && !grade.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "已出成绩的课程不能退选");
        }
        takesMapper.delete(studentId, courseId, secId, semester, year);
        studentMapper.recomputeTotCred(studentId);
    }

    /** 成绩单 */
    public TranscriptVO transcript(String studentId) {
        Student student = requireStudent(studentId);
        List<TranscriptRowVO> rows = studentMapper.selectTranscript(studentId);

        int earnedCredits = 0;
        double totalPoints = 0.0;
        int gradedCredits = 0;
        for (TranscriptRowVO row : rows) {
            Double point = GRADE_POINTS.get(row.getGrade());
            if (row.getGrade() != null && !row.getGrade().isEmpty() && point != null) {
                int credits = row.getCredits() == null ? 0 : row.getCredits();
                if (!"F".equals(row.getGrade())) {
                    earnedCredits += credits;
                }
                totalPoints += point * credits;
                gradedCredits += credits;
                row.setGradePoint(point);
            }
        }

        TranscriptVO vo = new TranscriptVO();
        vo.setStudentId(student.getId());
        vo.setStudentName(student.getName());
        vo.setDeptName(student.getDeptName());
        vo.setRows(rows);
        vo.setCourseCount(rows.size());
        vo.setEarnedCredits(earnedCredits);
        vo.setGpa(gradedCredits == 0 ? null : round2(totalPoints / gradedCredits));
        return vo;
    }
    /** 成绩单（分页显示明细，汇总统计仍基于全部记录） */
    public TranscriptVO transcript(String studentId, int page, int size) {
        TranscriptVO vo = transcript(studentId);
        List<TranscriptRowVO> rows = vo.getRows();
        size = PageResult.normalizeSize(size);
        long total = rows == null ? 0 : rows.size();
        int safePage = PageResult.clampPage(page, size, total);
        int from = Math.min((safePage - 1) * size, rows.size());
        int to = Math.min(from + size, rows.size());
        PageResult<TranscriptRowVO> pageResult = new PageResult<>(
                rows.subList(from, to), safePage, size, total);
        vo.setPageResult(pageResult);
        vo.setRows(pageResult.getRecords());
        return vo;
    }

    /** 导师信息 */
    public AdvisorVO advisor(String studentId) {
        AdvisorVO advisor = studentMapper.selectAdvisor(studentId);
        if (advisor == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "尚未分配导师");
        }
        return advisor;
    }

    private Student requireStudent(String studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return student;
    }

    private void validateEnrollParams(String courseId, String secId, String semester, Integer year) {
        if (courseId == null || courseId.isEmpty() || secId == null || secId.isEmpty()
                || semester == null || semester.isEmpty() || year == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "选课参数不完整");
        }
        validateTerm(semester, year);
    }

    private void validateTerm(String semester, Integer year) {
        if (semester == null || semester.isEmpty() || !SEMESTERS.contains(semester)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "学期取值仅支持 Fall / Spring / Summer");
        }
        if (year == null || year <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "年份不合法");
        }
    }

    private void checkTimeConflict(String studentId, String courseId, String secId,
                                   String semester, Integer year, String timeSlotId) {
        List<TimeSlot> newSlots = timeSlotMapper.selectById(timeSlotId);
        if (newSlots.isEmpty()) {
            return;
        }
        List<TimeSlot> enrolledSlots = studentMapper.selectEnrolledTimeSlots(studentId, semester, year);
        for (TimeSlot mine : enrolledSlots) {
            for (TimeSlot target : newSlots) {
                if (mine.getDay().equals(target.getDay())
                        && mine.getStartTime().isBefore(target.getEndTime())
                        && target.getStartTime().isBefore(mine.getEndTime())) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                            "与已选课程时间冲突：" + target.getDay() + " "
                                    + target.getStartTime() + "-" + target.getEndTime());
                }
            }
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}