package com.instamvp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instamvp.model.Post;
import com.instamvp.model.Profile;
import com.instamvp.model.ProfileSnapshot;
import com.instamvp.repository.PostRepository;
import com.instamvp.repository.ProfileRepository;
import com.instamvp.repository.ProfileSnapshotRepository;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.RequestOptions;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scraper baseado em navegador real (Playwright + Chromium, headless=false).
 *
 * Fluxo:
 * 1. Abre uma janela de Chromium com um perfil persistente em disco
 *    (./browser-data). Cookies e localStorage sobrevivem entre execuções,
 *    então uma vez logado manualmente, não é preciso logar de novo.
 * 2. Se não houver sessão válida (cookie "sessionid" ausente), navega até a
 *    tela de login do Instagram e ESPERA até 5 minutos que o usuário faça
 *    login manualmente na própria janela.
 * 3. Após autenticado, reaproveita os cookies da sessão do navegador para
 *    chamar diretamente o endpoint JSON que o próprio instagram.com usa
 *    internamente (web_profile_info) — muito mais estável que parsear HTML.
 *
 * Stealth aplicado (sem libs externas, já que não existe playwright-stealth
 * para Java): remove o flag navigator.webdriver, normaliza languages/plugins,
 * usa um User-Agent realista e desliga o flag de automação do Chromium.
 * Isso reduz a chance de bloqueio básico, mas não é infalível.
 */
@Service
public class BrowserScraperService {

    private static final Logger log = LoggerFactory.getLogger(BrowserScraperService.class);

    private static final Path USER_DATA_DIR = Paths.get(System.getProperty("user.dir"), "browser-data");
    private static final String IG_APP_ID = "936619743392459"; // app id público usado pelo próprio instagram.com
    private static final long LOGIN_TIMEOUT_MS = 5 * 60_000;
    private static final int MAX_POSTS = 100; // limite máximo de posts por perfil (ou todos, se tiver menos)
    private static final int FEED_PAGE_SIZE = 50; // tamanho de página do endpoint de feed por requisição

    // Remove/normaliza os sinais mais comuns que scripts antibot checam para
    // detectar automação via CDP (Chrome DevTools Protocol / Playwright).
    private static final String STEALTH_INIT_SCRIPT = """
            Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
            Object.defineProperty(navigator, 'languages', { get: () => ['pt-BR', 'pt', 'en-US', 'en'] });
            Object.defineProperty(navigator, 'plugins', {
                get: () => [
                    { name: 'Chrome PDF Plugin' },
                    { name: 'Chrome PDF Viewer' },
                    { name: 'Native Client' }
                ]
            });
            Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 8 });
            Object.defineProperty(navigator, 'deviceMemory', { get: () => 8 });
            Object.defineProperty(navigator, 'platform', { get: () => 'Win32' });
            Object.defineProperty(navigator, 'vendor', { get: () => 'Google Inc.' });

            window.chrome = window.chrome || { runtime: {}, loadTimes: () => {}, csi: () => {} };

            const originalQuery = window.navigator.permissions.query;
            window.navigator.permissions.query = (params) => (
                params.name === 'notifications'
                    ? Promise.resolve({ state: Notification.permission })
                    : originalQuery(params)
            );

            Object.defineProperty(navigator, 'connection', {
                get: () => ({ effectiveType: '4g', rtt: 50, downlink: 10, saveData: false })
            });

            const getParameter = WebGLRenderingContext.prototype.getParameter;
            WebGLRenderingContext.prototype.getParameter = function (parameter) {
                if (parameter === 37445) return 'Intel Inc.';
                if (parameter === 37446) return 'Intel Iris OpenGL Engine';
                return getParameter.call(this, parameter);
            };

            delete navigator.__proto__.webdriver;
            """;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    // Launch flags que reduzem sinais de automação expostos pelo Chromium/CDP.
    private static final List<String> STEALTH_LAUNCH_ARGS = List.of(
            "--disable-blink-features=AutomationControlled",
            "--disable-infobars",
            "--no-first-run",
            "--no-default-browser-check",
            "--disable-dev-shm-usage",
            "--disable-background-networking",
            "--disable-popup-blocking",
            "--disable-features=IsolateOrigins,site-per-process,TranslateUI",
            "--lang=pt-BR");

    private final ProfileRepository profileRepository;
    private final PostRepository postRepository;
    private final ProfileSnapshotRepository profileSnapshotRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final long minDelayBetweenProfilesMs;
    private final long maxDelayBetweenProfilesMs;
    private final java.util.Random random = new java.util.Random();

    private Playwright playwright;
    private BrowserContext context;
    private Page page;
    // Capturado da resposta (header x-ig-set-www-claim) e reenviado nas próximas
    // chamadas (header x-ig-www-claim) — a API privada do Instagram usa isso
    // como parte da validação da sessão; sem ele algumas respostas vêm truncadas.
    private volatile String igWwwClaim = "0";

    public BrowserScraperService(ProfileRepository profileRepository, PostRepository postRepository,
                                  ProfileSnapshotRepository profileSnapshotRepository,
                                  @org.springframework.beans.factory.annotation.Value(
                                          "${scraper.worker.delay-between-profiles-min-ms:8000}") long minDelayBetweenProfilesMs,
                                  @org.springframework.beans.factory.annotation.Value(
                                          "${scraper.worker.delay-between-profiles-max-ms:20000}") long maxDelayBetweenProfilesMs) {
        this.profileRepository = profileRepository;
        this.postRepository = postRepository;
        this.profileSnapshotRepository = profileSnapshotRepository;
        this.minDelayBetweenProfilesMs = minDelayBetweenProfilesMs;
        this.maxDelayBetweenProfilesMs = maxDelayBetweenProfilesMs;
    }

    public synchronized Map<String, String> syncUsernames(List<String> usernames) {
        ensureReady();

        Map<String, String> results = new LinkedHashMap<>();
        boolean first = true;
        for (String username : usernames) {
            if (!first) {
                sleepRandomDelay();
            }
            first = false;
            try {
                results.put(username, syncOne(username.trim()));
            } catch (Exception e) {
                log.error("Falha ao raspar {}", username, e);
                results.put(username, "ERROR: " + e.getMessage());
            }
        }
        return results;
    }

    /**
     * Garante que o navegador está aberto (headless=false) e autenticado.
     * Idempotente: se já estiver logado, retorna na hora; senão abre a tela
     * de login e bloqueia a thread chamadora até o login manual ou timeout.
     * Público de propósito para o {@link ScraperWorker} poder forçar a
     * autenticação assim que a aplicação sobe, antes do primeiro ciclo.
     */
    public synchronized void ensureReady() {
        if (page == null || page.isClosed()) {
            if (page != null) {
                log.warn("Janela do Chromium foi fechada/travou. Relançando o navegador para continuar o worker...");
            }
            relaunchBrowser();
        }

        if (isLoggedIn()) {
            log.info("Sessão existente detectada em {}", USER_DATA_DIR);
            return;
        }

        log.info("Nenhuma sessão válida encontrada. Navegando para a tela de login do Instagram...");
        try {
            page.navigate("https://www.instagram.com/accounts/login/",
                    new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                            .setTimeout(45000));
            log.info("Navegação concluída. URL atual: {}", page.url());
        } catch (Exception e) {
            log.error("FALHA ao navegar até a tela de login (URL atual: {}). "
                    + "Causa comum: Firewall do Windows bloqueando o Chromium controlado pelo Playwright "
                    + "(verifique se há um pop-up do Firewall atrás da janela do navegador) ou falta de "
                    + "conectividade de rede. Detalhe: {}", page.url(), e.getMessage(), e);
            throw e;
        }
        log.info(">>> Faça login manualmente na janela do Chromium aberta. Aguardando até {}s...",
                LOGIN_TIMEOUT_MS / 1000);

        long deadline = System.currentTimeMillis() + LOGIN_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (isLoggedIn()) {
                log.info("Login detectado. Sessão salva em {} para os próximos runs.", USER_DATA_DIR);
                return;
            }
            page.waitForTimeout(2000);
        }
        throw new IllegalStateException("Login manual não detectado dentro do tempo limite de "
                + (LOGIN_TIMEOUT_MS / 1000) + "s. Tente novamente.");
    }

    /** Espera um intervalo aleatório entre perfis, para parecer navegação humana em vez de varredura em rajada. */
    private void sleepRandomDelay() {
        long range = Math.max(1, maxDelayBetweenProfilesMs - minDelayBetweenProfilesMs);
        long delay = minDelayBetweenProfilesMs + (long) (random.nextDouble() * range);
        log.info("Aguardando {}ms antes do próximo perfil (cadência anti-bot)...", delay);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isLoggedIn() {
        // OBS: "sessionid" sozinho NÃO basta — o Instagram atribui esse cookie até para
        // visitantes anônimos (faz parte do sistema antibot deles). "ds_user_id" só é
        // setado depois de um login real, então é o indicador confiável.
        return context.cookies().stream()
                .anyMatch(c -> "ds_user_id".equals(c.name) && c.value != null && !c.value.isBlank());
    }

    /** Fecha qualquer resto de instância anterior (se houver) e abre o Chromium do zero. */
    private void relaunchBrowser() {
        try {
            if (context != null) context.close();
        } catch (Exception ignored) {
        }
        try {
            if (playwright != null) playwright.close();
        } catch (Exception ignored) {
        }
        context = null;
        page = null;
        playwright = null;
        launchBrowser();
    }

    private void launchBrowser() {
        log.info("Lançando Chromium (headless=false), perfil persistente em {}", USER_DATA_DIR);
        playwright = Playwright.create();
        context = playwright.chromium().launchPersistentContext(USER_DATA_DIR,
                new BrowserType.LaunchPersistentContextOptions()
                        .setHeadless(false)
                        .setViewportSize(1280, 800)
                        .setUserAgent(USER_AGENT)
                        .setLocale("pt-BR")
                        .setTimezoneId("America/Sao_Paulo")
                        .setArgs(STEALTH_LAUNCH_ARGS));
        context.addInitScript(STEALTH_INIT_SCRIPT);
        page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
        log.info("Chromium pronto. Página inicial: {}", page.url());
    }

    @Transactional
    String syncOne(String username) throws Exception {
        String profileUrl = "https://www.instagram.com/" + username + "/";

        // Navega a página VISÍVEL até o perfil antes de buscar os dados, só para
        // você poder acompanhar o scraper trabalhando na janela do Chromium.
        // (Os dados em si vêm da chamada de API logo abaixo, mais confiável que
        // fazer parsing do DOM renderizado.)
        try {
            page.navigate(profileUrl, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(30000));
        } catch (Exception e) {
            log.warn("Não foi possível navegar visivelmente até @{} (seguindo via API mesmo assim): {}",
                    username, e.getMessage());
        }

        String apiUrl = "https://www.instagram.com/api/v1/users/web_profile_info/?username=" + username;
        String csrfToken = cookieValue("csrftoken");

        RequestOptions requestOptions = RequestOptions.create()
                .setHeader("x-ig-app-id", IG_APP_ID)
                .setHeader("x-ig-www-claim", igWwwClaim)
                .setHeader("Accept", "application/json")
                .setHeader("Referer", profileUrl)
                .setHeader("X-Requested-With", "XMLHttpRequest");
        if (csrfToken != null) {
            requestOptions.setHeader("X-CSRFToken", csrfToken);
        }

        APIResponse response = context.request().get(apiUrl, requestOptions);

        String newClaim = response.headers().get("x-ig-set-www-claim");
        if (newClaim != null && !newClaim.isBlank()) {
            igWwwClaim = newClaim;
        }

        String rawBody = response.text();
        log.info("[{}] HTTP {} | tamanho do payload: {} bytes | x-ig-www-claim: {}",
                username, response.status(), rawBody.length(), igWwwClaim);

        if (response.status() == 404) {
            return "SKIPPED: usuário não encontrado";
        }
        if (!response.ok()) {
            log.warn("[{}] resposta não-OK, corpo: {}", username, truncate(rawBody, 2000));
            return "ERROR: HTTP " + response.status();
        }

        JsonNode root = objectMapper.readTree(rawBody);
        JsonNode userNode = root.path("data").path("user");
        if (userNode.isMissingNode() || userNode.isNull()) {
            log.warn("[{}] resposta sem 'data.user', corpo: {}", username, truncate(rawBody, 2000));
            return "SKIPPED: resposta sem dados de usuário (possível rate limit)";
        }
        if (userNode.path("is_private").asBoolean(false)) {
            return "SKIPPED: perfil privado";
        }

        Profile profile = profileRepository.findByUsername(username).orElseGet(Profile::new);
        profile.setUsername(username);
        profile.setFullName(textOrNull(userNode, "full_name"));
        profile.setBio(textOrNull(userNode, "biography"));
        profile.setFollowers(countOf(userNode, "edge_followed_by"));
        profile.setFollowing(countOf(userNode, "edge_follow"));
        profile.setPostsCount(countOf(userNode, "edge_owner_to_timeline_media"));

        int edgesAvailable = userNode.path("edge_owner_to_timeline_media").path("edges").size();
        log.info("[{}] full_name='{}' followers={} following={} posts_count={} edges_no_json={} is_private={} csrf_presente={}",
                username, profile.getFullName(), profile.getFollowers(), profile.getFollowing(),
                profile.getPostsCount(), edgesAvailable, userNode.path("is_private").asBoolean(false),
                csrfToken != null);

        Profile saved = profileRepository.save(profile);
        recordSnapshot(saved);

        String userId = textOrNull(userNode, "id");
        int count = syncPostsViaFeed(saved, userId, profileUrl, csrfToken);

        return "OK: synced profile and " + count + " posts";
    }

    /**
     * O endpoint web_profile_info parou de devolver os posts recentes
     * (edge_owner_to_timeline_media.edges) para várias contas — então buscamos
     * os posts num endpoint separado, paginando via max_id até atingir
     * {@link #MAX_POSTS} ou acabarem os posts do perfil (o que vier primeiro).
     */
    private int syncPostsViaFeed(Profile profile, String userId, String profileUrl, String csrfToken) {
        if (userId == null) {
            log.warn("[{}] sem id numérico do usuário, não foi possível buscar posts", profile.getUsername());
            return 0;
        }

        int count = 0;
        String maxId = null;
        boolean moreAvailable = true;

        while (count < MAX_POSTS && moreAvailable) {
            int remaining = MAX_POSTS - count;
            int pageSize = Math.min(FEED_PAGE_SIZE, remaining);

            StringBuilder feedUrl = new StringBuilder("https://www.instagram.com/api/v1/feed/user/")
                    .append(userId).append("/?count=").append(pageSize);
            if (maxId != null) {
                feedUrl.append("&max_id=").append(maxId);
            }

            RequestOptions requestOptions = RequestOptions.create()
                    .setHeader("x-ig-app-id", IG_APP_ID)
                    .setHeader("x-ig-www-claim", igWwwClaim)
                    .setHeader("Accept", "application/json")
                    .setHeader("Referer", profileUrl)
                    .setHeader("X-Requested-With", "XMLHttpRequest");
            if (csrfToken != null) {
                requestOptions.setHeader("X-CSRFToken", csrfToken);
            }

            APIResponse response = context.request().get(feedUrl.toString(), requestOptions);

            String newClaim = response.headers().get("x-ig-set-www-claim");
            if (newClaim != null && !newClaim.isBlank()) {
                igWwwClaim = newClaim;
            }

            if (!response.ok()) {
                log.warn("[{}] feed de posts retornou HTTP {}, parando paginação. Corpo: {}",
                        profile.getUsername(), response.status(), truncate(response.text(), 1000));
                break;
            }

            JsonNode feedRoot;
            try {
                feedRoot = objectMapper.readTree(response.text());
            } catch (Exception e) {
                log.warn("[{}] falha ao parsear JSON do feed de posts: {}", profile.getUsername(), e.getMessage());
                break;
            }

            JsonNode items = feedRoot.path("items");
            if (!items.isArray() || items.isEmpty()) {
                break;
            }

            for (JsonNode item : items) {
                if (count >= MAX_POSTS) break;
                if (saveFeedItem(profile, item)) {
                    count++;
                }
            }

            moreAvailable = feedRoot.path("more_available").asBoolean(false);
            maxId = feedRoot.path("next_max_id").asText(null);
            if (maxId == null || maxId.isBlank()) {
                moreAvailable = false;
            }
        }

        log.info("[{}] {} post(s) sincronizado(s) via feed (limite: {})", profile.getUsername(), count, MAX_POSTS);
        return count;
    }

    private boolean saveFeedItem(Profile profile, JsonNode item) {
        String postId = textOrNull(item, "id");
        if (postId == null) {
            postId = textOrNull(item, "pk");
        }
        if (postId == null) return false;

        Post post = postRepository.findByProfileIdAndInstagramPostId(profile.getId(), postId)
                .orElseGet(Post::new);
        post.setInstagramPostId(postId);
        post.setProfile(profile);
        post.setCaption(textOrNull(item.path("caption"), "text"));

        post.setLikes(item.path("like_count").isMissingNode() ? null : item.path("like_count").asLong());
        post.setComments(item.path("comment_count").isMissingNode() ? null : item.path("comment_count").asLong());

        long takenAt = item.path("taken_at").asLong(0);
        if (takenAt > 0) {
            post.setPostDate(LocalDateTime.ofInstant(Instant.ofEpochSecond(takenAt), ZoneId.systemDefault()));
        }

        String shortcode = textOrNull(item, "code");
        post.setPostUrl(shortcode != null ? "https://www.instagram.com/p/" + shortcode + "/" : null);

        post.setImageUrl(extractImageUrl(item));

        postRepository.save(post);
        return true;
    }

    /**
     * Pega a maior resolução disponível em image_versions2.candidates (para
     * vídeos/reels, isso é o frame de capa). Se o post for um carrossel sem
     * image_versions2 no nível raiz, cai pro primeiro item de carousel_media.
     */
    private String extractImageUrl(JsonNode item) {
        JsonNode candidates = item.path("image_versions2").path("candidates");
        if (candidates.isArray() && candidates.size() > 0) {
            return candidates.get(0).path("url").asText(null);
        }

        JsonNode carousel = item.path("carousel_media");
        if (carousel.isArray() && carousel.size() > 0) {
            JsonNode firstCandidates = carousel.get(0).path("image_versions2").path("candidates");
            if (firstCandidates.isArray() && firstCandidates.size() > 0) {
                return firstCandidates.get(0).path("url").asText(null);
            }
        }

        return null;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "null";
        return text.length() > maxLen ? text.substring(0, maxLen) + "...(truncado)" : text;
    }

    private String cookieValue(String name) {
        return context.cookies().stream()
                .filter(c -> name.equals(c.name))
                .map(c -> c.value)
                .findFirst()
                .orElse(null);
    }

    private void recordSnapshot(Profile profile) {
        ProfileSnapshot snapshot = new ProfileSnapshot();
        snapshot.setProfile(profile);
        snapshot.setFollowers(profile.getFollowers());
        snapshot.setFollowing(profile.getFollowing());
        snapshot.setPostsCount(profile.getPostsCount());
        profileSnapshotRepository.save(snapshot);
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    private Long countOf(JsonNode node, String edgeField) {
        JsonNode v = node.path(edgeField).path("count");
        return v.isMissingNode() ? null : v.asLong();
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (context != null) context.close();
        } catch (Exception ignored) {
            // janela já pode ter sido fechada manualmente pelo usuário
        }
        try {
            if (playwright != null) playwright.close();
        } catch (Exception ignored) {
        }
    }
}
