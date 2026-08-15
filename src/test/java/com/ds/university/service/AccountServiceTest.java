package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.entity.Instructor;
import com.ds.university.entity.Student;
import com.ds.university.mapper.InstructorMapper;
import com.ds.university.mapper.StudentMapper;
import com.ds.university.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 账号管理单元测试：开户校验（重复账号/重复人员/人员不存在/密码规范化）。
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private InstructorMapper instructorMapper;
    @Mock
    private AuditService auditService;

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(sysUserMapper, studentMapper, instructorMapper, auditService);
    }

    private Student student(String id) {
        Student s = new Student();
        s.setId(id);
        return s;
    }

    private Instructor instructor(String id) {
        Instructor i = new Instructor();
        i.setId(id);
        return i;
    }

    @Test
    void createRejectsDuplicateUserId() {
        when(sysUserMapper.selectByUserId("zhang")).thenReturn(new com.ds.university.entity.SysUser());
        assertThrows(BusinessException.class,
                () -> service.createAccount("zhang", "STUDENT", "00128", null));
        verify(sysUserMapper, never()).insert(any());
    }

    @Test
    void createRejectsDuplicateRef() {
        when(sysUserMapper.selectByUserId("newuser")).thenReturn(null);
        when(sysUserMapper.selectByRefId("STUDENT", "00128")).thenReturn(new com.ds.university.entity.SysUser());
        assertThrows(BusinessException.class,
                () -> service.createAccount("newuser", "STUDENT", "00128", null));
    }

    @Test
    void createRejectsMissingRef() {
        when(sysUserMapper.selectByUserId("newuser")).thenReturn(null);
        when(sysUserMapper.selectByRefId("STUDENT", "99999")).thenReturn(null);
        when(studentMapper.selectById("99999")).thenReturn(null);
        assertThrows(BusinessException.class,
                () -> service.createAccount("newuser", "STUDENT", "99999", null));
    }

    @Test
    void createSucceedsWithDefaultPasswordAndAudits() {
        when(sysUserMapper.selectByUserId("zhang2")).thenReturn(null);
        when(sysUserMapper.selectByRefId("STUDENT", "00128")).thenReturn(null);
        when(studentMapper.selectById("00128")).thenReturn(student("00128"));

        service.createAccount("zhang2", "STUDENT", "00128", null);
        verify(sysUserMapper).insert(any());
        verify(sysUserMapper).insertUserRole("zhang2", "STUDENT");
        verify(auditService).record(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void createRejectsWeakPassword() {
        when(sysUserMapper.selectByUserId("zhang3")).thenReturn(null);
        when(sysUserMapper.selectByRefId("STUDENT", "00128")).thenReturn(null);
        when(studentMapper.selectById("00128")).thenReturn(student("00128"));
        assertThrows(BusinessException.class,
                () -> service.createAccount("zhang3", "STUDENT", "00128", "123"));
    }

    @Test
    void resetPasswordRejectsMissingAccount() {
        when(sysUserMapper.selectByUserId("nobody")).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.resetPassword("nobody", null));
    }

    @Test
    void toggleEnabledFlipsStateAndProtectsAdmin() {
        com.ds.university.entity.SysUser admin = new com.ds.university.entity.SysUser();
        admin.setUserId("admin");
        admin.setUserType("ADMIN");
        when(sysUserMapper.selectByUserId("admin")).thenReturn(admin);
        assertThrows(BusinessException.class, () -> service.toggleEnabled("admin"));

        com.ds.university.entity.SysUser student = new com.ds.university.entity.SysUser();
        student.setUserId("zhang");
        student.setUserType("STUDENT");
        student.setEnabled(1);
        when(sysUserMapper.selectByUserId("zhang")).thenReturn(student);
        assertTrue(!service.toggleEnabled("zhang"));
        verify(sysUserMapper).updateEnabled("zhang", 0);
    }

    @Test
    void countAccountsDelegates() {
        when(sysUserMapper.count()).thenReturn(5L);
        assertEquals(5L, service.countAccounts());
    }
}
