package com.framework.listeners;

import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.framework.config.ConfigManager;
import com.framework.config.EnvironmentConfig;
import com.framework.utils.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.*;

import java.util.Arrays;

/**
 * TestNGListener - Hooks into the full TestNG lifecycle to:
 *   1. Create and update Extent Report entries per test
 *   2. Capture a Base64 screenshot on failure and embed it in the report
 *   3. Push Pass / Fail / Skip outcomes to Azure DevOps Test Plans via REST API
 *
 * Registered in two ways:
 *   a) testng.xml  <listener class-name="...TestNGListener"/>
 *   b) @Listeners(TestNGListener.class) on BaseTest (fallback for IDE runs)
 *
 * The Azure DevOps test run is opened when the suite starts and closed when
 * the suite finishes, so all individual test results land in a single run.
 */
public class TestNGListener implements ITestListener, ISuiteListener {

    private static final Logger logger = LogManager.getLogger(TestNGListener.class);
    private static final ConfigManager config    = ConfigManager.getInstance();
    private static final EnvironmentConfig envCfg = EnvironmentConfig.getInstance();

    private static AzureDevOpsClient azureClient;

    // =========================================================================
    // ISuiteListener — one Azure run per suite execution
    // =========================================================================

    @Override
    public void onStart(ISuite suite) {
        logger.info("========== Suite Started: {} | ENV: {} ==========",
                suite.getName(), envCfg.getActiveEnv().toUpperCase());

        if (config.isAzureEnabled()) {
            azureClient = new AzureDevOpsClient();
            String runName = config.getAzureTestRunName()
                    + " [" + envCfg.getActiveEnv().toUpperCase() + "] - " + suite.getName();
            int runId = azureClient.createTestRun(runName, config.getAzureTestPlanId());
            if (runId == -1) {
                logger.warn("⚠ Azure test run creation failed — results will not be pushed.");
            }
        }
    }

    @Override
    public void onFinish(ISuite suite) {
        logger.info("========== Suite Finished: {} ==========", suite.getName());
        ExtentReportManager.flush();

        if (config.isAzureEnabled() && azureClient != null) {
            azureClient.completeTestRun();
            logger.info("✅ Azure DevOps test run marked as Completed.");
        }
    }

    // =========================================================================
    // ITestListener — one Extent node + one Azure result per test method
    // =========================================================================

    @Override
    public void onTestStart(ITestResult result) {
        String testName = resolveTestName(result);
        logger.info("▶  START  : {}", testName);

        ExtentReportManager.createTest(testName, descriptionOf(result));

        // Assign class name as category for easier filtering in the HTML report
        ExtentReportManager.getTest()
                .assignCategory(result.getTestClass().getRealClass().getSimpleName());

        ExtentReportManager.getTest().log(Status.INFO,
                "ENV: " + envCfg.getActiveEnv().toUpperCase()
                + " | Base URL: " + envCfg.getBaseUrl());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        long ms       = result.getEndMillis() - result.getStartMillis();
        String name   = resolveTestName(result);
        logger.info("✅ PASSED  : {} ({}ms)", name, ms);

        ExtentReportManager.getTest().pass("Test PASSED ✅  (" + ms + " ms)");

        if (config.isAzureEnabled() && azureClient != null) {
            azureClient.updateResultByTitle(name, "Passed", null, ms);
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        long ms     = result.getEndMillis() - result.getStartMillis();
        String name = resolveTestName(result);
        String err  = result.getThrowable() != null
                ? result.getThrowable().getMessage() : "Unknown error";

        logger.error("❌ FAILED  : {} | {}", name, err);

        // Embed screenshot taken at moment of failure
        try {
            var driver = DriverManager.getDriver();
            if (driver != null) {
                String b64 = ScreenshotUtil.captureBase64(driver);
                if (b64 != null) {
                    ExtentReportManager.getTest().fail(
                            "Test FAILED ❌  (" + ms + " ms) — " + err,
                            MediaEntityBuilder.createScreenCaptureFromBase64String(b64).build()
                    );
                } else {
                    ExtentReportManager.getTest().fail("Test FAILED ❌  — " + err);
                }
            }
        } catch (Exception ignored) {
            ExtentReportManager.getTest().fail("Test FAILED ❌  — " + err);
        }

        if (result.getThrowable() != null) {
            ExtentReportManager.getTest().fail(result.getThrowable());
        }

        if (config.isAzureEnabled() && azureClient != null) {
            azureClient.updateResultByTitle(name, "Failed", err, ms);
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        long ms     = result.getEndMillis() - result.getStartMillis();
        String name = resolveTestName(result);
        logger.warn("⏭  SKIPPED : {}", name);

        ExtentReportManager.getTest().skip("Test SKIPPED ⏭");
        if (result.getThrowable() != null) {
            ExtentReportManager.getTest().skip(result.getThrowable());
        }

        if (config.isAzureEnabled() && azureClient != null) {
            azureClient.updateResultByTitle(name, "NotExecuted", null, ms);
        }
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        ExtentReportManager.getTest().warning("Failed within acceptable success percentage.");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Builds the display/lookup name for this test result.
     *
     * For regular tests  → "TC-101 - Valid Login - Admin user"   (from @Test testName)
     * For @DataProvider  → "TC-106 - Data-driven Login - All roles [ADMIN, qa-admin@..., ***]"
     *
     * The testName attribute is used first so it matches the Azure DevOps test case title exactly.
     */
    private String resolveTestName(ITestResult result) {
        // Prefer the explicit testName attribute set on @Test
        String explicit = result.getMethod().getTestName();
        String base     = (explicit != null && !explicit.isBlank())
                ? explicit
                : result.getTestClass().getRealClass().getSimpleName() + " - " + result.getName();

        // Append parameter summary for data-driven rows (mask passwords)
        Object[] params = result.getParameters();
        if (params != null && params.length > 0) {
            String paramStr = Arrays.stream(params)
                    .map(p -> {
                        String s = String.valueOf(p);
                        // Basic password masking: hide values that look like passwords
                        return (s.length() > 6 && s.matches(".*[@#!].*")) ? "***" : s;
                    })
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            return base + " [" + paramStr + "]";
        }
        return base;
    }

    private String descriptionOf(ITestResult result) {
        try {
            var ann = result.getMethod()
                    .getConstructorOrMethod()
                    .getMethod()
                    .getAnnotation(org.testng.annotations.Test.class);
            return (ann != null && !ann.description().isEmpty()) ? ann.description() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
