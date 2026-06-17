package com.instamvp.service;

import com.instamvp.model.ScrapeTarget;
import com.instamvp.repository.ScrapeTargetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Popula a watchlist com perfis padrão na primeira vez que a aplicação sobe
 * (não duplica se já existirem), para o worker já começar buscando algo sem
 * precisar de nenhuma chamada manual ao endpoint de watchlist.
 */
@Component
public class DefaultWatchlistSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultWatchlistSeeder.class);

    private final ScrapeTargetRepository scrapeTargetRepository;
    private final List<String> defaultUsernames;

    public DefaultWatchlistSeeder(ScrapeTargetRepository scrapeTargetRepository,
                                   @Value("${scraper.default-usernames:nike,adidas,puma}") List<String> defaultUsernames) {
        this.scrapeTargetRepository = scrapeTargetRepository;
        this.defaultUsernames = defaultUsernames;
    }

    @Override
    public void run(String... args) {
        for (String username : defaultUsernames) {
            String trimmed = username.trim();
            if (trimmed.isEmpty()) continue;

            scrapeTargetRepository.findByUsername(trimmed).orElseGet(() -> {
                ScrapeTarget target = new ScrapeTarget();
                target.setUsername(trimmed);
                target.setActive(true);
                ScrapeTarget saved = scrapeTargetRepository.save(target);
                log.info("Perfil padrão adicionado à watchlist: {}", trimmed);
                return saved;
            });
        }
    }
}
