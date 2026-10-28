# HostFit – Server Hosting Plan Recommender

HostFit helps small businesses, freelancers, startups and students pick a hosting plan. You enter what you are
hosting, your monthly traffic, storage need, budget and how much server management you want; HostFit ranks real
plans from many providers using transparent, rule-based matching and shows its reasoning.

Built for the *Application Development & Deployment Architecture* course (Review 1 → implementation).
Stack: Java 17, Spring Boot 3.3, Thymeleaf, Spring Data JPA, MySQL (H2 for zero-setup dev), Maven, JUnit 5,
Jenkins, SonarQube, Docker.

## Modules (as in the Review 1 deck)

| Deck module | Where it lives |
|---|---|
| 1. User & Requirement | `RequirementForm` (validation), `HomeController`, `templates/index.html` |
| 2. Hosting Plan & Provider | `HostingPlan`, `Provider` entities, `PlanService`, `ProviderService`, `PlanBrowserController` |
| 3. Comparison / Recommendation engine | `core/` package: `CapacityEstimator`, `MatchingEngine` (no framework dependencies) |
| 4. Recommendation Result | `RecommendationService`, `results.html`, `compare.html` (side-by-side) |
| 5. Admin / Data Management | `/admin/**`: add / update / remove plans, update pricing, providers, crawler |
| 6. Testing & Deployment | JUnit tests, `Jenkinsfile`, `Dockerfile`, `docker-compose.yml`, Sonar config |

Extra: a **price crawler** keeps prices current (see below) and a small **JSON API** (`/api/...`).

## Run it

Requirements: JDK 17+ and Maven 3.9+.

```bash
mvn spring-boot:run
# open http://localhost:8080
```

This uses a file-based H2 database in `./data`, seeds ~50 starter plans from 11 providers, and creates an admin
user. Admin login is at `/login`: user `admin`, password `admin123` unless you set `HOSTFIT_ADMIN_PASSWORD`
(**change it before deploying**).

### With MySQL and Docker

```bash
export HOSTFIT_ADMIN_PASSWORD='choose-a-strong-one'
docker compose up --build
```

Or run against your own MySQL: `SPRING_PROFILES_ACTIVE=mysql DB_HOST=... DB_USER=... DB_PASSWORD=... mvn spring-boot:run`.

### Configuration

| Environment variable | Default | Meaning |
|---|---|---|
| `HOSTFIT_ADMIN_USERNAME` / `HOSTFIT_ADMIN_PASSWORD` | `admin` / `admin123` | Administrator created on first start |
| `HOSTFIT_CRAWLER_ENABLED` | `true` | Turn the scheduled crawler on/off |
| `HOSTFIT_CRAWLER_CRON` | `0 0 */6 * * *` | Crawl schedule (Spring cron, every 6 hours) |
| `HOSTFIT_USD_INR` | `88.0` | USD→INR rate used for dollar-priced providers |
| `HOSTFIT_FX_AUTO_REFRESH` | `false` | Fetch a fresh USD→INR rate before each crawl |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | localhost / 3306 / hostfit / hostfit / hostfit | MySQL profile only |

More settings (user-agent, delay between sources, stale-after days, robots.txt) are under `hostfit.*` in `application.yml`.

## How recommendations work

1. **Estimate need** (`CapacityEstimator`): the use case sets a workload profile; monthly visits give minimum vCPU,
   RAM and transfer (with 25% headroom). Shared hosting is excluded for backends/databases and above a visit ceiling.
   Worked example from the deck: e-commerce, 50,000 visits, 50 GB, ₹2,000 → ≥2 vCPU, ≥3.5 GB RAM, ~458 GB transfer, VPS tier.
2. **Hard filters** (`MatchingEngine`): over budget, too little storage / bandwidth / vCPU / RAM, or unsuitable type → out.
   Plans failing only on budget are listed as "slightly over budget"; if nothing fits, the minimum budget needed is shown.
3. **Score 0–100**: resource fit 35, price 30, plan type 15, bandwidth 10, management 10. The results page shows each breakdown.

## Price data and the crawler

HostFit does **not** scrape on every user request. A scheduled job refreshes a database of plans, and all
recommendations run on that database (fast, and still works if a provider site is down).

* **Public APIs** (enabled by default): Linode and Vultr publish their catalogues without an API key.
* **HTML pages**: add a source in *Admin → Crawler → Add source* with CSS selectors (sample config pre-filled).
  Use **Test** to see what would be read before saving anything. Only server-rendered pages work; pages built by
  JavaScript need a different approach (provider API, or an admin-maintained price).
* Safeguards: identifies itself with a User-Agent, honours `robots.txt` for HTML pages, limits response size,
  refuses private/internal addresses, pauses between sources, and records every run and every price change.
* **Always check a provider's terms of service** before adding a scraping source. Prefer official APIs.
* Starter prices from `seed-data.json` are **rough estimates** and are labelled *Estimate* in the UI until a crawl
  or an admin confirms them (then *Live* / *Verified*; unrefreshed prices become *Stale* after 14 days).

## JSON API

* `GET /api/plans?type=VPS&provider=1&maxPrice=2000`
* `GET /api/plans/{id}`
* `POST /api/recommendations` with `{"useCase":"ECOMMERCE","monthlyVisits":50000,"storageGb":50,"budgetInr":2000,"expertise":"SELF_MANAGED"}`
* `GET /actuator/health`

## Tests and quality

```bash
mvn test              # unit + integration tests, JaCoCo report in target/site/jacoco
mvn verify
mvn sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<token>
```

* `core` and `crawler` parsers are plain Java and covered by fast unit tests (matching rules, scoring, parsing of
  Linode/Vultr/HTML fixtures, robots.txt rules).
* `WebFlowTest` drives the pages, admin security (login + CSRF) and the API with MockMvc.
* `CrawlPipelineTest` runs fetch → parse → upsert → price history with the network mocked.

## CI/CD (Jenkins → SonarQube → Docker)

`Jenkinsfile` stages: checkout → `mvn package` → JUnit → SonarQube analysis + quality gate → Docker image → deploy (main only).
Configure in Jenkins: tools `Maven-3.9` and `JDK-17`, a SonarQube server named `SonarQube`, Docker on the agent.

Suggested Git flow: `main` (deployable) ← `develop` ← `feature/*`; merge by pull request after review and a green pipeline.

## Project layout

```
src/main/java/com/hostfit
  core/      framework-free engine: CapacityEstimator, MatchingEngine, records
  crawler/   parsers (Linode, Vultr, CSS), PageFetcher, CrawlService, scheduler
  domain/    JPA entities     repo/  Spring Data repositories
  service/   forms and business services
  web/       controllers, JSON API, template helpers
  config/    security, properties, data seeding
src/main/resources  templates/ static/ seed-data.json application*.yml
```
