package cn.exercise.algs4.datastructure.huffman.db;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * code_table 表的 MyBatis Mapper 接口
 * <p>SQL 定义在 resources/mapper/CodeEntryMapper.xml</p>
 */
public interface CodeEntryMapper {

    /**
     * 批量插入哈夫曼编码表（一个字符一行）
     *
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<CodeEntry> entries);

    /**
     * 查询某文档的完整编码表（按编码长度升序）
     */
    List<CodeEntry> selectByDocId(@Param("docId") int docId);
}
