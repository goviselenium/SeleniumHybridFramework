package com.framework.stepdefs;

import com.aventstack.extentreports.Status;
import com.framework.pages.LoginPage;
import com.framework.utils.AzureDevOpsClient;
import com.framework.utils.DriverManager;
import com.framework.utils.ExtentReportManager;
import com.framework.config.ConfigManager;
import io.cucumber.java.*;
import io.cucumber.java.en.*;
import org.testng.Assert;

/**
 * LoginSteps - Cucumber step definitions for Login feature.
 * Hooks automatically update Extent Reports and Azure DevOps.
 */
public class LoginSteps {

    private LoginPage loginPage;
    private String currentTestName;
    private long testStartTime;
    private static AzureDevOpsClient azureClient;
    private static final ConfigManager config = ConfigManager.getInstance();

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
        DriverManager.getDriver().get(config.getBaseUrl());

        ExtentReportManager.createTest(currentTestName);
        ExtentReportManager.getTest().log(Status.INFO,
                "Scenario: " + currentTestName);

        // Tag-based Azure test case linking
        scenario.getSourceTagNames().stream()
                .filter(tag -> tag.startsWith("@TC-"))
                .forEach(tag -> ExtentReportManager.getTest()
                        .log(Status.INFO, "Azure Test Case: " + tag.replace("@", "")));
    }

    @After
    public void afterScenario(Scenario scenario) {
        long duration = System.currentTimeMillis() - testStartTime;

        if (scenario.isFailed()) {
            // Capture screenshot on failure
            byte[] screenshot = ((org.openqa.selenium.TakesScreenshot)
                    DriverManager.getDriver()).getScreenshotAs(
                    org.openqa.selenium.OutputType.BYTES);
            scenario.attach(screenshot, "image/png", "Failure Screenshot");
            ExtentReportManager.getTest().fail("Scenario FAILED ❌");
        } else {
            ExtentReportManager.getTest().pass("Scenario PASSED ✅");
        }

        // Push to Azure DevOps
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
        ExtentReportManager.getTest().log(Status.INFO, "Navigated to login page.");
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
                "Dashboard should be visible after login.");
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
                "Validation error should appear.");
        ExtentReportManager.getTest().pass("Validation error displayed ✅");
    }
}
