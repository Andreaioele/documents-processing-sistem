package com.acube.documentprocessing.api.dto;

import com.acube.documentprocessing.domain.DocumentReceivedEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * DTO che rappresenta la richiesta in ingresso
 * quando un nuovo documento viene ricevuto dal sistema.
 * Essendo un record, i dati trasportati sono immutabili.
 *
 * @param documentId L'identificativo univoco del documento.
 * @param storageRef Il riferimento (es. percorso o URL) a dove il file fisico è
 *                   stato salvato nello storage.
 * @param metadata   Mappa contenente i dati aggiuntivi o le caratteristiche del
 *                   documento.
 */
public record DocumentReceivedRequest(
        @NotBlank(message = "documentId must be present and not empty") String documentId,

        @NotBlank(message = "storageRef must be present and not empty") String storageRef,

        @NotNull(message = "metadata must be present and well-formed") Map<String, Object> metadata) {

    /**
     * Converte questa richiesta (DTO) in un evento di dominio.
     * Questo permette di mantenere separati i modelli usati per le API
     * da quelli usati per la logica di business.
     * 
     * @return Una nuova istanza di DocumentReceivedEvent inizializzata con i dati
     *         della richiesta.
     */
    public DocumentReceivedEvent toEvent() {
        return new DocumentReceivedEvent(documentId, storageRef, metadata);
    }
}
