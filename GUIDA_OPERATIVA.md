# Guida Operativa - Document Processing System

Questa guida spiega in modo completo come funziona l'API implementata in questo progetto, come usarla, quali componenti entrano in gioco, quali errori possono verificarsi e quali miglioramenti si potrebbero introdurre in futuro.

Il progetto implementa un backend Java con Spring Boot che riceve un evento relativo a un documento gia presente su filesystem locale, valida l'evento, legge il file, calcola l'hash SHA-256, genera un archivio ZIP e restituisce un evento finale di avvenuta elaborazione.

## 1. Obiettivo Del Sistema

Il sistema serve a trasformare un evento di input di tipo `DocumentReceived` in un evento di output di tipo `DocumentProcessed`.

In termini pratici:

1. Un client invia una richiesta HTTP con `documentId`, `storageRef` e `metadata`.
2. L'applicazione controlla che i dati siano validi.
3. L'applicazione cerca il file indicato da `storageRef` nel filesystem locale.
4. Il file viene letto come array di byte.
5. Viene calcolato l'hash SHA-256 del contenuto.
6. Viene creato un file ZIP contenente:
   - `invoice.pdf`: il file originale.
   - `metadata.json`: metadata originale piu metadata generata.
   - `hash.txt`: hash SHA-256 calcolato.
7. Lo ZIP viene salvato nella directory di output.
8. L'applicazione restituisce e logga un evento `DocumentProcessed`.

Il sistema e sincrono: la risposta HTTP viene restituita solo dopo aver completato l'elaborazione e aver scritto lo ZIP.

## 2. Tecnologie Usate

Il progetto usa:

- Java 21, configurato in `pom.xml`.
- Spring Boot 3.3.6.
- Spring Web, per esporre l'API REST.
- Spring Validation, per validare i campi base della request.
- Jackson, per serializzare/deserializzare JSON.
- JUnit 5, AssertJ e MockMvc, per i test.
- API standard Java:
  - `java.nio.file.Files` e `Path` per il filesystem.
  - `MessageDigest` per SHA-256.
  - `ZipOutputStream` per creare lo ZIP.
  - `Clock` e `Instant` per date UTC testabili.

Non vengono usati database, code di messaggi, Kafka, storage cloud o sistemi esterni. Questo e coerente con la traccia, che richiede storage locale e produzione/log di un evento finale.

## 3. Struttura Del Progetto

La struttura principale e:

```text
src/main/java/com/acube/documentprocessing
  DocumentProcessingApplication.java
  api/
    DocumentController.java
    ApiExceptionHandler.java
    dto/
      DocumentReceivedRequest.java
      DocumentProcessedResponse.java
      ErrorResponse.java
  application/
    DocumentProcessingService.java
  config/
    StorageProperties.java
  domain/
    DocumentReceivedEvent.java
    DocumentProcessedEvent.java
    ProcessingMetadata.java
  exception/
    DocumentNotFoundException.java
    ProcessingException.java
    ValidationException.java
  processing/
    HashService.java
    ZipArchiveService.java
  storage/
    DocumentStorage.java
    LocalFileSystemDocumentStorage.java
    StoredDocument.java
```

La separazione e importante:

- `api`: contiene il livello HTTP, cioe controller, DTO e gestione errori.
- `application`: contiene il caso d'uso principale.
- `domain`: contiene i modelli concettuali del problema.
- `storage`: contiene l'accesso al filesystem locale.
- `processing`: contiene logiche tecniche riusabili, cioe hash e ZIP.
- `config`: contiene configurazioni applicative.
- `exception`: contiene eccezioni applicative specifiche.

Questa struttura evita di mettere tutta la logica dentro il controller e rende il codice piu testabile.

## 4. Configurazione

La configurazione predefinita e in `src/main/resources/application.yml`:

```yaml
app:
  storage:
    input-root: .
    output-root: output

spring:
  application:
    name: document-processing-system
```

Significato:

- `app.storage.input-root`: directory base da cui leggere i file indicati da `storageRef`.
- `app.storage.output-root`: directory in cui scrivere gli ZIP prodotti.

Con i valori di default:

- se `storageRef` vale `input/invoice.pdf`, il sistema cerca il file in `./input/invoice.pdf`;
- se `documentId` vale `123`, lo ZIP viene scritto in `./output/123.zip`.

Esempio con configurazione custom:

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--app.storage.input-root=/tmp/documents/input --app.storage.output-root=/tmp/documents/output"
```

In quel caso una request con:

```json
{
  "storageRef": "invoice.pdf"
}
```

cerca il file:

```text
/tmp/documents/input/invoice.pdf
```

## 5. Avvio Dell'Applicazione

Prerequisiti:

- Java 21.
- Maven installato.

Comando:

```bash
mvn spring-boot:run
```

L'applicazione espone di default la porta `8080`.

Endpoint disponibile:

```http
POST http://localhost:8080/api/documents/process
```

Per eseguire i test:

```bash
mvn test
```

Nota: nell'ambiente in cui e stato scritto il codice Maven non era disponibile nel PATH, quindi i test non sono stati eseguiti qui. Il progetto e comunque configurato per essere testato con Maven.

## 6. Contratto Dell'API

### Endpoint

```http
POST /api/documents/process
Content-Type: application/json
```

### Request

Esempio:

```json
{
  "documentId": "123",
  "storageRef": "input/invoice.pdf",
  "metadata": {
    "type": "invoice",
    "receivedAt": "2026-05-01T10:00:00Z"
  }
}
```

Campi:

- `documentId`: identificativo logico del documento. Viene usato anche per creare il nome dello ZIP.
- `storageRef`: percorso relativo del file rispetto a `app.storage.input-root`.
- `metadata`: oggetto JSON con informazioni contestuali.

Metadata supportata:

- `type`: obbligatorio. Valori ammessi: `invoice`, `credit_note`.
- `receivedAt`: opzionale. Se presente deve essere un istante ISO-8601 valido, ad esempio `2026-05-01T10:00:00Z`.

### Response Di Successo

Status:

```http
200 OK
```

Body:

```json
{
  "documentId": "123",
  "zipPath": "output/123.zip",
  "metadata": {
    "type": "invoice",
    "processedAt": "2026-05-06T12:30:00Z",
    "hash": "abc123...",
    "sizeBytes": 20480
  }
}
```

Campi:

- `documentId`: lo stesso identificativo ricevuto in input.
- `zipPath`: path dello ZIP prodotto.
- `metadata.type`: tipo documento.
- `metadata.processedAt`: istante UTC in cui il documento e stato processato.
- `metadata.hash`: SHA-256 del contenuto originale.
- `metadata.sizeBytes`: dimensione del file originale in byte.

## 7. Esempio Operativo Completo

Partendo dalla root del progetto, crea una cartella input:

```bash
mkdir -p input
```

Copia un PDF dentro `input/invoice.pdf`.

Avvia l'app:

```bash
mvn spring-boot:run
```

Invia la richiesta:

```bash
curl -X POST http://localhost:8080/api/documents/process \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": "123",
    "storageRef": "input/invoice.pdf",
    "metadata": {
      "type": "invoice",
      "receivedAt": "2026-05-01T10:00:00Z"
    }
  }'
```

Se tutto va bene, il sistema crea:

```text
output/123.zip
```

Lo ZIP contiene:

```text
invoice.pdf
metadata.json
hash.txt
```

## 8. Flusso Interno Passo Per Passo

### 8.1 Ricezione HTTP

La request arriva a:

```java
DocumentController.process(...)
```

Il controller fa solo tre cose:

1. Riceve il JSON.
2. Lo converte in `DocumentReceivedRequest`.
3. Lo passa al `DocumentProcessingService`.

Il controller non contiene logica di business. Questa e una scelta corretta per mantenere separati trasporto HTTP e logica applicativa.

### 8.2 Validazione Base Della Request

`DocumentReceivedRequest` usa annotazioni Jakarta Validation:

- `@NotBlank` su `documentId`.
- `@NotBlank` su `storageRef`.
- `@NotNull` su `metadata`.

Se uno di questi campi manca, Spring genera un errore gestito da `ApiExceptionHandler`.

### 8.3 Conversione In Evento Di Dominio

La request viene trasformata in:

```java
DocumentReceivedEvent
```

Questo record rappresenta l'evento di input indipendentemente dal fatto che sia arrivato via HTTP.

Questo e utile perche in futuro lo stesso service potrebbe essere chiamato anche da:

- consumer Kafka;
- job batch;
- message queue;
- CLI;
- test automatici.

### 8.4 Validazione Applicativa

Il metodo principale e:

```java
DocumentProcessingService.process(...)
```

La prima operazione e `validateEvent`.

Controlli eseguiti:

- L'evento non deve essere `null`.
- `documentId` deve essere presente e non vuoto.
- `documentId` puo contenere solo lettere, numeri, punto, underscore e trattino.
- `documentId` non puo essere `.` o `..`.
- `storageRef` deve essere presente e non vuoto.
- `metadata` deve essere presente e non vuoto.
- `metadata.type` deve essere presente, stringa non vuota e uno tra `invoice`, `credit_note`.
- `metadata.receivedAt`, se presente, deve essere parseabile come `Instant`.

Questi controlli servono a prevenire input incompleti, ambigui o pericolosi.

### 8.5 Lettura Del File

La lettura e delegata a:

```java
LocalFileSystemDocumentStorage.readDocument(...)
```

Il metodo riceve `storageRef`, lo risolve rispetto a `app.storage.input-root` e controlla che il path finale resti dentro la root configurata.

Esempio valido:

```text
input-root = /data/input
storageRef = invoice.pdf
path finale = /data/input/invoice.pdf
```

Esempio non valido:

```text
input-root = /data/input
storageRef = ../secret.txt
path finale = /data/secret.txt
```

Il secondo caso viene bloccato perche uscirebbe dalla directory configurata. Questo si chiama protezione da path traversal.

Dopo la risoluzione del path:

- se il file non esiste o non e un file regolare, viene lanciata `DocumentNotFoundException`;
- se il file esiste, viene letto con `Files.readAllBytes`.

Il risultato e:

```java
StoredDocument(Path path, byte[] content)
```

### 8.6 Calcolo Dell'Hash

Il calcolo e delegato a:

```java
HashService.computeSha256Hex(...)
```

Il servizio usa:

```java
MessageDigest.getInstance("SHA-256")
```

L'output e una stringa esadecimale lowercase.

Esempio:

```text
2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
```

L'hash identifica il contenuto del file. Se cambia anche un solo byte, cambia l'hash.

### 8.7 Creazione Della Metadata Generata

Dopo aver letto il file e calcolato l'hash, il service crea:

```java
ProcessingMetadata
```

Contiene:

- `type`: preso da `metadata.type`.
- `processedAt`: istante corrente UTC.
- `hash`: hash SHA-256.
- `sizeBytes`: lunghezza dell'array di byte del documento.

L'uso di `Clock` rende il tempo testabile. Nei test si puo fissare un tempo preciso invece di dipendere dall'orologio reale.

### 8.8 Creazione Dello ZIP

La creazione dello ZIP e delegata a:

```java
ZipArchiveService.createArchive(...)
```

Lo ZIP viene creato in memoria usando:

```java
ByteArrayOutputStream
ZipOutputStream
```

Entry create:

```text
invoice.pdf
metadata.json
hash.txt
```

Contenuto:

- `invoice.pdf`: byte originali del documento.
- `hash.txt`: stringa hash in UTF-8.
- `metadata.json`: JSON con due sezioni:

```json
{
  "originalMetadata": {
    "type": "invoice",
    "receivedAt": "2026-05-01T10:00:00Z"
  },
  "processingMetadata": {
    "type": "invoice",
    "processedAt": "2026-05-06T12:30:00Z",
    "hash": "abc123...",
    "sizeBytes": 20480
  }
}
```

Nota importante: l'entry del documento nello ZIP si chiama sempre `invoice.pdf`, come richiesto dalla traccia, anche se `metadata.type` vale `credit_note`.

### 8.9 Scrittura Dello ZIP

La scrittura e delegata a:

```java
LocalFileSystemDocumentStorage.writeOutputArchive(...)
```

Il nome file e:

```text
{documentId}.zip
```

Esempio:

```text
documentId = 123
output = output/123.zip
```

Anche qui viene controllato che il path finale resti dentro `app.storage.output-root`.

La directory di output viene creata automaticamente se non esiste:

```java
Files.createDirectories(outputRoot)
```

Poi lo ZIP viene scritto con:

```java
Files.write(outputPath, archiveContent)
```

### 8.10 Produzione Dell'Evento Finale

Quando lo ZIP e stato scritto, il service crea:

```java
DocumentProcessedEvent
```

Contiene:

- `documentId`;
- `zipPath`;
- `metadata` generata.

L'evento viene:

1. loggato come JSON;
2. restituito al controller;
3. convertito in response HTTP.

## 9. Gestione Degli Errori

La gestione errori e centralizzata in:

```java
ApiExceptionHandler
```

Questo evita di duplicare `try/catch` nei controller.

### 9.1 Errore 400 - Bad Request

Viene restituito quando la richiesta e formalmente o semanticamente invalida.

Esempi:

- JSON malformato.
- `documentId` mancante.
- `documentId` vuoto.
- `documentId` contiene `/` o sequenze pericolose.
- `storageRef` mancante.
- `storageRef` vuoto.
- `storageRef` assoluto, ad esempio `/tmp/file.pdf`.
- `storageRef` tenta path traversal, ad esempio `../secret.pdf`.
- `metadata` mancante.
- `metadata.type` mancante.
- `metadata.type` diverso da `invoice` o `credit_note`.
- `metadata.receivedAt` non e una data ISO-8601 valida.

Esempio response:

```json
{
  "timestamp": "2026-05-18T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "metadata.type must be one of: invoice, credit_note",
  "path": "/api/documents/process"
}
```

### 9.2 Errore 404 - Not Found

Viene restituito quando `storageRef` e valido come path, ma il file non esiste.

Esempio:

```json
{
  "timestamp": "2026-05-18T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Document file not found: input/invoice.pdf",
  "path": "/api/documents/process"
}
```

### 9.3 Errore 500 - Internal Server Error

Viene restituito quando avviene un errore tecnico durante la lavorazione.

Esempi:

- impossibile leggere il file per problemi di permessi;
- impossibile creare la directory di output;
- impossibile scrivere lo ZIP;
- errore imprevisto durante la serializzazione dei metadata;
- errore interno inatteso.

Esempio:

```json
{
  "timestamp": "2026-05-18T10:00:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Unable to write output archive for documentId: 123",
  "path": "/api/documents/process"
}
```

## 10. Sicurezza E Validazioni Critiche

### 10.1 Path Traversal

Il rischio principale e permettere a un utente di leggere file fuori dalla directory prevista.

Esempio pericoloso:

```json
{
  "storageRef": "../../etc/passwd"
}
```

Il sistema lo blocca perche:

1. risolve il path rispetto a `input-root`;
2. normalizza il path;
3. verifica che il path finale inizi ancora con `input-root`.

Questo controllo e fondamentale.

### 10.2 Path Assoluti Non Ammessi

`storageRef` deve essere relativo.

Non e ammesso:

```text
/Users/user/secrets/file.pdf
```

Motivo: se si accettassero path assoluti, il client potrebbe chiedere al server di leggere qualunque file accessibile dal processo.

### 10.3 documentId Come Nome File

`documentId` viene usato per creare il nome dello ZIP. Quindi deve essere sicuro.

Valori ammessi:

```text
abc
ABC
123
invoice-123
invoice_123
invoice.123
```

Valori non ammessi:

```text
../123
folder/123
.
..
123/456
```

Questo evita che il client scriva file fuori dalla directory di output.

### 10.4 Lettura File In Memoria

Il sistema usa:

```java
Files.readAllBytes(...)
```

Questo e semplice e va bene per file piccoli o medi, ma e un punto critico per file grandi. Se arrivano file molto grandi, l'applicazione potrebbe consumare molta memoria.

### 10.5 ZIP Creato In Memoria

Anche lo ZIP viene creato in memoria con `ByteArrayOutputStream`.

Questo rende l'implementazione semplice e testabile, ma raddoppia il problema per file grandi:

- il file originale e in memoria;
- lo ZIP generato e in memoria;
- temporaneamente possono esistere piu copie dei dati.

Per produzione con file grandi conviene passare a uno streaming diretto su file.

### 10.6 Sovrascrittura Output

Se viene processato due volte lo stesso `documentId`, il file:

```text
output/{documentId}.zip
```

viene sovrascritto.

Questo comportamento e semplice, ma va valutato. In alcuni contesti e accettabile, in altri bisognerebbe evitare sovrascritture o versionare gli output.

### 10.7 Nessuna Autenticazione

L'API attuale non ha autenticazione o autorizzazione.

Questo va bene per un test tecnico locale, ma non e sufficiente per esporre il servizio in rete.

## 11. Test Implementati

Sono stati aggiunti test per coprire i comportamenti principali.

### 11.1 HashServiceTest

Verifica che l'hash SHA-256 di una stringa nota sia corretto.

Input:

```text
hello
```

Output atteso:

```text
2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
```

### 11.2 ZipArchiveServiceTest

Verifica che lo ZIP contenga:

- `invoice.pdf`;
- `metadata.json`;
- `hash.txt`.

Verifica anche che:

- il documento originale sia preservato;
- `hash.txt` contenga l'hash;
- `metadata.json` contenga metadata originale e metadata generata.

### 11.3 LocalFileSystemDocumentStorageTest

Verifica:

- lettura di un file esistente;
- blocco di path traversal;
- errore per file mancante;
- scrittura dello ZIP sotto output root;
- blocco di `documentId` non sicuri.

### 11.4 DocumentProcessingServiceTest

Verifica il flusso applicativo completo senza HTTP:

- validazione;
- lettura file;
- calcolo hash;
- creazione ZIP;
- scrittura output;
- evento finale.

Verifica anche casi negativi:

- `metadata.type` non supportato;
- `receivedAt` invalido;
- `documentId` non sicuro.

### 11.5 DocumentControllerIntegrationTest

Verifica l'API REST con MockMvc:

- `200 OK` su richiesta valida;
- `400 Bad Request` su body invalido;
- `404 Not Found` su file mancante.

## 12. Punti Critici Del Sistema

### 12.1 Gestione File Grandi

Attualmente il file viene letto interamente in memoria. Questo e il limite tecnico piu importante.

Rischio:

- consumo RAM elevato;
- rallentamenti;
- `OutOfMemoryError` con file molto grandi o molte richieste concorrenti.

Miglioramento futuro:

- usare streaming da input file a ZIP;
- calcolare hash in streaming;
- evitare `byte[]` per l'intero documento.

### 12.2 Concorrenza Sullo Stesso documentId

Due richieste simultanee con lo stesso `documentId` scrivono lo stesso file ZIP.

Rischio:

- race condition;
- file parzialmente sovrascritto;
- output non deterministico.

Miglioramento futuro:

- scrivere prima su file temporaneo;
- poi fare move atomico;
- bloccare documenti duplicati;
- aggiungere versioning o timestamp nel nome file.

### 12.3 Idempotenza

Non e definita una strategia di idempotenza.

Domanda aperta:

- se arriva due volte lo stesso evento, il sistema deve restituire lo stesso risultato?
- deve rigenerare lo ZIP?
- deve rifiutare il duplicato?

Miglioramento futuro:

- introdurre uno stato di processing;
- salvare gli eventi processati;
- usare `documentId` come chiave idempotente.

### 12.4 Mancanza Di Persistenza

Il sistema non salva lo storico delle elaborazioni.

Rischio:

- dopo un restart non si sa cosa e stato processato;
- non c'e audit trail strutturato;
- non si possono interrogare stati precedenti.

Miglioramento futuro:

- aggiungere database relazionale;
- salvare `documentId`, `storageRef`, hash, path ZIP, status, errori, timestamp.

### 12.5 Mancanza Di Coda/Event Bus

La traccia parla di eventi, ma l'implementazione espone una API HTTP sincrona.

Questo e accettabile per il test, ma in un sistema reale gli eventi potrebbero arrivare da:

- Kafka;
- RabbitMQ;
- SQS;
- Pub/Sub;
- filesystem watcher.

Miglioramento futuro:

- mantenere `DocumentProcessingService` invariato;
- aggiungere un adapter di ingresso asincrono;
- pubblicare `DocumentProcessed` su un topic/coda.

### 12.6 Error Handling Tecnico

Gli errori sono gestiti in modo chiaro lato API, ma non esiste retry automatico.

Rischio:

- un errore temporaneo di filesystem causa fallimento immediato;
- il client deve riprovare.

Miglioramento futuro:

- introdurre retry controllati per errori transienti;
- distinguere errori permanenti da errori temporanei;
- salvare eventi falliti per rielaborazione.

### 12.7 Osservabilita

Attualmente viene loggato l'evento finale, ma non ci sono metriche.

Miglioramento futuro:

- aggiungere actuator;
- metriche su numero documenti processati;
- durata media processing;
- dimensione media file;
- numero errori per tipo;
- tracing distribuito se il sistema cresce.

### 12.8 Validazione Metadata Limitata

Oggi `metadata` e una `Map<String, Object>`.

Vantaggio:

- flessibile;
- permette metadata extra.

Svantaggio:

- schema poco esplicito;
- validazione debole;
- errori possibili a runtime.

Miglioramento futuro:

- creare un DTO tipizzato per metadata;
- validare con Bean Validation;
- definire uno schema JSON ufficiale.

### 12.9 Nome Entry Documento Sempre invoice.pdf

Lo ZIP contiene sempre `invoice.pdf`, anche per `credit_note`.

Questo rispetta la traccia, ma potrebbe essere ambiguo.

Miglioramento futuro:

- usare il nome originale del file;
- oppure usare `document.pdf`;
- oppure usare `credit_note.pdf` quando il tipo e `credit_note`.

Va deciso in base al contratto richiesto dai consumer dello ZIP.

### 12.10 Nessuna Verifica Sul Tipo Reale Del File

Il sistema non controlla che il file sia davvero un PDF.

Rischio:

- un file `.txt` potrebbe essere archiviato come `invoice.pdf`;
- un file malevolo potrebbe essere accettato.

Miglioramento futuro:

- controllare MIME type;
- controllare magic bytes PDF `%PDF`;
- eventualmente validare o sanitizzare il documento.

### 12.11 Nessun Limite Di Dimensione

Non esiste un limite massimo alla dimensione del file.

Miglioramento futuro:

- configurare `app.storage.max-file-size`;
- rifiutare file troppo grandi con errore chiaro;
- proteggere il servizio da abusi.

### 12.12 Nessuna Pulizia Dei File

Il sistema crea ZIP ma non ha policy di retention.

Miglioramento futuro:

- cancellare output piu vecchi di una soglia;
- spostare archivi su storage esterno;
- schedulare cleanup periodico.

## 13. Miglioramenti Futuri Consigliati

### 13.1 Streaming End-To-End

Obiettivo: gestire file grandi in modo efficiente.

Come:

- aprire `InputStream` sul file;
- aggiornare `MessageDigest` mentre si legge;
- scrivere direttamente nello `ZipOutputStream` collegato al file di output;
- evitare di caricare tutto in RAM.

Beneficio:

- minore memoria usata;
- maggiore robustezza;
- migliore scalabilita.

### 13.2 Scrittura Atomica

Obiettivo: evitare ZIP corrotti se il processo fallisce durante la scrittura.

Come:

1. Scrivere su `output/{documentId}.zip.tmp`.
2. Chiudere correttamente lo stream.
3. Rinominare con move atomico a `output/{documentId}.zip`.

Beneficio:

- i consumer non vedono file parziali;
- recovery piu semplice.

### 13.3 OpenAPI/Swagger

Obiettivo: documentare e testare l'API dal browser.

Come:

- aggiungere `springdoc-openapi-starter-webmvc-ui`;
- esporre `/swagger-ui.html`;
- documentare request, response ed errori.

Beneficio:

- onboarding piu semplice;
- contratto API piu chiaro;
- test manuale facilitato.

### 13.4 Docker

Obiettivo: rendere l'app eseguibile ovunque.

Come:

- aggiungere `Dockerfile`;
- montare volumi per input e output;
- configurare porta e variabili ambiente.

Esempio concettuale:

```bash
docker run \
  -p 8080:8080 \
  -v ./input:/data/input \
  -v ./output:/data/output \
  document-processing-system
```

### 13.5 Database Per Stato Processing

Obiettivo: tracciare ogni elaborazione.

Tabella possibile:

```text
document_processing
  id
  document_id
  storage_ref
  zip_path
  hash
  size_bytes
  status
  error_message
  received_at
  processed_at
  created_at
  updated_at
```

Stati possibili:

- `RECEIVED`;
- `PROCESSING`;
- `PROCESSED`;
- `FAILED`.

### 13.6 Processing Asincrono

Obiettivo: non bloccare il client durante elaborazioni lunghe.

Possibile flusso:

1. API riceve evento.
2. Salva job con status `RECEIVED`.
3. Restituisce `202 Accepted`.
4. Worker processa in background.
5. Evento finale viene pubblicato o salvato.
6. Client consulta `GET /api/documents/{documentId}`.

### 13.7 Autenticazione E Autorizzazione

Obiettivo: proteggere l'API.

Possibili approcci:

- API key;
- OAuth2/JWT;
- Basic auth solo per ambienti interni;
- mTLS per integrazioni server-to-server.

### 13.8 Migliore Modellazione Dei Metadata

Oggi:

```java
Map<String, Object> metadata
```

Futuro:

```java
record DocumentMetadata(
    DocumentType type,
    Instant receivedAt
) {}
```

Con:

```java
enum DocumentType {
    INVOICE,
    CREDIT_NOTE
}
```

Beneficio:

- meno errori;
- validazione piu forte;
- codice piu leggibile.

### 13.9 Test Di Carico

Obiettivo: capire quanto regge il sistema.

Scenari:

- molti file piccoli;
- pochi file grandi;
- richieste concorrenti con documentId diversi;
- richieste concorrenti con stesso documentId;
- filesystem lento;
- output directory non scrivibile.

### 13.10 CI/CD

Obiettivo: impedire merge di codice rotto.

Pipeline minima:

```text
mvn test
mvn package
```

Eventualmente:

- checkstyle;
- spotbugs;
- jacoco coverage;
- build Docker image.

## 14. Decisioni Implementative Da Conoscere

### 14.1 Perche Spring Boot

Spring Boot e adatto perche offre:

- API REST pronte;
- dependency injection;
- validazione;
- test integration semplici;
- configurazione standard;
- gestione errori pulita.

Per questo problema sarebbe possibile usare Java puro, ma Spring Boot rende piu leggibile la separazione tra API, service e storage.

### 14.2 Perche Non Usare Un Database

La traccia non richiede persistenza applicativa. Aggiungere subito un database aumenterebbe complessita senza valore immediato.

### 14.3 Perche Non Usare Kafka

La traccia parla di eventi, ma non richiede un broker. L'API HTTP simula l'ingresso di un evento e restituisce l'evento finale.

La struttura e comunque pronta per un broker: basterebbe aggiungere un adapter che chiama `DocumentProcessingService`.

### 14.4 Perche Usare Clock

`Clock` permette di testare `processedAt` in modo deterministico.

Senza `Clock`, nei test bisognerebbe confrontare date dinamiche, rendendo i test piu fragili.

### 14.5 Perche Separare Storage Da Service

Il service deve orchestrare il caso d'uso, non conoscere tutti i dettagli del filesystem.

Separando `DocumentStorage`, in futuro si puo sostituire il filesystem locale con:

- S3;
- Azure Blob Storage;
- database;
- storage remoto;
- filesystem distribuito.

## 15. Checklist Per Capire Se Funziona

Per verificare manualmente:

1. L'app parte senza errori.
2. Esiste un file leggibile sotto `input-root`.
3. La request contiene `documentId`, `storageRef`, `metadata.type`.
4. `storageRef` e relativo.
5. `metadata.type` e `invoice` o `credit_note`.
6. La response e `200 OK`.
7. `output/{documentId}.zip` viene creato.
8. Lo ZIP contiene `invoice.pdf`, `metadata.json`, `hash.txt`.
9. `hash.txt` coincide con l'hash del file originale.
10. `metadata.json` contiene metadata originale e metadata generata.
11. Nei log compare l'evento `DocumentProcessed`.

## 16. File Principali Da Leggere

Per capire il progetto in ordine:

1. `README.md`: uso rapido.
2. `src/main/resources/application.yml`: configurazione.
3. `DocumentController.java`: ingresso HTTP.
4. `DocumentProcessingService.java`: flusso principale.
5. `LocalFileSystemDocumentStorage.java`: lettura/scrittura filesystem.
6. `HashService.java`: calcolo SHA-256.
7. `ZipArchiveService.java`: creazione ZIP.
8. `ApiExceptionHandler.java`: gestione errori.
9. `src/test/java/...`: esempi eseguibili del comportamento atteso.

## 17. Sintesi Finale

Questa API implementa una pipeline semplice ma completa:

```text
HTTP request
  -> validazione
  -> lettura file locale
  -> hash SHA-256
  -> metadata generata
  -> ZIP
  -> scrittura output
  -> response DocumentProcessed
```

La versione attuale e adatta a un test tecnico e a file piccoli o medi. I punti da migliorare prima di un uso produttivo sono soprattutto streaming per file grandi, idempotenza, concorrenza sullo stesso `documentId`, persistenza dello stato, autenticazione e osservabilita.
