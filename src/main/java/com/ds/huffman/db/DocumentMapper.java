package com.ds.huffman.db;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * documents 表的 MyBatis Mapper 接口
 * <p>SQL 定义在 resources/mapper/DocumentMapper.xml</p>
 */
public interface DocumentMapper {

    /**
     * 插入一条哈夫曼压缩文档记录，主键回填到 {@code doc.id}
     */
    int insert(Document doc);

    /**
     * 按主键查询文档
     */
    Document selectById(@Param("id") int id);

    /**
     * 查询所有文档（按 id 升序）
     */
    List<Document> selectAll();
}
