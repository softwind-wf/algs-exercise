package com.ds.temporal;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;

/**
 * 离线测试入口（不依赖 Maven / IDE），与 {@code com.ds.fd.FdTestRunner} 同一个套路。
 *
 * <p>用 JUnit Platform Launcher API 跑 {@code com.ds.temporal} 下的全部测试类，
 * 打印统计与失败详情，测试失败时以非 0 退出码结束，便于脚本判定成败。
 *
 * <p>运行见 {@code scripts/run-temporal-demo.ps1}。
 */
public final class TemporalTestRunner {

    /** 需要执行的测试类（新增测试类时在这里加一行）。 */
    private static final Class<?>[] TEST_CLASSES = {
            TemporalTableTest.class,
    };

    private TemporalTestRunner() {
    }

    public static void main(String[] args) {
        LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request();
        for (Class<?> testClass : TEST_CLASSES) {
            builder.selectors(DiscoverySelectors.selectClass(testClass));
        }
        LauncherDiscoveryRequest request = builder.build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.execute(request, listener);

        TestExecutionSummary summary = listener.getSummary();
        PrintWriter out = new PrintWriter(System.out, true);
        summary.printTo(out);
        summary.printFailuresTo(out);
        System.out.println("containersFound=" + summary.getContainersFoundCount()
                + " testsFound=" + summary.getTestsFoundCount()
                + " succeeded=" + summary.getTestsSucceededCount()
                + " failed=" + summary.getTestsFailedCount()
                + " skipped=" + summary.getTestsSkippedCount());
        System.exit(summary.getTestsFailedCount() == 0 ? 0 : 1);
    }
}
