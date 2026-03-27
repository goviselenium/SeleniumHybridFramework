package com.framework.tests;

import com.framework.config.ConfigManager;
import com.framework.utils.DriverManager;
import com.framework.utils.ExtentReportManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.*;

/**
 * BaseTest - Parent class for all TestNG test classes.
 * Handles driver setup/teardown. Listener handles reporting.
 */
@Listeners(com.framework.listeners.TestNGListener.class)
public abstract class BaseTest {

    protected static final Logger logger = LogManager.getLogger(BaseTest.class);
    protected static final ConfigManager config = ConfigManager.getInstance();
    protected WebDriver driver;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        DriverManager.initDriver();
        driver = DriverManager.getDriver();
        driver.get(config.getBaseUrl());
        logger.info("Navigated to base URL: {}", config.getBaseUrl());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        ExtentReportManager.removeTest();
        DriverManager.quitDriver();
    }
}
