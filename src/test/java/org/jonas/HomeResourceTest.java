package org.jonas;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class HomeResourceTest {

    @Test
    public void testIndexEndpoint() {
        given()
          .when().get("/")
          .then()
             .statusCode(200)
             .body(containsString("jonasstenholt"))
             .body(containsString("Projects"))
             .body(containsString("Training Commits"))
             .body(containsString("heatmap-container"));
    }

    @Test
    public void testProjectMarkdownAndPdfLifecycle() throws java.io.IOException {
        // Ensure test project directory exists in CI/test environment
        java.nio.file.Path testProjectDir = java.nio.file.Path.of("content/projects/test-project");
        java.nio.file.Files.createDirectories(testProjectDir);
        java.nio.file.Files.writeString(testProjectDir.resolve("project.md"), """
                ---
                title: Test Project
                description: Testing project rendering
                tag: Test
                date: 2026-09-28
                ---
                ## System Description
                Testing markdown rendering.
                """);
        java.nio.file.Files.write(testProjectDir.resolve("resource_report.pdf"), "%PDF-1.4 dummy".getBytes());

        // 1. View project detail HTML page
        given()
                .when()
                .get("/projects/test-project")
                .then()
                .statusCode(200)
                .body(containsString("Test Project"))
                .body(containsString("System Description"));

        // 2. View project document
        given()
                .when()
                .get("/projects/test-project/documents/resource_report.pdf")
                .then()
                .statusCode(200)
                .contentType("application/pdf");
    }
}
