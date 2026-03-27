package com.framework.utils;

import com.framework.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * WaitUtil - Fluent explicit wait helpers for common WebDriver conditions.
 */
public class WaitUtil {

    private static final Logger logger = LogManager.getLogger(WaitUtil.class);
    private static final int DEFAULT_TIMEOUT = ConfigManager.getInstance().getExplicitWait();

    private WaitUtil() {}

    public static WebElement waitForVisible(WebDriver driver, By locator) {
        return getWait(driver).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForClickable(WebDriver driver, By locator) {
        return getWait(driver).until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static boolean waitForInvisible(WebDriver driver, By locator) {
        return getWait(driver).until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public static String waitForTitle(WebDriver driver, String titleContains) {
        getWait(driver).until(ExpectedConditions.titleContains(titleContains));
        return driver.getTitle();
    }

    public static boolean waitForUrl(WebDriver driver, String urlContains) {
        return getWait(driver).until(ExpectedConditions.urlContains(urlContains));
    }

    public static void waitForPageLoad(WebDriver driver) {
        getWait(driver).until((ExpectedCondition<Boolean>) d ->
                ((JavascriptExecutor) d).executeScript("return document.readyState")
                        .equals("complete"));
    }

    public static void hardWait(long millis) {
        try { Thread.sleep(millis); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static WebDriverWait getWait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
    }

    private static WebDriverWait getWait(WebDriver driver, int timeoutSec) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSec));
    }
}
