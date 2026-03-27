package com.framework.tests;

import com.framework.pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * LoginTest - TestNG tests for login functionality.
 *
 * Azure DevOps Test Case IDs are embedded in the test name/description.
 * The TestNGListener automatically syncs results to Azure DevOps.
 *
 * Convention: Use @Test(testName = "TC-<AzureTestCaseId> - <Description>")
 *             This allows the listener to correlate results with Azure test cases.
 */
public class LoginTest extends BaseTest {

    /**
     * TC-101: Valid Login - maps to Azure DevOps Test Case ID 101
     */
    @Test(
        testName   = "TC-101 - Valid Login with correct credentials",
        description = "Verify user can login with valid username and password",
        groups     = { "smoke", "regression" },
        priority   = 1
    )
    public void validLoginTest() {
        LoginPage loginPage = new LoginPage();
        loginPage.login("admin@example.com", "Admin@123");

        Assert.assertTrue(
            loginPage.isDashboardVisible(),
            "Dashboard should be visible after successful login"
        );
    }

    /**
     * TC-102: Invalid Login - maps to Azure DevOps Test Case ID 102
     */
    @Test(
        testName   = "TC-102 - Invalid Login with wrong password",
        description = "Verify error message is shown for invalid credentials",
        groups     = { "smoke", "regression" },
        priority   = 2
    )
    public void invalidLoginTest() {
        LoginPage loginPage = new LoginPage();
        loginPage.login("admin@example.com", "WrongPassword");

        Assert.assertTrue(
            loginPage.isErrorDisplayed(),
            "Error message should appear for invalid credentials"
        );
        Assert.assertEquals(
            loginPage.getErrorMessage(),
            "Invalid username or password.",
            "Error message text mismatch"
        );
    }

    /**
     * TC-103: Empty Credentials - maps to Azure DevOps Test Case ID 103
     */
    @Test(
        testName   = "TC-103 - Login with empty credentials",
        description = "Verify validation when username and password are empty",
        groups     = { "regression" },
        priority   = 3
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
