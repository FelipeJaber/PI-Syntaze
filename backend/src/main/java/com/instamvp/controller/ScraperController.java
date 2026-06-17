package com.instamvp.controller;

import com.instamvp.dto.SyncRequest;
import com.instamvp.service.BrowserScraperService;
import com.instamvp.service.ScraperService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scraper")
public class ScraperController {

    private final ScraperService scraperService;
    private final BrowserScraperService browserScraperService;

    public ScraperController(ScraperService scraperService, BrowserScraperService browserScraperService) {
        this.scraperService = scraperService;
        this.browserScraperService = browserScraperService;
    }

    /** Scraper "leve" via Jsoup (sem JS). Rápido, mas o Instagram costuma bloquear sem login. */
    @PostMapping("/sync")
    public Map<String, String> sync(@RequestBody SyncRequest request) {
        List<String> usernames = request.getUsernames();
        if (usernames == null || usernames.isEmpty()) {
            return Map.of();
        }
        return scraperService.syncUsernames(usernames);
    }

    /**
     * Scraper com navegador real (Playwright, headless=false). Na primeira
     * chamada, abre uma janela do Chromium e aguarda login manual; depois
     * disso a sessão fica salva em ./browser-data e é reaproveitada.
     */
    @PostMapping("/sync-browser")
    public Map<String, String> syncBrowser(@RequestBody SyncRequest request) {
        List<String> usernames = request.getUsernames();
        if (usernames == null || usernames.isEmpty()) {
            return Map.of();
        }
        return browserScraperService.syncUsernames(usernames);
    }
}
