package cn.exercise.algs4.datastructure.imagedb;

import java.util.Date;

/**
 * images 表实体：一张入库图片的持久化记录
 *
 * <p>对应表结构：</p>
 * <pre>
 *   id         INT 自增主键
 *   name       VARCHAR    图片文件名
 *   mime_type  VARCHAR    MIME 类型（image/png、image/jpeg…）
 *   width      INT        图片宽（像素）
 *   height     INT        图片高（像素）
 *   data       LONGBLOB   图片二进制数据（BLOB 大对象）
 *   created_at DATETIME   入库时间
 * </pre>
 */
public class ImageRecord {

    private Integer id;
    private String name;
    private String mimeType;
    private int width;
    private int height;
    private byte[] data;
    private Date createdAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ImageRecord{id=" + id + ", name='" + name + "', mimeType='" + mimeType
                + "', size=" + width + "x" + height
                + ", data.len=" + (data == null ? 0 : data.length)
                + ", createdAt=" + createdAt + "}";
    }
}
