package com.ds.huffman.db;

import java.util.Date;

/**
 * documents 表实体：一份哈夫曼压缩文档的持久化记录
 *
 * <p>对应表结构：</p>
 * <pre>
 *   id            INT 自增主键
 *   doc_name      VARCHAR    文档名
 *   original_text LONGTEXT   原文明文
 *   encoded_bits  LONGTEXT   哈夫曼编码 bit 串（'0'/'1'）
 *   tree_blob     LONGBLOB   哈夫曼树序列化字节
 *   created_at    DATETIME   入库时间
 * </pre>
 */
public class Document {

    private Integer id;
    private String docName;
    private String originalText;
    private String encodedBits;
    private byte[] treeBlob;
    private Date createdAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDocName() {
        return docName;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getEncodedBits() {
        return encodedBits;
    }

    public void setEncodedBits(String encodedBits) {
        this.encodedBits = encodedBits;
    }

    public byte[] getTreeBlob() {
        return treeBlob;
    }

    public void setTreeBlob(byte[] treeBlob) {
        this.treeBlob = treeBlob;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Document{id=" + id + ", docName='" + docName + "', originalText='"
                + originalText + "', encodedBits.len=" + (encodedBits == null ? 0 : encodedBits.length())
                + ", treeBlob.len=" + (treeBlob == null ? 0 : treeBlob.length)
                + ", createdAt=" + createdAt + "}";
    }
}
