package com.acube.documentprocessing.processing;

import com.acube.documentprocessing.domain.ProcessingMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ZipArchiveServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final ZipArchiveService zipArchiveService = new ZipArchiveService(objectMapper);

    @Test
    void createsArchiveWithDocumentMetadataAndHash() throws Exception {
        ProcessingMetadata processingMetadata = new ProcessingMetadata(
                "invoice",
                Instant.parse("2026-05-06T12:30:00Z"),
                "hash-value",
                7
        );

        byte[] archive = zipArchiveService.createArchive(
                "content".getBytes(StandardCharsets.UTF_8),
                Map.of("type", "invoice", "receivedAt", "2026-05-01T10:00:00Z"),
                processingMetadata
        );

        Map<String, byte[]> entries = ZipTestUtils.readEntries(archive);
        assertThat(entries).containsOnlyKeys("invoice.pdf", "metadata.json", "hash.txt");
        assertThat(new String(entries.get("invoice.pdf"), StandardCharsets.UTF_8)).isEqualTo("content");
        assertThat(new String(entries.get("hash.txt"), StandardCharsets.UTF_8)).isEqualTo("hash-value");

        JsonNode metadata = objectMapper.readTree(entries.get("metadata.json"));
        assertThat(metadata.at("/originalMetadata/type").asText()).isEqualTo("invoice");
        assertThat(metadata.at("/processingMetadata/hash").asText()).isEqualTo("hash-value");
        assertThat(metadata.at("/processingMetadata/sizeBytes").asLong()).isEqualTo(7);
    }

    private static final class ZipTestUtils {

        private static Map<String, byte[]> readEntries(byte[] archive) throws Exception {
            java.util.Map<String, byte[]> entries = new java.util.LinkedHashMap<>();
            try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(archive))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zipInput.getNextEntry()) != null) {
                    entries.put(entry.getName(), zipInput.readAllBytes());
                    zipInput.closeEntry();
                }
            }
            return entries;
        }
    }
}
