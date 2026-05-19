package com.acube.documentprocessing.storage;

import com.acube.documentprocessing.config.StorageProperties;
import com.acube.documentprocessing.exception.DocumentNotFoundException;
import com.acube.documentprocessing.exception.ProcessingException;
import com.acube.documentprocessing.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Implementazione concreta del Persistence Layer che utilizza il file system
 * locale per la memorizzazione dei documenti.
 * Mappa i riferimenti logici a percorsi fisici sul disco e gestisce la
 * lettura/scrittura.
 */
@Component
public class LocalFileSystemDocumentStorage implements DocumentStorage {

    private final StorageProperties properties;

    public LocalFileSystemDocumentStorage(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StoredDocument readDocument(String storageRef) {
        Path documentPath = resolveInsideRoot(properties.getInputRoot(), storageRef, "storageRef");
        if (!Files.isRegularFile(documentPath)) {
            throw new DocumentNotFoundException("Document file not found: " + storageRef);
        }

        try {
            return new StoredDocument(documentPath, Files.readAllBytes(documentPath));
        } catch (IOException e) {
            throw new ProcessingException("Unable to read document file: " + storageRef, e);
        }
    }

    @Override
    public Path writeOutputArchive(String documentId, byte[] archiveContent) {
        validateDocumentIdForFileName(documentId);

        Path outputRoot = properties.getOutputRoot().toAbsolutePath().normalize();
        Path outputPath = outputRoot.resolve(documentId + ".zip").normalize();
        if (!outputPath.startsWith(outputRoot)) {
            throw new ValidationException("documentId must resolve inside configured output root");
        }

        try {
            Files.createDirectories(outputRoot);
            Files.write(outputPath, archiveContent);
            return outputPath;
        } catch (IOException e) {
            throw new ProcessingException("Unable to write output archive for documentId: " + documentId, e);
        }
    }

    @Override
    public String outputReference(Path outputPath) {
        Path outputRoot = properties.getOutputRoot();
        String reference = outputRoot.isAbsolute()
                ? outputPath.toString()
                : outputRoot.resolve(outputPath.getFileName()).normalize().toString();
        return reference.replace('\\', '/');
    }

    private Path resolveInsideRoot(Path root, String reference, String fieldName) {
        Path refPath = Path.of(reference);
        if (refPath.isAbsolute()) {
            throw new ValidationException(fieldName + " must be relative to the configured storage root");
        }

        Path absoluteRoot = root.toAbsolutePath().normalize();
        Path resolved = absoluteRoot.resolve(refPath).normalize();
        if (!resolved.startsWith(absoluteRoot)) {
            throw new ValidationException(fieldName + " must resolve inside the configured storage root");
        }
        return resolved;
    }

    private void validateDocumentIdForFileName(String documentId) {
        if (!documentId.matches("[A-Za-z0-9._-]+") || ".".equals(documentId) || "..".equals(documentId)) {
            throw new ValidationException("documentId can only contain letters, numbers, dot, underscore and dash");
        }
    }
}
