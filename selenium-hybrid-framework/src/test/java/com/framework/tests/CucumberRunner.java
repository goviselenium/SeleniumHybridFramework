package com.framework.tests;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * CucumberRunner - Runs Cucumber BDD scenarios via TestNG.
 *
 * Tags can be overridden at runtime:
 *   mvn test -Dcucumber.filter.tags="@smoke"
 */
@CucumberOptions(
    features = "src/test/resources/features",
    glue     = "com.framework.stepdefs",
    plugin   = {
        "pretty",
        "html:reports/cucumber-html-report.html",
        "json:reports/cucumber.json",
        "junit:reports/cucumber-junit.xml"
    },
    tags     = "@regression",
    monochrome = true
)
public class CucumberRunner extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
