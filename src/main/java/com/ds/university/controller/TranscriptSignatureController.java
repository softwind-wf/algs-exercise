/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.controller;

import com.ds.university.common.BusinessException;
import com.ds.university.common.ErrorCode;
import com.ds.university.service.StudentService;
import com.ds.university.service.TranscriptSignatureService;
import com.ds.university.vo.LoginUser;
import com.ds.university.vo.SignedTranscriptVO;
import com.ds.university.vo.TranscriptVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 学生成绩单签名下载：登录学生下载自己成绩单的"内容 + 签名"。
 *
 * <p>默认输出**可打印的 HTML 成绩单**（表格 + 汇总 + 签名信息，文末附完整签名数据），
 * 打开即可阅读、可打印/另存 PDF；需要给程序消费时用 {@code ?format=json} 下载 JSON。
 * 签名不加密内容，成绩单仍是明文；需要保密请另做加密。
 */
@Controller
@RequestMapping("/student/transcript")
public class TranscriptSignatureController {

    private static final MediaType JSON_UTF8 = MediaType.parseMediaType("application/json;charset=UTF-8");
    private static final MediaType HTML_UTF8 = MediaType.parseMediaType("text/html;charset=UTF-8");

    private final StudentService studentService;
    private final TranscriptSignatureService transcriptSignatureService;
    private final SpringTemplateEngine templateEngine;
    private final ObjectMapper objectMapper;

    public TranscriptSignatureController(StudentService studentService,
                                         TranscriptSignatureService transcriptSignatureService,
                                         SpringTemplateEngine templateEngine,
                                         ObjectMapper objectMapper) {
        this.studentService = studentService;
        this.transcriptSignatureService = transcriptSignatureService;
        this.templateEngine = templateEngine;
        this.objectMapper = objectMapper;
    }

    /**
     * 下载签名版成绩单。
     *
     * @param format html（默认，可打印表格）| json（机器可读）
     */
    @GetMapping("/signed")
    public ResponseEntity<?> signedTranscript(HttpSession session,
                                              @RequestParam(name = "format", required = false,
                                                      defaultValue = "html") String format) throws Exception {
        String studentId = currentStudentId(session);
        TranscriptVO transcript = studentService.transcript(studentId);
        SignedTranscriptVO signed = transcriptSignatureService.sign(transcript);

        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .contentType(JSON_UTF8)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"transcript-" + studentId + "-signed.json\"")
                    .body(signed);
        }

        Context context = new Context(Locale.CHINA);
        context.setVariable("transcript", transcript);
        context.setVariable("signed", signed);
        context.setVariable("signedJson", objectMapper.writeValueAsString(signed));
        String html = templateEngine.process("transcript-signed", context);
        return ResponseEntity.ok()
                .contentType(HTML_UTF8)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"transcript-" + studentId + "-signed.html\"")
                .body(html.getBytes(StandardCharsets.UTF_8));
    }

    private String currentStudentId(HttpSession session) {
        LoginUser loginUser = (LoginUser) session.getAttribute(AuthController.SESSION_USER);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (loginUser.getRefId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前账号未关联学生信息");
        }
        return loginUser.getRefId();
    }
}
