/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.entity;

import java.util.Date;

/** 成绩单签名记录（对应 transcript_signature 表，只增不改） */
public class TranscriptSignature {

    private Long id;
    /** 学号 */
    private String studentId;
    /** 学期（多学期成绩单记 ALL） */
    private String term;
    /** 签名流水号（被签内容里也带着，防重放） */
    private String serialNo;
    /** SHA-256(规范化成绩单 JSON) */
    private String payloadHash;
    /** Base64 签名 */
    private String signature;
    /** 签名证书序列号（换钥后仍能定位历史公钥） */
    private String certSerial;
    /** 签发时间 */
    private Date issuedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }

    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getCertSerial() { return certSerial; }
    public void setCertSerial(String certSerial) { this.certSerial = certSerial; }

    public Date getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Date issuedAt) { this.issuedAt = issuedAt; }
}
