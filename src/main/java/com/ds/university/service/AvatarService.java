package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.config.UploadProperties;
import com.ds.university.entity.SysUser;
import com.ds.university.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 头像上传/移除：
 * <ul>
 *   <li>文件落盘到 app.upload.avatar-dir，文件名用 UUID 生成（不信任客户端文件名，防路径穿越/恶意命名）；</li>
 *   <li>数据库 sys_user.avatar 仅存文件名，通过 /uploads/avatars/{文件名} 访问；</li>
 *   <li>扩展名白名单 + 大小上限双重校验，替换时删除旧文件，失败时清理新文件不留孤儿。</li>
 * </ul>
 */
@Service
public class AvatarService {

    /** 允许的图片扩展名（小写） */
    private static final List<String> ALLOWED_EXTENSIONS =
            Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    /** 单张头像大小上限（字节） */
    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final SysUserMapper sysUserMapper;
    private final AuditService auditService;
    private final UploadProperties uploadProperties;

    public AvatarService(SysUserMapper sysUserMapper, AuditService auditService,
                         UploadProperties uploadProperties) {
        this.sysUserMapper = sysUserMapper;
        this.auditService = auditService;
        this.uploadProperties = uploadProperties;
    }

    /** 上传（覆盖）头像，返回新文件名 */
    public String saveAvatar(String userId, MultipartFile file) {
        String filename = validateAndGenerateFilename(file);
        SysUser user = sysUserMapper.selectByUserId(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号不存在");
        }
        Path dir = uploadProperties.avatarDirPath();
        Path target = dir.resolve(filename);
        try {
            Files.createDirectories(dir);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "头像保存失败，请稍后重试");
        }
        try {
            sysUserMapper.updateAvatar(userId, filename);
        } catch (RuntimeException e) {
            deleteQuietly(filename);
            throw e;
        }
        deleteQuietly(user.getAvatar());
        auditService.record(AuditService.ACTION_AVATAR_UPDATE, AuditService.TARGET_ACCOUNT, userId,
                "更新头像：" + userId);
        return filename;
    }

    /** 移除头像：清空数据库记录并删除文件（无头像时静默忽略） */
    public void removeAvatar(String userId) {
        SysUser user = sysUserMapper.selectByUserId(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号不存在");
        }
        String old = user.getAvatar();
        if (old == null || old.isEmpty()) {
            return;
        }
        sysUserMapper.updateAvatar(userId, null);
        deleteQuietly(old);
        auditService.record(AuditService.ACTION_AVATAR_REMOVE, AuditService.TARGET_ACCOUNT, userId,
                "移除头像：" + userId);
    }

    /** 校验文件并生成随机文件名；不合法直接抛业务异常 */
    private String validateAndGenerateFilename(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要上传的图片文件");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "头像图片不能超过 5MB");
        }
        String ext = resolveExtension(file);
        if (ext == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持 JPG / PNG / GIF / WebP 格式的图片");
        }
        return UUID.randomUUID().toString().replace("-", "") + "." + ext;
    }

    /**
     * 扩展名白名单校验：原始文件名扩展名小写后必须命中白名单；
     * Content-Type 非 image/* 且非 application/octet-stream 时直接拒绝（双保险，不信任浏览器）。
     */
    private String resolveExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isEmpty()
                && !contentType.startsWith("image/")
                && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            return null;
        }
        String original = file.getOriginalFilename();
        if (original == null) {
            return null;
        }
        int dot = original.lastIndexOf('.');
        if (dot < 0 || dot == original.length() - 1) {
            return null;
        }
        String ext = original.substring(dot + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.contains(ext) ? ext : null;
    }

    private void deleteQuietly(String filename) {
        if (filename == null || filename.isEmpty()) {
            return;
        }
        try {
            Files.deleteIfExists(uploadProperties.avatarDirPath().resolve(filename));
        } catch (IOException ignored) {
            // 删除失败仅遗留孤儿文件，不影响主流程
        }
    }
}
