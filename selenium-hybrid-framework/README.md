# Selenium Hybrid Framework with Azure DevOps Integration

A production-ready **Java Maven + Selenium + TestNG** test automation framework with **ExtentReports** HTML reporting and **Azure DevOps Test Plans** integration for real-time result publishing.

---

## 📁 Project Structure

```
selenium-hybrid-framework/
├── .azure/
│   └── azure-pipelines.yml               # Azure DevOps CI/CD pipeline
├── src/
│   ├── main/java/com/framework/
│   │   ├── config/
│   │   │   ├── ConfigManager.java        # Central config (supports sys/env overrides)
│   │   │   └── EnvironmentConfig.java    # Per-env credentials (qa/staging/prod)
│   │   ├── listeners/
│   │   │   └── TestNGListener.java       # Suite/test hooks → Extent + Azure DevOps
│   │   ├── pages/
│   │   │   ├── BasePage.java             # Parent POM with WebDriver helpers
│   │   │   └── LoginPage.java            # Sample Page Object
│   │   └── utils/
│   │       ├── DriverManager.java        # Thread-safe WebDriver (ThreadLocal)
│   │       ├── ExtentReportManager.java  # Extent Reports setup & management
│   │       ├── AzureDevOpsClient.java    # Azure DevOps REST API v7.1 client
│   │       ├── ScreenshotUtil.java       # Base64 screenshot capture
│   │       ├── WaitUtil.java             # Explicit wait helpers
│   │       └── ExcelDataUtil.java        # Apache POI Excel data reader
│   └── test/
│       ├── java/com/framework/tests/
│       │   ├── BaseTest.java             # @BeforeMethod / @AfterMethod setup
│       │   └── LoginTest.java            # TC-101 → TC-106 (incl. @DataProvider)
│       └── resources/
│           ├── env/
│           │   ├── qa.properties         # QA credentials & base URL
│           │   ├── staging.properties    # Staging credentials & base URL
│           │   └── prod.properties       # Prod credentials (secrets via pipeline)
│           ├── config.properties         # Browser, waits, Azure settings
│           └── log4j2.xml                # Logging configuration
├── reports/                              # Generated at runtime
│   ├── ExtentReport_<timestamp>.html
│   ├── screenshots/
│   └── logs/
├── testng.xml                            # Suite definition (Smoke + Regression)
└── pom.xml                               # Maven dependencies
```

---

## ⚙️ Setup

### Prerequisites
- Java 11+
- Maven 3.8+
- Chrome / Firefox / Edge

### Install
```bash
git clone https://github.com/your-org/selenium-hybrid-framework.git
cd selenium-hybrid-framework
mvn clean install -DskipTests
```

### Configure `src/test/resources/config.properties`
```properties
browser=chrome
headless=false
env=qa

# Azure DevOps
azure.org.url=https://dev.azure.com/YOUR_ORG
azure.project.name=YOUR_PROJECT
azure.pat=YOUR_PAT
azure.test.plan.id=1
azure.update.enabled=true
```

### Configure `src/test/resources/env/qa.properties`
```properties
base.url=https://qa.your-app.com

login.admin.username=qa-admin@example.com
login.admin.password=QaAdmin@123

login.standard.username=qa-user@example.com
login.standard.password=QaUser@123

login.readonly.username=qa-readonly@example.com
login.readonly.password=QaReadOnly@123
```

---

## ▶️ Running Tests

```bash
# Run full suite (testng.xml)
mvn clean test

# Smoke tests only
mvn clean test -Dgroups=smoke

# Regression tests only
mvn clean test -Dgroups=regression

# Different environment
mvn clean test -Denv=staging

# Headless Chrome (CI)
mvn clean test -Dheadless=true -Denv=qa

# Override a credential at runtime
mvn clean test -Denv=prod -Dlogin.admin.password=MyRealProdPass
```

---

## 🔗 Azure DevOps Integration

### How it works

```
TestNG Suite starts
        │
        ▼
ISuiteListener.onStart()
   └─ AzureDevOpsClient.createTestRun()     → opens run in Test Plans
        │
   Each @Test method runs
        │
        ├─ onTestSuccess() → updateResultByTitle("Passed")
        ├─ onTestFailure() → screenshot embedded + updateResultByTitle("Failed")
        └─ onTestSkipped() → updateResultByTitle("NotExecuted")
        │
        ▼
ISuiteListener.onFinish()
   ├─ ExtentReportManager.flush()           → saves HTML report
   └─ AzureDevOpsClient.completeTestRun()   → closes run in Test Plans
```

### Mapping tests to Azure Test Cases

Use `testName` on `@Test` — it must match the Azure Test Case title exactly:

```java
@Test(testName = "TC-101 - Valid Login - Admin user", ...)
public void validLoginAsAdmin() { ... }
```

### Setup in Azure DevOps
1. Create a **Test Plan** → note the numeric ID
2. Create **Test Cases** (TC-101, TC-102, etc.) whose titles match `testName`
3. Generate a **PAT** with scope `Test Management (Read & Write)`
4. Create a **Variable Group** `selenium-framework-secrets` in Pipelines → Library

---

## 📊 Reports

| Report | Location |
|--------|----------|
| Extent HTML | `reports/ExtentReport_<timestamp>.html` |
| Screenshots | `reports/screenshots/` |
| Logs | `reports/logs/framework.log` |
| Surefire XML | `target/surefire-reports/*.xml` (picked up by pipeline) |

---

## 🚀 Azure Pipeline Variables

Set these as **secret** variables in the `selenium-framework-secrets` Variable Group:

| Variable | Description | Secret? |
|----------|-------------|---------|
| `AZURE_ORG_URL` | `https://dev.azure.com/your-org` | No |
| `AZURE_PAT` | Personal Access Token | ✅ |
| `AZURE_PROJECT_NAME` | Azure project name | No |
| `AZURE_TEST_PLAN_ID` | Numeric test plan ID | No |
| `LOGIN_ADMIN_USERNAME` | Admin login for target env | No |
| `LOGIN_ADMIN_PASSWORD` | Admin password | ✅ |
| `LOGIN_STANDARD_USERNAME` | Standard user login | No |
| `LOGIN_STANDARD_PASSWORD` | Standard user password | ✅ |
| `LOGIN_READONLY_USERNAME` | Read-only user login | No |
| `LOGIN_READONLY_PASSWORD` | Read-only user password | ✅ |

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

### 2. Add a test class
```java
public class DashboardTest extends BaseTest {

    @Test(testName = "TC-201 - Dashboard shows welcome message",
          groups = {"regression"})
    public void dashboardWelcomeMessage() {
        DashboardPage dashboard = new DashboardPage();
        Assert.assertTrue(dashboard.getWelcomeMessage().contains("Welcome"));
    }
}
```

### 3. Register in testng.xml
```xml
<class name="com.framework.tests.DashboardTest"/>
```

---

## 🛠️ Tech Stack

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 11 | Language |
| Maven | 3.8+ | Build & dependency management |
| Selenium WebDriver | 4.18.1 | Browser automation |
| TestNG | 7.9.0 | Test execution, assertions, `@DataProvider` |
| ExtentReports | 5.1.1 | HTML test reports with screenshots |
| WebDriverManager | 5.7.0 | Auto browser driver setup |
| OkHttp | 4.12.0 | Azure DevOps REST API calls |
| Log4j2 | 2.22.1 | Logging |
| Apache POI | 5.2.5 | Excel data-driven testing |
