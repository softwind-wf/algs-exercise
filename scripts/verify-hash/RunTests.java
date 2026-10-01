import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/**
 * 极简 JUnit Platform 启动器:用 JDK 8 直接跑测试,避免每次启动 Maven。
 * 退出码:0 = 全部通过;1 = 有失败;2 = 一个测试都没找到(防止"0 个测试也算通过"的假通过)。
 */
public class RunTests {

    public static void main(String[] args) {
        LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request();
        for (String className : args) {
            builder.selectors(DiscoverySelectors.selectClass(className));
        }
        LauncherDiscoveryRequest request = builder.build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.execute(request, listener);

        TestExecutionSummary summary = listener.getSummary();
        System.out.println("FOUND=" + summary.getTestsFoundCount()
                + " STARTED=" + summary.getTestsStartedCount()
                + " SUCCEEDED=" + summary.getTestsSucceededCount()
                + " FAILED=" + summary.getTestsFailedCount());
        for (TestExecutionSummary.Failure failure : summary.getFailures()) {
            System.out.println("  FAILED: " + failure.getTestIdentifier().getDisplayName()
                    + " | " + String.valueOf(failure.getException()).replace('\n', ' '));
        }
        if (summary.getTestsFoundCount() == 0) {
            System.out.println("NO TESTS FOUND: 视为失败");
            System.exit(2);
        }
        System.exit(summary.getTestsFailedCount() > 0 ? 1 : 0);
    }
}
