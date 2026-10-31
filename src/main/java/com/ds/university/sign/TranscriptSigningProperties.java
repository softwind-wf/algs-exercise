/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.sign;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 成绩单签名配置（application.yml 的 app.transcript-signing.*）。
 *
 * <p>签名私钥与 HTTPS 证书私钥必须分开：专钥专用，证书 KeyUsage 应为 digitalSignature。
 */
@Component
@ConfigurationProperties(prefix = "app.transcript-signing")
public class TranscriptSigningProperties {

    /** 是否启用成绩单签名；关闭后接口返回"未启用" */
    private boolean enabled = true;

    /** 签名密钥库（PKCS12）；相对路径基于应用启动目录 */
    private String keystore = "./keystore/transcript-signing.p12";

    /** 密钥库口令；生产环境用环境变量注入，不要写死在配置文件里 */
    private String storePassword = "changeit";

    /** 签名密钥别名 */
    private String alias = "transcript-signer";

    /** 密钥库不存在时是否生成临时密钥（仅开发环境；重启后历史签名无法再验证） */
    private boolean generateIfMissing = true;

    /** 临时密钥长度 */
    private int keySize = 2048;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getKeystore() { return keystore; }
    public void setKeystore(String keystore) { this.keystore = keystore; }

    public String getStorePassword() { return storePassword; }
    public void setStorePassword(String storePassword) { this.storePassword = storePassword; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public boolean isGenerateIfMissing() { return generateIfMissing; }
    public void setGenerateIfMissing(boolean generateIfMissing) { this.generateIfMissing = generateIfMissing; }

    public int getKeySize() { return keySize; }
    public void setKeySize(int keySize) { this.keySize = keySize; }
}
