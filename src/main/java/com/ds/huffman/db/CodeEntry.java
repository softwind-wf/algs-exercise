package com.ds.huffman.db;

/**
 * code_table 表实体：单个字符的哈夫曼编码
 *
 * <p>对应表结构：</p>
 * <pre>
 *   doc_id INT 关联文档（外键 → documents.id）
 *   ch     VARCHAR 字符
 *   code   VARCHAR 哈夫曼编码（0/1 串）
 *   主键: (doc_id, ch)
 * </pre>
 */
public class CodeEntry {

    private Integer docId;
    private String ch;
    private String code;

    public Integer getDocId() {
        return docId;
    }

    public void setDocId(Integer docId) {
        this.docId = docId;
    }

    public String getCh() {
        return ch;
    }

    public void setCh(String ch) {
        this.ch = ch;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "CodeEntry{docId=" + docId + ", ch='" + ch + "', code='" + code + "'}";
    }
}
