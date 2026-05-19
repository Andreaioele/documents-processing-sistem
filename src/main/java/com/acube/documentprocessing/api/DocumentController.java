package com.acube.documentprocessing.api;

import com.acube.documentprocessing.api.dto.DocumentProcessedResponse;
import com.acube.documentprocessing.api.dto.DocumentReceivedRequest;
import com.acube.documentprocessing.application.DocumentProcessingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST che espone le API verso l'esterno per la gestione dei
 * documenti.
 * Rappresenta il punto di ingresso (endpoint) della nostra applicazione.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentProcessingService processingService;

    /**
     * Costruttore per l'iniezione delle dipendenze (Dependency Injection).
     * 
     * @param processingService Il servizio che contiene la logica di business per
     *                          elaborare i documenti.
     */
    public DocumentController(DocumentProcessingService processingService) {
        this.processingService = processingService;
    }

    /**
     * Endpoint per avviare l'elaborazione di un nuovo documento.
     * Risponde a richieste HTTP POST sull'URL: /api/documents/process
     *
     * @param request Il corpo (body) della richiesta HTTP contenente i dati del
     *                documento.
     *                L'annotazione @Valid fa scattare in automatico i controlli di
     *                validazione
     *                definiti nel DTO (come @NotBlank). Se falliscono, viene
     *                lanciata un'eccezione
     *                che sarà catturata e gestita automaticamente
     *                dall'ApiExceptionHandler.
     * @return Una risposta HTTP 200 (OK) contenente i dati del documento elaborato
     *         (DocumentProcessedResponse).
     */
    @PostMapping("/process")
    public ResponseEntity<DocumentProcessedResponse> process(@Valid @RequestBody DocumentReceivedRequest request) {
        // 1. request.toEvent() -> converte il DTO in ingresso in un evento per il
        // dominio.
        // 2. processingService.process(...) -> esegue la logica di business vera e
        // propria.
        // 3. DocumentProcessedResponse.from(...) -> converte il risultato dal dominio
        // in un DTO di uscita.
        // 4. ResponseEntity.ok(...) -> impacchetta il tutto in una risposta HTTP di
        // successo.
        return ResponseEntity.ok(DocumentProcessedResponse.from(processingService.process(request.toEvent())));
    }
}
