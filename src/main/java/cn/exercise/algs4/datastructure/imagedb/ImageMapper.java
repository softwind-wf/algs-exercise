package cn.exercise.algs4.datastructure.imagedb;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * images 表的 MyBatis Mapper 接口
 * <p>SQL 定义在 resources/mapper/ImageMapper.xml</p>
 */
public interface ImageMapper {

    /**
     * 插入一张图片，主键回填到 {@code img.id}
     */
    int insert(ImageRecord img);

    /**
     * 按主键查询图片（含 BLOB 数据）
     */
    ImageRecord selectById(@Param("id") int id);

    /**
     * 查询所有图片（按 id 升序）
     */
    List<ImageRecord> selectAll();
}
