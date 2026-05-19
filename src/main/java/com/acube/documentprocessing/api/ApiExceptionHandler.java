package com.acube.documentprocessing.api;

import com.acube.documentprocessing.api.dto.ErrorResponse;
import com.acube.documentprocessing.exception.DocumentNotFoundException;
import com.acube.documentprocessing.exception.ProcessingException;
import com.acube.documentprocessing.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Gestore globale delle eccezioni per le API REST.
 * Tramite l'annotazione @RestControllerAdvice, questa classe intercetta
 * centralmente tutte le eccezioni sollevate dai controller dell'applicazione,
 * traducendole in risposte HTTP standardizzate e sicure.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Gestisce gli errori di validazione automatica sui DTO in ingresso.
     * Estrae tutti i messaggi di errore relativi ai singoli campi non validi
     * e li concatena in un unico messaggio riassuntivo.
     *
     * @param exception L'eccezione generata dalla fallita validazione del DTO.
     * @param request   La richiesta HTTP originale.
     * @return Risposta con codice HTTP 400 (Bad Request).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleInvalidRequest(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage() == null ? error.getField() + " is invalid" : error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Gestisce le eccezioni legate a validazioni logiche manuali o a
     * richieste con corpo (JSON) malformato o assente.
     *
     * @param exception L'eccezione generata dal dominio o dal framework.
     * @param request   La richiesta HTTP originale.
     * @return Risposta con codice HTTP 400 (Bad Request).
     */
    @ExceptionHandler({ValidationException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    /**
     * Gestisce il caso in cui un documento specifico non venga trovato 
     * nel sistema di storage.
     *
     * @param exception L'eccezione sollevata in caso di documento mancante.
     * @param request   La richiesta HTTP originale.
     * @return Risposta con codice HTTP 404 (Not Found).
     */
    @ExceptionHandler(DocumentNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(DocumentNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    /**
     * Gestisce gli errori interni imprevisti generati durante l'elaborazione
     * di un documento da parte della logica di business.
     *
     * @param exception L'eccezione generata dal dominio.
     * @param request   La richiesta HTTP originale.
     * @return Risposta con codice HTTP 500 (Internal Server Error).
     */
    @ExceptionHandler(ProcessingException.class)
    ResponseEntity<ErrorResponse> handleProcessingError(ProcessingException exception, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request);
    }

    /**
     * Gestore di fallback per qualsiasi altra eccezione non prevista (es. NullPointerException).
     * Nasconde i dettagli tecnici allo scopo di prevenire la potenziale
     * esposizione di vulnerabilità (information leakage).
     *
     * @param exception L'eccezione imprevista.
     * @param request   La richiesta HTTP originale.
     * @return Risposta generica con codice HTTP 500 (Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected processing error", request);
    }

    /**
     * Metodo factory interno per costruire in modo uniforme l'oggetto di risposta.
     * 
     * @param status  Lo stato HTTP da restituire.
     * @param message Il messaggio descrittivo dell'errore.
     * @param request La richiesta HTTP originale per estrarre l'URI.
     * @return La risposta ResponseEntity confezionata con i dati richiesti.
     */
    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        ));
    }
}
