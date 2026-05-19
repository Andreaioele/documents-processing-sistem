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
    input-root: .
    output-root: output
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

Example:

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

Successful response:

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

The generated ZIP contains:

- `invoice.pdf`: original document bytes
- `metadata.json`: original metadata plus generated processing metadata
- `hash.txt`: computed SHA-256 hash

## Test

```bash
mvn test
```

Covered scenarios include hashing, ZIP contents, filesystem storage safety, processing flow, and REST error handling.
