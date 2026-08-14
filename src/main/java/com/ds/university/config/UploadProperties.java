package com.ds.university.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 应用上传配置（application.yml 的 app.upload.*）。
 * 头像文件存储在本地磁盘，数据库仅存文件名。
 */
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    /** 头像文件存储目录（相对路径基于应用启动目录；Docker 部署用环境变量 APP_UPLOAD_DIR 覆盖） */
    private String avatarDir = "./uploads/avatars";

    public String getAvatarDir() {
        return avatarDir;
    }

    public void setAvatarDir(String avatarDir) {
        this.avatarDir = avatarDir;
    }

    /** 头像目录绝对路径（normalize 后便于比较与拼接） */
    public Path avatarDirPath() {
        return Paths.get(avatarDir).toAbsolutePath().normalize();
    }

    /** 供静态资源映射使用的 file: URI（以 / 结尾） */
    public String avatarDirResourceUri() {
        String uri = avatarDirPath().toUri().toString();
        return uri.endsWith("/") ? uri : uri + "/";
    }
}
