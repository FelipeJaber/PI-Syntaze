# InstaMVP — Guia de Execução Local

## Estrutura

```
instamvp/
  backend/        Spring Boot (Java 21, Maven, H2, Jsoup)
  flutter_app/     Flutter (Provider, http)
  GUIA_EXECUCAO.md
```

## -1. Autenticação (registro + login, HTTP Basic)

Toda a API (`/api/**`, exceto `/api/auth/register`) e o dashboard web exigem
autenticação básica (HTTP Basic Auth), validada contra usuários reais
persistidos no banco (tabela `app_users`, senha com hash BCrypt) — não é mais
um único usuário fixo no código.

**Usuário padrão** (criado automaticamente no primeiro boot, configurável em
`application.yml` → `security.demo.*`):
```
usuário: admin
senha:   admin123
```

**Cadastrar um novo usuário:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "felipe", "password": "minhasenha"}'
```

**Validar login** (HTTP Basic + GET /api/auth/me — retorna 200 com o
username se as credenciais forem válidas, 401 se não):
```bash
curl -u felipe:minhasenha http://localhost:8080/api/auth/me
```

No navegador, ao acessar `http://localhost:8080/`, um popup nativo de login
aparece automaticamente — depois disso, todas as chamadas da própria página
(incluindo as do dashboard) reaproveitam a mesma credencial. No `curl`, use
`-u usuario:senha`.

**No Flutter**, o app agora tem telas de Login e Registro de verdade
(`lib/screens/login_screen.dart` e `register_screen.dart`) — ao abrir o app,
`AuthGate` (em `main.dart`) mostra a tela de login até o usuário autenticar;
depois disso, libera a navegação principal (`HomeShell`), que tem um botão
de logout no topo. As credenciais ficam em memória no `ApiService`
(`setCredentials`/`clearCredentials`) e são reenviadas em toda chamada — não
há token de sessão persistido (fechar o app desloga).

⚠️ Continua sendo uma autenticação propositalmente simples (sem roles,
recuperação de senha, ou token persistido) — adequada ao escopo do projeto
acadêmico, não para produção real. O H2 Console (`/h2-console`) fica
liberado sem autenticação, por ser ferramenta só de desenvolvimento local.

## 0. Dashboard web (servido pelo próprio backend)

O Spring Boot serve um dashboard estático em `backend/src/main/resources/static/index.html`
direto na raiz: **http://localhost:8080/**. É HTML/JS puro (sem build, sem
dependências), consumindo a própria API REST. Dá pra ver leaderboard, watchlist
(com toggle e adicionar perfil) e posts de cada perfil, com auto-refresh a cada
30s. Só pra acompanhamento rápido — a versão "de verdade" é o app Flutter.

## ⚠️ Limitação importante do scraper

O Instagram bloqueia a maior parte do scraping anônimo (sem login) — a maioria
das páginas públicas hoje exige JavaScript/login para liberar os dados completos
(seguidores, posts, curtidas). O `ScraperService` tenta extrair o JSON embutido
no HTML (`script#__NEXT_DATA__` ou `window._sharedData`), que é a única forma
viável com Jsoup puro (sem headless browser), mas o Instagram pode:

- Servir uma página de login sem dados (→ endpoint retorna `SKIPPED`).
- Mudar o layout/estrutura JSON a qualquer momento.
- Rate-limitar/bloquear o IP após poucas requisições.

Para a demo do MVP, isso é aceitável: o endpoint `POST /api/scraper/sync` retorna,
por username, o status (`OK`, `SKIPPED`, `ERROR: ...`). Se o scraping real falhar
no ambiente de demo, é possível inserir dados manualmente no H2 Console
(`/h2-console`) para validar o restante do fluxo (API + app).

## 1.1 Scraper alternativo com login manual (Playwright)

Existe um segundo scraper, mais robusto, em `BrowserScraperService.java`, que
abre uma janela real do Chromium (`headless=false`) para você logar manualmente
uma vez. A sessão (cookies/localStorage) fica salva em `backend/browser-data/`
e é reaproveitada nas próximas execuções — não precisa logar de novo.

**Setup único** (baixa o binário do Chromium usado pelo Playwright):

```bash
cd backend
mvn compile
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

Se o plugin `exec` não estiver configurado, baixe direto via:

```bash
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/classes;$(cat cp.txt)" com.microsoft.playwright.CLI install chromium
```

**Uso:**

```bash
curl -u admin:admin123 -X POST http://localhost:8080/api/scraper/sync-browser \
  -H "Content-Type: application/json" \
  -d '{"usernames": ["nike", "adidas"]}'
```

Na primeira chamada, uma janela do Chromium abre na tela de login do Instagram.
Faça login manualmente nela — o backend fica monitorando até 5 minutos esperando
o cookie de sessão aparecer. Depois disso, a janela continua aberta e as próximas
chamadas ao endpoint reaproveitam a mesma sessão automaticamente (mesmo após
reiniciar a aplicação, graças ao perfil persistente em `browser-data/`).

Stealth aplicado (em `BrowserScraperService`): remove `navigator.webdriver`,
normaliza `languages`/`plugins`/`hardwareConcurrency`/`deviceMemory`/`platform`,
spoofa `WebGL` vendor/renderer e `navigator.connection`, usa locale/timezone
`pt-BR`/`America/Sao_Paulo`, User-Agent realista e várias flags de lançamento
do Chromium que reduzem sinais de automação (`--disable-blink-features=...`,
`--disable-infobars`, etc.). Isso reduz bastante a detecção básica, mas **não
é garantia** contra todos os mecanismos anti-bot do Instagram — para reduzir
ainda mais o risco de ser flagado, o scraper também espera um intervalo
aleatório entre perfis (cadência humana) em vez de varrer tudo em rajada,
configurável via `scraper.worker.delay-between-profiles-min-ms` e
`-max-ms` no `application.yml` (padrão: 8s a 20s entre cada perfil).

⚠️ Esse fluxo precisa de ambiente com interface gráfica (não funciona em
servidor headless puro) e usa a sua conta pessoal logada — respeite os Termos
de Uso do Instagram ao usar.

## 1.2 Worker eterno de verificação contínua

O `ScraperWorker` roda numa thread própria (não é uma chamada HTTP), iniciada
automaticamente quando o backend sobe, e fica em loop infinito para sempre:
a cada ciclo (padrão 5 min, configurável em `application.yml` via
`scraper.worker.interval-ms`), ele busca os usernames **ativos** da
"watchlist" e roda o `BrowserScraperService` neles, atualizando o banco.

**Perfis padrão**: na primeira subida da aplicação, o `DefaultWatchlistSeeder`
popula a watchlist automaticamente com a lista em `application.yml`
(`scraper.default-usernames`) — hoje são ~44 marcas do ramo esportivo (Nike,
Adidas, Puma, Under Armour, Reebok, New Balance, Asics, Lululemon, Gymshark,
The North Face, Patagonia, Decathlon, entre outras), assim o worker já começa
buscando bastante coisa sem precisar de nenhuma chamada manual. Perfis que não
existirem ou estiverem com username incorreto simplesmente ficam marcados
`NOT_FOUND` na watchlist (ver seção 1.1.1), sem quebrar o worker. Edite essa
propriedade para mudar os perfis iniciais.

⚠️ Com ~44 perfis e a cadência anti-bot de 8–20s entre cada um (seção 1.1),
um ciclo completo do worker leva uns 8–12 minutos. Isso é esperado — reduza
a lista ou o `delay-between-profiles-*-ms` se quiser ciclos mais rápidos
para teste/demo.

**Adicionar um perfil à watchlist** (só o username):

```bash
curl -u admin:admin123 -X POST http://localhost:8080/api/scraper/watchlist \
  -H "Content-Type: application/json" \
  -d '{"username": "nike"}'
```

**Listar perfis monitorados** (cada um com `active: true/false` e o status da
última verificação):

```bash
curl -u admin:admin123 http://localhost:8080/api/scraper/watchlist
```

Cada item vem com `lastStatus` (`PENDING`, `OK`, `PRIVATE`, `NOT_FOUND` ou
`ERROR`), `lastMessage` (detalhe textual, ex: "SKIPPED: perfil privado") e
`lastCheckedAt`. Isso existe pra deixar claro quando um perfil não está
sincronizando por uma razão legítima (privado, não existe) em vez de parecer
bug do scraper — útil pra não ficar caçando fantasma quando, por exemplo,
você adiciona seu próprio perfil privado à watchlist e ele nunca traz dados.
O dashboard web já mostra esse status como badge na seção da watchlist.

**Ligar/desligar a busca de um perfil** (toggle, sem remover da lista):

```bash
curl -u admin:admin123 -X PATCH http://localhost:8080/api/scraper/watchlist/nike/toggle
```

**Remover da watchlist:**

```bash
curl -u admin:admin123 -X DELETE http://localhost:8080/api/scraper/watchlist/nike
```

Assim que houver pelo menos um username ativo e o próximo ciclo do worker
rodar, a janela do Chromium abre pedindo login manual (uma única vez — a
sessão fica salva). Depois disso o worker continua verificando os perfis
ativos da watchlist indefinidamente, sem precisar de intervenção manual.
Perfis desligados (`active: false`) ficam na lista mas são pulados pelo
worker até serem reativados.

Para desligar o worker (ex: ambiente sem interface gráfica), defina
`scraper.worker.enabled=false` no `application.yml` ou via variável de
ambiente `SCRAPER_WORKER_ENABLED=false`.

## 1.2.1 Posts: até 100 por perfil

O `BrowserScraperService` busca posts via um endpoint separado
(`/api/v1/feed/user/{id}/`), paginando com `max_id` até **100 posts** por
perfil (ou todos, se o perfil tiver menos). Isso foi necessário porque o
endpoint `web_profile_info` parou de devolver a lista de posts recentes para
diversas contas — só os dados agregados do perfil (followers, posts_count
etc.) continuam vindo por ali. O `ScraperService` (Jsoup, sem login) também
respeita o limite de 100, mas sem paginação real — pega só o que vier nos
`edges` embutidos no HTML, tipicamente bem menos.

## 1.3 Histórico e crescimento (tendências de concorrência)

Cada vez que um perfil é sincronizado (pelo worker ou por `/sync`/`/sync-browser`),
um registro é gravado em `profile_snapshots` (followers/following/posts_count +
timestamp), além de atualizar o `Profile` em si. Isso é o que permite calcular
crescimento ao longo do tempo — sem isso só teríamos a "foto" mais recente.

**Crescimento de um perfil em uma janela de dias:**

```bash
curl -u admin:admin123 "http://localhost:8080/api/profiles/1/growth?days=7"
```

Retorna `followersDelta`, `followersGrowthPercent`, `postsCountDelta` etc.
Se o worker ainda não rodou vezes suficientes na janela pedida, a resposta
vem com `insufficientData: true` em vez de números fabricados — é esperado
logo após o setup, até o worker acumular pelo menos 2 ciclos.

**Taxa de engajamento por post:** `GET /api/profiles/{id}/posts` agora retorna
também `engagementRate` em cada post (`(likes + comments) / followers` do
perfil), útil para comparar quais posts de um concorrente performaram acima
da média.

## 1.4 Leaderboard (comparação entre concorrentes)

```bash
curl -u admin:admin123 "http://localhost:8080/api/leaderboard?days=7"
```

Retorna todos os perfis monitorados, um `GrowthDTO` por perfil, ordenados por
`followersGrowthPercent` (maior crescimento primeiro) — cada item já vem com
`rank` (posição) e `avgEngagementRate` (engajamento médio dos posts coletados).
Perfis com `insufficientData: true` (histórico insuficiente ainda) aparecem
no fim da lista em vez de sumirem, pra ficar claro quem ainda precisa de mais
ciclos do worker antes do ranking ficar confiável.

## 1.5 Inteligência de conteúdo (melhores posts e hashtags em alta)

**Melhores posts por engajamento** (`(likes+comments)/followers`), publicados
num período (`days=1` = hoje):

```bash
curl -u admin:admin123 "http://localhost:8080/api/insights/top-posts?days=1&limit=10"
```

**Hashtags em alta** entre todos os concorrentes monitorados (extraídas via
regex das legendas, `#exemplo` contado no máximo 1x por post):

```bash
curl -u admin:admin123 "http://localhost:8080/api/insights/hashtags?days=7&limit=20"
```

**Imagens dos posts:** `Post.imageUrl` (e portanto `PostDTO.imageUrl` e
`TopPostDTO.imageUrl`) guarda a URL da imagem de capa de cada post, capturada
de `image_versions2.candidates[0].url` no feed do Instagram (ou do primeiro
item do carrossel, se for um post multi-imagem). São URLs do CDN do próprio
Instagram — funcionam direto em `<img src="...">`, sem precisar baixar/servir
o arquivo. O dashboard web já exibe a miniatura nos cards de "Melhores posts"
e na visão de posts de um perfil; se a URL expirar ou falhar, mostra um
placeholder "sem imagem" em vez de quebrar o layout.

Cada hashtag retorna `postCount` (quantos posts usaram), `profileCount`
(quantos concorrentes distintos usaram) e `usernames` (quais). Útil pra ver
se um termo está sendo usado por vários concorrentes ao mesmo tempo
(tendência de mercado) ou só por um (campanha isolada). Ambos endpoints já
aparecem no dashboard web (seção "🔥 Melhores posts" e "# Hashtags em alta").

## 1. Backend (Spring Boot)

Pré-requisitos: JDK 21, Maven (ou usar o wrapper se adicionado).

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`.

Banco H2 em arquivo (`./backend/data/instamvp.mv.db`), criado automaticamente
(`ddl-auto: update`). Console disponível em `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:file:./data/instamvp`, usuário `sa`, senha vazia).

### Testar endpoints (curl)

```bash
# Sincronizar perfis (dispara o scraper)
curl -u admin:admin123 -X POST http://localhost:8080/api/scraper/sync \
  -H "Content-Type: application/json" \
  -d '{"usernames": ["nike", "adidas", "puma"]}'

# Listar perfis
curl -u admin:admin123 http://localhost:8080/api/profiles

# Detalhe de um perfil
curl -u admin:admin123 http://localhost:8080/api/profiles/1

# Posts de um perfil
curl -u admin:admin123 http://localhost:8080/api/profiles/1/posts
```

## 2. Flutter App

Pré-requisitos: Flutter SDK instalado (`flutter doctor`).

```bash
cd flutter_app
flutter pub get
flutter run
```

### Configuração da URL da API

Em `lib/services/api_service.dart`, ajuste `baseUrl` conforme o alvo:

- **Emulador Android**: `http://10.0.2.2:8080/api` (já configurado por padrão)
- **iOS Simulator / Web / Desktop**: `http://localhost:8080/api`
- **Dispositivo físico**: `http://<IP-da-sua-máquina-na-rede>:8080/api`

### Fluxo no app

1. Tela "Perfis" abre vazia (ou já populada se o backend tiver dados).
2. Toque no botão flutuante (🔄) para digitar usernames separados por vírgula
   e disparar `POST /api/scraper/sync`.
3. A lista recarrega automaticamente após a sincronização.
4. Toque em um perfil para ver detalhes + posts recentes.

## 3. Build rápido de validação (sem Instagram real)

Caso o scraping contra o Instagram esteja bloqueado no ambiente de teste,
insira dados de exemplo direto no H2 Console para validar API + Flutter:

```sql
INSERT INTO PROFILES (USERNAME, FULL_NAME, BIO, FOLLOWERS, FOLLOWING, POSTS_COUNT, CREATED_AT, UPDATED_AT)
VALUES ('nike', 'Nike', 'Just Do It', 300000000, 150, 1200, NOW(), NOW());

INSERT INTO POSTS (INSTAGRAM_POST_ID, PROFILE_ID, CAPTION, LIKES, COMMENTS, POST_DATE, POST_URL)
VALUES ('123456', 1, 'New drop 🔥', 50000, 800, NOW(), 'https://www.instagram.com/p/abc123/');
```

## 4. Comandos Maven úteis

```bash
mvn clean install        # build completo
mvn spring-boot:run      # roda a aplicação
mvn test                 # roda testes (se adicionados)
mvn package              # gera o JAR em target/
java -jar target/instamvp-backend-0.0.1-SNAPSHOT.jar   # roda o JAR gerado
```

## 5. Checklist de entrega (MVP < 5h)

- [x] Modelo JPA (`Profile`, `Post`) com relacionamento 1:N
- [x] DTOs simples (`ProfileDTO`, `PostDTO`, `SyncRequest`)
- [x] Repositories (Spring Data JPA)
- [x] Services (`ProfileService`, `ScraperService`)
- [x] Controllers REST (`/api/profiles`, `/api/scraper/sync`)
- [x] Scraper Jsoup best-effort (perfis públicos, ignora privados)
- [x] Banco H2 em arquivo (sem infraestrutura extra)
- [x] App Flutter (lista + detalhe, Provider, Material 3)
- [x] Sem autenticação, sem Docker, sem microsserviços, sem filas
