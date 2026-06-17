package com.instamvp.service;

import com.instamvp.model.ScrapeStatus;

/**
 * Traduz a string de resultado de uma tentativa de scraping (ex: "OK: synced
 * profile and 100 posts", "SKIPPED: perfil privado") para um
 * {@link ScrapeStatus}. Extraído como classe própria (em vez de método
 * privado do {@link ScraperWorker}) para ser testável isoladamente, sem
 * precisar de contexto Spring, banco ou navegador.
 */
public final class ScrapeStatusParser {

    private ScrapeStatusParser() {
    }

    public static ScrapeStatus parse(String resultMessage) {
        if (resultMessage == null) {
            return ScrapeStatus.ERROR;
        }

        String lower = resultMessage.toLowerCase();
        if (lower.startsWith("ok")) return ScrapeStatus.OK;
        if (lower.contains("privad")) return ScrapeStatus.PRIVATE;
        if (lower.contains("não encontrado") || lower.contains("nao encontrado")) return ScrapeStatus.NOT_FOUND;
        return ScrapeStatus.ERROR;
    }
}
