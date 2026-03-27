package com.framework.pages;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * LoginPage - Page Object for the Login screen.
 *
 * Azure DevOps Test Case Mapping:
 *   TC-101: Valid login
 *   TC-102: Invalid login
 *   TC-103: Empty credentials
 */
public class LoginPage extends BasePage {

    @FindBy(id = "username")
    private WebElement usernameField;

    @FindBy(id = "password")
    private WebElement passwordField;

    @FindBy(id = "login-button")
    private WebElement loginButton;

    @FindBy(css = ".error-message")
    private WebElement errorMessage;

    @FindBy(css = ".dashboard-header")
    private WebElement dashboardHeader;

    public LoginPage() {
        super();
    }

    public void enterUsername(String username) {
        type(usernameField, username, "Username field");
    }

    public void enterPassword(String password) {
        type(passwordField, password, "Password field");
    }

    public void clickLogin() {
        click(loginButton, "Login button");
    }

    public void login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
        waitForPageLoad();
    }

    public boolean isDashboardVisible() {
        return isDisplayed(dashboardHeader);
    }

    public String getErrorMessage() {
        return getText(errorMessage, "Error message");
    }

    public boolean isErrorDisplayed() {
        return isDisplayed(errorMessage);
    }
}
