package com.acube.documentprocessing.exception;

/**
 * Eccezione di Dominio generica usata per segnalare errori imprevisti durante
 * il processo di elaborazione.
 * Essendo figlia di RuntimeException, viene lanciata automaticamente dal
 * sistema senza interrompere il flusso principale (a meno che non venga
 * catturata esplicitamente).
 *
 * @param message Messaggio descrittivo dell'errore.
 * @param cause   La causa radice (inner exception) che ha originato l'errore.
 */
public class ProcessingException extends RuntimeException {

    public ProcessingException(String message) {
        super(message);
    }

    public ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
