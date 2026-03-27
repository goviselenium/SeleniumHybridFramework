package com.framework.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * EnvironmentConfig - Loads the environment-specific properties file
 * (qa.properties | staging.properties | prod.properties) based on the
 * "env" value set in config.properties or passed via -Denv=<value>.
 *
 * Credential resolution order (highest → lowest priority):
 *   1. System property  (-Dlogin.admin.username=...)   ← CI/CD secret injection
 *   2. Environment var  (LOGIN_ADMIN_USERNAME=...)      ← Docker / pipeline env
 *   3. env/<env>.properties file                       ← per-environment defaults
 *
 * Usage:
 *   EnvironmentConfig env = EnvironmentConfig.getInstance();
 *   env.getUsername(UserRole.ADMIN)    // → "qa-admin@example.com"
 *   env.getPassword(UserRole.STANDARD) // → "QaUser@123"
 *   env.getBaseUrl()                   // → "https://qa.your-app.com"
 */
public class EnvironmentConfig {

    private static final Logger logger = LogManager.getLogger(EnvironmentConfig.class);
    private static EnvironmentConfig instance;

    private final Properties envProps = new Properties();
    private final String activeEnv;

    // -------------------------------------------------------------------------
    // User Roles - maps to property key prefixes in env/*.properties
    // -------------------------------------------------------------------------
    public enum UserRole {
        ADMIN    ("login.admin"),
        STANDARD ("login.standard"),
        READONLY ("login.readonly");

        private final String prefix;
        UserRole(String prefix) { this.prefix = prefix; }
        public String getPrefix() { return prefix; }
    }

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------
    private EnvironmentConfig() {
        this.activeEnv = ConfigManager.getInstance().get("env", "qa").toLowerCase().trim();
        loadEnvProperties();
    }

    public static EnvironmentConfig getInstance() {
        if (instance == null) {
            synchronized (EnvironmentConfig.class) {
                if (instance == null) {
                    instance = new EnvironmentConfig();
                }
            }
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Load env/<env>.properties
    // -------------------------------------------------------------------------
    private void loadEnvProperties() {
        String filePath = "src/test/resources/env/" + activeEnv + ".properties";
        try (FileInputStream fis = new FileInputStream(filePath)) {
            envProps.load(fis);
            logger.info("Environment config loaded: {} ({})", activeEnv.toUpperCase(), filePath);
        } catch (IOException e) {
            logger.error("Could not load env properties for '{}': {}", activeEnv, e.getMessage());
            throw new RuntimeException(
                "Environment config file not found: " + filePath +
                "\nSupported envs: qa | staging | prod"
            );
        }
    }

    // -------------------------------------------------------------------------
    // Property resolution (sys prop → env var → file)
    // -------------------------------------------------------------------------
    private String resolve(String key) {
        // 1. System property (-Dkey=value)
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isEmpty()) {
            logger.debug("Resolved '{}' from system property.", key);
            return sysProp;
        }
        // 2. Environment variable (KEY_WITH_UNDERSCORES)
        String envKey = key.replace(".", "_").toUpperCase();
        String envVar = System.getenv(envKey);
        if (envVar != null && !envVar.isEmpty()) {
            logger.debug("Resolved '{}' from environment variable '{}'.", key, envKey);
            return envVar;
        }
        // 3. env/<env>.properties file
        String fileProp = envProps.getProperty(key);
        if (fileProp != null && !fileProp.isEmpty()) {
            return fileProp;
        }
        logger.warn("Property '{}' not found for env '{}'.", key, activeEnv);
        return null;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Active environment name: qa | staging | prod */
    public String getActiveEnv() {
        return activeEnv;
    }

    /** Base URL for the active environment */
    public String getBaseUrl() {
        // env-specific URL overrides the global config.properties base.url
        String envUrl = resolve("base.url");
        return (envUrl != null) ? envUrl : ConfigManager.getInstance().getBaseUrl();
    }

    /** Username for a given role in the active environment */
    public String getUsername(UserRole role) {
        return resolve(role.getPrefix() + ".username");
    }

    /** Password for a given role in the active environment */
    public String getPassword(UserRole role) {
        return resolve(role.getPrefix() + ".password");
    }

    // -------------------------------------------------------------------------
    // Convenience shortcuts
    // -------------------------------------------------------------------------

    public String getAdminUsername()    { return getUsername(UserRole.ADMIN); }
    public String getAdminPassword()    { return getPassword(UserRole.ADMIN); }

    public String getStandardUsername() { return getUsername(UserRole.STANDARD); }
    public String getStandardPassword() { return getPassword(UserRole.STANDARD); }

    public String getReadOnlyUsername() { return getUsername(UserRole.READONLY); }
    public String getReadOnlyPassword() { return getPassword(UserRole.READONLY); }

    @Override
    public String toString() {
        return String.format("EnvironmentConfig[env=%s, baseUrl=%s, adminUser=%s]",
                activeEnv, getBaseUrl(), getAdminUsername());
    }
}
