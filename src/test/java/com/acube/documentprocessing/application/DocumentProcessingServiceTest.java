package com.acube.documentprocessing.application;

import com.acube.documentprocessing.config.StorageProperties;
import com.acube.documentprocessing.domain.DocumentProcessedEvent;
import com.acube.documentprocessing.domain.DocumentReceivedEvent;
import com.acube.documentprocessing.exception.ValidationException;
import com.acube.documentprocessing.processing.HashService;
import com.acube.documentprocessing.processing.ZipArchiveService;
import com.acube.documentprocessing.storage.LocalFileSystemDocumentStorage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentProcessingServiceTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void processesDocumentAndCreatesOutputArchive() throws Exception {
        Path inputRoot = tempDir.resolve("input");
        Path outputRoot = tempDir.resolve("output");
        Files.createDirectories(inputRoot);
        Files.writeString(inputRoot.resolve("invoice.pdf"), "document-content");
        DocumentProcessingService service = service(inputRoot, outputRoot);

        DocumentProcessedEvent event = service.process(new DocumentReceivedEvent(
                "123",
                "invoice.pdf",
                Map.of("type", "invoice", "receivedAt", "2026-05-01T10:00:00Z")
        ));

        assertThat(event.documentId()).isEqualTo("123");
        assertThat(event.zipPath()).endsWith("123.zip");
        assertThat(event.metadata().type()).isEqualTo("invoice");
        assertThat(event.metadata().processedAt()).isEqualTo(Instant.parse("2026-05-06T12:30:00Z"));
        assertThat(event.metadata().sizeBytes()).isEqualTo("document-content".getBytes(StandardCharsets.UTF_8).length);
        assertThat(outputRoot.resolve("123.zip")).exists();

        Map<String, byte[]> entries = readZipEntries(Files.readAllBytes(outputRoot.resolve("123.zip")));
        assertThat(entries).containsOnlyKeys("invoice.pdf", "metadata.json", "hash.txt");
        assertThat(new String(entries.get("hash.txt"), StandardCharsets.UTF_8)).isEqualTo(event.metadata().hash());

        JsonNode metadata = objectMapper.readTree(entries.get("metadata.json"));
        assertThat(metadata.at("/originalMetadata/type").asText()).isEqualTo("invoice");
        assertThat(metadata.at("/processingMetadata/hash").asText()).isEqualTo(event.metadata().hash());
    }

    @Test
    void rejectsUnsupportedMetadataType() {
        DocumentProcessingService service = service(tempDir.resolve("input"), tempDir.resolve("output"));

        assertThatThrownBy(() -> service.process(new DocumentReceivedEvent(
                "123",
                "invoice.pdf",
                Map.of("type", "receipt")
        )))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("metadata.type");
    }

    @Test
    void rejectsInvalidReceivedAt() {
        DocumentProcessingService service = service(tempDir.resolve("input"), tempDir.resolve("output"));

        assertThatThrownBy(() -> service.process(new DocumentReceivedEvent(
                "123",
                "invoice.pdf",
                Map.of("type", "invoice", "receivedAt", "not-a-date")
        )))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("receivedAt");
    }

    @Test
    void rejectsUnsafeDocumentIdBeforeReadingFile() {
        DocumentProcessingService service = service(tempDir.resolve("input"), tempDir.resolve("output"));

        assertThatThrownBy(() -> service.process(new DocumentReceivedEvent(
                "../123",
                "missing.pdf",
                Map.of("type", "invoice")
        )))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("documentId");
    }

    private DocumentProcessingService service(Path inputRoot, Path outputRoot) {
        StorageProperties properties = new StorageProperties();
        properties.setInputRoot(inputRoot);
        properties.setOutputRoot(outputRoot);
        return new DocumentProcessingService(
                new LocalFileSystemDocumentStorage(properties),
                new HashService(),
                new ZipArchiveService(objectMapper),
                Clock.fixed(Instant.parse("2026-05-06T12:30:00Z"), ZoneOffset.UTC),
                objectMapper
        );
    }

    private Map<String, byte[]> readZipEntries(byte[] archive) throws Exception {
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
