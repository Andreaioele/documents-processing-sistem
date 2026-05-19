package com.acube.documentprocessing.api.dto;

import com.acube.documentprocessing.domain.DocumentProcessedEvent;
import com.acube.documentprocessing.domain.ProcessingMetadata;

/**
 * DTO che rappresenta la risposta restituita dal sistema
 * una volta che il documento è stato elaborato con successo.
 *
 * @param documentId L'identificativo univoco del documento elaborato.
 * @param zipPath Il percorso o riferimento in cui è stato salvato l'archivio ZIP con i risultati dell'elaborazione.
 * @param metadata Metadati e dettagli tecnici relativi al processo di elaborazione.
 */
public record DocumentProcessedResponse(
        String documentId,
        String zipPath,
        ProcessingMetadata metadata
) {

    /**
     * Metodo di utilità (factory method) per creare una risposta DTO 
     * a partire dall'evento di dominio generato alla fine dell'elaborazione.
     *
     * @param event L'evento di dominio che contiene i risultati dell'elaborazione.
     * @return Una nuova istanza di DocumentProcessedResponse.
     */
    public static DocumentProcessedResponse from(DocumentProcessedEvent event) {
        return new DocumentProcessedResponse(event.documentId(), event.zipPath(), event.metadata());
    }
}
