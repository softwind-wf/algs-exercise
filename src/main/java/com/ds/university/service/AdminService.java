package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.entity.Classroom;
import com.ds.university.entity.Course;
import com.ds.university.entity.Department;
import com.ds.university.entity.Instructor;
import com.ds.university.entity.Section;
import com.ds.university.entity.TimeSlot;
import com.ds.university.entity.Student;
import com.ds.university.mapper.ClassroomMapper;
import com.ds.university.mapper.CourseMapper;
import com.ds.university.mapper.DepartmentMapper;
import com.ds.university.mapper.InstructorMapper;
import com.ds.university.mapper.PrereqMapper;
import com.ds.university.mapper.SectionMapper;
import com.ds.university.mapper.StudentMapper;
import com.ds.university.mapper.TeachesMapper;
import com.ds.university.mapper.TimeSlotMapper;
import com.ds.university.vo.PrereqVO;
import com.ds.university.vo.SchedulingBoardVO;
import com.ds.university.vo.WeeklyScheduleVO;
import com.ds.university.vo.TimeSlotVO;
import com.ds.university.vo.SectionVO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 教务管理业务：基础数据维护、排课、先修管理。
 *  参数格式类校验由 Bean Validation 声明式拦截（约束违例统一转译为 BusinessException）。 */
@Service
@Validated
public class AdminService {

    private static final List<String> SEMESTERS = Arrays.asList("Fall", "Spring", "Summer");
    private static final List<String> DAY_ORDER = Arrays.asList("M", "W", "F");

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

    private final DepartmentMapper departmentMapper;
    private final CourseMapper courseMapper;
    private final InstructorMapper instructorMapper;
    private final StudentMapper studentMapper;
    private final ClassroomMapper classroomMapper;
    private final SectionMapper sectionMapper;
    private final TimeSlotMapper timeSlotMapper;
    private final TeachesMapper teachesMapper;
    private final PrereqMapper prereqMapper;
    private final AccountService accountService;
    private final AuditService auditService;

    public AdminService(DepartmentMapper departmentMapper, CourseMapper courseMapper,
                        InstructorMapper instructorMapper, StudentMapper studentMapper,
                        ClassroomMapper classroomMapper, SectionMapper sectionMapper,
                        TimeSlotMapper timeSlotMapper, TeachesMapper teachesMapper,
                        PrereqMapper prereqMapper, AccountService accountService,
                        AuditService auditService) {
        this.departmentMapper = departmentMapper;
        this.courseMapper = courseMapper;
        this.instructorMapper = instructorMapper;
        this.studentMapper = studentMapper;
        this.classroomMapper = classroomMapper;
        this.sectionMapper = sectionMapper;
        this.timeSlotMapper = timeSlotMapper;
        this.teachesMapper = teachesMapper;
        this.prereqMapper = prereqMapper;
        this.accountService = accountService;
        this.auditService = auditService;
    }

    // ========== 基础数据：院系 ==========

    public List<Department> departments() {
        return departmentMapper.selectAllSimple();
    }

    public void createDepartment(@NotBlank(message = "系名不能为空") String deptName,
                                 String building,
                                 @DecimalMin(value = "0", message = "预算不能为负") BigDecimal budget) {
        try {
            departmentMapper.insert(new Department(deptName, building, budget));
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该系已存在");
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "院系数据不合法");
        }
        auditService.record(AuditService.ACTION_CREATE, AuditService.TARGET_DEPARTMENT, deptName,
                "新建院系：" + deptName + "，楼字 " + building + "，预算 " + budget);
    }

    public void updateDepartment(@NotBlank(message = "系名不能为空") String deptName,
                                 String building,
                                 @DecimalMin(value = "0", message = "预算不能为负") BigDecimal budget) {
        departmentMapper.update(new Department(deptName, building, budget));
        auditService.record(AuditService.ACTION_UPDATE, AuditService.TARGET_DEPARTMENT, deptName,
                "更新院系：" + deptName + "，楼字 " + building + "，预算 " + budget);
    }

    public void deleteDepartment(String deptName) {
        try {
            departmentMapper.delete(deptName);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该系下存在课程/教师/学生，无法删除");
        }
        auditService.record(AuditService.ACTION_DELETE, AuditService.TARGET_DEPARTMENT, deptName,
                "删除院系：" + deptName);
    }

    // ========== 基础数据：课程 ==========

    public List<Course> courses() {
        return courseMapper.selectAll(null, null);
    }

    public void createCourse(@NotBlank(message = "课程号不能为空") @Size(max = 8, message = "课程号不能超过 8 个字符") String courseId,
                             @NotBlank(message = "课程名不能为空") String title,
                             @NotBlank(message = "所属院系不能为空") String deptName,
                             @NotNull(message = "学分必须大于 0") @DecimalMin(value = "0", inclusive = false, message = "学分必须大于 0") BigDecimal credits) {
        if (departmentMapper.selectById(deptName) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "所属院系不存在");
        }
        try {
            courseMapper.insert(new Course(courseId, title, deptName, credits));
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该课程号已存在");
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "课程数据不合法");
        }
        auditService.record(AuditService.ACTION_CREATE, AuditService.TARGET_COURSE, courseId,
                "新建课程：" + courseId + " " + title + "（" + deptName + "，" + credits + " 学分）");
    }

    public void updateCourse(@NotBlank(message = "课程号不能为空") @Size(max = 8, message = "课程号不能超过 8 个字符") String courseId,
                             @NotBlank(message = "课程名不能为空") String title,
                             @NotBlank(message = "所属院系不能为空") String deptName,
                             @NotNull(message = "学分必须大于 0") @DecimalMin(value = "0", inclusive = false, message = "学分必须大于 0") BigDecimal credits) {
        if (departmentMapper.selectById(deptName) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "所属院系不存在");
        }
        courseMapper.update(new Course(courseId, title, deptName, credits));
        auditService.record(AuditService.ACTION_UPDATE, AuditService.TARGET_COURSE, courseId,
                "更新课程：" + courseId + " " + title + "（" + deptName + "，" + credits + " 学分）");
    }

    public void deleteCourse(String courseId) {
        try {
            courseMapper.delete(courseId);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该课程存在开课班/选课/先修关联，无法删除");
        }
        auditService.record(AuditService.ACTION_DELETE, AuditService.TARGET_COURSE, courseId,
                "删除课程：" + courseId);
    }

    // ========== 基础数据：教师 ==========

    public List<Instructor> instructors() {
        return instructorMapper.selectAll(null);
    }

@Transactional
    public void createInstructor(@NotBlank(message = "工号不能为空") @Size(max = 5, message = "工号不能超过 5 个字符") String id,
                                 @NotBlank(message = "姓名不能为空") String name,
                                 @NotBlank(message = "所属院系不能为空") String deptName,
                                 @DecimalMin(value = "0", message = "工资不能为负") BigDecimal salary) {
        if (departmentMapper.selectById(deptName) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "所属院系不存在");
        }
        try {
            instructorMapper.insert(new Instructor(id, name, deptName, salary));
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该工号已存在");
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "教师数据不合法");
        }
        accountService.createAccount(id, "INSTRUCTOR", id, null);
        auditService.record(AuditService.ACTION_CREATE, AuditService.TARGET_INSTRUCTOR, id,
                "新建教师：" + id + " " + name + "（" + deptName + "）");
    }

    public void updateInstructor(@NotBlank(message = "工号不能为空") @Size(max = 5, message = "工号不能超过 5 个字符") String id,
                                 @NotBlank(message = "姓名不能为空") String name,
                                 @NotBlank(message = "所属院系不能为空") String deptName,
                                 @DecimalMin(value = "0", message = "工资不能为负") BigDecimal salary) {
        if (departmentMapper.selectById(deptName) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "所属院系不存在");
        }
        instructorMapper.update(new Instructor(id, name, deptName, salary));
        auditService.record(AuditService.ACTION_UPDATE, AuditService.TARGET_INSTRUCTOR, id,
                "更新教师：" + id + " " + name + "（" + deptName + "，工资 " + salary + "）");
    }

@Transactional
    public void deleteInstructor(String id) {
        try {
            instructorMapper.delete(id);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该教师存在授课/指导关联，无法删除");
        }
        accountService.deleteAccount(id);
        auditService.record(AuditService.ACTION_DELETE, AuditService.TARGET_INSTRUCTOR, id,
                "删除教师：" + id + "（含登录账号）");
    }

    // ========== 基础数据：学生 ==========

    public List<Student> students() {
        return studentMapper.selectAllSimple();
    }

@Transactional
    public void createStudent(@NotBlank(message = "学号不能为空") @Size(max = 5, message = "学号不能超过 5 个字符") String id,
                              @NotBlank(message = "姓名不能为空") String name,
                              @NotBlank(message = "所属院系不能为空") String deptName,
                              @Min(value = 0, message = "已修学分不能为负") Integer totCred) {
        if (departmentMapper.selectById(deptName) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "所属院系不存在");
        }
        try {
            studentMapper.insert(new Student(id, name, deptName, totCred));
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该学号已存在");
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "学生数据不合法");
        }
        accountService.createAccount(id, "STUDENT", id, null);
        auditService.record(AuditService.ACTION_CREATE, AuditService.TARGET_STUDENT, id,
                "新建学生：" + id + " " + name + "（" + deptName + "）");
    }

    public void updateStudent(@NotBlank(message = "学号不能为空") @Size(max = 5, message = "学号不能超过 5 个字符") String id,
                              @NotBlank(message = "姓名不能为空") String name,
                              @NotBlank(message = "所属院系不能为空") String deptName,
                              @Min(value = 0, message = "已修学分不能为负") Integer totCred) {
        if (departmentMapper.selectById(deptName) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "所属院系不存在");
        }
        studentMapper.update(new Student(id, name, deptName, totCred));
        auditService.record(AuditService.ACTION_UPDATE, AuditService.TARGET_STUDENT, id,
                "更新学生：" + id + " " + name + "（" + deptName + "，已修学分 " + totCred + "）");
    }

@Transactional
    public void deleteStudent(String id) {
        try {
            studentMapper.delete(id);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该学生存在选课/导师关联，无法删除");
        }
        accountService.deleteAccount(id);
        auditService.record(AuditService.ACTION_DELETE, AuditService.TARGET_STUDENT, id,
                "删除学生：" + id + "（含登录账号）");
    }

    // ========== 基础数据：教室 ==========

    public List<Classroom> classrooms() {
        return classroomMapper.selectAll();
    }

    public void createClassroom(@NotBlank(message = "教学楼不能为空") String building,
                                @NotBlank(message = "教室号不能为空") String roomNumber,
                                @NotNull(message = "容量必须大于 0") @Min(value = 1, message = "容量必须大于 0") Integer capacity) {
        try {
            classroomMapper.insert(new Classroom(building, roomNumber, capacity));
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该教室已存在");
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "教室数据不合法");
        }
        auditService.record(AuditService.ACTION_CREATE, AuditService.TARGET_CLASSROOM,
                building + "/" + roomNumber,
                "新建教室：" + building + " " + roomNumber + "，容量 " + capacity);
    }

    public void updateClassroom(@NotBlank(message = "教学楼不能为空") String building,
                                @NotBlank(message = "教室号不能为空") String roomNumber,
                                @NotNull(message = "容量必须大于 0") @Min(value = 1, message = "容量必须大于 0") Integer capacity) {
        classroomMapper.update(new Classroom(building, roomNumber, capacity));
        auditService.record(AuditService.ACTION_UPDATE, AuditService.TARGET_CLASSROOM,
                building + "/" + roomNumber,
                "更新教室：" + building + " " + roomNumber + "，容量 " + capacity);
    }

    public void deleteClassroom(String building, String roomNumber) {
        try {
            classroomMapper.delete(building, roomNumber);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该教室存在排课引用，无法删除");
        }
        auditService.record(AuditService.ACTION_DELETE, AuditService.TARGET_CLASSROOM,
                building + "/" + roomNumber,
                "删除教室：" + building + " " + roomNumber);
    }

    // ========== 排课：开课班 ==========

    public List<SectionVO> sections() {
        return sectionMapper.selectAll(null, null, null);
    }

    public Section sectionById(String courseId, String secId, String semester, Integer year) {
        return sectionMapper.selectById(courseId, secId, semester, year);
    }

    public List<String> instructorIdsOfSection(String courseId, String secId, String semester, Integer year) {
        return teachesMapper.selectInstructorIds(courseId, secId, semester, year);
    }

    public List<Course> allCourses() {
        return courseMapper.selectAll(null, null);
    }

    public List<Instructor> allInstructors() {
        return instructorMapper.selectAll(null);
    }

    public List<String> timeSlotIds() {
        return timeSlotMapper.selectDistinctIds();
    }

    /** 排课时间段选项（含具体时段描述，便于识别时间重叠） */
    public List<TimeSlotVO> timeSlotOptions() {
        Map<String, List<TimeSlot>> groups = timeSlotMapper.selectAll().stream()
                .collect(Collectors.groupingBy(TimeSlot::getTimeSlotId,
                        LinkedHashMap::new, Collectors.toList()));
        List<TimeSlotVO> options = new ArrayList<>();
        for (Map.Entry<String, List<TimeSlot>> e : groups.entrySet()) {
            String label = e.getKey() + " · " + e.getValue().stream()
                    .sorted(Comparator.comparingInt(s -> DAY_ORDER.indexOf(s.getDay())))
                    .map(s -> s.getDay() + " " + s.getStartTime() + "-" + s.getEndTime())
                    .collect(Collectors.joining(" / "));
            TimeSlotVO vo = new TimeSlotVO();
            vo.setTimeSlotId(e.getKey());
            vo.setLabel(label);
            options.add(vo);
        }
        return options;
    }

    public void createSection(@Valid Section section, String instructorId) {
        validateSection(section, instructorId);
        try {
            sectionMapper.insert(section);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该开课班已存在");
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "排课数据不合法（请检查教室/时间段是否存在）");
        }
        assignInstructor(section, instructorId);
        auditService.record(AuditService.ACTION_CREATE, AuditService.TARGET_SECTION, sectionKey(section),
                "新建开课班：" + sectionDesc(section) + "，授课教师 " + orNone(instructorId));
    }

    public void updateSection(@Valid Section section, String instructorId) {
        validateSection(section, instructorId);
        sectionMapper.update(section);
        assignInstructor(section, instructorId);
        auditService.record(AuditService.ACTION_UPDATE, AuditService.TARGET_SECTION, sectionKey(section),
                "更新开课班：" + sectionDesc(section) + "，授课教师 " + orNone(instructorId));
    }

    public void deleteSection(String courseId, String secId, String semester, Integer year) {
        teachesMapper.deleteBySection(courseId, secId, semester, year);
        try {
            sectionMapper.delete(courseId, secId, semester, year);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该开课班已有学生选课，无法删除");
        }
        auditService.record(AuditService.ACTION_DELETE, AuditService.TARGET_SECTION,
                courseId + "/" + secId + "/" + semester + "/" + year,
                "删除开课班：课程 " + courseId + "，班 " + secId + "，" + semester + " " + year);
    }

    // ========== 排课看板（拖拽排课） ==========

    public List<Integer> sectionYears() {
        return sectionMapper.selectYears();
    }

    /** 排课看板数据：教室 x 时间段二维表格 + 待排课班列表 */
    public SchedulingBoardVO schedulingBoard(String semester, Integer year) {
        SchedulingBoardVO vo = new SchedulingBoardVO();
        vo.setSemester(semester);
        vo.setYear(year);
        vo.setSemesters(SEMESTERS);
        vo.setYears(sectionYears());
        vo.setClassrooms(classrooms());
        vo.setTimeSlots(timeSlotOptions());
        List<SectionVO> termSections = sectionMapper.selectAll(semester, year, null);
        List<SectionVO> pending = new ArrayList<>();
        Map<String, SectionVO> cellMap = new HashMap<>();
        for (SectionVO s : termSections) {
            if (s.getBuilding() == null || s.getBuilding().isEmpty()) {
                pending.add(s);
            } else {
                cellMap.put(s.getBuilding() + "|" + s.getRoomNumber() + "|" + s.getTimeSlotId(), s);
            }
        }
        vo.setPendingSections(pending);
        vo.setCellMap(cellMap);
        vo.setTimeSlotDays(timeSlotDays());
        vo.setTeacherLoad(teacherLoad(termSections));
        return vo;
    }

    /** 创建暂未排课的开课班（教室/时间段留空，等待拖拽排课） */
    public void createUnassignedSection(@Valid Section section, String instructorId) {
        if (courseMapper.selectById(section.getCourseId()) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "课程不存在");
        }
        section.setBuilding(null);
        section.setRoomNumber(null);
        section.setTimeSlotId(null);
        try {
            sectionMapper.insert(section);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该开课班已存在");
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开课班数据不合法");
        }
        assignInstructor(section, instructorId);
        auditService.record(AuditService.ACTION_CREATE, AuditService.TARGET_SECTION, sectionKey(section),
                "新建待排课开课班：" + sectionDesc(section) + "，授课教师 " + orNone(instructorId));
    }

    /** 拖拽排课：把开课班放入指定教室+时间段（含冲突校验），未指定教师则保留原教师 */
    public void assignSchedule(@Valid Section section, String instructorId) {
        if (sectionMapper.selectById(section.getCourseId(), section.getSecId(),
                section.getSemester(), section.getYear()) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开课班不存在");
        }
        validateSection(section, instructorId);
        sectionMapper.update(section);
        if (instructorId != null && !instructorId.isEmpty()) {
            assignInstructor(section, instructorId);
        }
        auditService.record(AuditService.ACTION_UPDATE, AuditService.TARGET_SECTION, sectionKey(section),
                "排课：" + sectionDesc(section) + "，授课教师 " + orNone(instructorId));
    }

    /** 拖拽回待排课区：取消该开课班的教室/时间段（保留教师与选课） */
    public void unassignSchedule(String courseId, String secId, String semester, Integer year) {
        if (sectionMapper.selectById(courseId, secId, semester, year) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开课班不存在");
        }
        Section section = new Section();
        section.setCourseId(courseId);
        section.setSecId(secId);
        section.setSemester(semester);
        section.setYear(year);
        section.setBuilding(null);
        section.setRoomNumber(null);
        section.setTimeSlotId(null);
        sectionMapper.update(section);
        auditService.record(AuditService.ACTION_UPDATE, AuditService.TARGET_SECTION,
                courseId + "/" + secId + "/" + semester + "/" + year,
                "取消排课：课程 " + courseId + "，班 " + secId + "，" + semester + " " + year + "（教室/时间段清空）");
    }
    /** 时间段标识 -> 具体时段列表（如 A -> ["M 08:00-08:50", "W 08:00-08:50", "F 09:00-09:50"]） */
    private Map<String, List<String>> timeSlotDays() {
        Map<String, List<String>> days = new LinkedHashMap<>();
        Map<String, List<TimeSlot>> groups = timeSlotMapper.selectAll().stream()
                .collect(Collectors.groupingBy(TimeSlot::getTimeSlotId,
                        LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<TimeSlot>> e : groups.entrySet()) {
            List<String> labels = e.getValue().stream()
                    .sorted(Comparator.comparingInt(s -> DAY_ORDER.indexOf(s.getDay())))
                    .map(s -> s.getDay() + " " + s.getStartTime() + "-" + s.getEndTime())
                    .collect(Collectors.toList());
            days.put(e.getKey(), labels);
        }
        return days;
    }

    /** 教师编号 -> 本学期授课列表（courseId|secId|timeSlotId），供前端做教师冲突预判 */
    private Map<String, List<String>> teacherLoad(List<SectionVO> termSections) {
        Map<String, List<String>> load = new HashMap<>();
        for (SectionVO s : termSections) {
            if (s.getInstructorId() == null || s.getInstructorId().isEmpty()) { continue; }
            if (s.getBuilding() == null || s.getBuilding().isEmpty()) { continue; } // 未排课的班不占时段
            load.computeIfAbsent(s.getInstructorId(), k -> new ArrayList<>())
                    .add(s.getCourseId() + "|" + s.getSecId() + "|" + s.getTimeSlotId());
        }
        return load;
    }

    /** 周课表：按 星期 x 时段 展示本学期排课，可按教室/教师筛选 */
    public WeeklyScheduleVO weeklySchedule(String semester, Integer year, String type, String key) {
        WeeklyScheduleVO vo = new WeeklyScheduleVO();
        vo.setSemester(semester);
        vo.setYear(year);
        String typeNorm = (type == null || type.isEmpty()) ? "all" : type;
        String keyNorm = key == null ? "" : key;
        vo.setFilterType(typeNorm);
        vo.setFilterKey(keyNorm);

        List<TimeSlot> allSlots = timeSlotMapper.selectAll();
        Map<String, List<TimeSlot>> slotRows = allSlots.stream()
                .collect(Collectors.groupingBy(TimeSlot::getTimeSlotId,
                        LinkedHashMap::new, Collectors.toList()));

        // 星期列表与显示名
        LinkedHashSet<String> daySet = new LinkedHashSet<>();
        allSlots.forEach(t -> daySet.add(t.getDay()));
        List<String> days = new ArrayList<>(daySet);
        days.sort(Comparator.comparingInt(d -> {
            int idx = DAY_ORDER.indexOf(d);
            return idx < 0 ? Integer.MAX_VALUE : idx;
        }));
        Map<String, String> dayLabels = new LinkedHashMap<>();
        for (String d : days) {
            dayLabels.put(d, DAY_LABELS.getOrDefault(d, d));
        }
        vo.setDays(days);
        vo.setDayLabels(dayLabels);

        // 时段（去重后按开始时间排序）
        LinkedHashSet<String> periodSet = new LinkedHashSet<>();
        allSlots.forEach(t -> periodSet.add(t.getStartTime() + "-" + t.getEndTime()));
        List<String> periods = new ArrayList<>(periodSet);
        periods.sort(Comparator.comparing((String p) -> LocalTime.parse(p.substring(0, 5))));
        vo.setPeriods(periods);

        // 筛选开课班并填充格子
        List<SectionVO> termSections = sectionMapper.selectAll(semester, year, null);
        Map<String, List<SectionVO>> cells = new HashMap<>();
        for (SectionVO s : termSections) {
            if (s.getBuilding() == null || s.getBuilding().isEmpty()) { continue; }
            if ("room".equals(typeNorm)) {
                if (!(s.getBuilding() + "|" + s.getRoomNumber()).equals(keyNorm)) { continue; }
            } else if ("instructor".equals(typeNorm)) {
                if (s.getInstructorId() == null || !s.getInstructorId().equals(keyNorm)) { continue; }
            }
            List<TimeSlot> rows = slotRows.get(s.getTimeSlotId());
            if (rows == null) { continue; }
            for (TimeSlot ts : rows) {
                cells.computeIfAbsent(ts.getDay() + "|" + ts.getStartTime() + "-" + ts.getEndTime(),
                        k -> new ArrayList<>()).add(s);
            }
        }
        vo.setCells(cells);
        vo.setClassrooms(classrooms());
        vo.setInstructors(allInstructors());

        // 筛选描述
        if ("room".equals(typeNorm)) {
            int idx = keyNorm.indexOf('|');
            vo.setFilterLabel(idx >= 0 ? keyNorm.substring(0, idx) + " " + keyNorm.substring(idx + 1) : keyNorm);
        } else if ("instructor".equals(typeNorm)) {
            String label = keyNorm;
            for (Instructor i : vo.getInstructors()) {
                if (i.getId().equals(keyNorm)) {
                    label = i.getId() + " - " + i.getName();
                    break;
                }
            }
            vo.setFilterLabel(label);
        } else {
            vo.setFilterLabel("全部");
        }
        return vo;
    }
    /** 开课班业务校验：主键格式约束已由 Section 实体上的 Bean Validation 注解接管，
     *  此处仅保留依赖数据库的业务校验（存在性/冲突/容量） */
    private void validateSection(Section section, String instructorId) {
        if (courseMapper.selectById(section.getCourseId()) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "课程不存在");
        }
        if (section.getTimeSlotId() == null || section.getTimeSlotId().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择时间段");
        }
        if (!timeSlotMapper.selectDistinctIds().contains(section.getTimeSlotId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "时间段不存在");
        }
        if (section.getBuilding() == null || section.getBuilding().isEmpty()
                || section.getRoomNumber() == null || section.getRoomNumber().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择教室");
        }
        Classroom room = classroomMapper.selectById(section.getBuilding(), section.getRoomNumber());
        if (room == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "教室不存在");
        }
        // 空间 + 时间冲突：同一教室同一时段（含部分重叠）只能有一个开课班
        int roomConflict = sectionMapper.countRoomTimeConflict(section.getCourseId(), section.getSecId(),
                section.getSemester(), section.getYear(),
                section.getBuilding(), section.getRoomNumber(), section.getTimeSlotId());
        if (roomConflict > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该教室在此时段已被其他开课班占用（含时间重叠）");
        }
        // 教师时间冲突：同一教师同一时段（含部分重叠）不能同时上两个班
        if (instructorId != null && !instructorId.isEmpty()) {
            if (instructorMapper.selectById(instructorId) == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "授课教师不存在");
            }
            int teacherConflict = sectionMapper.countInstructorTimeConflict(instructorId,
                    section.getCourseId(), section.getSecId(),
                    section.getSemester(), section.getYear(), section.getTimeSlotId());
            if (teacherConflict > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "该教师在此时段已有其他授课安排（含时间重叠）");
            }
        }
        // 容量校验：教室容量不得小于该开课班当前已选人数
        int enrolled = sectionMapper.countEnrolled(section.getCourseId(), section.getSecId(),
                section.getSemester(), section.getYear());
        if (room.getCapacity() < enrolled) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "教室容量不足：该开课班已选 " + enrolled + " 人，教室容量 " + room.getCapacity());
        }
    }

    private void assignInstructor(Section section, String instructorId) {
        teachesMapper.deleteBySection(section.getCourseId(), section.getSecId(),
                section.getSemester(), section.getYear());
        if (instructorId != null && !instructorId.isEmpty()) {
            if (instructorMapper.selectById(instructorId) == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "授课教师不存在");
            }
            teachesMapper.insert(instructorId, section.getCourseId(), section.getSecId(),
                    section.getSemester(), section.getYear());
        }
    }

    // ========== 先修管理 ==========

    public List<PrereqVO> prereqs() {
        return prereqMapper.selectAll();
    }

    public void addPrereq(@NotBlank(message = "课程不能为空") String courseId,
                          @NotBlank(message = "先修课程不能为空") String prereqId) {
        if (courseId.equals(prereqId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "课程不能作为自己的先修");
        }
        if (courseMapper.selectById(courseId) == null || courseMapper.selectById(prereqId) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "课程不存在");
        }
        if (prereqMapper.exists(courseId, prereqId) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该先修关系已存在");
        }
        if (wouldCreateCycle(courseId, prereqId)) {
            throw new BusinessException(ErrorCode.PREREQ_CYCLE);
        }
        try {
            prereqMapper.insert(courseId, prereqId);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该先修关系已存在");
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "先修数据不合法");
        }
        auditService.record(AuditService.ACTION_CREATE, AuditService.TARGET_PREREQ,
                courseId + "/" + prereqId,
                "新增先修关系：" + courseId + " 依赖 " + prereqId);
    }

    public void deletePrereq(String courseId, String prereqId) {
        prereqMapper.delete(courseId, prereqId);
        auditService.record(AuditService.ACTION_DELETE, AuditService.TARGET_PREREQ,
                courseId + "/" + prereqId,
                "删除先修关系：" + courseId + " 依赖 " + prereqId);
    }

    /** 新增先修 (courseId 依赖 prereqId)：若 prereqId 传递依赖 courseId 则成环 */
    private boolean wouldCreateCycle(String courseId, String prereqId) {
        Set<String> visited = new HashSet<>();
        return hasPathTo(prereqId, courseId, visited);
    }

    private boolean hasPathTo(String from, String target, Set<String> visited) {
        if (from.equals(target)) {
            return true;
        }
        if (!visited.add(from)) {
            return false;
        }
        for (Course prereq : courseMapper.selectPrereqs(from)) {
            if (hasPathTo(prereq.getCourseId(), target, visited)) {
                return true;
            }
        }
        return false;
    }

    // ========== 首页统计 ==========

    public long countDepartments() { return departmentMapper.count(); }
    public long countCourses() { return courseMapper.count(); }
    public long countInstructors() { return instructorMapper.count(); }
    public long countStudents() { return studentMapper.count(); }
    public long countSections() { return sectionMapper.count(); }
    public long countClassrooms() { return classroomMapper.count(); }
    public long countPrereqs() { return prereqMapper.count(); }
    // ========== 基础数据查询辅助 ==========
    public Department departmentById(String deptName) { return departmentMapper.selectById(deptName); }
    public Course courseById(String courseId) { return courseMapper.selectById(courseId); }
    public Instructor instructorById(String id) { return instructorMapper.selectById(id); }
    public Student studentById(String id) { return studentMapper.selectById(id); }
    public Classroom classroomById(String building, String roomNumber) { return classroomMapper.selectById(building, roomNumber); }
    /** 排课用的教学楼列表（去重） */
    public List<String> buildingNames() {
        return classrooms().stream().map(Classroom::getBuilding)
                .distinct().sorted().collect(Collectors.toList());
    }

    /** 开课班审计标识：组合键 */
    private static String sectionKey(Section s) {
        return s.getCourseId() + "/" + s.getSecId() + "/" + s.getSemester() + "/" + s.getYear();
    }

    /** 开课班审计描述 */
    private static String sectionDesc(Section s) {
        return "课程 " + s.getCourseId() + "，班 " + s.getSecId() + "，" + s.getSemester() + " " + s.getYear()
                + "，教室 " + orNone(s.getBuilding()) + " " + orNone(s.getRoomNumber())
                + "，时段 " + orNone(s.getTimeSlotId());
    }

    private static String orNone(String v) {
        return v == null || v.isEmpty() ? "无" : v;
    }
}