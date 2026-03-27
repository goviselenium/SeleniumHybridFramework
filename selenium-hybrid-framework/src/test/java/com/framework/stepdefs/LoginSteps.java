package com.framework.stepdefs;

import com.aventstack.extentreports.Status;
import com.framework.config.ConfigManager;
import com.framework.config.EnvironmentConfig;
import com.framework.config.EnvironmentConfig.UserRole;
import com.framework.pages.LoginPage;
import com.framework.utils.AzureDevOpsClient;
import com.framework.utils.DriverManager;
import com.framework.utils.ExtentReportManager;
import io.cucumber.java.*;
import io.cucumber.java.en.*;
import org.testng.Assert;

/**
 * LoginSteps - Cucumber step definitions for Login feature.
 *
 * Credentials are resolved from the active environment:
 *   -Denv=qa      → qa.properties
 *   -Denv=staging → staging.properties
 *   -Denv=prod    → prod.properties  (secrets injected via Azure DevOps Library)
 *
 * Step "the user logs in as {string}" accepts role names:
 *   "admin" | "standard" | "readonly"
 */
public class LoginSteps {

    private LoginPage loginPage;
    private String currentTestName;
    private long testStartTime;

    private static AzureDevOpsClient azureClient;
    private static final ConfigManager config    = ConfigManager.getInstance();
    private static final EnvironmentConfig envCfg = EnvironmentConfig.getInstance();

    // =========================================================================
    // Cucumber Hooks
    // =========================================================================

    @BeforeAll
    public static void beforeAll() {
        if (config.isAzureEnabled()) {
            azureClient = new AzureDevOpsClient();
            azureClient.createTestRun(
                config.getAzureTestRunName(),
                config.getAzureTestPlanId()
            );
        }
    }

    @Before
    public void beforeScenario(Scenario scenario) {
        currentTestName = scenario.getName();
        testStartTime   = System.currentTimeMillis();

        DriverManager.initDriver();
        DriverManager.getDriver().get(envCfg.getBaseUrl());

        ExtentReportManager.createTest(currentTestName);
        ExtentReportManager.getTest().log(Status.INFO,
            String.format("Scenario: %s | ENV: %s | URL: %s",
                currentTestName,
                envCfg.getActiveEnv().toUpperCase(),
                envCfg.getBaseUrl())
        );

        // Log Azure TC tag if present
        scenario.getSourceTagNames().stream()
            .filter(tag -> tag.startsWith("@TC-"))
            .forEach(tag -> ExtentReportManager.getTest()
                .log(Status.INFO, "Azure Test Case: " + tag.replace("@", "")));
    }

    @After
    public void afterScenario(Scenario scenario) {
        long duration = System.currentTimeMillis() - testStartTime;

        if (scenario.isFailed()) {
            byte[] screenshot = ((org.openqa.selenium.TakesScreenshot)
                DriverManager.getDriver()).getScreenshotAs(
                org.openqa.selenium.OutputType.BYTES);
            scenario.attach(screenshot, "image/png", "Failure Screenshot");
            ExtentReportManager.getTest().fail("Scenario FAILED ❌");
        } else {
            ExtentReportManager.getTest().pass("Scenario PASSED ✅");
        }

        if (config.isAzureEnabled() && azureClient != null) {
            String outcome = scenario.isFailed() ? "Failed" : "Passed";
            String error   = scenario.isFailed() ? "Scenario failed: " + currentTestName : null;
            azureClient.updateResultByTitle(currentTestName, outcome, error, duration);
        }

        ExtentReportManager.removeTest();
        DriverManager.quitDriver();
    }

    @AfterAll
    public static void afterAll() {
        ExtentReportManager.flush();
        if (config.isAzureEnabled() && azureClient != null) {
            azureClient.completeTestRun();
        }
    }

    // =========================================================================
    // Step Definitions
    // =========================================================================

    @Given("the user navigates to the login page")
    public void navigateToLoginPage() {
        loginPage = new LoginPage();
        ExtentReportManager.getTest().log(Status.INFO,
            "Navigated to login page on ENV: " + envCfg.getActiveEnv().toUpperCase());
    }

    /**
     * Role-based login step — resolves username/password from the active env.
     * Accepted roles: "admin" | "standard" | "readonly"
     */
    @When("the user logs in as {string}")
    public void loginAsRole(String role) {
        UserRole userRole = resolveRole(role);
        String username   = envCfg.getUsername(userRole);
        String password   = envCfg.getPassword(userRole);

        ExtentReportManager.getTest().log(Status.INFO,
            String.format("Logging in as [%s] user: %s on ENV: %s",
                role, username, envCfg.getActiveEnv().toUpperCase()));

        loginPage.login(username, password);
    }

    /**
     * Enters only the username for the given role (for negative test scenarios).
     */
    @When("the user enters the username for role {string}")
    public void enterUsernameForRole(String role) {
        UserRole userRole = resolveRole(role);
        String username   = envCfg.getUsername(userRole);

        ExtentReportManager.getTest().log(Status.INFO,
            String.format("Entering username for role [%s]: %s", role, username));
        loginPage.enterUsername(username);
    }

    @When("the user enters username {string}")
    public void enterUsername(String username) {
        loginPage.enterUsername(username);
    }

    @When("the user enters password {string}")
    public void enterPassword(String password) {
        loginPage.enterPassword(password);
    }

    @When("the user clicks the login button")
    public void clickLoginButton() {
        loginPage.clickLogin();
    }

    @Then("the user should see the dashboard")
    public void verifyDashboardVisible() {
        Assert.assertTrue(loginPage.isDashboardVisible(),
            "Dashboard should be visible after login on env: " + envCfg.getActiveEnv());
        ExtentReportManager.getTest().pass("Dashboard is visible ✅");
    }

    @Then("the user should see an error message {string}")
    public void verifyErrorMessage(String expectedMessage) {
        Assert.assertTrue(loginPage.isErrorDisplayed(),
            "Error message should be displayed.");
        Assert.assertEquals(loginPage.getErrorMessage(), expectedMessage,
            "Error message text mismatch.");
        ExtentReportManager.getTest().pass("Error message verified: " + expectedMessage);
    }

    @Then("the user should see a validation error")
    public void verifyValidationError() {
        Assert.assertTrue(loginPage.isErrorDisplayed(),
            "Validation error should appear for empty credentials.");
        ExtentReportManager.getTest().pass("Validation error displayed ✅");
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private UserRole resolveRole(String role) {
        return switch (role.toLowerCase().trim()) {
            case "admin"    -> UserRole.ADMIN;
            case "standard" -> UserRole.STANDARD;
            case "readonly" -> UserRole.READONLY;
            default -> throw new IllegalArgumentException(
                "Unknown role: '" + role + "'. Use: admin | standard | readonly");
        };
    }
}

