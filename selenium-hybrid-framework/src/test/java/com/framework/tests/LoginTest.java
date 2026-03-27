package com.framework.tests;

import com.framework.config.EnvironmentConfig.UserRole;
import com.framework.pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * LoginTest - Pure TestNG tests for login functionality.
 *
 * Credentials are resolved from the active environment automatically:
 *   -Denv=qa      → src/test/resources/env/qa.properties
 *   -Denv=staging → src/test/resources/env/staging.properties
 *   -Denv=prod    → src/test/resources/env/prod.properties  (secrets via Azure DevOps Library)
 *
 * Test naming convention:  "TC-<ID> - <Description>"
 * The TestNGListener uses this to sync results back to Azure DevOps Test Plans.
 */
public class LoginTest extends BaseTest {

    // =========================================================================
    // TC-101  Valid login — Admin
    // =========================================================================
    @Test(
        testName    = "TC-101 - Valid Login - Admin user",
        description = "Verify admin user can login with env-specific credentials",
        groups      = { "smoke", "regression" },
        priority    = 1
    )
    public void validLoginAsAdmin() {
        String username = envConfig.getAdminUsername();
        String password = envConfig.getAdminPassword();

        logger.info("[{}] TC-101 Admin login → {}", envConfig.getActiveEnv().toUpperCase(), username);

        LoginPage loginPage = new LoginPage();
        loginPage.login(username, password);

        Assert.assertTrue(
            loginPage.isDashboardVisible(),
            "Dashboard should be visible after admin login [env=" + envConfig.getActiveEnv() + "]"
        );
    }

    // =========================================================================
    // TC-102  Valid login — Standard user
    // =========================================================================
    @Test(
        testName    = "TC-102 - Valid Login - Standard user",
        description = "Verify standard user can login with env-specific credentials",
        groups      = { "regression" },
        priority    = 2
    )
    public void validLoginAsStandardUser() {
        String username = envConfig.getStandardUsername();
        String password = envConfig.getStandardPassword();

        logger.info("[{}] TC-102 Standard login → {}", envConfig.getActiveEnv().toUpperCase(), username);

        LoginPage loginPage = new LoginPage();
        loginPage.login(username, password);

        Assert.assertTrue(
            loginPage.isDashboardVisible(),
            "Dashboard should be visible after standard user login [env=" + envConfig.getActiveEnv() + "]"
        );
    }

    // =========================================================================
    // TC-103  Valid login — Read-only user
    // =========================================================================
    @Test(
        testName    = "TC-103 - Valid Login - Read-only user",
        description = "Verify read-only user can login with env-specific credentials",
        groups      = { "regression" },
        priority    = 3
    )
    public void validLoginAsReadOnlyUser() {
        String username = envConfig.getUsername(UserRole.READONLY);
        String password = envConfig.getPassword(UserRole.READONLY);

        logger.info("[{}] TC-103 ReadOnly login → {}", envConfig.getActiveEnv().toUpperCase(), username);

        LoginPage loginPage = new LoginPage();
        loginPage.login(username, password);

        Assert.assertTrue(
            loginPage.isDashboardVisible(),
            "Dashboard should be visible after read-only login [env=" + envConfig.getActiveEnv() + "]"
        );
    }

    // =========================================================================
    // TC-104  Invalid login — wrong password (admin username, bad password)
    // =========================================================================
    @Test(
        testName    = "TC-104 - Invalid Login - Wrong password",
        description = "Verify error message when password is incorrect",
        groups      = { "smoke", "regression" },
        priority    = 4
    )
    public void invalidLoginWrongPassword() {
        String username = envConfig.getAdminUsername();
        String wrongPassword = "WrongPassword@999";

        logger.info("[{}] TC-104 Invalid login → {}", envConfig.getActiveEnv().toUpperCase(), username);

        LoginPage loginPage = new LoginPage();
        loginPage.login(username, wrongPassword);

        Assert.assertTrue(loginPage.isErrorDisplayed(),
            "Error message should appear for wrong password");
        Assert.assertEquals(loginPage.getErrorMessage(),
            "Invalid username or password.",
            "Error message text mismatch");
    }

    // =========================================================================
    // TC-105  Empty credentials
    // =========================================================================
    @Test(
        testName    = "TC-105 - Login with empty credentials",
        description = "Verify validation fires when both fields are blank",
        groups      = { "regression" },
        priority    = 5
    )
    public void emptyCredentialsTest() {
        logger.info("[{}] TC-105 Empty credentials test", envConfig.getActiveEnv().toUpperCase());

        LoginPage loginPage = new LoginPage();
        loginPage.clickLogin();

        Assert.assertTrue(loginPage.isErrorDisplayed(),
            "Validation error should appear for empty credentials");
    }

    // =========================================================================
    // TC-106  Data-driven login — all three roles must succeed on active env
    //
    // Data-driven equivalent using TestNG @DataProvider — runs once per role.
    // @DataProvider feeds role name + expected username from EnvironmentConfig
    // so the same test body runs once per row.
    // =========================================================================
    @DataProvider(name = "allRoles", parallel = false)
    public Object[][] allRoles() {
        return new Object[][] {
            { UserRole.ADMIN,    envConfig.getAdminUsername(),    envConfig.getAdminPassword()    },
            { UserRole.STANDARD, envConfig.getStandardUsername(), envConfig.getStandardPassword() },
            { UserRole.READONLY, envConfig.getReadOnlyUsername(), envConfig.getReadOnlyPassword() },
        };
    }

    @Test(
        testName     = "TC-106 - Data-driven Login - All roles",
        description  = "Verify every role can login on the active environment",
        dataProvider = "allRoles",
        groups       = { "regression" },
        priority     = 6
    )
    public void loginAllRoles(UserRole role, String username, String password) {
        logger.info("[{}] TC-106 Login as {} → {}",
            envConfig.getActiveEnv().toUpperCase(), role, username);

        LoginPage loginPage = new LoginPage();
        loginPage.login(username, password);

        Assert.assertTrue(
            loginPage.isDashboardVisible(),
            "Dashboard should be visible for role=" + role + " [env=" + envConfig.getActiveEnv() + "]"
        );
    }
}
