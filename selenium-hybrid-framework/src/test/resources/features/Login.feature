# Feature: Login
# Azure DevOps Test Plan: Regression Suite
# Maps to Azure Test Cases: TC-101, TC-102, TC-103

@regression @smoke
Feature: Login Functionality

  Background:
    Given the user navigates to the login page

  @TC-101 @smoke
  Scenario: Successful login with valid credentials
    When the user enters username "admin@example.com"
    And the user enters password "Admin@123"
    And the user clicks the login button
    Then the user should see the dashboard

  @TC-102
  Scenario: Failed login with invalid credentials
    When the user enters username "admin@example.com"
    And the user enters password "WrongPassword"
    And the user clicks the login button
    Then the user should see an error message "Invalid username or password."

  @TC-103
  Scenario: Login with empty credentials
    When the user clicks the login button
    Then the user should see a validation error

  @TC-104 @data-driven
  Scenario Outline: Login with multiple invalid credentials
    When the user enters username "<username>"
    And the user enters password "<password>"
    And the user clicks the login button
    Then the user should see an error message "<errorMessage>"

    Examples:
      | username              | password      | errorMessage                     |
      | invalid@test.com      | Admin@123     | Invalid username or password.    |
      | admin@example.com     | short         | Invalid username or password.    |
      | notexist@example.com  | NoPass@123    | Invalid username or password.    |
