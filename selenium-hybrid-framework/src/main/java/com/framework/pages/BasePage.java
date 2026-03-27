package com.framework.pages;

import com.aventstack.extentreports.Status;
import com.framework.utils.DriverManager;
import com.framework.utils.ExtentReportManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * BasePage - Parent class for all Page Objects.
 * Provides common WebDriver actions with logging and report integration.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final Logger logger = LogManager.getLogger(getClass());

    protected BasePage() {
        this.driver = DriverManager.getDriver();
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(20));
        PageFactory.initElements(driver, this);
    }

    // -------------------------------------------------------------------------
    // Core Actions
    // -------------------------------------------------------------------------

    protected void click(WebElement element, String elementName) {
        wait.until(ExpectedConditions.elementToBeClickable(element));
        element.click();
        log("Clicked: " + elementName);
    }

    protected void type(WebElement element, String text, String elementName) {
        wait.until(ExpectedConditions.visibilityOf(element));
        element.clear();
        element.sendKeys(text);
        log("Typed '" + text + "' into: " + elementName);
    }

    protected String getText(WebElement element, String elementName) {
        wait.until(ExpectedConditions.visibilityOf(element));
        String text = element.getText();
        log("Got text from " + elementName + ": " + text);
        return text;
    }

    protected boolean isDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    protected void navigateTo(String url) {
        driver.get(url);
        log("Navigated to: " + url);
    }

    protected String getTitle() {
        return driver.getTitle();
    }

    protected WebElement waitForVisibility(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void waitForPageLoad() {
        wait.until(d -> ((JavascriptExecutor) d)
                .executeScript("return document.readyState").equals("complete"));
    }

    // -------------------------------------------------------------------------
    // Logging to console + Extent Report
    // -------------------------------------------------------------------------

    protected void log(String message) {
        logger.info(message);
        if (ExtentReportManager.getTest() != null) {
            ExtentReportManager.getTest().log(Status.INFO, message);
        }
    }
}
