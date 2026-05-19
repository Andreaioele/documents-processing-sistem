package com.acube.documentprocessing.api;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerIntegrationTest {

    private static final Path TEST_ROOT = Path.of(
            System.getProperty("java.io.tmpdir"),
            "document-processing-controller-test-" + UUID.randomUUID()
    );
    private static final Path INPUT_ROOT = TEST_ROOT.resolve("input");
    private static final Path OUTPUT_ROOT = TEST_ROOT.resolve("output");

    @Autowired
    MockMvc mockMvc;

    @DynamicPropertySource
    static void configureStorage(DynamicPropertyRegistry registry) {
        registry.add("app.storage.input-root", INPUT_ROOT::toString);
        registry.add("app.storage.output-root", OUTPUT_ROOT::toString);
    }

    @BeforeAll
    static void setUpFiles() throws Exception {
        Files.createDirectories(INPUT_ROOT);
        Files.writeString(INPUT_ROOT.resolve("invoice.pdf"), "document-content");
    }

    @Test
    void processesDocumentRequest() throws Exception {
        mockMvc.perform(post("/api/documents/process")
                        .contentType("application/json")
                        .content("""
                                {
                                  "documentId": "123",
                                  "storageRef": "invoice.pdf",
                                  "metadata": {
                                    "type": "invoice",
                                    "receivedAt": "2026-05-01T10:00:00Z"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value("123"))
                .andExpect(jsonPath("$.zipPath", endsWith("123.zip")))
                .andExpect(jsonPath("$.metadata.type").value("invoice"))
                .andExpect(jsonPath("$.metadata.hash", notNullValue()))
                .andExpect(jsonPath("$.metadata.sizeBytes").value(16));
    }

    @Test
    void returnsBadRequestForInvalidBody() throws Exception {
        mockMvc.perform(post("/api/documents/process")
                        .contentType("application/json")
                        .content("""
                                {
                                  "storageRef": "invoice.pdf",
                                  "metadata": {
                                    "type": "invoice"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    void returnsNotFoundForMissingFile() throws Exception {
        mockMvc.perform(post("/api/documents/process")
                        .contentType("application/json")
                        .content("""
                                {
                                  "documentId": "missing",
                                  "storageRef": "missing.pdf",
                                  "metadata": {
                                    "type": "invoice"
                                  }
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", notNullValue()));
    }
}
