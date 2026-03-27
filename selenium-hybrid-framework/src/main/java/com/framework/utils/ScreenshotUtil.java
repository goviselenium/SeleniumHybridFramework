package com.framework.utils;

import com.framework.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ScreenshotUtil - Captures screenshots and returns path for report embedding.
 */
public class ScreenshotUtil {

    private static final Logger logger = LogManager.getLogger(ScreenshotUtil.class);
    private static final ConfigManager config = ConfigManager.getInstance();

    private ScreenshotUtil() {}

    public static String capture(WebDriver driver, String testName) {
        if (!config.screenshotOnFail()) return null;

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String screenshotDir = config.getScreenshotPath();
        new File(screenshotDir).mkdirs();

        String fileName = screenshotDir + testName.replaceAll("[^a-zA-Z0-9]", "_")
                + "_" + timestamp + ".png";

        try {
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(Paths.get(fileName), screenshot);
            logger.info("Screenshot captured: {}", fileName);
            return fileName;
        } catch (IOException e) {
            logger.error("Failed to save screenshot: {}", e.getMessage());
            return null;
        }
    }

    public static String captureBase64(WebDriver driver) {
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            logger.error("Failed to capture base64 screenshot: {}", e.getMessage());
            return null;
        }
    }
}
