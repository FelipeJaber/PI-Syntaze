package com.instamvp.model;

/** Resultado da última tentativa de sincronização de um {@link ScrapeTarget}. */
public enum ScrapeStatus {
    /** Ainda não foi verificado nenhuma vez. */
    PENDING,
    /** Sincronizado com sucesso. */
    OK,
    /** Perfil existe mas é privado — o scraper não tem como ler os dados. */
    PRIVATE,
    /** Username não existe (ou foi deletado/banido). */
    NOT_FOUND,
    /** Falha de scraping (rede, rate limit, parsing) — geralmente temporária. */
    ERROR
}
