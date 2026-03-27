package com.framework.tests;

import com.framework.config.EnvironmentConfig.UserRole;
import com.framework.pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * LoginTest - TestNG tests for login functionality.
 *
 * Credentials are resolved from the active environment automatically:
 *   -Denv=qa      → src/test/resources/env/qa.properties
 *   -Denv=staging → src/test/resources/env/staging.properties
 *   -Denv=prod    → src/test/resources/env/prod.properties
 *
 * In Azure DevOps pipelines, secret credentials are injected as
 * system properties or environment variables and take top priority.
 *
 * Azure Test Case mapping: TC-101, TC-102, TC-103
 */
public class LoginTest extends BaseTest {

    /**
     * TC-101: Admin user valid login
     */
    @Test(
        testName    = "TC-101 - Valid Login - Admin user",
        description = "Verify admin user can login with env-specific credentials",
        groups      = { "smoke", "regression" },
        priority    = 1
    )
    public void validLoginAsAdmin() {
        String username = envConfig.getAdminUsername();
        String password = envConfig.getAdminPassword();

        logger.info("[{}] Admin login → user: {}", envConfig.getActiveEnv().toUpperCase(), username);

        LoginPage loginPage = new LoginPage();
        loginPage.login(username, password);

        Assert.assertTrue(
            loginPage.isDashboardVisible(),
            "Dashboard should be visible after admin login on env: " + envConfig.getActiveEnv()
        );
    }

    /**
     * TC-102: Standard user valid login
     */
    @Test(
        testName    = "TC-102 - Valid Login - Standard user",
        description = "Verify standard user can login with env-specific credentials",
        groups      = { "regression" },
        priority    = 2
    )
    public void validLoginAsStandardUser() {
        String username = envConfig.getStandardUsername();
        String password = envConfig.getStandardPassword();

        logger.info("[{}] Standard login → user: {}", envConfig.getActiveEnv().toUpperCase(), username);

        LoginPage loginPage = new LoginPage();
        loginPage.login(username, password);

        Assert.assertTrue(
            loginPage.isDashboardVisible(),
            "Dashboard should be visible after standard user login on env: " + envConfig.getActiveEnv()
        );
    }

    /**
     * TC-103: Read-only user valid login
     */
    @Test(
        testName    = "TC-103 - Valid Login - Read-only user",
        description = "Verify read-only user can login with env-specific credentials",
        groups      = { "regression" },
        priority    = 3
    )
    public void validLoginAsReadOnlyUser() {
        String username = envConfig.getUsername(UserRole.READONLY);
        String password = envConfig.getPassword(UserRole.READONLY);

        logger.info("[{}] ReadOnly login → user: {}", envConfig.getActiveEnv().toUpperCase(), username);

        LoginPage loginPage = new LoginPage();
        loginPage.login(username, password);

        Assert.assertTrue(
            loginPage.isDashboardVisible(),
            "Dashboard should be visible after read-only login on env: " + envConfig.getActiveEnv()
        );
    }

    /**
     * TC-104: Invalid login - wrong password
     */
    @Test(
        testName    = "TC-104 - Invalid Login - wrong password",
        description = "Verify error shown when password is incorrect (uses admin username from env)",
        groups      = { "smoke", "regression" },
        priority    = 4
    )
    public void invalidLoginWrongPassword() {
        String username = envConfig.getAdminUsername(); // correct user, wrong pass
        String wrongPassword = "WrongPassword@999";

        logger.info("[{}] Invalid login test → user: {}", envConfig.getActiveEnv().toUpperCase(), username);

        LoginPage loginPage = new LoginPage();
        loginPage.login(username, wrongPassword);

        Assert.assertTrue(
            loginPage.isErrorDisplayed(),
            "Error message should appear for wrong password"
        );
        Assert.assertEquals(
            loginPage.getErrorMessage(),
            "Invalid username or password.",
            "Error message text mismatch"
        );
    }

    /**
     * TC-105: Empty credentials
     */
    @Test(
        testName    = "TC-105 - Login with empty credentials",
        description = "Verify validation fires when username and password are empty",
        groups      = { "regression" },
        priority    = 5
    )
    public void emptyCredentialsTest() {
        LoginPage loginPage = new LoginPage();
        loginPage.clickLogin();

        Assert.assertTrue(
            loginPage.isErrorDisplayed(),
            "Validation error should appear for empty credentials"
        );
    }
}

