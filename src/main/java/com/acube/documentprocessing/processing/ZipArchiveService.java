package com.acube.documentprocessing.processing;

import com.acube.documentprocessing.domain.ProcessingMetadata;
import com.acube.documentprocessing.exception.ProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipArchiveService {

    static final String DOCUMENT_ENTRY_NAME = "invoice.pdf";
    static final String METADATA_ENTRY_NAME = "metadata.json";
    static final String HASH_ENTRY_NAME = "hash.txt";

    private final ObjectMapper objectMapper;

    public ZipArchiveService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] createArchive(
            byte[] originalContent,
            Map<String, Object> originalMetadata,
            ProcessingMetadata processingMetadata) {
        try (ByteArrayOutputStream archiveBytes = new ByteArrayOutputStream();
                ZipOutputStream zipOutput = new ZipOutputStream(archiveBytes)) {
            writeEntry(zipOutput, DOCUMENT_ENTRY_NAME, originalContent);
            writeEntry(zipOutput, METADATA_ENTRY_NAME, serializeMetadata(originalMetadata, processingMetadata));
            writeEntry(zipOutput, HASH_ENTRY_NAME, processingMetadata.hash().getBytes(StandardCharsets.UTF_8));
            zipOutput.finish();
            return archiveBytes.toByteArray();
        } catch (IOException e) {
            throw new ProcessingException("Unable to create output archive", e);
        }
    }

    private byte[] serializeMetadata(
            Map<String, Object> originalMetadata,
            ProcessingMetadata processingMetadata) throws IOException {
        Map<String, Object> archiveMetadata = new LinkedHashMap<>();
        archiveMetadata.put("originalMetadata", originalMetadata);
        archiveMetadata.put("processingMetadata", processingMetadata);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(archiveMetadata);
    }

    private void writeEntry(ZipOutputStream zipOutput, String name, byte[] content) throws IOException {
        zipOutput.putNextEntry(new ZipEntry(name));
        zipOutput.write(content);
        zipOutput.closeEntry();
    }
}
