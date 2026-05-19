package com.acube.documentprocessing.domain;

import java.time.Instant;

/**
 * Entità del Dominio (Value Object) che rappresenta i metadati di un documento
 * una volta che questo è stato elaborato. Raccoglie informazioni tecniche
 * fondamentali indipendenti dal modo in cui verranno salvate.
 *
 * @param type Il tipo del documento (es. fattura, nota di credito).
 * @param processedAt Il momento esatto (timestamp) in cui è terminata l'elaborazione.
 * @param hash L'impronta digitale (SHA-256) per garantire l'integrità del documento originale.
 * @param sizeBytes La dimensione in byte del documento elaborato.
 */
public record ProcessingMetadata(
        String type,
        Instant processedAt,
        String hash,
        long sizeBytes
) {
}
