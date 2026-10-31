/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.controller;

import com.ds.university.service.TranscriptSignatureService;
import com.ds.university.sign.TranscriptVerifyInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 成绩单公开验签页（无需登录）：
 * <ul>
 *   <li>GET  /verify：表单页；</li>
 *   <li>POST /verify：验签，只显示"通过/不通过"，不显示成绩内容（避免成为免登录数据接口）。</li>
 * </ul>
 *
 * <p>输入宽容：支持"整个 signed JSON 文件"或"分别粘贴 payload + signature"，
 * 并自动处理"从 JSON 文件里复制 payload 导致的 {\" 转义"错误（见 {@link TranscriptVerifyInput}）。
 * 该路径已加入 AuthInterceptor 白名单（CSRF 仍生效：GET 下发 token、表单带 _csrf）。
 */
@Controller
public class TranscriptVerifyController {

    private final TranscriptSignatureService transcriptSignatureService;
    private final ObjectMapper objectMapper;

    public TranscriptVerifyController(TranscriptSignatureService transcriptSignatureService,
                                      ObjectMapper objectMapper) {
        this.transcriptSignatureService = transcriptSignatureService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/verify")
    public String page(Model model) {
        addSignerInfo(model);
        return "verify";
    }

    @PostMapping("/verify")
    public String verify(@RequestParam(name = "document", required = false) String document,
                         @RequestParam(name = "payload", required = false) String payload,
                         @RequestParam(name = "signature", required = false) String signature,
                         Model model) {
        addSignerInfo(model);
        model.addAttribute("document", document);
        model.addAttribute("payload", payload);
        model.addAttribute("signature", signature);

        if (!transcriptSignatureService.isEnabled()) {
            model.addAttribute("result", false);
            model.addAttribute("message", "签名功能未启用（app.transcript-signing.enabled=false）");
            return "verify";
        }

        TranscriptVerifyInput input = TranscriptVerifyInput.parse(objectMapper, document, payload, signature);
        if (input == null) {
            model.addAttribute("result", false);
            model.addAttribute("message", "没法识别输入：请粘贴下载的整个 JSON 文件，"
                    + "或分别粘贴 payload（以 { 开头的 JSON）与 signature（Base64）");
            return "verify";
        }
        if (input.payload().length() > TranscriptSignatureService.MAX_PAYLOAD_CHARS) {
            model.addAttribute("result", false);
            model.addAttribute("message", "内容过长，请确认粘贴的是成绩单 JSON");
            return "verify";
        }

        boolean passed = transcriptSignatureService.verify(input.payload(), input.signature());
        if (!passed) {
            // 兼容"从 JSON 文件里直接复制 payload 原文"：内容带 \" 转义，原样验签必然失败
            String unescaped = TranscriptVerifyInput.tryUnescape(objectMapper, input.payload());
            if (unescaped != null) {
                passed = transcriptSignatureService.verify(unescaped, input.signature());
            }
        }
        model.addAttribute("result", passed);
        model.addAttribute("message", passed
                ? "验签通过：内容与签名匹配，且由本校签名私钥签发"
                : "验签不通过：内容或签名不匹配（可能被修改、复制时被改动，或不是本校签发）");
        return "verify";
    }

    private void addSignerInfo(Model model) {
        model.addAttribute("algorithm", transcriptSignatureService.algorithm());
        model.addAttribute("fingerprint", transcriptSignatureService.certificateFingerprint());
    }
}
