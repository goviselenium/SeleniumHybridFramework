# Feature: Login
# Azure DevOps Test Plan: Regression Suite
# Credentials are resolved per environment via EnvironmentConfig.
# Roles: admin | standard | readonly

@regression @smoke
Feature: Login Functionality

  Background:
    Given the user navigates to the login page

  @TC-101 @smoke
  Scenario: Admin user can login successfully
    When the user logs in as "admin"
    Then the user should see the dashboard

  @TC-102 @regression
  Scenario: Standard user can login successfully
    When the user logs in as "standard"
    Then the user should see the dashboard

  @TC-103 @regression
  Scenario: Read-only user can login successfully
    When the user logs in as "readonly"
    Then the user should see the dashboard

  @TC-104 @smoke
  Scenario: Login fails with wrong password
    When the user enters the username for role "admin"
    And the user enters password "WrongPassword@999"
    And the user clicks the login button
    Then the user should see an error message "Invalid username or password."

  @TC-105 @regression
  Scenario: Login fails with empty credentials
    When the user clicks the login button
    Then the user should see a validation error

  @TC-106 @regression @data-driven
  Scenario Outline: Multiple roles can login on active environment
    When the user logs in as "<role>"
    Then the user should see the dashboard

    Examples:
      | role     |
      | admin    |
      | standard |
      | readonly |

