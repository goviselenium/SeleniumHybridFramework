package com.framework.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.framework.config.ConfigManager;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * AzureDevOpsClient - Integrates test results with Azure DevOps Test Plans.
 *
 * Flow:
 *  1. createTestRun()       -> Creates a new Test Run under a Test Plan
 *  2. addTestResults()      -> Adds test case results to the run
 *  3. updateTestResults()   -> Updates result outcome (Passed/Failed/etc.)
 *  4. completeTestRun()     -> Marks the run as Completed
 *
 * Azure DevOps REST API version: 7.1
 */
public class AzureDevOpsClient {

    private static final Logger logger = LogManager.getLogger(AzureDevOpsClient.class);
    private static final String API_VERSION = "api-version=7.1";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    private final String orgUrl;
    private final String project;
    private final String authHeader;

    private int testRunId = -1;

    public AzureDevOpsClient() {
        ConfigManager config = ConfigManager.getInstance();
        this.orgUrl    = config.getAzureOrgUrl();
        this.project   = config.getAzureProject();
        String pat     = config.getAzurePat();
        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((":" + pat).getBytes(StandardCharsets.UTF_8));
        this.httpClient = new OkHttpClient();
        this.mapper     = new ObjectMapper();
    }

    // -------------------------------------------------------------------------
    // 1. Create a new Test Run
    // -------------------------------------------------------------------------
    public int createTestRun(String runName, int testPlanId) {
        String url = String.format("%s/%s/_apis/test/runs?%s", orgUrl, project, API_VERSION);

        ObjectNode body = mapper.createObjectNode();
        body.put("name", runName);
        body.put("isAutomated", true);
        ObjectNode plan = mapper.createObjectNode();
        plan.put("id", testPlanId);
        body.set("plan", plan);

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authHeader)
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body().string();
                if (response.isSuccessful()) {
                    JsonNode json = mapper.readTree(responseBody);
                    testRunId = json.get("id").asInt();
                    logger.info("✅ Azure Test Run created. ID: {}", testRunId);
                    return testRunId;
                } else {
                    logger.error("❌ Failed to create test run. Status: {} | Body: {}", response.code(), responseBody);
                }
            }
        } catch (IOException e) {
            logger.error("❌ Exception creating test run: {}", e.getMessage());
        }
        return -1;
    }

    // -------------------------------------------------------------------------
    // 2. Add Test Results (test cases) to the Run
    // -------------------------------------------------------------------------
    public void addTestResults(List<Map<String, Object>> testCases) {
        if (testRunId == -1) {
            logger.warn("No active test run. Call createTestRun() first.");
            return;
        }
        String url = String.format("%s/%s/_apis/test/runs/%d/results?%s",
                orgUrl, project, testRunId, API_VERSION);

        ArrayNode results = mapper.createArrayNode();
        for (Map<String, Object> tc : testCases) {
            ObjectNode result = mapper.createObjectNode();
            result.put("testCaseTitle", tc.get("title").toString());
            result.put("outcome", "NotExecuted");
            result.put("state", "InProgress");

            if (tc.containsKey("testCaseId")) {
                ObjectNode testCase = mapper.createObjectNode();
                testCase.put("id", Integer.parseInt(tc.get("testCaseId").toString()));
                result.set("testCase", testCase);
            }
            results.add(result);
        }

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authHeader)
                    .post(RequestBody.create(results.toString(), JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("✅ Test results added to run ID: {}", testRunId);
                } else {
                    logger.error("❌ Failed to add results. Status: {}", response.code());
                }
            }
        } catch (IOException e) {
            logger.error("❌ Exception adding test results: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 3. Update a single test result outcome
    // -------------------------------------------------------------------------
    public void updateTestResult(int resultId, String outcome, String errorMessage, long durationMs) {
        if (testRunId == -1) return;

        String url = String.format("%s/%s/_apis/test/runs/%d/results?%s",
                orgUrl, project, testRunId, API_VERSION);

        ArrayNode results = mapper.createArrayNode();
        ObjectNode result = mapper.createObjectNode();
        result.put("id", resultId);
        result.put("outcome", outcome);   // Passed | Failed | NotExecuted | Blocked
        result.put("state", "Completed");
        result.put("durationInMs", durationMs);
        if (errorMessage != null && !errorMessage.isEmpty()) {
            result.put("errorMessage", errorMessage);
            result.put("comment", "Automated failure: " + errorMessage);
        }
        results.add(result);

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authHeader)
                    .patch(RequestBody.create(results.toString(), JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("✅ Result ID {} updated to: {}", resultId, outcome);
                } else {
                    logger.error("❌ Failed to update result {}. Status: {}", resultId, response.code());
                }
            }
        } catch (IOException e) {
            logger.error("❌ Exception updating result: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 4. Update test result by test case title (bulk update by name)
    // -------------------------------------------------------------------------
    public void updateResultByTitle(String testCaseTitle, String outcome,
                                     String errorMessage, long durationMs) {
        if (testRunId == -1) return;
        int resultId = getResultIdByTitle(testCaseTitle);
        if (resultId != -1) {
            updateTestResult(resultId, outcome, errorMessage, durationMs);
        }
    }

    // -------------------------------------------------------------------------
    // 5. Get result ID by test case title
    // -------------------------------------------------------------------------
    public int getResultIdByTitle(String title) {
        String url = String.format("%s/%s/_apis/test/runs/%d/results?%s",
                orgUrl, project, testRunId, API_VERSION);
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authHeader)
                    .get().build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JsonNode json = mapper.readTree(response.body().string());
                    for (JsonNode r : json.get("value")) {
                        if (r.get("testCaseTitle").asText().equalsIgnoreCase(title)) {
                            return r.get("id").asInt();
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("❌ Exception fetching results: {}", e.getMessage());
        }
        return -1;
    }

    // -------------------------------------------------------------------------
    // 6. Complete the Test Run
    // -------------------------------------------------------------------------
    public void completeTestRun() {
        if (testRunId == -1) return;
        String url = String.format("%s/%s/_apis/test/runs/%d?%s",
                orgUrl, project, testRunId, API_VERSION);

        ObjectNode body = mapper.createObjectNode();
        body.put("state", "Completed");

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authHeader)
                    .patch(RequestBody.create(body.toString(), JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("✅ Test Run {} completed in Azure DevOps.", testRunId);
                } else {
                    logger.error("❌ Failed to complete run. Status: {}", response.code());
                }
            }
        } catch (IOException e) {
            logger.error("❌ Exception completing test run: {}", e.getMessage());
        }
    }

    public int getTestRunId() { return testRunId; }
}
