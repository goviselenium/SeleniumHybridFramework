package com.framework.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * ConfigManager - Loads and provides access to framework configuration.
 * Supports system property overrides for CI/CD pipeline injection.
 */
public class ConfigManager {

    private static final Logger logger = LogManager.getLogger(ConfigManager.class);
    private static ConfigManager instance;
    private final Properties properties = new Properties();

    private static final String CONFIG_FILE = "src/test/resources/config.properties";

    private ConfigManager() {
        loadProperties();
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }

    private void loadProperties() {
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
            logger.info("Configuration loaded from: {}", CONFIG_FILE);
        } catch (IOException e) {
            logger.error("Failed to load config.properties: {}", e.getMessage());
            throw new RuntimeException("Config file not found: " + CONFIG_FILE);
        }
    }

    /**
     * Get property value. System property takes precedence over config file.
     * This allows Azure Pipeline variables to override config values.
     */
    public String get(String key) {
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isEmpty()) {
            return sysProp;
        }
        String envVar = System.getenv(key.replace(".", "_").toUpperCase());
        if (envVar != null && !envVar.isEmpty()) {
            return envVar;
        }
        return properties.getProperty(key);
    }

    public String get(String key, String defaultValue) {
        String value = get(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return (value != null) ? Boolean.parseBoolean(value) : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        String value = get(key);
        try {
            return (value != null) ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ---- Convenience Getters ----

    public String getBrowser()          { return get("browser", "chrome"); }
    public boolean isHeadless()         { return getBoolean("headless", false); }
    public String getBaseUrl()          { return get("base.url"); }
    public int getImplicitWait()        { return getInt("implicit.wait", 10); }
    public int getExplicitWait()        { return getInt("explicit.wait", 20); }
    public int getPageLoadTimeout()     { return getInt("page.load.timeout", 30); }
    public boolean screenshotOnFail()   { return getBoolean("screenshot.on.failure", true); }
    public String getScreenshotPath()   { return get("screenshot.path", "reports/screenshots/"); }
    public String getReportPath()       { return get("report.path", "reports/ExtentReport.html"); }

    // Active environment
    public String getEnv()              { return get("env", "qa").toLowerCase().trim(); }

    // Azure DevOps
    public String getAzureOrgUrl()      { return get("azure.org.url"); }
    public String getAzureProject()     { return get("azure.project.name"); }
    public String getAzurePat()         { return get("azure.pat"); }
    public int getAzureTestPlanId()     { return getInt("azure.test.plan.id", 0); }
    public String getAzureTestRunName() { return get("azure.test.run.name", "Automated Run"); }
    public boolean isAzureEnabled()     { return getBoolean("azure.update.enabled", false); }
}
