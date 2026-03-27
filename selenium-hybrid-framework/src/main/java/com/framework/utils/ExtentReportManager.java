package com.framework.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.framework.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ExtentReportManager - Thread-safe Extent Reports manager.
 * Generates rich HTML reports with screenshots and test details.
 */
public class ExtentReportManager {

    private static final Logger logger = LogManager.getLogger(ExtentReportManager.class);
    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> testThread = new ThreadLocal<>();
    private static final ConfigManager config = ConfigManager.getInstance();

    private ExtentReportManager() {}

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            initReport();
        }
        return extent;
    }

    private static void initReport() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String reportPath = "reports/ExtentReport_" + timestamp + ".html";

        new File("reports").mkdirs();
        new File(config.getScreenshotPath()).mkdirs();

        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle(config.get("report.title", "Test Execution Report"));
        spark.config().setReportName(config.get("report.name", "Selenium Hybrid Framework"));
        spark.config().setEncoding("utf-8");
        spark.config().setTimeStampFormat("dd-MMM-yyyy HH:mm:ss");
        spark.config().setCss(
            ".badge-primary { background-color: #6f42c1; } " +
            ".test-status.pass { color: #00c853; } " +
            ".test-status.fail { color: #d50000; }"
        );

        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Framework", "Selenium Hybrid (TestNG)");
        extent.setSystemInfo("Browser",   config.getBrowser());
        extent.setSystemInfo("OS",        System.getProperty("os.name"));
        extent.setSystemInfo("Java",      System.getProperty("java.version"));
        extent.setSystemInfo("Env",       config.get("env", "qa"));
        extent.setSystemInfo("Base URL",  config.getBaseUrl());

        logger.info("Extent Report initialized at: {}", reportPath);
    }

    public static void createTest(String testName, String description) {
        ExtentTest test = getInstance().createTest(testName, description);
        testThread.set(test);
    }

    public static void createTest(String testName) {
        createTest(testName, "");
    }

    public static ExtentTest getTest() {
        return testThread.get();
    }

    public static void removeTest() {
        testThread.remove();
    }

    public static synchronized void flush() {
        if (extent != null) {
            extent.flush();
            logger.info("Extent Report flushed/saved.");
        }
    }
}
