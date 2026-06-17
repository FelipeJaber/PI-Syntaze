package com.instamvp.service;

import com.instamvp.model.ScrapeStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testa a tradução das strings de resultado do scraper (ex: "OK: synced
 * profile and 100 posts") para o enum {@link ScrapeStatus}. Lógica pura,
 * sem dependência de Spring, banco ou navegador.
 */
class ScrapeStatusParserTest {

    @Test
    void parsesSuccessfulSyncAsOk() {
        ScrapeStatus status = ScrapeStatusParser.parse("OK: synced profile and 100 posts");
        assertEquals(ScrapeStatus.OK, status);
    }

    @Test
    void parsesPrivateProfileMessageAsPrivate() {
        ScrapeStatus status = ScrapeStatusParser.parse("SKIPPED: perfil privado");
        assertEquals(ScrapeStatus.PRIVATE, status);
    }

    @Test
    void parsesNotFoundMessageAsNotFound() {
        ScrapeStatus status = ScrapeStatusParser.parse("SKIPPED: usuário não encontrado");
        assertEquals(ScrapeStatus.NOT_FOUND, status);
    }

    @Test
    void parsesUnknownOrErrorMessageAsError() {
        assertEquals(ScrapeStatus.ERROR, ScrapeStatusParser.parse("ERROR: HTTP 500"));
        assertEquals(ScrapeStatus.ERROR, ScrapeStatusParser.parse(null));
    }
}
