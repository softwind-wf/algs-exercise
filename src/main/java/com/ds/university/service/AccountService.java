package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.entity.Instructor;
import com.ds.university.entity.Student;
import com.ds.university.entity.SysUser;
import com.ds.university.mapper.InstructorMapper;
import com.ds.university.mapper.StudentMapper;
import com.ds.university.mapper.SysUserMapper;
import com.ds.university.vo.SysUserVO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 账号管理：为学生/教师开户、重置密码、启用禁用。 */
@Service
public class AccountService {

    /** 系统默认初始密码 */
    public static final String DEFAULT_PASSWORD = "password";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SysUserMapper sysUserMapper;
    private final StudentMapper studentMapper;
    private final InstructorMapper instructorMapper;
    private final AuditService auditService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AccountService(SysUserMapper sysUserMapper, StudentMapper studentMapper,
                          InstructorMapper instructorMapper, AuditService auditService) {
        this.sysUserMapper = sysUserMapper;
        this.studentMapper = studentMapper;
        this.instructorMapper = instructorMapper;
        this.auditService = auditService;
    }

    public long countAccounts() {
        return sysUserMapper.count();
    }

    /** 全部账号（含关联人员信息） */
    public List<SysUserVO> listAccounts() {
        Map<String, Student> students = studentMapper.selectAllSimple().stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        Map<String, Instructor> instructors = instructorMapper.selectAll(null).stream()
                .collect(Collectors.toMap(Instructor::getId, Function.identity()));

        List<SysUserVO> result = new ArrayList<>();
        for (SysUser user : sysUserMapper.selectAll()) {
            SysUserVO vo = new SysUserVO();
            vo.setUserId(user.getUserId());
            vo.setUserType(user.getUserType());
            vo.setRefId(user.getRefId());
            vo.setEnabled(user.getEnabled());
            vo.setCreateTimeText(user.getCreateTime() == null ? null : TIME_FMT.format(user.getCreateTime()));
            vo.setRoles(sysUserMapper.selectRoleIds(user.getUserId()));
            fillRefInfo(vo, students, instructors);
            result.add(vo);
        }
        return result;
    }

    private void fillRefInfo(SysUserVO vo, Map<String, Student> students, Map<String, Instructor> instructors) {
        if (vo.getRefId() == null) {
            return;
        }
        if ("STUDENT".equals(vo.getUserType())) {
            Student s = students.get(vo.getRefId());
            if (s != null) {
                vo.setRefName(s.getName());
                vo.setRefInfo("学号 " + s.getId() + " · " + s.getDeptName());
            }
        } else if ("INSTRUCTOR".equals(vo.getUserType())) {
            Instructor i = instructors.get(vo.getRefId());
            if (i != null) {
                vo.setRefName(i.getName());
                vo.setRefInfo("工号 " + i.getId() + " · " + i.getDeptName());
            }
        }
    }

    /** 尚未开户的学生：本类型未开户，且学号未被任何登录账号占用 */
    public List<Student> studentsWithoutAccount() {
        Set<String> opened = openedRefIds("STUDENT");
        Set<String> taken = takenUserIds();
        return studentMapper.selectAllSimple().stream()
                .filter(s -> !opened.contains(s.getId()) && !taken.contains(s.getId()))
                .collect(Collectors.toList());
    }

    /** 尚未开户的教师：本类型未开户，且工号未被任何登录账号占用 */
    public List<Instructor> instructorsWithoutAccount() {
        Set<String> opened = openedRefIds("INSTRUCTOR");
        Set<String> taken = takenUserIds();
        return instructorMapper.selectAll(null).stream()
                .filter(i -> !opened.contains(i.getId()) && !taken.contains(i.getId()))
                .collect(Collectors.toList());
    }

    /** 本类型下已开户的业务主键集合 */
    private Set<String> openedRefIds(String userType) {
        return new HashSet<>(sysUserMapper.selectRefIds(userType));
    }

    /** 已被占用的登录账号集合 */
    private Set<String> takenUserIds() {
        return sysUserMapper.selectAll().stream()
                .map(SysUser::getUserId)
                .collect(Collectors.toSet());
    }

    /** 创建账号 */
    public void createAccount(String userId, String userType, String refId, String rawPassword) {
        requireText(userId, "登录账号不能为空");
        requireText(refId, "请选择要开户的学生或教师");
        if (!"STUDENT".equals(userType) && !"INSTRUCTOR".equals(userType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号类型不正确");
        }
        String trimmedUserId = userId.trim();
        if (sysUserMapper.selectByUserId(trimmedUserId) != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该登录账号已存在");
        }
        if (sysUserMapper.selectByRefId(userType, refId) != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该人员已有登录账号");
        }
        if ("STUDENT".equals(userType) && studentMapper.selectById(refId) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "关联学生不存在");
        }
        if ("INSTRUCTOR".equals(userType) && instructorMapper.selectById(refId) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "关联教师不存在");
        }
        String pwd = normalizePassword(rawPassword);
        SysUser user = new SysUser();
        user.setUserId(trimmedUserId);
        user.setPassword(passwordEncoder.encode(pwd));
        user.setUserType(userType);
        user.setRefId(refId);
        user.setEnabled(1);
        sysUserMapper.insert(user);
        sysUserMapper.insertUserRole(trimmedUserId, userType);
        auditService.record(AuditService.ACTION_ACCOUNT_CREATE, AuditService.TARGET_ACCOUNT, trimmedUserId,
                "开户：" + trimmedUserId + "（" + userType + "，关联人员 " + refId + "）");
    }

    /** 重置密码 */
    public void resetPassword(String userId, String rawPassword) {
        if (sysUserMapper.selectByUserId(userId) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号不存在");
        }
        sysUserMapper.updatePassword(userId, passwordEncoder.encode(normalizePassword(rawPassword)));
        auditService.record(AuditService.ACTION_PASSWORD_RESET, AuditService.TARGET_ACCOUNT, userId,
                "重置密码：" + userId);
    }

    /** 启用/禁用账号，返回操作后是否启用 */
    public boolean toggleEnabled(String userId) {
        SysUser user = sysUserMapper.selectByUserId(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号不存在");
        }
        if ("ADMIN".equals(user.getUserType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "管理员账号不能禁用");
        }
        int enabled = (user.getEnabled() != null && user.getEnabled() == 1) ? 0 : 1;
        sysUserMapper.updateEnabled(userId, enabled);
        auditService.record(AuditService.ACTION_ACCOUNT_TOGGLE, AuditService.TARGET_ACCOUNT, userId,
                (enabled == 1 ? "启用账号：" : "禁用账号：") + userId);
        return enabled == 1;
    }

    /** 删除账号（含角色关联），不存在则忽略 */
    public void deleteAccount(String userId) {
        if (sysUserMapper.selectByUserId(userId) == null) {
            return;
        }
        sysUserMapper.deleteUserRoles(userId);
        sysUserMapper.deleteByUserId(userId);
        auditService.record(AuditService.ACTION_ACCOUNT_DELETE, AuditService.TARGET_ACCOUNT, userId,
                "删除账号：" + userId);
    }

    /** 为所有未开户的学生/教师一键开户（账号=学号/工号，默认密码），返回 [学生数, 教师数] */
    @Transactional
    public int[] openAllPendingAccounts() {
        int studentCount = 0;
        for (Student s : studentsWithoutAccount()) {
            if (tryOpenAccount(s.getId(), "STUDENT", s.getId())) {
                studentCount++;
            }
        }
        int instructorCount = 0;
        for (Instructor i : instructorsWithoutAccount()) {
            if (tryOpenAccount(i.getId(), "INSTRUCTOR", i.getId())) {
                instructorCount++;
            }
        }
        auditService.record(AuditService.ACTION_ACCOUNT_BATCH_CREATE, AuditService.TARGET_ACCOUNT, null,
                "一键批量开户：学生 " + studentCount + " 人，教师 " + instructorCount + " 人");
        return new int[]{studentCount, instructorCount};
    }

    /** 尝试开户，若并发下已被开户或登录名被占用则跳过 */
    private boolean tryOpenAccount(String userId, String userType, String refId) {
        try {
            createAccount(userId, userType, refId, null);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    private String normalizePassword(String rawPassword) {
        String pwd = (rawPassword == null || rawPassword.isEmpty()) ? DEFAULT_PASSWORD : rawPassword;
        if (pwd.length() < 6 || pwd.length() > 32) {
            throw new BusinessException(ErrorCode.PASSWORD_INVALID);
        }
        return pwd;
    }

    private void requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }
}