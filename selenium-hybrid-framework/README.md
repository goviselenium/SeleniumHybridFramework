# Selenium Hybrid Framework with Azure DevOps Integration

A production-ready Java Maven Selenium hybrid test automation framework integrating **TestNG**, **Cucumber BDD**, **ExtentReports**, and **Azure DevOps Test Plans** for end-to-end automated testing with real-time result publishing.

---

## 📁 Project Structure

```
selenium-hybrid-framework/
├── .azure/
│   └── azure-pipelines.yml          # Azure DevOps CI/CD pipeline
├── src/
│   ├── main/java/com/framework/
│   │   ├── config/
│   │   │   └── ConfigManager.java   # Centralized config (supports env/sys overrides)
│   │   ├── listeners/
│   │   │   └── TestNGListener.java  # Hooks: Extent Reports + Azure DevOps sync
│   │   ├── pages/
│   │   │   ├── BasePage.java        # Parent POM with common WebDriver actions
│   │   │   └── LoginPage.java       # Sample Page Object
│   │   └── utils/
│   │       ├── DriverManager.java       # Thread-safe WebDriver (ThreadLocal)
│   │       ├── ExtentReportManager.java # Extent Reports setup & management
│   │       ├── AzureDevOpsClient.java   # Azure DevOps REST API integration
│   │       └── ScreenshotUtil.java      # Screenshot capture utility
│   └── test/
│       ├── java/com/framework/
│       │   ├── tests/
│       │   │   ├── BaseTest.java        # TestNG base class (setup/teardown)
│       │   │   ├── LoginTest.java       # Sample TestNG tests with TC IDs
│       │   │   └── CucumberRunner.java  # Cucumber-TestNG runner
│       │   └── stepdefs/
│       │       └── LoginSteps.java      # Cucumber step definitions
│       └── resources/
│           ├── features/
│           │   └── Login.feature        # BDD feature file with TC tags
│           ├── config.properties        # Framework configuration
│           └── log4j2.xml               # Logging configuration
├── reports/                             # Generated at runtime
│   ├── ExtentReport_<timestamp>.html
│   ├── cucumber-html-report.html
│   ├── screenshots/
│   └── logs/
├── testng.xml                           # TestNG suite configuration
└── pom.xml                              # Maven dependencies
```

---

## ⚙️ Setup & Configuration

### 1. Prerequisites
- Java 11+
- Maven 3.8+
- Chrome / Firefox / Edge browser

### 2. Clone & Install
```bash
git clone https://github.com/your-org/selenium-hybrid-framework.git
cd selenium-hybrid-framework
mvn clean install -DskipTests
```

### 3. Configure `src/test/resources/config.properties`

```properties
# Browser & App
browser=chrome
headless=false
base.url=https://your-app-url.com

# Azure DevOps
azure.org.url=https://dev.azure.com/YOUR_ORG
azure.project.name=YOUR_PROJECT
azure.pat=YOUR_PERSONAL_ACCESS_TOKEN
azure.test.plan.id=1
azure.test.run.name=Automated Test Run
azure.update.enabled=true
```

---

## ▶️ Running Tests

### Run All Tests
```bash
mvn clean test
```

### Run Specific Suite
```bash
mvn clean test -DsuiteXmlFile=testng.xml
```

### Run with Browser Override
```bash
mvn clean test -Dbrowser=firefox -Dheadless=true
```

### Run Specific Cucumber Tags
```bash
mvn clean test -Dcucumber.filter.tags="@smoke"
mvn clean test -Dcucumber.filter.tags="@TC-101"
mvn clean test -Dcucumber.filter.tags="@regression and not @TC-103"
```

### Run Against Different Environments
```bash
mvn clean test -Denv=staging -Dbase.url=https://staging.your-app.com
```

---

## 🔗 Azure DevOps Integration

### How It Works

```
Test Execution
     │
     ▼
TestNGListener / Cucumber @After Hook
     │
     ├──► ExtentReport  →  HTML Report (local + pipeline artifact)
     │
     └──► AzureDevOpsClient
               │
               ├─ createTestRun()        Creates run under Test Plan
               ├─ updateResultByTitle()  Updates each test case outcome
               └─ completeTestRun()      Marks run as Completed
```

### Azure DevOps Setup Steps

1. **Create a Test Plan** in Azure DevOps → Test Plans
2. **Create Test Cases** with IDs (e.g., 101, 102, 103)
3. **Generate a Personal Access Token (PAT)**:
   - Azure DevOps → User Settings → Personal Access Tokens
   - Scope: `Test Management (Read & Write)`
4. **Update `config.properties`** with your org URL, project, PAT, and Test Plan ID
5. **Map tests** using `testName = "TC-<ID> - Description"` in `@Test` annotations

### Mapping Test Cases to Azure IDs

**TestNG style:**
```java
@Test(testName = "TC-101 - Valid Login with correct credentials")
public void validLoginTest() { ... }
```

**Cucumber style (tag-based):**
```gherkin
@TC-101 @smoke
Scenario: Successful login with valid credentials
```

---

## 📊 Reports

| Report | Location | Description |
|--------|----------|-------------|
| Extent HTML | `reports/ExtentReport_<timestamp>.html` | Rich HTML with screenshots |
| Cucumber HTML | `reports/cucumber-html-report.html` | BDD scenario report |
| Cucumber JSON | `reports/cucumber.json` | Machine-readable results |
| JUnit XML | `reports/cucumber-junit.xml` | Azure pipeline integration |
| Logs | `reports/logs/framework.log` | Execution log |
| Screenshots | `reports/screenshots/` | Failure screenshots |

---

## 🚀 Azure DevOps Pipeline

### Pipeline Variables (set in Azure DevOps Library → Variable Groups)

| Variable Name | Description | Secret? |
|---------------|-------------|---------|
| `AZURE_ORG_URL` | `https://dev.azure.com/your-org` | No |
| `AZURE_PAT` | Personal Access Token | ✅ Yes |
| `AZURE_PROJECT_NAME` | Azure project name | No |
| `AZURE_TEST_PLAN_ID` | Test plan numeric ID | No |

### Setup Variable Group
1. Azure DevOps → Pipelines → Library
2. Create group: `selenium-framework-secrets`
3. Add variables listed above
4. Link group to pipeline

### Create Pipeline
1. Azure DevOps → Pipelines → New Pipeline
2. Select your repository
3. Choose "Existing Azure Pipelines YAML file"
4. Select `.azure/azure-pipelines.yml`

---

## 🧩 Framework Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Test Layer                            │
│   TestNG Tests          Cucumber BDD Scenarios          │
│   (LoginTest.java)      (Login.feature)                 │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                   Listener / Hooks                      │
│   TestNGListener.java    LoginSteps.java (@After)       │
└──────┬──────────────────────────────────┬───────────────┘
       │                                  │
┌──────▼──────────┐             ┌─────────▼──────────────┐
│  Extent Reports │             │   Azure DevOps Client  │
│  (HTML Report)  │             │   (REST API v7.1)      │
└─────────────────┘             └────────────────────────┘
       │
┌──────▼──────────────────────────────────────────────────┐
│                   Core Utilities                        │
│   DriverManager   ConfigManager   ScreenshotUtil        │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                   Page Object Layer                     │
│   BasePage.java          LoginPage.java                 │
└─────────────────────────────────────────────────────────┘
```

---

## ➕ Adding New Tests

### 1. Create a Page Object
```java
public class DashboardPage extends BasePage {
    @FindBy(id = "welcome-msg")
    private WebElement welcomeMessage;

    public String getWelcomeMessage() {
        return getText(welcomeMessage, "Welcome Message");
    }
}
```

### 2. Create a TestNG Test
```java
@Test(testName = "TC-201 - Dashboard loads correctly", groups = {"regression"})
public void dashboardLoadTest() {
    DashboardPage dashboard = new DashboardPage();
    Assert.assertTrue(dashboard.getWelcomeMessage().contains("Welcome"));
}
```

### 3. Create a Cucumber Scenario
```gherkin
@TC-201
Scenario: Dashboard loads after login
  Given the user navigates to the login page
  When the user enters username "admin@example.com"
  And the user enters password "Admin@123"
  And the user clicks the login button
  Then the user should see the dashboard
```

---

## 🛠️ Tech Stack

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 11 | Language |
| Maven | 3.8+ | Build & dependency management |
| Selenium WebDriver | 4.18.1 | Browser automation |
| TestNG | 7.9.0 | Test execution & assertions |
| Cucumber | 7.15.0 | BDD framework |
| ExtentReports | 5.1.1 | HTML test reports |
| WebDriverManager | 5.7.0 | Auto browser driver setup |
| OkHttp | 4.12.0 | Azure DevOps REST API calls |
| Log4j2 | 2.22.1 | Logging |
| Apache POI | 5.2.5 | Excel data-driven testing |
