package com.acube.documentprocessing.storage;

import java.nio.file.Path;

/**
 * Record (Value Object) del Persistence Layer utilizzato per rappresentare un
 * documento che è stato memorizzato. È un contenitore di dati (DTO) che espone
 * sia il riferimento logico/fisico al file sia il suo contenuto grezzo.
 *
 * @param path    Il percorso (Path) del file sul sistema di storage.
 * @param content L'intero contenuto del file sotto forma di array di byte.
 */
public record StoredDocument(Path path, byte[] content) {
}
