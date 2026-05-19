# Document Processing System

Spring Boot API that receives a `DocumentReceived` event, reads the referenced local file, computes its SHA-256 hash, creates a ZIP archive, and returns/logs a `DocumentProcessed` event.

## Requirements

- Java 21
- Maven 3.9+

## Run

```bash
mvn spring-boot:run
```

Default storage configuration:

```yaml
app:
  storage:
    input-root: data/input
    output-root: data/output
```

Override it when needed:

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--app.storage.input-root=/path/to/input --app.storage.output-root=/path/to/output"
```

## API

```http
POST /api/documents/process
Content-Type: application/json
```

You can test the API using `curl`:

```bash
curl -X POST http://localhost:8080/api/documents/process \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": "FATTURA-123",
    "storageRef": "invoice.pdf",
    "metadata": {
      "type": "invoice",
      "receivedAt": "2026-05-01T10:00:00Z"
    }
  }'
```

Successful response:

```json
{
  "documentId": "FATTURA-123",
  "zipPath": "data/output/FATTURA-123.zip",
  "metadata": {
    "type": "invoice",
    "processedAt": "2026-05-19T10:16:49.658473Z",
    "hash": "6ab14b05c2fc9b621fe1cbcd231ef819fea0b1d8f67093f1bb6f816a3b75840e",
    "sizeBytes": 24
  }
}
```

The generated ZIP contains:

- `invoice.pdf`: original document bytes
- `metadata.json`: original metadata plus generated processing metadata
- `hash.txt`: computed SHA-256 hash

## Test

```bash
mvn test
```

Covered scenarios include hashing, ZIP contents, filesystem storage safety, processing flow, and REST error handling.
