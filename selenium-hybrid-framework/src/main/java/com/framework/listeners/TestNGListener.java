package com.framework.listeners;

import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.framework.config.ConfigManager;
import com.framework.utils.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.*;

import java.util.Arrays;

/**
 * TestNGListener - Hooks into TestNG lifecycle to:
 *   1. Create/update Extent Report entries
 *   2. Capture screenshots on failure
 *   3. Push results to Azure DevOps Test Runs
 */
public class TestNGListener implements ITestListener, ISuiteListener {

    private static final Logger logger = LogManager.getLogger(TestNGListener.class);
    private static final ConfigManager config = ConfigManager.getInstance();
    private static AzureDevOpsClient azureClient;
    private static long suiteStartTime;

    // =========================================================================
    // Suite Events
    // =========================================================================

    @Override
    public void onStart(ISuite suite) {
        suiteStartTime = System.currentTimeMillis();
        logger.info("========== Suite Started: {} ==========", suite.getName());

        if (config.isAzureEnabled()) {
            azureClient = new AzureDevOpsClient();
            int runId = azureClient.createTestRun(
                    config.getAzureTestRunName() + " - " + suite.getName(),
                    config.getAzureTestPlanId()
            );
            if (runId == -1) {
                logger.warn("Azure test run creation failed. Results won't be pushed.");
            }
        }
    }

    @Override
    public void onFinish(ISuite suite) {
        logger.info("========== Suite Finished: {} ==========", suite.getName());
        ExtentReportManager.flush();

        if (config.isAzureEnabled() && azureClient != null) {
            azureClient.completeTestRun();
        }
    }

    // =========================================================================
    // Test Events
    // =========================================================================

    @Override
    public void onTestStart(ITestResult result) {
        String testName = getFullTestName(result);
        logger.info("▶ Test Started: {}", testName);

        ExtentReportManager.createTest(
                testName,
                getTestDescription(result)
        );
        ExtentReportManager.getTest().assignCategory(
                result.getTestClass().getName()
                        .replace("com.framework.tests.", "")
        );
        ExtentReportManager.getTest().log(Status.INFO, "Test started.");
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        long duration = result.getEndMillis() - result.getStartMillis();
        String testName = getFullTestName(result);
        logger.info("✅ PASSED: {} ({}ms)", testName, duration);

        ExtentReportManager.getTest().pass("Test PASSED ✅");

        if (config.isAzureEnabled() && azureClient != null) {
            azureClient.updateResultByTitle(testName, "Passed", null, duration);
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        long duration = result.getEndMillis() - result.getStartMillis();
        String testName = getFullTestName(result);
        String error = result.getThrowable() != null
                ? result.getThrowable().getMessage() : "Unknown error";
        logger.error("❌ FAILED: {} | Error: {}", testName, error);

        // Screenshot
        try {
            var driver = DriverManager.getDriver();
            if (driver != null) {
                String base64 = ScreenshotUtil.captureBase64(driver);
                if (base64 != null) {
                    ExtentReportManager.getTest().fail(
                            "Test FAILED ❌ - " + error,
                            MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build()
                    );
                } else {
                    ExtentReportManager.getTest().fail("Test FAILED ❌ - " + error);
                }
            }
        } catch (Exception e) {
            ExtentReportManager.getTest().fail("Test FAILED ❌ - " + error);
        }

        if (result.getThrowable() != null) {
            ExtentReportManager.getTest().fail(result.getThrowable());
        }

        if (config.isAzureEnabled() && azureClient != null) {
            azureClient.updateResultByTitle(testName, "Failed", error, duration);
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        long duration = result.getEndMillis() - result.getStartMillis();
        String testName = getFullTestName(result);
        logger.warn("⏭ SKIPPED: {}", testName);

        ExtentReportManager.getTest().skip("Test SKIPPED ⏭");
        if (result.getThrowable() != null) {
            ExtentReportManager.getTest().skip(result.getThrowable());
        }

        if (config.isAzureEnabled() && azureClient != null) {
            azureClient.updateResultByTitle(testName, "NotExecuted", null, duration);
        }
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        ExtentReportManager.getTest().warning("Test failed within success percentage threshold.");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String getFullTestName(ITestResult result) {
        String params = Arrays.toString(result.getParameters());
        String base = result.getTestClass().getRealClass().getSimpleName()
                + " - " + result.getName();
        return params.equals("[]") ? base : base + " " + params;
    }

    private String getTestDescription(ITestResult result) {
        var method = result.getMethod().getConstructorOrMethod().getMethod();
        var testAnnotation = method.getAnnotation(org.testng.annotations.Test.class);
        return (testAnnotation != null && !testAnnotation.description().isEmpty())
                ? testAnnotation.description() : "";
    }
}
