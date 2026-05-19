package com.acube.documentprocessing.storage;

import com.acube.documentprocessing.config.StorageProperties;
import com.acube.documentprocessing.exception.DocumentNotFoundException;
import com.acube.documentprocessing.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileSystemDocumentStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void readsDocumentFromConfiguredInputRoot() throws Exception {
        Path inputRoot = tempDir.resolve("input");
        Files.createDirectories(inputRoot);
        Files.writeString(inputRoot.resolve("invoice.pdf"), "document");
        LocalFileSystemDocumentStorage storage = storage(inputRoot, tempDir.resolve("output"));

        StoredDocument document = storage.readDocument("invoice.pdf");

        assertThat(document.content()).isEqualTo("document".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsStorageRefPathTraversal() {
        LocalFileSystemDocumentStorage storage = storage(tempDir.resolve("input"), tempDir.resolve("output"));

        assertThatThrownBy(() -> storage.readDocument("../secret.pdf"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("storageRef");
    }

    @Test
    void reportsMissingDocumentAsNotFound() {
        LocalFileSystemDocumentStorage storage = storage(tempDir.resolve("input"), tempDir.resolve("output"));

        assertThatThrownBy(() -> storage.readDocument("missing.pdf"))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void writesArchiveUnderConfiguredOutputRoot() {
        LocalFileSystemDocumentStorage storage = storage(tempDir.resolve("input"), tempDir.resolve("output"));

        Path outputPath = storage.writeOutputArchive("123", "zip".getBytes(StandardCharsets.UTF_8));

        assertThat(outputPath).exists();
        assertThat(outputPath.getFileName().toString()).isEqualTo("123.zip");
    }

    @Test
    void rejectsUnsafeDocumentId() {
        LocalFileSystemDocumentStorage storage = storage(tempDir.resolve("input"), tempDir.resolve("output"));

        assertThatThrownBy(() -> storage.writeOutputArchive("../123", new byte[0]))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("documentId");
    }

    private LocalFileSystemDocumentStorage storage(Path inputRoot, Path outputRoot) {
        StorageProperties properties = new StorageProperties();
        properties.setInputRoot(inputRoot);
        properties.setOutputRoot(outputRoot);
        return new LocalFileSystemDocumentStorage(properties);
    }
}
