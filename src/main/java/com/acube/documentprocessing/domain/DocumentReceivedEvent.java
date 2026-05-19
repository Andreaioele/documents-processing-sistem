package com.acube.documentprocessing.domain;

import java.util.Map;

/**
 * Evento di Dominio che indica che il sistema ha appena ricevuto un nuovo documento.
 * Rappresenta un fatto compiuto all'interno della logica di business e funge da
 * punto di partenza per il processo di elaborazione.
 *
 * @param documentId L'ID univoco assegnato al documento in ingresso.
 * @param storageRef Il riferimento fisico o logico al file grezzo originale.
 * @param metadata Mappa contenente le informazioni preliminari del documento (es. tipo, data ricezione).
 */
public record DocumentReceivedEvent(
        String documentId,
        String storageRef,
        Map<String, Object> metadata
) {
}
