/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.mapper;

import com.ds.university.entity.TranscriptSignature;
import org.apache.ibatis.annotations.Param;

/** 成绩单签名记录（只增不改） */
public interface TranscriptSignatureMapper {

    /** 写入一条签名记录；useGeneratedKeys 回填 id */
    int insert(TranscriptSignature record);

    /** 按流水号查询（定位、追溯用） */
    TranscriptSignature selectBySerialNo(@Param("serialNo") String serialNo);
}
