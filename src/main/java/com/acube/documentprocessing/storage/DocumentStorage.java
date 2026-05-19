package com.acube.documentprocessing.storage;

import java.nio.file.Path;

/**
 * Interfaccia del Persistence Layer per la gestione della memorizzazione e del
 * recupero dei documenti.
 * Questo livello astrae completamente la tecnologia di storage (es. file
 * system,
 * database, cloud) dal resto dell'applicazione.
 */
public interface DocumentStorage {

    StoredDocument readDocument(String storageRef);

    Path writeOutputArchive(String documentId, byte[] archiveContent);

    String outputReference(Path outputPath);
}
