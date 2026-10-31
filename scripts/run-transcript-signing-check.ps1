<#
.SYNOPSIS
    离线编译并验证"成绩单签名"实现：JUnit 测试 + 真实 PKCS12 密钥库冒烟，输出留档。

.DESCRIPTION
    不依赖 Maven / Spring 容器 / 数据库，只用 javac + JUnit Platform Launcher：

      1) 密钥库不存在时调用 scripts/gen-transcript-signing-keystore.ps1 生成；
      2) 编译全部 src/main/java（--release 8，classpath 取 target\lib）；
      3) 编译并运行 TranscriptSignatureServiceTest（9 个用例：签/验/篡改/错钥/规范化/转义）；
      4) 用真实 keystore\transcript-signing.p12 冒烟：加载证书 -> 签名 -> 验签 -> 改 GPA/改签名拒绝；
      5) 结果写入 docs\transcript-signing-check.txt，任一步失败则退出码非 0。

    说明：JUnit launcher 默认从 D:\maven-repo 解析（本机离线仓库），可用 -MavenRepo 覆盖。

.EXAMPLE
    .\scripts\run-transcript-signing-check.ps1
    .\scripts\run-transcript-signing-check.ps1 -MavenRepo D:\maven-repo
#>
[CmdletBinding()]
param(
    [string]$MavenRepo = 'D:\maven-repo',
    [string]$StorePass = 'changeit',
    [string]$Alias = 'transcript-signer'
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
New-Item -ItemType Directory -Force -Path docs | Out-Null

$javaHome = (& java -XshowSettings:properties -version 2>&1 |
             Select-String '^\s*java\.home\s*=' |
             Select-Object -First 1).ToString() -replace '.*=\s*', ''
if (-not $javaHome -or -not (Test-Path (Join-Path $javaHome 'bin\javac.exe'))) {
    throw "定位不到 JDK：java.home=$javaHome"
}
Write-Host "JDK: $javaHome"
$javac = Join-Path $javaHome 'bin\javac.exe'
$java = Join-Path $javaHome 'bin\java.exe'

# ---------- 1) 签名密钥库 ----------
$keystore = 'keystore\transcript-signing.p12'
if (-not (Test-Path $keystore)) {
    Write-Host '=== 1) 生成签名专用证书 ==='
    & pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\gen-transcript-signing-keystore.ps1 -StorePass $StorePass -Alias $Alias
    if ($LASTEXITCODE -ne 0) { throw '生成签名密钥库失败' }
} else {
    Write-Host '=== 1) 复用已有签名密钥库 keystore\transcript-signing.p12 ==='
}

$work = 'target\uni-sig-check'
New-Item -ItemType Directory -Force -Path $work | Out-Null
$mainClasses = Join-Path $work 'classes'
$testClasses = Join-Path $work 'test-classes'
New-Item -ItemType Directory -Force -Path $mainClasses, $testClasses | Out-Null

# ---------- 2) 编译全部主源码 ----------
Write-Host '=== 2) 编译全部 src\main\java（--release 8） ==='
$sourceList = Join-Path $work 'sources.txt'
Get-ChildItem -Path src\main\java -Recurse -Filter *.java |
    ForEach-Object { $_.FullName } | Set-Content -Path $sourceList -Encoding utf8
& $javac -encoding UTF-8 --release 8 -parameters -cp "target/lib/*" -d $mainClasses "@$sourceList" 2>&1 | ForEach-Object { Write-Host "    $_" }
if ($LASTEXITCODE -ne 0) { throw "主源码编译失败，退出码 $LASTEXITCODE" }

# ---------- 3) JUnit 测试 ----------
$launcherCandidates = Get-ChildItem (Join-Path $MavenRepo 'org\junit\platform\junit-platform-launcher') -Recurse `
    -Filter 'junit-platform-launcher-*.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch 'sources|javadoc' }
# 优先与本机 target\lib 的 JUnit 5.8.2 / Platform 1.8.2 对齐
$launcher = $launcherCandidates | Where-Object { $_.FullName -match '1\.8\.2' } | Select-Object -First 1
if (-not $launcher) { $launcher = $launcherCandidates | Sort-Object FullName -Descending | Select-Object -First 1 }
if (-not $launcher) { throw "找不到 junit-platform-launcher（可用 -MavenRepo 指定本地仓库）" }
$launcherPath = $launcher.FullName
$cp = "target/lib/*;$mainClasses;$launcherPath"
Write-Host '=== 3) 编译并运行 JUnit 测试 ==='
& $javac -encoding UTF-8 --release 8 -parameters -cp $cp -d $testClasses `
    src\test\java\com\ds\university\sign\TranscriptSignatureServiceTest.java `
    src\test\java\com\ds\university\sign\TranscriptVerifyInputTest.java
if ($LASTEXITCODE -ne 0) { throw "测试源码编译失败，退出码 $LASTEXITCODE" }

$runnerSource = @'
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import java.io.PrintWriter;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public final class RunTests {
    public static void main(String[] args) throws Exception {
        java.util.List<org.junit.platform.engine.DiscoverySelector> selectors = new java.util.ArrayList<>();
        for (String className : args) {
            selectors.add(selectClass(Class.forName(className)));
        }
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectors)
                .build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.execute(request, listener);
        PrintWriter out = new PrintWriter(System.out);
        listener.getSummary().printTo(out);
        listener.getSummary().printFailuresTo(out);
        out.flush();
        System.exit(listener.getSummary().getTotalFailureCount() == 0 ? 0 : 1);
    }
}
'@
$runnerPath = Join-Path $work 'RunTests.java'
[IO.File]::WriteAllText($runnerPath, $runnerSource, (New-Object Text.UTF8Encoding($false)))
& $javac -encoding UTF-8 -cp $cp -d $work $runnerPath
if ($LASTEXITCODE -ne 0) { throw "测试运行器编译失败" }

$testOut = & $java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' `
    -cp "target/lib/*;$mainClasses;$testClasses;$work;$launcherPath" `
    RunTests com.ds.university.sign.TranscriptSignatureServiceTest com.ds.university.sign.TranscriptVerifyInputTest 2>&1
$testExit = $LASTEXITCODE
$testOut | ForEach-Object { Write-Host "    $_" }
if ($testExit -ne 0) { throw "JUnit 测试失败，退出码 $testExit" }

# ---------- 4) 真实密钥库冒烟 ----------
Write-Host '=== 4) 真实 PKCS12 密钥库冒烟（签名/验签/篡改） ==='
$smokeSource = @'
package com.ds.university.sign;

import com.ds.university.entity.TranscriptSignature;
import com.ds.university.mapper.TranscriptSignatureMapper;
import com.ds.university.service.TranscriptSignatureService;
import com.ds.university.vo.SignedTranscriptVO;
import com.ds.university.vo.TranscriptRowVO;
import com.ds.university.vo.TranscriptVO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class TranscriptSigningSmokeCheck {

    public static void main(String[] args) throws Exception {
        TranscriptSigningProperties props = new TranscriptSigningProperties();
        props.setKeystore(args[0]);
        props.setStorePassword(args[1]);
        props.setAlias(args[2]);
        props.setGenerateIfMissing(false);

        TranscriptSigner signer = new TranscriptSigner(props);
        signer.init();
        System.out.println("算法: " + signer.algorithm());
        System.out.println("证书指纹: " + signer.certificateFingerprint());
        System.out.println("证书序列号: " + signer.certificateSerial());

        TranscriptSignatureService service = new TranscriptSignatureService(new MemoryMapper(), signer);
        SignedTranscriptVO signed = service.sign(sample());
        System.out.println("流水号: " + signed.getSerialNo());
        System.out.println("payloadHash: " + signed.getPayloadHash());
        System.out.println("payload: " + signed.getPayload());
        System.out.println("签名(Base64 前 48 字符): " + signed.getSignature().substring(0, 48) + "...");
        System.out.println("验签(原内容): " + service.verify(signed.getPayload(), signed.getSignature()));
        String tampered = signed.getPayload().replace("\"gpa\":3.5", "\"gpa\":4.0");
        System.out.println("验签(改 GPA): " + service.verify(tampered, signed.getSignature()));
        System.out.println("验签(改签名): " + service.verify(signed.getPayload(), flip(signed.getSignature())));
        System.exit(service.verify(signed.getPayload(), signed.getSignature()) ? 0 : 1);
    }

    private static String flip(String base64) {
        char[] chars = base64.toCharArray();
        chars[chars.length / 2] = chars[chars.length / 2] == 'A' ? 'B' : 'A';
        return new String(chars);
    }

    private static TranscriptVO sample() {
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

    private static final class MemoryMapper implements TranscriptSignatureMapper {
        private final Map<String, TranscriptSignature> store = new HashMap<>();
        @Override
        public int insert(TranscriptSignature record) {
            store.put(record.getSerialNo(), record);
            return 1;
        }
        @Override
        public TranscriptSignature selectBySerialNo(String serialNo) {
            return store.get(serialNo);
        }
    }
}
'@
$smokePath = Join-Path $work 'TranscriptSigningSmokeCheck.java'
[IO.File]::WriteAllText($smokePath, $smokeSource, (New-Object Text.UTF8Encoding($false)))
& $javac -encoding UTF-8 --release 8 -cp "target/lib/*;$mainClasses" -d $work $smokePath
if ($LASTEXITCODE -ne 0) { throw "冒烟程序编译失败" }

$smokeOut = & $java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' `
    -cp "target/lib/*;$mainClasses;$work" com.ds.university.sign.TranscriptSigningSmokeCheck `
    $keystore $StorePass $Alias 2>&1
$smokeExit = $LASTEXITCODE
$smokeOut | ForEach-Object { Write-Host "    $_" }
if ($smokeExit -ne 0) { throw "冒烟失败，退出码 $smokeExit" }

# ---------- 5) 留档 ----------
$lines = New-Object System.Collections.Generic.List[string]
$lines.Add('=== 成绩单签名实现自检 ===')
$lines.Add('环境: OpenJDK ' + ((& $java -version 2>&1 | Select-Object -First 1)))
$lines.Add('密钥库: ' + (Resolve-Path $keystore))
$lines.Add('')
$lines.Add('[1] JUnit: TranscriptSignatureServiceTest')
$lines.Add(($testOut | Out-String).TrimEnd())
$lines.Add('')
$lines.Add('[2] 真实 PKCS12 冒烟')
$lines.Add(($smokeOut | Out-String).TrimEnd())
$lines | Out-File -Encoding utf8NoBOM 'docs\transcript-signing-check.txt'
Write-Host "输出: docs\transcript-signing-check.txt"

Write-Host '=== 完成 ==='
Write-Host '实现说明见 docs\成绩单签名-实现说明.md'
