package com.acube.documentprocessing.exception;

/**
 * Eccezione lanciata quando i dati in ingresso non superano i controlli di
 * validazione.
 * Essendo figlia di RuntimeException, viene gestita automaticamente
 * dall'ApiExceptionHandler.
 *
 * @param message Messaggio descrittivo dell'errore di validazione.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
