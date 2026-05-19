package com.acube.documentprocessing.processing;

import com.acube.documentprocessing.exception.ProcessingException;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Servizio incaricato del calcolo dell'hash SHA-256 dei documenti.
 * Viene usato come utility all'interno del processo di elaborazione
 * per generare l'impronta digitale del file.
 */
@Service
public class HashService {

    /**
     * Calcola l'hash SHA-256 dato un array di byte contenente il file.
     *
     * @param content L'intero contenuto del file rappresentato come array di byte.
     * @return La rappresentazione esadecimale della stringa hash SHA-256.
     * @throws ProcessingException Se l'algoritmo SHA-256 non è disponibile sulla
     *                             JVM.
     */
    public String computeSha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new ProcessingException("SHA-256 algorithm is not available", e);
        }
    }
}
