/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.entity.TranscriptSignature;
import com.ds.university.mapper.TranscriptSignatureMapper;
import com.ds.university.sign.TranscriptCanonicalizer;
import com.ds.university.sign.TranscriptSigner;
import com.ds.university.vo.SignedTranscriptVO;
import com.ds.university.vo.TranscriptRowVO;
import com.ds.university.vo.TranscriptVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 成绩单签名服务：生成"规范化内容 + 签名"，并把签名记录落库；同时提供验签。
 *
 * <p>签名内容覆盖：类型/版本、学号/姓名/院系、每门课明细、汇总、签发时间、流水号。
 * 验证端只需要"内容 + 签名 + 学校公钥"，不需要访问数据库（见 /verify 页面）。
 */
@Service
public class TranscriptSignatureService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter ISSUED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.ROOT);
    private static final DateTimeFormatter SERIAL_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT);
    private static final SecureRandom RANDOM = new SecureRandom();
    /** 验签页单次接受的最大内容长度（防大 payload 打满内存） */
    public static final int MAX_PAYLOAD_CHARS = 20000;

    private final TranscriptSignatureMapper mapper;
    private final TranscriptSigner signer;

    public TranscriptSignatureService(TranscriptSignatureMapper mapper, TranscriptSigner signer) {
        this.mapper = mapper;
        this.signer = signer;
    }

    /** 是否为成绩单签发一份签名（只依赖成绩单快照，不依赖分页） */
    @Transactional
    public SignedTranscriptVO sign(TranscriptVO transcript) {
        if (!signer.isEnabled()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "成绩单签名未启用");
        }
        if (transcript == null || transcript.getStudentId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "成绩单数据为空，无法签名");
        }
        OffsetDateTime now = OffsetDateTime.now(ZONE);
        String issuedAt = now.format(ISSUED_AT);
        String serialNo = "TR-" + now.format(SERIAL_TIME) + "-" + randomHex();
        String payload = TranscriptCanonicalizer.canonicalJson(transcript, serialNo, issuedAt);
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

        byte[] signature;
        try {
            signature = signer.sign(payloadBytes);
        } catch (GeneralSecurityException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "成绩单签名失败");
        }
        String payloadHash = sha256Hex(payloadBytes);
        String signatureBase64 = Base64.getEncoder().encodeToString(signature);

        TranscriptSignature record = new TranscriptSignature();
        record.setStudentId(transcript.getStudentId());
        record.setTerm(termOf(transcript));
        record.setSerialNo(serialNo);
        record.setPayloadHash(payloadHash);
        record.setSignature(signatureBase64);
        record.setCertSerial(signer.certificateSerial());
        record.setIssuedAt(Date.from(now.toInstant()));
        mapper.insert(record);

        SignedTranscriptVO signed = new SignedTranscriptVO();
        signed.setStudentId(transcript.getStudentId());
        signed.setSerialNo(serialNo);
        signed.setIssuedAt(issuedAt);
        signed.setAlgorithm(signer.algorithm());
        signed.setPayloadHash(payloadHash);
        signed.setPayload(payload);
        signed.setSignature(signatureBase64);
        signed.setPublicKey(signer.publicKeyBase64());
        signed.setCertificateFingerprint(signer.certificateFingerprint());
        return signed;
    }

    /** 验签：内容 + Base64 签名。任何异常（含 Base64 非法）都按"不通过"处理。 */
    public boolean verify(String payload, String signatureBase64) {
        if (!signer.isEnabled()) {
            return false;
        }
        if (payload == null || payload.isEmpty() || signatureBase64 == null || signatureBase64.isEmpty()) {
            return false;
        }
        if (payload.length() > MAX_PAYLOAD_CHARS) {
            return false;
        }
        byte[] signatureBytes;
        try {
            signatureBytes = Base64.getDecoder().decode(signatureBase64.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
        return signer.verify(payload.getBytes(StandardCharsets.UTF_8), signatureBytes);
    }

    /** 当前签名算法（展示/诊断用） */
    public String algorithm() {
        return signer.algorithm();
    }

    /** 当前签名证书指纹（展示/诊断用） */
    public String certificateFingerprint() {
        return signer.certificateFingerprint();
    }

    /** 签名功能是否启用 */
    public boolean isEnabled() {
        return signer.isEnabled();
    }

    /** 学期标签：单学期记学期，多学期记 ALL */
    private static String termOf(TranscriptVO transcript) {
        if (transcript.getRows() == null || transcript.getRows().isEmpty()) {
            return null;
        }
        Set<String> terms = new LinkedHashSet<>();
        for (TranscriptRowVO row : transcript.getRows()) {
            if (row.getYear() != null && row.getSemester() != null) {
                terms.add(row.getYear() + "-" + row.getSemester());
            }
        }
        if (terms.isEmpty()) {
            return null;
        }
        return terms.size() == 1 ? terms.iterator().next() : "ALL";
    }

    private static String randomHex() {
        byte[] bytes = new byte[3];
        RANDOM.nextBytes(bytes);
        StringBuilder hex = new StringBuilder(6);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b & 0xff));
        }
        return hex.toString();
    }

    private static String sha256Hex(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
