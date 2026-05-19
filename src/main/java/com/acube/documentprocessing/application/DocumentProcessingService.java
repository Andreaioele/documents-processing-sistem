package com.acube.documentprocessing.application;

import com.acube.documentprocessing.domain.DocumentProcessedEvent;
import com.acube.documentprocessing.domain.DocumentReceivedEvent;
import com.acube.documentprocessing.domain.ProcessingMetadata;
import com.acube.documentprocessing.exception.ValidationException;
import com.acube.documentprocessing.processing.HashService;
import com.acube.documentprocessing.processing.ZipArchiveService;
import com.acube.documentprocessing.storage.DocumentStorage;
import com.acube.documentprocessing.storage.StoredDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Servizio applicativo centrale (Core Business Logic) che orchestra
 * l'elaborazione dei documenti.
 * L'annotazione @Service dice a Spring di gestire questa classe come un "Bean"
 * (un componente iniettabile).
 */
@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);
    private static final Set<String> SUPPORTED_TYPES = Set.of("invoice", "credit_note");

    private final DocumentStorage documentStorage;
    private final HashService hashService;
    private final ZipArchiveService zipArchiveService;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    /**
     * Costruttore per l'iniezione delle dipendenze necessarie per completare
     * l'elaborazione.
     */
    public DocumentProcessingService(
            DocumentStorage documentStorage,
            HashService hashService,
            ZipArchiveService zipArchiveService,
            Clock clock,
            ObjectMapper objectMapper) {
        this.documentStorage = documentStorage;
        this.hashService = hashService;
        this.zipArchiveService = zipArchiveService;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    /**
     * Metodo principale che orchestra tutto il flusso di elaborazione:
     * 1. Valida rigorosamente i dati in ingresso.
     * 2. Legge il contenuto del file fisico dallo storage.
     * 3. Calcola l'hash SHA-256 del file per assicurarne l'integrità.
     * 4. Crea un archivio ZIP contenente il documento originale e i metadati
     * aggiornati.
     * 5. Salva l'archivio ZIP generato nel sistema di archiviazione.
     * 6. Genera e logga un evento finale di completamento.
     *
     * @param event L'evento di dominio che contiene i dettagli del documento da
     *              elaborare.
     * @return L'evento finale contenente i dati e i riferimenti al documento
     *         elaborato.
     */
    public DocumentProcessedEvent process(DocumentReceivedEvent event) {
        validateEvent(event);

        StoredDocument document = documentStorage.readDocument(event.storageRef());
        String hash = hashService.computeSha256Hex(document.content());
        String type = event.metadata().get("type").toString();
        ProcessingMetadata processingMetadata = new ProcessingMetadata(
                type,
                Instant.now(clock),
                hash,
                document.content().length);

        byte[] archive = zipArchiveService.createArchive(
                document.content(),
                event.metadata(),
                processingMetadata);
        Path outputPath = documentStorage.writeOutputArchive(event.documentId(), archive);

        DocumentProcessedEvent processedEvent = new DocumentProcessedEvent(
                event.documentId(),
                documentStorage.outputReference(outputPath),
                processingMetadata);
        logProcessedEvent(processedEvent);
        return processedEvent;
    }

    /**
     * Valida i parametri principali dell'evento di dominio per assicurarsi
     * che rispettino le regole di business (es. previene Path Traversal bloccando
     * "..").
     */
    private void validateEvent(DocumentReceivedEvent event) {
        if (event == null) {
            throw new ValidationException("Event body is required");
        }
        if (event.documentId() == null || event.documentId().isBlank()) {
            throw new ValidationException("documentId must be present and not empty");
        }
        if (!event.documentId().matches("[A-Za-z0-9._-]+")
                || ".".equals(event.documentId())
                || "..".equals(event.documentId())) {
            throw new ValidationException("documentId can only contain letters, numbers, dot, underscore and dash");
        }
        if (event.storageRef() == null || event.storageRef().isBlank()) {
            throw new ValidationException("storageRef must be present and not empty");
        }
        validateMetadata(event.metadata());
    }

    /**
     * Valida i metadati assicurandosi che contengano le informazioni obbligatorie
     * e che il formato (come il tipo di documento o la data ISO-8601) sia corretto.
     */
    private void validateMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            throw new ValidationException("metadata must be present and well-formed");
        }

        Object type = metadata.get("type");
        if (!(type instanceof String typeValue) || typeValue.isBlank()) {
            throw new ValidationException("metadata.type must be present and not empty");
        }
        if (!SUPPORTED_TYPES.contains(typeValue)) {
            throw new ValidationException("metadata.type must be one of: invoice, credit_note");
        }

        Object receivedAt = metadata.get("receivedAt");
        if (receivedAt != null) {
            try {
                Instant.parse(receivedAt.toString());
            } catch (RuntimeException e) {
                throw new ValidationException("metadata.receivedAt must be a valid ISO-8601 instant");
            }
        }
    }

    /**
     * Loggo l'evento elaborato in formato JSON per facilitare l'analisi su sistemi
     * centralizzati.
     * Se fallisce per qualche motivo, logga almeno l'ID del documento in testo
     * semplice.
     */
    private void logProcessedEvent(DocumentProcessedEvent processedEvent) {
        try {
            log.info("DocumentProcessed event: {}", objectMapper.writeValueAsString(processedEvent));
        } catch (JsonProcessingException e) {
            log.info("DocumentProcessed event for documentId={}", processedEvent.documentId());
        }
    }
}
