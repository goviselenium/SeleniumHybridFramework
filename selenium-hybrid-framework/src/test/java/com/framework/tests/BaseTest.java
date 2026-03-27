package com.framework.tests;

import com.framework.config.ConfigManager;
import com.framework.config.EnvironmentConfig;
import com.framework.utils.DriverManager;
import com.framework.utils.ExtentReportManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.*;

/**
 * BaseTest - Parent class for all TestNG test classes.
 * Handles driver setup/teardown. Listener handles reporting.
 *
 * Exposes envConfig so every subclass can call:
 *   envConfig.getAdminUsername()
 *   envConfig.getPassword(UserRole.STANDARD)
 */
@Listeners(com.framework.listeners.TestNGListener.class)
public abstract class BaseTest {

    protected static final Logger logger        = LogManager.getLogger(BaseTest.class);
    protected static final ConfigManager config = ConfigManager.getInstance();
    protected static final EnvironmentConfig envConfig = EnvironmentConfig.getInstance();
    protected WebDriver driver;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        DriverManager.initDriver();
        driver = DriverManager.getDriver();
        // Use env-specific base URL (qa / staging / prod)
        String baseUrl = envConfig.getBaseUrl();
        driver.get(baseUrl);
        logger.info("ENV: {} | Navigated to: {}", envConfig.getActiveEnv().toUpperCase(), baseUrl);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        ExtentReportManager.removeTest();
        DriverManager.quitDriver();
    }
}
