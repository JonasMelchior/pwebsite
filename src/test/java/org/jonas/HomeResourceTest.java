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
        java.nio.file.Path testProjectDir = java.nio.file.Path.of("target/test-content/projects/test-project");
        try {
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

            given()
                    .when()
                    .get("/projects/fpga-poker")
                    .then()
                    .statusCode(200)
                    .body(containsString("Poker Game on FPGA"));
        } finally {
            // Clean up test files if any were created
            if (java.nio.file.Files.exists(testProjectDir)) {
                try (java.util.stream.Stream<java.nio.file.Path> s = java.nio.file.Files.walk(testProjectDir.getParent())) {
                    s.sorted(java.util.Comparator.reverseOrder())
                     .map(java.nio.file.Path::toFile)
                     .forEach(java.io.File::delete);
                } catch (Exception ignored) {}
            }
        }
    }
}
