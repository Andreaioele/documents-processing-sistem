package com.acube.documentprocessing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * Classe di configurazione che mappa le proprietà definite nel file application.yml (o application.properties).
 * Grazie all'annotazione @ConfigurationProperties, Spring prende in automatico i valori
 * definiti sotto il prefisso "app.storage" e li riversa nei campi di questa classe.
 * 
 * Esempio di file application.yml:
 * app:
 *   storage:
 *     input-root: "/percorso/file/ricevuti"
 *     output-root: "/percorso/file/elaborati"
 */
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /**
     * Cartella base da cui verranno letti i documenti in ingresso (fisici).
     * Valore di default: "." (ovvero la cartella corrente di esecuzione del progetto).
     */
    private Path inputRoot = Path.of(".");
    
    /**
     * Cartella base dove verranno salvati gli archivi ZIP (l'output dell'elaborazione).
     * Valore di default: "output" (creerà una cartella "output" nella directory corrente).
     */
    private Path outputRoot = Path.of("output");

    public Path getInputRoot() {
        return inputRoot;
    }

    public void setInputRoot(Path inputRoot) {
        this.inputRoot = inputRoot;
    }

    public Path getOutputRoot() {
        return outputRoot;
    }

    public void setOutputRoot(Path outputRoot) {
        this.outputRoot = outputRoot;
    }
}
