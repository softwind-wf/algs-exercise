/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.sign;

import com.ds.university.entity.TranscriptSignature;
import com.ds.university.mapper.TranscriptSignatureMapper;
import com.ds.university.service.TranscriptSignatureService;
import com.ds.university.vo.SignedTranscriptVO;
import com.ds.university.vo.TranscriptRowVO;
import com.ds.university.vo.TranscriptVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 成绩单签名服务测试：签名/验签、篡改检测、错钥拒绝、规范化稳定性、JSON 转义。
 *
 * <p>不依赖 Spring 容器与数据库：用内存 Mapper + 临时密钥对，验证签名逻辑本身。
 */
class TranscriptSignatureServiceTest {

    private InMemoryMapper mapper;
    private TranscriptSignatureService service;
    private TranscriptSigner signer;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        signer = new TranscriptSigner(keyPair.getPrivate(), keyPair.getPublic());
        mapper = new InMemoryMapper();
        service = new TranscriptSignatureService(mapper, signer);
    }

    @Test
    @DisplayName("签出的成绩单可以用同一把公钥验签通过")
    void signThenVerify() {
        SignedTranscriptVO signed = service.sign(sampleTranscript());

        assertNotNull(signed.getSerialNo());
        assertTrue(signed.getSerialNo().startsWith("TR-"));
        assertTrue(signed.getPayload().contains("\"studentId\":\"2021001\""));
        assertTrue(signed.getPayload().contains("\"type\":\"TRANSCRIPT\""));
        assertEquals(64, signed.getPayloadHash().length());
        assertTrue(service.verify(signed.getPayload(), signed.getSignature()));
    }

    @Test
    @DisplayName("成绩单记录已落库，且哈希与内容一致")
    void signatureRecordPersisted() throws Exception {
        SignedTranscriptVO signed = service.sign(sampleTranscript());
        TranscriptSignature record = mapper.selectBySerialNo(signed.getSerialNo());

        assertNotNull(record);
        assertEquals("2021001", record.getStudentId());
        assertEquals(signed.getPayloadHash(), record.getPayloadHash());
        assertEquals(sha256Hex(signed.getPayload()), record.getPayloadHash());
        assertEquals("2026-FALL", record.getTerm());
        assertNotNull(record.getIssuedAt());
    }

    @Test
    @DisplayName("改一个分数：验签不通过")
    void tamperedScoreRejected() {
        SignedTranscriptVO signed = service.sign(sampleTranscript());
        String tampered = signed.getPayload().replace("\"grade\":\"B+\"", "\"grade\":\"A+\"");

        assertNotEquals(signed.getPayload(), tampered);
        assertFalse(service.verify(tampered, signed.getSignature()));
    }

    @Test
    @DisplayName("改流水号 / 换签名：验签不通过")
    void tamperedSerialOrSignatureRejected() {
        SignedTranscriptVO signed = service.sign(sampleTranscript());
        String payload = signed.getPayload();

        assertFalse(service.verify(payload.replace("TR-", "XX-"), signed.getSignature()));

        char[] chars = signed.getSignature().toCharArray();
        chars[chars.length / 2] = chars[chars.length / 2] == 'A' ? 'B' : 'A';
        assertFalse(service.verify(payload, new String(chars)));
    }

    @Test
    @DisplayName("用别的密钥签的内容：验签不通过")
    void wrongKeyRejected() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair other = generator.generateKeyPair();
        TranscriptSigner otherSigner = new TranscriptSigner(other.getPrivate(), other.getPublic());
        TranscriptSignatureService otherService =
                new TranscriptSignatureService(new InMemoryMapper(), otherSigner);

        SignedTranscriptVO signedByOther = otherService.sign(sampleTranscript());
        assertFalse(service.verify(signedByOther.getPayload(), signedByOther.getSignature()));
        assertTrue(otherService.verify(signedByOther.getPayload(), signedByOther.getSignature()));
    }

    @Test
    @DisplayName("规范化与行顺序无关：同样的成绩单，行倒序也生成相同内容")
    void canonicalPayloadIsOrderIndependent() {
        TranscriptVO a = sampleTranscript();
        TranscriptVO b = sampleTranscript();
        List<TranscriptRowVO> reversed = new ArrayList<>(b.getRows());
        java.util.Collections.reverse(reversed);
        b.setRows(reversed);

        String jsonA = TranscriptCanonicalizer.canonicalJson(a, "TR-TEST-0001", "2026-11-01T00:00:00+08:00");
        String jsonB = TranscriptCanonicalizer.canonicalJson(b, "TR-TEST-0001", "2026-11-01T00:00:00+08:00");
        assertEquals(jsonA, jsonB);
    }

    @Test
    @DisplayName("姓名里带引号/反斜杠：转义后仍能验签")
    void jsonEscapingKeepsSignatureValid() {
        TranscriptVO vo = sampleTranscript();
        vo.setStudentName("张\"伟\"\\同学");
        SignedTranscriptVO signed = service.sign(vo);

        assertTrue(signed.getPayload().contains("张\\\"伟\\\"\\\\同学"));
        assertTrue(service.verify(signed.getPayload(), signed.getSignature()));
    }

    @Test
    @DisplayName("空内容 / 非法 Base64：直接判不通过，不抛异常")
    void invalidInputRejected() {
        assertFalse(service.verify(null, "abc"));
        assertFalse(service.verify("", ""));
        assertFalse(service.verify("{}", "不是Base64"));
        assertFalse(service.verify("{}", ""));
    }

    @Test
    @DisplayName("成绩单行缺少学期时按 null 输出，仍然可签可验")
    void nullFieldsHandled() {
        TranscriptVO vo = sampleTranscript();
        vo.getRows().get(0).setSemester(null);
        vo.setGpa(null);
        SignedTranscriptVO signed = service.sign(vo);

        assertTrue(signed.getPayload().contains("\"semester\":null"));
        assertTrue(signed.getPayload().contains("\"gpa\":null"));
        assertTrue(service.verify(signed.getPayload(), signed.getSignature()));
    }

    // ------------------------------------------------------------------
    // 测试数据
    // ------------------------------------------------------------------

    private static TranscriptVO sampleTranscript() {
        TranscriptVO vo = new TranscriptVO();
        vo.setStudentId("2021001");
        vo.setStudentName("张伟");
        vo.setDeptName("计算机科学与技术");

        TranscriptRowVO row1 = new TranscriptRowVO();
        row1.setCourseId("CS301");
        row1.setTitle("数据库系统");
        row1.setCredits(4);
        row1.setYear(2026);
        row1.setSemester("FALL");
        row1.setGrade("B+");
        row1.setGradePoint(3.3);

        TranscriptRowVO row2 = new TranscriptRowVO();
        row2.setCourseId("CS302");
        row2.setTitle("操作系统");
        row2.setCredits(4);
        row2.setYear(2026);
        row2.setSemester("FALL");
        row2.setGrade("A-");
        row2.setGradePoint(3.7);

        vo.setRows(new ArrayList<>(Arrays.asList(row1, row2)));
        vo.setCourseCount(2);
        vo.setEarnedCredits(8);
        vo.setGpa(3.5);
        return vo;
    }

    private static String sha256Hex(String text) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(64);
        for (byte b : digest) {
            hex.append(String.format("%02x", b & 0xff));
        }
        return hex.toString();
    }

    /** 内存版 Mapper：测试不碰数据库 */
    private static final class InMemoryMapper implements TranscriptSignatureMapper {

        private final Map<String, TranscriptSignature> store = new HashMap<>();

        @Override
        public int insert(TranscriptSignature record) {
            record.setId((long) store.size() + 1);
            store.put(record.getSerialNo(), record);
            return 1;
        }

        @Override
        public TranscriptSignature selectBySerialNo(String serialNo) {
            return store.get(serialNo);
        }
    }
}
