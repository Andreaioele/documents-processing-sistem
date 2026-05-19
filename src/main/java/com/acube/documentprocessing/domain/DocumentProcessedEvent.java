package com.acube.documentprocessing.domain;

/**
 * Evento di Dominio che indica che un documento è stato elaborato con successo.
 * Essendo parte del Dominio, è una classe pura (nessuna dipendenza da framework esterni).
 *
 * @param documentId L'ID univoco del documento che è stato elaborato.
 * @param zipPath Il percorso o riferimento all'archivio ZIP generato dal sistema.
 * @param metadata I metadati estratti e arricchiti durante il processo di elaborazione.
 */
public record DocumentProcessedEvent(
        String documentId,
        String zipPath,
        ProcessingMetadata metadata
) {
}
