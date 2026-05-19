package com.acube.documentprocessing.processing;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class HashServiceTest {

    private final HashService hashService = new HashService();

    @Test
    void computesSha256HashAsLowercaseHex() {
        String hash = hashService.computeSha256Hex("hello".getBytes(StandardCharsets.UTF_8));

        assertThat(hash).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }
}
