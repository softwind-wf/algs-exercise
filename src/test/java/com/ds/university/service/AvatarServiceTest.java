package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.config.UploadProperties;
import com.ds.university.entity.SysUser;
import com.ds.university.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 头像服务单元测试：文件校验（扩展名白名单/大小/类型）、保存落盘、旧文件清理、移除。
 */
@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private AuditService auditService;

    @TempDir
    Path tempDir;

    private AvatarService service;

    @BeforeEach
    void setUp() {
        UploadProperties props = new UploadProperties();
        props.setAvatarDir(tempDir.toString());
        service = new AvatarService(sysUserMapper, auditService, props);
    }

    private SysUser userWithAvatar(String avatar) {
        SysUser user = new SysUser();
        user.setUserId("zhang");
        user.setAvatar(avatar);
        return user;
    }

    private MockMultipartFile png(String name, byte[] bytes) {
        return new MockMultipartFile("file", name, "image/png", bytes);
    }

    @Test
    void saveRejectsEmptyFile() {
        assertThrows(BusinessException.class, () -> service.saveAvatar("zhang", png("a.png", new byte[0])));
        verify(sysUserMapper, never()).updateAvatar(anyString(), anyString());
    }

    @Test
    void saveRejectsBadExtension() {
        assertThrows(BusinessException.class, () -> service.saveAvatar("zhang", png("evil.txt", new byte[]{1})));
        assertThrows(BusinessException.class, () -> service.saveAvatar("zhang", png("noext", new byte[]{1})));
    }

    @Test
    void saveRejectsOversize() {
        byte[] big = new byte[(int) AvatarService.MAX_SIZE_BYTES + 1];
        assertThrows(BusinessException.class, () -> service.saveAvatar("zhang", png("big.png", big)));
    }

    @Test
    void savePersistsFileAndDeletesOld() throws Exception {
        when(sysUserMapper.selectByUserId("zhang")).thenReturn(userWithAvatar("old.png"));
        Files.write(tempDir.resolve("old.png"), new byte[]{1, 2, 3});

        byte[] content = new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47};
        String filename = service.saveAvatar("zhang", png("new.png", content));

        assertTrue(filename.endsWith(".png"), "文件名应为随机 UUID + .png");
        assertTrue(Files.exists(tempDir.resolve(filename)), "新文件应落盘");
        assertFalse(Files.exists(tempDir.resolve("old.png")), "旧文件应被删除");
        verify(sysUserMapper).updateAvatar(eq("zhang"), eq(filename));
        verify(auditService).record(anyString(), eq(AuditService.TARGET_ACCOUNT), anyString(), anyString());
    }

    @Test
    void removeClearsAvatarAndDeletesFile() throws Exception {
        when(sysUserMapper.selectByUserId("zhang")).thenReturn(userWithAvatar("cur.png"));
        Files.write(tempDir.resolve("cur.png"), new byte[]{1, 2, 3});
        service.removeAvatar("zhang");
        verify(sysUserMapper).updateAvatar("zhang", null);
        assertFalse(Files.exists(tempDir.resolve("cur.png")), "文件应被删除");
    }

    @Test
    void removeWithoutAvatarIsNoop() {
        when(sysUserMapper.selectByUserId("zhang")).thenReturn(userWithAvatar(null));
        service.removeAvatar("zhang");
        verify(sysUserMapper, never()).updateAvatar(anyString(), anyString());
    }
}
