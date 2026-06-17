package com.instamvp.service;

import com.instamvp.model.ScrapeTarget;
import com.instamvp.repository.ScrapeTargetRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Worker eterno, separado da thread de requisições HTTP: roda em loop infinito
 * numa thread própria, verificando os usernames da watchlist
 * ({@code POST /api/scraper/watchlist}) de tempos em tempos, para sempre,
 * enquanto a aplicação estiver de pé.
 *
 * Não usa @Scheduled de propósito: é uma única thread dedicada com seu próprio
 * loop, então nunca há duas execuções concorrentes disputando o mesmo
 * navegador (o BrowserScraperService mantém uma única janela do Chromium).
 */
@Component
public class ScraperWorker {

    private static final Logger log = LoggerFactory.getLogger(ScraperWorker.class);

    private final ScrapeTargetRepository scrapeTargetRepository;
    private final BrowserScraperService browserScraperService;
    private final long intervalMs;
    private final boolean enabled;

    private Thread workerThread;
    private volatile boolean running = true;

    public ScraperWorker(ScrapeTargetRepository scrapeTargetRepository,
                          BrowserScraperService browserScraperService,
                          @Value("${scraper.worker.interval-ms:300000}") long intervalMs,
                          @Value("${scraper.worker.enabled:true}") boolean enabled) {
        this.scrapeTargetRepository = scrapeTargetRepository;
        this.browserScraperService = browserScraperService;
        this.intervalMs = intervalMs;
        this.enabled = enabled;
    }

    @PostConstruct
    public void start() {
        if (!enabled) {
            log.info("ScraperWorker desabilitado (scraper.worker.enabled=false)");
            return;
        }
        workerThread = new Thread(this::loop, "scraper-worker");
        workerThread.setDaemon(true);
        workerThread.start();
        log.info("ScraperWorker iniciado. Intervalo entre ciclos: {}ms", intervalMs);
    }

    private void loop() {
        try {
            log.info("Abrindo navegador (headless=false) para autenticação manual antes de iniciar os ciclos...");
            browserScraperService.ensureReady();
            log.info("Autenticado. Iniciando ciclos de verificação da watchlist.");
        } catch (Exception e) {
            log.error("Falha na autenticação inicial do navegador. O worker tentará de novo no próximo ciclo.", e);
        }

        while (running) {
            try {
                runCycle();
            } catch (Exception e) {
                log.error("Ciclo do ScraperWorker falhou, seguindo para o próximo ciclo", e);
            }
            if (running) {
                log.info("Ciclo concluído. Próxima verificação da watchlist em {}s.", intervalMs / 1000);
            }
            sleepQuietly(intervalMs);
        }
        log.info("ScraperWorker finalizado.");
    }

    private void runCycle() {
        List<String> usernames = scrapeTargetRepository.findByActiveTrue().stream()
                .map(ScrapeTarget::getUsername)
                .collect(Collectors.toList());

        if (usernames.isEmpty()) {
            log.debug("Nenhum perfil ativo na watchlist, nada para verificar neste ciclo.");
            return;
        }

        log.info("Iniciando ciclo de verificação para {} perfil(is): {}", usernames.size(), usernames);
        Map<String, String> results = browserScraperService.syncUsernames(usernames);
        results.forEach((username, status) -> {
            log.info("[{}] {}", username, status);
            updateTargetStatus(username, status);
        });
    }

    private void updateTargetStatus(String username, String resultMessage) {
        scrapeTargetRepository.findByUsername(username).ifPresent(target -> {
            target.setLastStatus(ScrapeStatusParser.parse(resultMessage));
            target.setLastMessage(resultMessage);
            target.setLastCheckedAt(LocalDateTime.now());
            scrapeTargetRepository.save(target);
        });
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }
}
