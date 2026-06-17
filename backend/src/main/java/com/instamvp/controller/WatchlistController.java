package com.instamvp.controller;

import com.instamvp.dto.AddWatchTargetRequest;
import com.instamvp.dto.ScrapeTargetDTO;
import com.instamvp.model.ScrapeTarget;
import com.instamvp.repository.ScrapeTargetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/** Lista de usernames que o worker de scraping em background verifica continuamente. */
@RestController
@RequestMapping("/api/scraper/watchlist")
public class WatchlistController {

    private final ScrapeTargetRepository scrapeTargetRepository;

    public WatchlistController(ScrapeTargetRepository scrapeTargetRepository) {
        this.scrapeTargetRepository = scrapeTargetRepository;
    }

    @GetMapping
    public List<ScrapeTargetDTO> list() {
        return scrapeTargetRepository.findAll().stream()
                .map(ScrapeTargetDTO::from)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<ScrapeTargetDTO> add(@RequestBody AddWatchTargetRequest request) {
        String username = request.getUsername() == null ? null : request.getUsername().trim();
        if (username == null || username.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ScrapeTarget target = scrapeTargetRepository.findByUsername(username)
                .orElseGet(() -> {
                    ScrapeTarget t = new ScrapeTarget();
                    t.setUsername(username);
                    return scrapeTargetRepository.save(t);
                });

        return ResponseEntity.status(HttpStatus.CREATED).body(ScrapeTargetDTO.from(target));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> remove(@PathVariable String username) {
        scrapeTargetRepository.findByUsername(username)
                .ifPresent(scrapeTargetRepository::delete);
        return ResponseEntity.noContent().build();
    }

    /** Liga/desliga a busca contínua para um perfil sem removê-lo da lista. */
    @PatchMapping("/{username}/toggle")
    public ResponseEntity<ScrapeTargetDTO> toggle(@PathVariable String username) {
        return scrapeTargetRepository.findByUsername(username)
                .map(target -> {
                    target.setActive(!target.isActive());
                    ScrapeTarget saved = scrapeTargetRepository.save(target);
                    return ResponseEntity.ok(ScrapeTargetDTO.from(saved));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
