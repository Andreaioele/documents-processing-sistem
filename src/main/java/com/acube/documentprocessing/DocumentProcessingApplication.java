package com.acube.documentprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/**
 * Classe principale di avvio dell'applicazione Spring Boot.
 * Contiene la logica di setup base e definisce i Bean globali (come il Clock).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DocumentProcessingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentProcessingApplication.class, args);
    }

    /**
     * Configura e restituisce un Bean di tipo Clock che opera in formato UTC
     * (Coordinated Universal Time).
     * Viene usato globalmente in tutta l'applicazione per garantire che tutte le
     * registrazioni temporali
     * (es. receivedAt, processedAt) siano coerenti e standardizzate.
     * Essendo definito come Bean, può essere iniettato automaticamente in altri
     * componenti tramite @Autowired.
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
