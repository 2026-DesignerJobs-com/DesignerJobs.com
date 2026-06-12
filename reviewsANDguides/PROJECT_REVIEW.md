# DesignerJobs.com — Project Review & Walkthrough

> A shared reference for our 6-day final push. It explains the whole project to **any** audience — from someone who has never seen Spring Boot, to a senior dev joining the review — with deep dives on **Spring Boot usage** and **session/auth management**, plus an honest list of **bugs, gaps, and remaining work**.

> **Update 2026-06-11:** Big day. Fixed: **B1, B5** (email casing), **B2** (partially — `listApplications` ownership), **B7** (mostly — unique applications, clean 409, conversation race). New since yesterday: **`DELETE /jobs/{id}`** + FE delete button (closes **M6**, half of M7), **search** wired end-to-end, **job-random/job-detail pages fixed** (B4 symptom gone), and a **second external REST API** (countriesnow.space → closes **S1**) with location autofill in profile-edit. `mvn test` now auto-selects JDK 17 via Maven toolchains. The red TDD board is down to **2 tests: b3 (PUT /jobs) and b4 (GET /jobs/random)**. Details inline below — each touched item is marked.

> **Update 2026-06-11 (eod):** **B2** fully fixed (status/hire owner-only, get owner-or-applicant), **B3** done (`PUT /jobs/{id}` with ownership, body can't set `clientId`/`createdAt`), **B22** fixed (delete owner-only), **B4** resolved as client-side-by-design (`getRandomJob()` removed, `b4` retired). `SecurityConfig` narrowed `GET /jobs/**` → `/jobs` + `/jobs/*` so nested sub-resources are authenticated at the filter level. **TDD board complete: `mvn test` = BUILD SUCCESS (115 tests).** H1 (JaCoCo) resolves itself now that Surefire passes. Also shipped: **C2** (JSON+XML content negotiation via `jackson-dataformat-xml`, `ContentNegotiationTest`). New finding **H6**: the request-logging filter writes plaintext passwords and bearer JWTs to `logs/app.log` (local-only, gitignored — fix pending, see §9b).

> **Update 2026-06-12:** Team day. **Lika** (`ee6fd9e` + follow-ups on `main`): removed the `@CrossOrigin` regression from `AuthController` (back to central CORS); **B23** user soft-delete (anonymize-in-place — no more orphaned ids); **B24** typed `ProfileUpdateRequest` DTO (wrong type → 400, not 500); and **removed the external chat-API placeholder** (`ExternalChatApiClient` + `USE_EXTERNAL_CHAT_API` gone → the latent chat-ordering finding is resolved). The time API now returns `null` instead of throwing — **but there is still no HTTP timeout (B15 stands), and `WorldClockService` doesn't null-check the result → NPE/500 on upstream failure** (new follow-up). **Kat/Yarah**: W3C fixes to `index.html` (duplicate `</script>`) + homepage header (S3 progress). DB file untracked + `*.db` gitignored. **Scope note:** this review now also covers the **`kat-second-frontend`** branch (the S2 admin dashboard) — see §9d. Remaining gaps on B23/B24 are tracked in §9.

---

## 0. How to use this document

Read the section that matches who you're talking to:

| Audience | Start at | Skip |
|---|---|---|
| **Total beginner** (non-coder, stakeholder) | §1 "What it is" + §2 "The 60-second tour" | the code deep-dives |
| **Junior dev / student** | §1–§5 | nothing |
| **Senior dev / reviewer** | §3 (architecture), §6 (Spring deep dive), §7 (session deep dive), §9/§9b/§9c (bugs: backend, harness, frontend) | §2 |
| **Grader / team lead** | the ⭐ Grading requirements section, then §9–§9c and §10 (plan) | the deep dives |

**Companion docs:** `test.md` (the JUnit + Postman test suite, coverage, the TDD red board), `jgrasp-guide.md` (visualizing/tracing execution), `postman/` (black-box API tests).

Three explanation levels are marked inline:
- 🟢 **Beginner** — plain language, no jargon.
- 🟡 **Intermediate** — assumes basic web/programming knowledge.
- 🔴 **Pro** — implementation detail and trade-offs.

---

## 1. What this project is

**DesignerJobs.com** is a student web project (FH Campus Wien) — a small job marketplace connecting **clients** (who post design jobs) with **designers** (who apply and get hired).

🟢 **Beginner version:** Think of it like a tiny "Upwork for designers." Companies post jobs; designers browse them, apply, chat with the company, and get hired. There's a website (the part you see) and a server (the part that stores everything and enforces the rules).

🟡 **What's built:** a REST API in **Java + Spring Boot** (the `backend/`) and a **vanilla-JavaScript + Bootstrap** website (`frontend/design3/`). One single Java program runs both — it answers data requests *and* serves the web pages. Everything lives at `http://localhost:8080` while developing.

🔴 **Stack:** Spring Boot 3.2, Java 17, embedded H2 file database, **raw JDBC** (no JPA/Hibernate), stateless **JWT** auth via Spring's OAuth2 Resource Server, BCrypt password hashing. No build of the frontend — plain `.html`/`.js` files served as static resources.

---

## ⭐ Grading requirements status (MUST / SHOULD / COULD)

This is the official rubric mapped against the **actual code today** (verified 2026-06-10). Legend: ✅ met · ⚠️ partial/at-risk · ❌ not met · ❓ needs verification.

### MUST — 21 points (all required; two are currently at risk)

| # | Requirement | Status | Evidence / what's missing |
|---|---|---|---|
| **M1** | BE is an individual component | ✅ | `backend/` Spring Boot app, separate from FE |
| **M2** | FE in HTML5 + CSS + JS | ✅ | `frontend/design3/` vanilla JS + Bootstrap |
| **M3** | FE↔BE over HTTP(S) | ✅ | all calls to `http://localhost:8080` |
| **M4** | Asynchronous transfer (AJAX) | ✅ | FE uses `fetch()` / `Auth.authFetch()` (21 calls) |
| **M5** | BE returns JSON or XML | ✅ | JSON via `@RestController` |
| **M6** | BE uses GET, POST, PUT **and DELETE**, each on ≥1 endpoint | ✅ | ~~DELETE only 501 stubs~~ **Fixed 2026-06-10:** functional `DELETE /jobs/{id}` with auth + ownership check (commit `42fb250`). PUT was already functional (`PUT /applications/{id}/status`). |
| **M7** | FE consumes GET, POST, PUT **and DELETE** from ≥1 endpoint | ✅ | **Met 2026-06-12:** `profile-edit.html` now issues **PUT** and **DELETE** `/auth/me` (commits `826d09c`/`20d3429`); the delete-job button already covered DELETE. The FE now exercises all four verbs. (Job edit/delete UI still nice-to-have — see F3.) |
| **M8** | Consume ≥1 external REST service | ✅ | `ExternalTimeApiClient` → `timeapi.io` (`GET /world-clock`) |
| **M9** | Session management (Login/JWT) | ✅ | stateless JWT — see §7 |

> **Action for full 21 points:** ~~only **M7** remains~~ — **M7 met 2026-06-12** (`profile-edit.html` issues `PUT`/`DELETE /auth/me`, commits `826d09c`/`20d3429`). All MUST requirements are now satisfied.

### SHOULD — 8 points

| # | Requirement | Status | Evidence / what's missing |
|---|---|---|---|
| **S1** | Consume ≥2 external REST services | ✅ | **Closed 2026-06-10:** `ExternalLocationApiClient` → `countriesnow.space` (`GET /locations/cities?country=…`, commit `371b55f`), consumed by the profile-edit location autofill. Plus `timeapi.io`. (`ExternalChatApiClient` remains a disabled placeholder.) |
| **S2** | A second FE component using ≥3 BE endpoints | 🟠 | **In progress on `kat-second-frontend`** (admin dashboard, `frontend/admin/`). Currently calls **2** endpoints (`GET /jobs` real; `GET /designers` still a 501 stub → demo fallback) — needs **≥3 real**. Not yet merged to `main`. Full eval in §9d. |
| **S3** | FE is W3C compliant | 🟠 | **6 pages failed 2026-06-10** (~17 errors; see §9c). **2026-06-12:** `index.html` (duplicate `</script>`) and `homepage.html` header fixed (Kat/Yarah). Re-validate the remaining `post-a-job` / `profile-edit` / `register` / `search-results` / `advanced-search`. |
| **S4** | FE responsive (mobile + desktop views) | ⚠️ | Bootstrap grid is responsive; confirm a **dedicated** mobile vs desktop view (breakpoints, nav collapse) and document it. |

### COULD — 5 points

| # | Requirement | Status | Evidence / what's missing |
|---|---|---|---|
| **C1** | Consume ≥3 external REST services | ❌ | needs three — **have two now** (`timeapi.io`, `countriesnow.space`). One more real API closes it. |
| **C2** | BE returns JSON **and** XML | ✅ *(2026-06-11)* | `jackson-dataformat-xml` + content negotiation: JSON is the default, `Accept: application/xml` returns XML (explicit `produces` on `GET /jobs` + `GET /jobs/{id}`, works app-wide via the registered converter). Covered by `ContentNegotiationTest`. |
| **C3** | BE PATCH endpoint consumed by FE | ❌ | no `@PatchMapping` exists (the only "PATCH" in code is a CORS *allowed-method* entry in `SecurityConfig`, not an endpoint). Add a PATCH endpoint (e.g. partial job/profile update) and call it from the FE. |

### Points summary (honest self-assessment)

- **MUST (21):** all 9 met *(M6 closed 2026-06-10, M7 closed 2026-06-12 via profile `PUT`/`DELETE /auth/me`)* → full 21.
- **SHOULD (8):** **S1 met** (2 external APIs); S4 likely; S3 still failing (6 pages); S2 needs work.
- **COULD (5):** none met yet (C1 is one API away).

These map directly onto the 6-day plan in §10 — the requirement gaps are folded in there with priority.

---

## 2. The 60-second tour (for anyone)

1. **A client registers** → enters name, email, password, picks role "CLIENT". The server stores the account (password is scrambled, never saved in plain text) and hands back a **token** (a digital wristband proving who you are).
2. **The client posts a job** → "Need a logo, €500." The server records it and stamps it with the client's ID.
3. **A designer registers** as "DESIGNER", browses jobs (no login needed just to *look*), and **applies** to one.
4. **They chat** in-platform about the job.
5. **The client accepts/hires** the designer. (Hiring is *meant* to generate a contract — that part isn't finished yet.)

The pieces that fully work today: **accounts/login, posting & browsing jobs, applying/hiring flow, and chat.** The pieces still stubbed: **designer profiles & portfolios, contracts, and moderation.** (See §8 for the exact status table.)

---

## 3. Architecture at a glance

```
                       http://localhost:8080
                                │
        ┌───────────────────────┴────────────────────────┐
        │              ONE Spring Boot process            │
        │                                                 │
   ┌────▼─────┐   matches a @RestController?               │
   │ Request  │──── yes ──►  Controller ► Service ► Repository ► H2 file DB
   └────┬─────┘                                            │
        │ no  (any unmatched URL)                          │
        └────────────►  WebConfig serves a static file     │
                        from frontend/design3/             │
        └─────────────────────────────────────────────────┘
```

🟡 **Two roles, one program.** `Main.java` first calls `DatabaseInitializer.init()` (creates the H2 tables) *then* `SpringApplication.run()`. After that, every HTTP request either:
- hits a **REST controller** (e.g. `/jobs`, `/auth/login`), or
- falls through to **`config/WebConfig`**, which serves a file from `frontend/design3/` (so `/jobs.html` returns the page, `/jobs` returns JSON).

🔴 **Key design decisions and where they live:**
- **Persistence = raw JDBC.** No Spring Data, no `JdbcTemplate`. Each repository opens a `Connection` via `Database.getConnection()` and writes `PreparedStatement` SQL by hand. Canonical example: `job/JobRepository.java`.
- **DB = embedded H2, file mode** at `data/projectdb.mv.db` (`jdbc:h2:file:./data/projectdb`, user `sa`, no password — dev only). **No migrations framework** — schema changes mean editing a `CREATE TABLE` and deleting the DB file (or hand-writing `ALTER TABLE`).
- **Security centralized** in `config/SecurityConfig` — one filter chain, one `PasswordEncoder` bean, one CORS config. Don't scatter `@CrossOrigin` or `new BCryptPasswordEncoder()` elsewhere.
- **Per-package READMEs are authoritative** — each package under `at.ac.fhcampuswien/` documents its own endpoints. Read them before editing, but **verify against the code** (some are stale — see §9).

---

## 4. Repository layout

```
DesignerJobs.com/
├── backend/
│   ├── pom.xml
│   ├── data/projectdb.mv.db          ← the H2 database file (delete to reset state)
│   └── src/main/
│       ├── java/at/ac/fhcampuswien/
│       │   ├── Main.java             ← entry point
│       │   ├── Database/             ← H2 connection + table bootstrap
│       │   ├── config/               ← SecurityConfig, WebConfig
│       │   ├── auth/                 ← register / login / me   (DONE)
│       │   ├── session/              ← JwtService (token issuing) (DONE)
│       │   ├── job/                  ← job listings + search   (mostly DONE)
│       │   ├── application/          ← apply / hire flow        (DONE, needs review)
│       │   ├── chat/                 ← in-platform messaging    (DONE)
│       │   ├── user/                 ← designer profiles        (STUB — 501s)
│       │   ├── contract/             ← freelance contracts      (STUB)
│       │   ├── moderation/           ← reports/moderation       (STUB)
│       │   ├── worldclock/           ← demo: proxies timeapi.io
│       │   └── external/             ← HTTP clients (time real, chat placeholder)
│       └── resources/application.properties
└── frontend/design3/                 ← the live frontend (NOT design1/design2)
    ├── index.html  (iframe shell)
    ├── auth.js     (Auth.authFetch + localStorage token store)
    └── *.html      (one file per page)
```

---

## 5. How to run, test & verify

```sh
cd backend
mvn spring-boot:run        # serves API + frontend on http://localhost:8080
mvn package                # build the jar
mvn test                   # run the test suite (see test.md)
```

Requires **JDK 17 + Maven**. *(Updated 2026-06-11:)* the build now uses **Maven toolchains** to compile and test with JDK 17 even if Maven itself runs on a newer JDK (which breaks Mockito/JaCoCo). One-time setup per machine: a `~/.m2/toolchains.xml` pointing at a local JDK 17 — see `backend/README.md`. Without it the build fails fast with a clear message.

> ✅ **There is now a JUnit test suite** under `backend/src/test` (JUnit 5 + Mockito + AssertJ, plus `@SpringBootTest`/MockMvc for security), and a Postman/Newman black-box suite in `postman/`. Full details, coverage numbers, and the TDD red board are in **`test.md`**.
>
> ⚠️ ~~**`mvn test` is intentionally RED right now.**~~ **Board complete since 2026-06-11 (eod):** `b3` green (`PUT /jobs/{id}` implemented), `b4` retired (random-job client-side by design) — `mvn test` = BUILD SUCCESS and the JaCoCo report generates again (H1 ✅). The test-first rule stands for future bugs: one red test per open bug; don't "fix" the build by deleting tests.

**Reset all state:** delete `backend/data/projectdb.mv.db` and restart. (Note: this file is currently tracked in git and churns — see **H3**.)

**Smoke-test recipe (manual / black-box, copy/paste during review):**
```sh
# 1. register a client
curl -s -X POST localhost:8080/auth/register -H 'Content-Type: application/json' \
  -d '{"fullName":"Acme","email":"acme@test.com","password":"secret123","role":"CLIENT"}'
# → returns { token, userId, role }   (HTTP 201)

# 2. use the token to post a job
TOKEN=...paste...
curl -s -X POST localhost:8080/jobs -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"title":"Logo design","budget":"500"}'

# 3. browse jobs (no token needed)
curl -s localhost:8080/jobs
```

---

## 6. Spring Boot — the deep dive

This is the part most worth understanding well, because the whole backend is "just Spring."

### 6.1 🟢 What Spring Boot even is

Spring Boot is a framework that removes boilerplate from building a Java web server. Instead of manually wiring up an HTTP server, parsing requests, routing URLs, and converting JSON, you **annotate** plain Java classes and Spring does the plumbing.

A helpful mental model: you write the *"what"* (here's a method that handles `POST /jobs`), and Spring handles the *"how"* (listen on a port, accept the TCP connection, parse the HTTP, find your method, give it the parsed data, turn your return value back into JSON).

### 6.2 🟡 The annotations we actually use

| Annotation | Where | What it does |
|---|---|---|
| `@SpringBootApplication` | `Main.java` | Marks the entry class; turns on auto-configuration + component scanning of the whole `at.ac.fhcampuswien` package tree. |
| `@RestController` | every controller | "This class handles HTTP and returns JSON." Combines `@Controller` + `@ResponseBody`. |
| `@RequestMapping("/jobs")` | class level | Common URL prefix for the controller. |
| `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` | methods | Map an HTTP method + path to a Java method. |
| `@PathVariable` | params | Pulls `{id}` out of the URL. |
| `@RequestParam` | params | Pulls `?q=logo` query parameters. |
| `@RequestBody` | params | Deserializes the JSON request body into a Java object (via Jackson). |
| `@Service` | `JwtService`, `ChatService` | Marks a business-logic bean Spring should create and inject. |
| `@Configuration` / `@Bean` | `SecurityConfig`, `WebConfig` | Java-based config; `@Bean` methods produce singletons Spring manages. |
| `@Value("${app.jwt.secret}")` | config | Injects a value from `application.properties`. |

### 6.3 🟡 Dependency Injection (DI) — the core idea

Notice that no controller ever does `new JobRepository()`. Instead:

```java
public JobController(JobRepository jobRepository) {   // constructor injection
    this.jobRepository = jobRepository;
}
```

🟢 **Plain version:** You don't build your own tools; you ask for them in the constructor and Spring hands you ready-made, shared instances. This keeps pieces loosely coupled and swappable.

🔴 Spring builds a dependency graph at startup: it sees `JobController` needs a `JobRepository`, creates one (a singleton bean), and passes it in. The same `ChatService` constructor receives `ConversationRepository`, `MessageRepository`, and `ExternalChatApiClient` — all auto-wired. There are **no `@Autowired` field annotations**; we use **constructor injection** everywhere, which is the recommended style (immutable, testable, fails fast if a dependency is missing).

### 6.4 🔴 The request lifecycle (what happens on every call)

```
TCP → Embedded Tomcat → Spring Security filter chain → DispatcherServlet
    → HandlerMapping picks the @…Mapping method
    → argument resolvers fill @PathVariable / @RequestParam / @RequestBody / Authentication
    → your controller method runs (calls Service → Repository → JDBC → H2)
    → return value (object or ResponseEntity) → Jackson → JSON → HTTP response
```

Two things worth calling out for reviewers:
- **`Authentication` as a method parameter** is injected by Spring Security from the `SecurityContext` (set by the JWT filter). That's how controllers get the caller's identity *without trusting the request body*.
- **`ResponseEntity<?>`** lets a method choose its own status code (`201 Created`, `403 Forbidden`, etc.). Methods that just return a `List<Job>` (like `JobController.search`) implicitly return `200 OK`.

### 6.5 🔴 Auto-configuration choices we made

- `@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })` in `Main.java` — we **disable** Spring Security's default in-memory user/password generator, because we authenticate via JWT, not username/password form login.
- `DatabaseInitializer.init()` runs **before** `SpringApplication.run()` — tables must exist before any repository touches them. This is deliberate ordering, not an accident.
- **Static file serving** is handled by `config/WebConfig` (a `WebMvcConfigurer`) mapping unmatched URLs to `frontend/design3/`. That's why adding a controller path that collides with a page name matters.

### 6.6 🔴 Why raw JDBC instead of JPA?

It's a **teaching choice** — the project came off a hand-rolled `HttpServer` (prog2), so staying close to SQL keeps the data layer transparent. Trade-off: more boilerplate, manual `PreparedStatement` mapping, no automatic schema management. Every repository follows the `job/JobRepository.java` pattern; copy it when implementing a new one.

---

## 7. Session & authentication — the deep dive

> **This is the second area to understand thoroughly.** The headline: **there is no server-side session.** No `HttpSession`, no `JSESSIONID`, nothing stored about "who is logged in" on the server.

### 7.1 🟢 The wristband analogy

When you log in, the server gives you a **signed wristband** (a JWT). Every time you ask the server to do something, you show the wristband. The server checks the signature is genuine, reads your name and role off it, and acts — *without* looking anything up or remembering you between requests. When you "log out," you just throw the wristband away; the server doesn't track it.

### 7.2 🟡 What a JWT is

A JWT (JSON Web Token) is a string in three parts: `header.payload.signature`.
- **payload** carries claims — here: `sub` (the user id), `role`, `iat` (issued-at), `exp` (expiry).
- **signature** is an HMAC-SHA256 hash of header+payload using a **secret key only the server knows**. If anyone tampers with the payload, the signature no longer matches and the token is rejected.

So the token is *readable by anyone* (don't put secrets in it) but *unforgeable without the key*.

### 7.3 🟡 The end-to-end flow

```
REGISTER / LOGIN  (public)
  client → POST /auth/login {email, password}
  server → verify BCrypt hash → JwtService.issue(userId, role)
         → returns { token, userId, role }
  frontend (auth.js) → stores token in localStorage
                       (keys: designer_jobs_token / _userId / _role)

EVERY LATER REQUEST  (authenticated)
  frontend → Auth.authFetch() attaches header:  Authorization: Bearer <token>
  server → BearerTokenAuthenticationFilter reads it
         → JwtDecoder verifies HS256 signature + expiry
         → JwtAuthenticationConverter builds an Authentication
            (name = "sub" claim = userId, authority = "ROLE_" + role)
         → controller reads auth.getName() / auth.getAuthorities()

LOGOUT
  POST /auth/logout → 204, does NOTHING server-side (client discards token)
```

### 7.4 🔴 Where each piece lives in code

- **Issuing** — `session/JwtService.issue()`. Builds claims (`subject = userId`, `claim("role", role)`, `issuedAt`, `expiresAt = now + app.jwt.expiry-millis`), signs HS256 via `JwtEncoder`.
- **Signing keys** — `config/SecurityConfig`: `hmacSecretKey()` turns `app.jwt.secret` into a `SecretKeySpec`; `jwtEncoder()` (Nimbus) signs; `jwtDecoder()` verifies. Same secret both directions (symmetric HS256).
- **Verifying** — we **don't** write verification code. `oauth2ResourceServer().jwt(...)` wires in Spring's `BearerTokenAuthenticationFilter`, which uses our `JwtDecoder`. Invalid/expired/missing token → 401 before the controller runs.
- **Mapping claims → identity** — `jwtAuthenticationConverter()`:
  - `setPrincipalClaimName("sub")` so `auth.getName()` returns the **userId**.
  - role claim is a single string, wrapped into `ROLE_<role>` as a `GrantedAuthority`.
- **Stateless policy** — `sessionManagement(SessionCreationPolicy.STATELESS)`: Spring never creates an `HttpSession`.
- **Password security** — one `BCryptPasswordEncoder` bean; `AuthController.register` calls `encode()`, `login` calls `matches()`. Plain passwords are never stored.

### 7.5 🔴 The golden rule: never trust identity from the body

Controllers set ownership from the token, not the payload:
- `JobController.create`: `job.clientId = auth.getName();` — the body's `clientId` is ignored.
- `ChatService.sendMessage`: `message.senderId = currentUserId;` — server-set.
- `ApplicationController.apply`: `designerId = auth.getName();`.

This is the single most important security invariant in the codebase. Any new endpoint must follow it.

### 7.6 🔴 Authorization rules (who can hit what)

From `SecurityConfig.filterChain`, in order:
- `permitAll`: `/auth/**`, `/world-clock`, **GET** `/jobs/**`, **GET** `/designers/**`, and static assets (`/*.html`, `/*.css`, `/*.js`, `/images/**`, …).
- everything else → `authenticated()`.

🔴 **To add a public endpoint you must add its matcher here first**, otherwise it 401s before reaching your controller. Role-level checks (e.g. "only designers can apply") are currently done **inside controllers** (`ApplicationController.isDesigner`), not via `hasRole(...)` in the filter chain — worth noting for reviewers, it's a slight inconsistency.

### 7.7 🔴 Known limitations of this auth model

- **No token revocation / logout is a noop.** A stolen or leaked token is valid until `exp` (2h default). Acceptable for a student project; would need a blacklist or short-lived + refresh tokens in production.
- **Symmetric secret** — anyone with `app.jwt.secret` can mint tokens. The default dev secret **must** be overridden via `APP_JWT_SECRET` (≥32 chars) in any non-dev environment.
- **Tokens in `localStorage`** (frontend) are readable by any JS on the page → XSS-exposed. Fine for the assignment; httpOnly cookies would be the hardened alternative.

---

## 8. Feature & endpoint status (verified against code, June 2026)

> ⚠️ The `backend/README.md` "status at a glance" table is **out of date** — it lists `application/`, `chat/` as stubs, but they are implemented. Trust this table (built by reading the controllers).

| Package | Real state | Endpoints | Notes |
|---|---|---|---|
| `auth/` | ✅ Done | `POST /auth/register`, `POST /auth/login`, `POST /auth/logout` (noop), `GET /auth/me` | Solid. |
| `session/` | ✅ Done | — | `JwtService` only. |
| `config/` | ✅ Done | — | Security + static serving. |
| `job/` | ✅ Done | `POST /jobs`, `GET /jobs` (+ search params), `GET /jobs/{id}`, `PUT /jobs/{id}` *(new 2026-06-11)*, `DELETE /jobs/{id}` | Search wired end-to-end (`23d4dda`). PUT and DELETE are owner-only (B3 ✅, B22 ✅). |
| `application/` | ✅ Done (review) | `POST /jobs/{jobId}/apply`, `GET /jobs/{jobId}/applications`, `GET /applications/{id}`, `PUT /applications/{id}/status`, `POST /applications/{id}/hire` | Hire is a stub-trigger (no contract yet). Apply validates job existence + rejects duplicates (B7 ✅); list/status/hire are owner-only and get is owner-or-applicant (B2 ✅). |
| `location/` | ✅ Done *(new 2026-06-10)* | `GET /locations/countries`, `GET /locations/cities?country=…` | Countries hardcoded list; cities proxied from `countriesnow.space` (2nd external API → S1). Public via `SecurityConfig`. |
| `chat/` | ✅ Done | `GET/POST /conversations`, `GET/POST /conversations/{id}/messages` | Local H2 mode; external API path behind `USE_EXTERNAL_CHAT_API=false`. |
| `worldclock/` | ✅ Done | `GET /world-clock` | Demo proxy to timeapi.io. |
| `user/` | ❌ Stub | `/designers`, `/designers/{id}`, `/designers/{id}/portfolio…`, `/users/{id}` | **All return `501 not_implemented`.** Profiles + portfolio. |
| `contract/` | ❌ Stub | `/contracts…` | Phase 2. Hire flow has a `TODO` to call it. |
| `moderation/` | ❌ Stub | `/moderation…`, reports | Phase 2. |

---

## 9. Bugs & correctness issues (review these first)

Ordered by severity. Found by reading every backend source file (controllers, services, repositories) line-by-line and tracing call sites against the actual code (multiple `xhigh` review passes — backend, frontend, and test harness). **These are the concrete things to fix in the 6 days.** Numbering: **B1–B13** first pass, **B14–B21** the deep backend review (§9), **B22** new 2026-06-11, **B23–B25** new 2026-06-12 (B23/B24 profile commits, B25 time-API), harness **H1–H6** in §9b, frontend **F1–F15** in §9c, second-FE/S2 eval in §9d. Status markers: ✅ fixed · 🟠/🟡 partially fixed or downgraded · unmarked = still open.

### ✅ B1 — ~~Login is case-sensitive on email~~ — FIXED 2026-06-11 (`219e278`)
`login` now normalizes the email exactly like registration (`trim().toLowerCase()`) before `findByEmail`. Regression test `KnownBugsTest.b1` is green.
<details><summary>Original finding</summary>
`AuthController.register` stored `user.email = req.email.trim().toLowerCase()`, but `login` queried `findByEmail(req.email)` with the **raw** input. Someone registering as `John@Example.com` (stored lowercase) could never log in typing the same casing → `401 invalid email or password`.
</details>

### ✅ B2 — ~~Authorization gaps in the application/hire flow~~ — FIXED 2026-06-11 (`22c5c9b` + follow-ups)
In `ApplicationController`:
- ✅ `GET /jobs/{jobId}/applications` — loads the job, 404 if missing, 403 unless `job.clientId == auth.getName()`. Test `b2` green.
- ✅ `GET /applications/{id}` — owner **or** applicant only (403 for unrelated users).
- ✅ `PUT /applications/{id}/status` and `POST /applications/{id}/hire` — owner-only via a shared `isJobOwner(application, auth)` helper.

All paths covered by unit tests in `ApplicationControllerTest` (403 for non-owners, happy paths for owner/applicant). Defense-in-depth: `SecurityConfig` no longer permitAlls `GET /jobs/**` — only `/jobs` and `/jobs/*` — so `GET /jobs/{id}/applications` is also authenticated at the filter level.

### ✅ B3 — ~~`PUT`/`DELETE /jobs/{id}` advertised but unreachable~~ — FIXED 2026-06-11
- ✅ **`DELETE /jobs/{id}`** exists (`42fb250`) and the FE calls it (delete button in `job-detail.html`, `f3d26e9`). Closes **M6**. The too-loose authz rule was tightened to owner-only — see **B22** (✅).
- ✅ **`PUT /jobs/{id}`** implemented 2026-06-11: 401/400/404/403 paths + owner-only check; the caution was followed — `clientId` and `createdAt` are copied from the existing row before `JobRepository.update()`, so the body cannot set them. Test `b3` green + unit tests in `JobControllerTest` (incl. spoofed-body preservation test).

### ✅ B4 — ~~Random-job page broken~~ — RESOLVED 2026-06-11 (client-side by design)
The user-facing bug was fixed 2026-06-10 (`7ddc891`): `job-random.html` was rewritten from a hardcoded fake job to fetching `GET /jobs` and picking a random one **client-side**. The same commit pointed the search-result "View Job" buttons at the real `job-detail.html?id=…` page.
**Team decision 2026-06-11:** the client-side approach is accepted as final — random-job is a frontend feature, there is deliberately no `GET /jobs/random` route. The dead `JobRepository.getRandomJob()` and its repository tests were removed, and the `b4` test retired (decision noted in `KnownBugsWebTest`). *Caveat for later: if `GET /jobs` ever gets pagination, the client-side pick only samples the first page — revisit then.*

### ✅ B5 — ~~Duplicate-email registration returns 500 instead of 409~~ — FIXED 2026-06-11 (`219e278`)
`register` now normalizes the email once and uses the normalized value for both the `existsByEmail` guard and storage, so `Foo@x.com` vs existing `foo@x.com` is caught up-front as a clean **409**. Test `b5` green. (Same root cause as B1.)

### 🟠 B6 — Hire flow doesn't create a contract
`ApplicationController.hire` sets status to `HIRED` and leaves `// TODO: trigger contract creation`. The core "happy path" promised in the UI (hire → contract) is incomplete because `contract/` is a stub.

### 🟡 B7 — No validation/uniqueness on applications & conversations — MOSTLY FIXED 2026-06-11 (`a72592d`)
- ✅ `applications` now has **`UNIQUE (job_id, designer_id)`** (incl. `ALTER TABLE … IF NOT EXISTS` migration for existing DB files — if an old file already contains duplicates, startup fails: reset the DB).
- ✅ `apply` validates the job exists (**404**) and rejects duplicates up-front (**409** "you have already applied"), with the constraint as backstop. Test `b7` green.
- ✅ The `ConversationRepository.create` **TOCTOU race** is fixed: the insert-loser catches the unique violation and returns the existing conversation instead of a 500.
- ❌ **Still open:** `ChatService.createConversation` does not verify that the referenced `jobId`/counterparty exist (no FK constraints either) → orphan conversations remain possible. Overlaps with **B14** (conversation spoofing) — fix both together by validating job + participants there.

### 🟠 B8 — Stale package/READMEs vs. code
`chat/README.md` and the backend README "status at a glance" still call `chat/` and `application/` "501 stubs," but they're implemented. Misleading docs cause people to re-implement or distrust working code. Update them (and trust §8 of this doc).

### 🟡 B9 — Message pagination returns oldest messages first
`MessageRepository.findByConversationId` orders `created_at ASC` with `LIMIT 50 OFFSET page*50`, so page 0 is the **oldest** 50 messages and new messages land on ever-higher page numbers. For a chat UI you almost always want the most recent page first. *Minor, but it makes the chat feel broken as history grows.*

### 🟡 B10 — Designer profile data is split & unreachable
Registration stores `designType`/`skills` on the user (`auth`), but the **`/designers` endpoints that would surface them are all 501** (`user/` package). So designer-facing pages can't actually show profiles yet.

### 🟡 B11 — No global exception handling outside chat
Only `ChatController` has an `@ExceptionHandler`. Any repository `RuntimeException` (every repo wraps `SQLException` in one) in another controller surfaces as a default Spring 500 with a stack trace. Add a `@ControllerAdvice`.

### 🟡 B12 — Dev secret & DB credentials are committed defaults
`app.jwt.secret` default and H2 `sa`/no-password are dev-only but easy to ship by accident. The symmetric secret means anyone who has it can mint valid tokens. Make sure any deploy/grader overrides `APP_JWT_SECRET` (≥32 chars).

### ⚪ B13 — Top-level `README.md` references `design1/`/`design2/`
The live frontend is `design3/`. Minor, but confusing for newcomers. Trust `frontend/design3/README.md`.

### Additional backend findings (from the deep `xhigh` review, 2026-06-10)

### 🔴 B14 — Conversation spoofing in `ChatService.createConversation`
`createConversation` only checks `currentUserId.equals(clientId) || currentUserId.equals(designerId)` — i.e. the caller must be *one* of the two participants, but the **other** participant, the job ownership, and existence are never validated. A designer can post `{clientId: <any victim>, designerId: self, jobId: anything}` and force a conversation onto any user, then send them unsolicited messages. *Fix: verify the job exists, that `clientId` is the job's real owner, and that the counterparty actually relates to the job.*

### 🔴 B15 — External time API has no HTTP timeout
`ExternalTimeApiClient` builds `HttpClient.newHttpClient()` and an `HttpRequest` with **no connect or request timeout**. If `timeapi.io` hangs, `GET /world-clock` blocks a Tomcat worker indefinitely; repeated calls exhaust the pool. *Fix: set `.connectTimeout(...)` on the client and `.timeout(...)` on the request.*

### 🟠 B16 — Lexicographic timestamp ordering is wrong on second boundaries
`created_at` is stored as `Instant.toString()` (variable-length fractional seconds) in a `VARCHAR` and ordered with `ORDER BY created_at`. `'2026-…T12:00:00Z'` sorts *after* `'2026-…T12:00:00.5Z'` lexicographically (`'.'`=46 < `'Z'`=90), which is the wrong chronological order. Affects jobs list/search, messages, conversations whenever a timestamp lands on a whole second. *Fix: store a fixed-width/UTC-millis timestamp or a sortable numeric column.*

### 🟠 B17 — Conflicting CORS configuration
`config/WebConfig.addCorsMappings` registers a **second** CORS policy (hardcoded `localhost:63342` origins) competing with the single `SecurityConfig.corsConfigurationSource` the design intends (CORS should be configured once, centrally). Behavior diverges between security-filtered API paths and MVC/handler paths. *Fix: delete `WebConfig.addCorsMappings`; keep only the `SecurityConfig` source.*
**Regressed wider 2026-06-12:** commit `826d09c` added `@CrossOrigin(origins="*", allowedHeaders="*", methods={GET,POST,PUT,DELETE})` directly on `AuthController` (with a leftover `// DAS HIER ERWEITERN` comment) — a *third* CORS source, now with a wildcard origin, against our convention that CORS is configured once centrally (no per-controller `@CrossOrigin`). *Fix: remove the annotation; rely on `SecurityConfig` + `app.cors.allowed-origins`.*

### 🟡 B18 — `/world-clock` is all-or-nothing and sequential
`WorldClockService` makes 4 blocking external calls in sequence and lets any single failure throw, so one slow/failing city fails the whole endpoint and latency is the sum of 4 round-trips. *Fix: fetch in parallel and degrade gracefully per city.*

### 🟡 B19 — Hire/status transition has a TOCTOU
`ApplicationController.hire`/`updateStatus` check the current status in Java then `UPDATE` unconditionally. Concurrent requests can both pass the check and double-process — and once hire generates a contract (B6), produce two contracts. *Fix: conditional `UPDATE … WHERE id=? AND status=?` and check affected rows.*

### 🟡 B20 — `app.frontend.path` default points at stale `design1/`
`WebConfig`'s `@Value("${app.frontend.path:../frontend/design1/}")` defaults to a directory that doesn't exist (live FE is `design3/`). Only works because `application.properties` overrides it; any environment missing that property serves 404s. *Fix: make the default `../frontend/design3/`.*

### ⚪ B21 — No connection pooling
`Database.getConnection()` opens a fresh `DriverManager` connection per repository call (no pool). Functionally OK for embedded H2 but wasteful and unbounded under concurrency. *Fix: a pooled `DataSource` (HikariCP).* *(Efficiency/altitude, not a correctness bug.)*

### ✅ B22 — ~~`DELETE /jobs/{id}` lets **any designer** delete **any job**~~ — FIXED 2026-06-11
The delete endpoint (`42fb250`) authorized `isOwnerClient || isDesigner` — every logged-in designer could delete every job. Fixed exactly as proposed: the `isDesigner` branch was dropped, only `job.clientId == auth.getName()` may delete (Javadoc updated, non-owner 403 covered in `JobControllerTest`).

### 🟡 B23 — ~~`DELETE /auth/me` hard-deletes the user, orphaning owned data~~ — FIXED 2026-06-12 (gap remains)
~~The self-service delete ran `DELETE FROM users WHERE id=?` with no cleanup, leaving `jobs.client_id` / `applications.designer_id` / chat references dangling.~~ Fixed in `ee6fd9e` (Lika): `deleteById` now **soft-deletes by anonymizing in place** — `UPDATE users SET role='DELETED', full_name='Deleted Account', email='deleted_<id>@…', password_hash='NO_ACCESS', …`. The row + id survive, so nothing dangles; login is blocked and the real email is freed. **Remaining gap:** read queries don't exclude soft-deleted rows — `findById`/`findByEmail` are still `SELECT … WHERE id/email=?` with no `AND role <> 'DELETED'`, so a deleted account is still fetchable (`GET /auth/me` returns it; a still-valid JWT could `PUT /auth/me` and repopulate it; future `/designers` listing would show it). *Fix: add `AND role <> 'DELETED'` to the read queries.*

### 🟡 B24 — `PUT /auth/me` binds an untyped map with no validation — PARTIALLY FIXED 2026-06-12
~~`AuthController.updateProfile` read a `Map<String,Object>` with unchecked `(String)`/`(Number)` casts.~~ `ee6fd9e` (Lika) replaced it with a typed `ProfileUpdateRequest` DTO, so a wrong JSON type now yields a Jackson **400** instead of a `ClassCastException` 500. **Still open:** (a) **no length validation** — `spring-boot-starter-validation` isn't on the classpath and there's no `@Size`/`@Valid`, so an over-long `bio`/`portfolioUrl` past the `VARCHAR` caps still throws H2 "value too long" → 500; (b) **new regression** — the rate fields are primitive `int` in the DTO and assigned unconditionally, so a `PUT` that omits `hourlyMin`/`hourlyMax`/`projectMin` overwrites the stored values with `0` (the string fields are guarded with `!= null`; ints can't be). *Fix: add validation + use boxed `Integer` with a null check for the rates.*

### 🟠 B25 — External time API: graceful-null without a timeout or a null-check — new 2026-06-12
`ee6fd9e`/`aa28246`/`e072ab6` (Lika) changed `ExternalTimeApiClient` to **return `null`** (instead of throwing) on a non-2xx / IOException, to "prevent server crashes." Two problems remain: (a) **still no connect/request timeout**, so a hung `timeapi.io` still pins a Tomcat worker indefinitely (the original **B15**); and (b) **`WorldClockService.loadCityTime` never null-checks** the client's result — it calls `apiResponse.path("date")` on the returned value, so a `null` now throws a **NullPointerException → 500**, i.e. the crash wasn't actually prevented, just moved. *Fix: set `.connectTimeout`/`.timeout`, and have the service skip/degrade per city when the client returns `null`.* *(Note: the external **chat** client + `USE_EXTERNAL_CHAT_API` were removed the same day, which resolves the latent chat-ordering finding.)*

---

## 9b. Test & build harness findings (from code review)

These are **not** application bugs — they're issues in the test suite, build config, and repo hygiene introduced/uncovered while adding the test harness. Listed because they undermine the safety net itself. (Verified 2026-06-10.)

### ✅ H1 — ~~`mvn test` no longer produces a JaCoCo coverage report~~ — RESOLVED 2026-06-11 (board green)
The JaCoCo `report` goal is bound to the `test` phase, and the intentional red TDD tests made Surefire fail and abort the phase before `jacoco:report` could run. Since the §5 board went green (b3 fixed, b4 retired), `mvn test` exits `0` and the report regenerates normally. *(The structural fragility remains: any future red test silently kills coverage again — binding `jacoco:report` to `verify` or adding a coverage profile is still worth doing if red-TDD boards return.)*

### 🟠 H6 — *(new 2026-06-11)* Request logging writes plaintext passwords and bearer tokens to `backend/logs/app.log`
`config/RequestLoggingConfig` registers a `CommonsRequestLoggingFilter` with `includeHeaders(true)` + `includePayload(true)`, and `logging.file.name=logs/app.log` persists it at DEBUG. Verified in the live log: full `Authorization: Bearer …` JWTs on every authenticated request, and register/login payloads with `"password":"…"` in plaintext (11 occurrences after one evening, file ~555 KB). `*.log` is gitignored, so nothing reaches the repo — but the local disk holds live credentials and the file grows unbounded. **Fix:** exclude the `Authorization` header via `setHeaderPredicate`, skip payload logging on `/auth/**` (or `includePayload(false)` globally), and consider a rolling-file policy.

### 🟠 H2 — Vacuous password-encoding assertion (false confidence)
In `AuthControllerTest`, the "password is hashed, not raw" check is `assertThat(saved.passwordHash).isNotEqualTo("secret123")`. But `passwordEncoder` is a Mockito mock whose unstubbed `encode()` returns `null`, so the assertion is `null != "secret123"` → trivially true. **It would still pass even if `register()` stored the raw password.**
**Fix:** assert the call instead — `verify(passwordEncoder).encode("secret123")` — so a regression that drops encoding actually fails the test.

### 🟠 H3 — The H2 database file is committed and churns on every run
`backend/data/projectdb.mv.db` is tracked in git and is rewritten by every `mvn spring-boot:run`, file-DB test, and Newman run (currently dirty: `36KB → 61KB`). The `.gitignore` entry doesn't help because the file is already tracked.
**Impact:** noisy binary diffs and accidental commits of user/job/test data.
**Fix:** `git rm --cached backend/data/projectdb.mv.db` (keep it gitignored).

### 🟡 H4 — Fragile in-memory DB selection across `@SpringBootTest` classes
`SecurityIntegrationTest` and `KnownBugsWebTest` pick the in-memory DB by setting the `db.url` system property in a `static` block. Spring caches **one** context across both, while the repositories read `db.url` **per call**. They only work because both static blocks set the *same* URL (`jdbc:h2:mem:springboottest`).
**Trap:** add a third `@SpringBootTest` with a different `db.url` and it silently breaks — the cached context's tables live in the first DB while requests hit the second → `RuntimeException: Failed to create job` (the exact error hit while building this suite).
**Fix:** centralize the test DB URL in one shared base class/constant and document the invariant.

### 🟡 H5 — `H2TestSupport` leaks the `db.url` system property (no teardown)
`H2TestSupport` sets the global `db.url` in `@BeforeEach` but never restores it, and each test uses a fresh `mem:…;DB_CLOSE_DELAY=-1` database that is retained for the JVM's lifetime.
**Impact:** after repository tests run, `db.url` stays pointed at a random throwaway DB; any later same-JVM code expecting the default *file* DB would silently read an empty stale in-memory DB. Harmless today (nothing depends on the file DB in tests), but a latent cross-test-pollution trap; also accumulates ~25+ in-memory DBs per run.
**Fix:** clear/restore the property in `@AfterEach`.

> Note: the one **production** change made for testability — `Database.getConnection()` reading `db.url`/`db.user`/`db.password` system properties — is correct and preserves the original file-DB defaults exactly. No app-behavior regression.

---

## 9c. Frontend findings (from code review, 2026-06-10)

Reviewed `frontend/design3/`. Good news first: XSS is handled where it matters — `jobs.html`, `job-detail.html`, `chat.html` escape API/user data with `escapeHtml`; other pages use `textContent`/DOM building.

### ✅ F1 — ~~Navbar shell uses the wrong `localStorage` keys~~ — FIXED (verified 2026-06-12)
`index.html:122` now reads `localStorage.getItem('designer_jobs_token')` and logout (`147-149`) removes all three `designer_jobs_*` keys, so navbar auth state and Logout work. *(Original bug: the shell read `'token'`/`'userId'`/`'role'` while `auth.js` writes the `designer_jobs_*` keys, so the navbar never updated and Logout left the real session intact.)*

### 🟠 F2 — Open redirect / `javascript:` XSS via the login `next` param
`login.html:179` does `window.location.href = next || "homepage.html"` with `next` taken unvalidated from the query string. `login.html?next=https://evil.com` redirects off-site after login; `login.html?next=javascript:…` executes script in the app origin (and can read the localStorage token). *Fix: accept only a relative path — reject values containing `:` or starting with `//`.*

### 🟠 F3 — FE PUT/DELETE — profile done, job CRUD still missing (M7 now met) — updated 2026-06-12
`profile-edit.html` now issues `PUT /auth/me` (Save) and `DELETE /auth/me` (Delete) — commits `826d09c`/`20d3429` — so the FE exercises GET+POST+PUT+DELETE and **M7 is satisfied**. Remaining gap: there is still no UI to edit (PUT) or delete (DELETE) a **job** (`PUT`/`DELETE /jobs/{id}` exist server-side since B3 but are unused by the FE). *Fix: add job edit/delete actions wired to `PUT`/`DELETE /jobs/{id}`. (New issues introduced by the profile wiring: see F13 and §9 B23/B24.)*

### 🟡 F4 — World-clock renders external data unescaped
`homepage.html:168` and `login.html:223` interpolate `entry.city`/`entry.time` straight into `innerHTML`. Low risk (server-fixed city, trusted upstream) but it's the lone network-fed `innerHTML` sink without escaping. *Fix: use `textContent`/`escapeHtml` for consistency.*

### 🟡 F5 — Some pages bypass `Auth.authFetch` — widened 2026-06-12
`jobs.html:224`, `job-detail.html:259+` read the token manually and call raw `fetch`, so they miss the centralized 401/expiry → login redirect that `Auth.authFetch` provides. **The new `profile-edit.html` does the same at 3 sites** (load/save/delete), with a comment that admits dodging `Auth.authFetch` — so a missing token sends `Bearer null`. Inconsistent session handling + duplicated logic. *Fix: route protected calls through `Auth.authFetch`.*

### ⚪ F6 — JWT stored in `localStorage` (XSS-exposed)
Any script on the origin can read `designer_jobs_token`; combined with F2 it's directly exploitable. Acceptable for a student project but worth noting; httpOnly cookies would harden it. (Already flagged architecturally in §7.7.)

### New frontend findings (added 2026-06-12 from the scheduled frontend review + the profile commits)

> The 2026-06-12 CODE_REVIEW report uses its own F1–F16 labels; these canonical IDs continue from F6. Mapping is noted per item.

### 🔴 F7 — Search filter values never match stored job values (report F4)
Three mismatches under the backend's `LOWER(x)=LOWER(?)` match: budget `"3"→"large"` (`search-results.js:118`) vs stored `"big"` (`post-a-job.html`); `advanced-search.html` sends `"onsite"` vs stored `"on site"`; discipline codes `"web"`/`"ux"` vs stored categories `"Web Design"`/`"UI/UX Design"`. Any of these three facets returns **"No Results Found"** for jobs that exist. *Fix: align the option values with what `post-a-job.html` stores (or normalize server-side).*

### 🔴 F8 — Shell `message` listener has no `event.origin` check (report F14)
`index.html:203` handles `{type:"auth-changed"}` from **any** origin and, when `event.data.page` is present, calls `navigate(page)` which sets `frame.src`. Compounded with F4 (unescaped world-clock `innerHTML`), an injected city name can `postMessage({type:"auth-changed", page:"https://evil/"})` to the shell and point the iframe at an attacker URL while the address bar still shows `localhost`. *Fix: `if (event.origin !== window.location.origin) return;` at the top of the listener.*

### 🟡 F9 — `index.html` pushState hash never restored on load (report F8)
`navigate()` does `history.pushState({page}, '', '#'+page)` (`index.html:157`) but nothing reads `location.hash` on load, so refresh/deep-link always shows the default homepage. *Fix: on load, restore the iframe `src` from `location.hash`.*

### 🟡 F10 — Chat never paginates; only the oldest 50 render (report F9; FE half of B9)
`chat.html:498` `loadMessages` fetches `/conversations/{id}/messages` with no `?page=`; the backend returns the oldest 50 ASC, so in a long thread the newest messages are unreachable. *Fix: page or scroll-to-load (pairs with backend B9).*

### 🟡 F11 — `register.html` doesn't post `auth-changed` after auto-login (report F10)
After register + auto-login, `register.html:171` navigates without `window.parent.postMessage({type:"auth-changed"},"*")`, so the shell navbar stays on Login/Register until a manual refresh (`login.html` does this correctly). *Fix: post `auth-changed` before navigating.*

### 🟡 F12 — `loadCities` leaves the city `<select>` permanently disabled (report F11)
`profile-edit.html` sets `citySelect.disabled = true` before the `!country` early-return and before the catch block; neither re-enables it. Clearing the country (or a failed cities API call) strands the select disabled. *Fix: re-enable on the early-return/catch paths.*

### 🟠 F13 — Delete Profile leaves stale identity + doesn't notify the shell (report F13)
On successful `DELETE /auth/me`, `profile-edit.html:350` removes only `designer_jobs_token`; `designer_jobs_userId`/`_role` persist and no `auth-changed` is posted, so `Auth.getUserId()/getRole()` still return the deleted account for in-flight callbacks and the navbar lags until full reload. *Fix: `Auth.clearSession()` + post `auth-changed` before navigating.*

### 🟠 F14 — `search-results` sidebar filters are dead; multi-discipline truncated (report F15)
The sidebar filter tiles have no `name`/form/handlers (clicking does nothing), and `search-results.js:11` uses `pageParams.get("discipline")`, so `?discipline=web&discipline=ux` keeps only the first value. *Fix: wire the tiles; use `URLSearchParams.getAll()`.*

### 🟠 F15 — `search-results.html` ships 7 static placeholder cards linking to `job-random.html` (report F16)
Hardcoded mock cards (`search-results.html:269-471`) are visible before `DOMContentLoaded` — or permanently if `search-results.js` throws before `resultsContainer.innerHTML=""` (`:31`) — and their "View Job" buttons point at `job-random.html`, not real detail pages. *Fix: delete the static cards; the JS already renders results + empty state.*

### S3 — W3C validation results (validator.w3.org/nu, all 15 pages)

**Not met — 6 pages have errors (~17 total).** Fixes are quick:

| Page | Errors | Issue |
|---|---|---|
| `post-a-job.html` | 6 | `autocomplete` on inputs whose `type` doesn't allow it |
| `profile-edit.html` | 6 | same `autocomplete` misuse |
| `register.html` | 2 | same `autocomplete` misuse |
| `index.html` | 1 | stray `</script>` end tag |
| `search-results.html` | 1 | `aria-label` on a `div` with no `role` |
| `advanced-search.html` | 1 | `label[for]` points at a hidden/missing control |

Clean (0 errors): `login`, `profile`, `chat`, `job-random`, `homepage`, `jobs`, `job-detail` (last three have a minor warning). `about`/`impressum` warn only (Lorem-ipsum vs `lang="en"`). *Fix these 6 pages to claim S3's points.*

---

## 9d. Second-FE / S2 evaluation — branch `kat-second-frontend` (2026-06-12)

Kat's admin dashboard (`frontend/admin/dashboard.html` + `dashboard.js` + `dashboard.css`), the S2 candidate. **Not yet merged to `main`** — evaluated on the branch. Backend wiring added on the branch: `SecurityConfig` permits `/admin/**`, `WebConfig` adds an `/admin/**` resource handler (`→ ../frontend/admin/`) and fixed the `design1`→`design3` default path.

### S2 requirement: 🟠 not yet met
S2 needs **≥3 BE endpoints**. The dashboard calls **2** — `GET /jobs` (real) and `GET /designers` (still a 501 stub → demo-row fallback). The action buttons (`editJob`/`deleteJob`/`editUser`/`banUser`) are `alert()` stubs, not endpoint calls. *To pass: wire a third real endpoint (e.g. delete → `DELETE /jobs/{id}`, or implement `UserController.listDesigners` so `/designers` counts).*

### Bugs found (branch state)
- **Was completely broken** — `dashboard.js` had a missing closing brace in `banUser` → the whole file failed to parse, so neither table loaded. (Fixed on the branch.)
- **`/designers` 501 mishandled** — `loadUsersFromServer` threw on `!response.ok` before reaching the `not_implemented` fallback, so the user table showed "Fehler beim Laden der Benutzerdaten" (501 must be let through first). (Fixed on the branch.)
- **Stylesheet path** — `../design3/theme.css` resolves to `/design3/theme.css` → **401** (design3 is served at the web root); must be `/theme.css`. This only works when the page is loaded via **Spring on `:8080`** — opening it from IntelliJ's built-in server / `file://` yields wrong-MIME / 404 blocks. (Path-vs-server-root was the root cause of the "rejected"/MIME confusion.)
- **Framing** — the shell footer linked the dashboard with `data-page="/admin/dashboard.html"`, which loads it **inside the content iframe**; with `X-Frame-Options: SAMEORIGIN` that's blocked cross-origin (and 404s on the wrong origin). A standalone admin page should be a top-level link, not a `data-page` iframe swap.
- **No auth gate** — `/admin/**` static files are public and the page pulls in neither `auth.js` nor a role check, so anyone can open the dashboard. There's no `ADMIN` role yet; gate on authentication now and on the role once it exists.
- **CORS regressed wider (B17)** — the branch *widened* `WebConfig.addCorsMappings` (added `localhost:8080`) instead of removing it; that second CORS source still conflicts with the central `SecurityConfig` one. Same-origin on `:8080` means the dashboard needs no CORS at all.
- Minor: inline `onclick="…('${id}')"` injects unescaped ids; `bi-exclame-triangle` icon typo; stat cards only partially wired (`4c9a1d4` "Job counter works"); empty/misnamed placeholder files (`JobsVerwalte.js` etc.).

### Verdict
A solid scaffold that now loads on `:8080`, but **S2 is not satisfied** (2 endpoints, one a stub) and it carries the CORS/auth/framing issues above. Don't merge to `main` until it hits ≥3 real endpoints and drops the duplicate `WebConfig` CORS.

---

## 10. Remaining work to "finish" (the 6-day plan)

Ordering is driven by **grading points first** (see the ⭐ requirements section), then security/correctness, then polish. Items reference both bug IDs (§9) and requirement IDs (M/S/C).

### Tier 0 — secure the required 21 points (MUST gaps — do these first)
1. ✅ ~~**B3 + M6 + M7**~~ → **DONE:** `DELETE /jobs/{id}` + FE delete button (M6 ✅); `PUT /jobs/{id}` with ownership check, body cannot set `clientId`/`createdAt` (B3 ✅); delete authz tightened (B22 ✅); **M7 closed 2026-06-12** — `profile-edit.html` issues `PUT`/`DELETE /auth/me`. *(Optional polish: a job edit/delete UI — see F3.)*
2. **Verify M4/M5/M9 stay intact** while editing (AJAX, JSON, JWT) — they're met today; don't regress them.

### Tier 1 — security & cheap-but-broken correctness
3. ✅ ~~**B1 / B5 — normalize email**~~ — **DONE 2026-06-11** (`219e278`). `b1`/`b5` green.
4. ✅ ~~**B2 — fix authorization** in `ApplicationController`~~ — **DONE 2026-06-11**: list/status/hire owner-only, get owner-or-applicant; `b2` green + unit tests.
5. ✅ ~~**B4 — `GET /jobs/random`**~~ — **RESOLVED 2026-06-11**: client-side approach accepted as final; `getRandomJob()` removed, `b4` test retired.
6. ✅ ~~**B7 — referential validation + uniqueness**~~ — **MOSTLY DONE 2026-06-11** (`a72592d`): unique `(job_id, designer_id)` + clean 404/409 in apply + idempotent conversation create. `b7` green. Remaining: job/user existence checks in `createConversation` (fold into **B14**).

> Tracking: every fix above flips one red test on the §5 board of `test.md` to green. **Status 2026-06-11 (eod): board complete — `b3` green, `b4` retired (client-side by design); `mvn test` = BUILD SUCCESS, 115 tests, 0 failures.**

### Tier 2 — chase the SHOULD points (8)
7. ✅ ~~**S1 — add a second external REST service.**~~ — **DONE 2026-06-10** (`371b55f`): `countriesnow.space` via `location/` + profile-edit autofill. Pairs toward **C1** (one more API needed).
8. **S2 — a second FE component** hitting ≥3 BE endpoints (e.g. a moderation dashboard or a public designer-portfolio page).
9. **S3 — run every FE page through `validator.w3.org`** and fix HTML errors.
10. **S4 — confirm/finish responsive** mobile + desktop views; document the breakpoints.
11. **`user/` package — designer profiles & portfolio** (`GET /designers`, `GET /designers/{id}`, then `PUT` + portfolio CRUD). Feeds S2 and turns the §6 stub tests green.

### Tier 3 — reach for the COULD points (5) + remaining polish
12. **C1 — third external REST service** (after S1).
13. ✅ ~~**C2 — XML output** alongside JSON~~ — **DONE 2026-06-11** (`jackson-dataformat-xml` + `produces` on the job reads; `ContentNegotiationTest`).
14. **C3 — a `PATCH` endpoint** (partial job/profile update) consumed by the FE.
15. **B6 — contract generation on hire**; **B11 — global `@ControllerAdvice`**; **B9 — newest-first message pagination**; **B8 — sync package READMEs**; minimal **moderation/**.

### Newly surfaced by the deep reviews — slot into the tiers above
- **Tier 0 (required points):** ~~**F1** navbar localStorage keys~~ ✅ fixed; ~~**F3/M7** FE PUT action~~ ✅ met 2026-06-12 (profile `PUT`/`DELETE /auth/me`). Tier 0 is clear — but the profile wiring added **F8** (shell postMessage origin), **F13**, and backend **B23/B24** to address.
- **Tier 1 (security):** **B14** conversation-spoofing check in `ChatService` *(also closes the B7 leftover)*; ~~**B22** tighten `DELETE /jobs/{id}` authz~~ ✅ done 2026-06-11; **F2** validate the login `next` param (open-redirect / `javascript:` XSS).
- **Tier 1–2 (robustness/correctness):** **B15** add HTTP timeouts to `ExternalTimeApiClient`; **B16** fix lexicographic timestamp ordering; **B17** delete the duplicate CORS config in `WebConfig`; **B19** make the hire/status transition atomic; **B20** fix the `design1/` path default.
- **Tier 2 (SHOULD points):** **S3** fix the 6 W3C-failing pages (mostly stray `</script>` + `autocomplete`/`aria`/`label` attributes — see §9c).
- **Build hygiene:** **H1** add a coverage profile so `mvn test` still emits JaCoCo while the red board is red; **H3** `git rm --cached` the H2 DB file.

### Explicitly out of scope (say so to graders)
- External chat server (`ExternalChatApiClient` stays a placeholder, `USE_EXTERNAL_CHAT_API=false`).
- Token revocation / refresh tokens.
- Real database (staying on embedded H2).

---

## 11. Quick reference card

| Thing | Value |
|---|---|
| Run | `cd backend && mvn spring-boot:run` → `localhost:8080` |
| Build | `mvn package` |
| Reset DB | delete `backend/data/projectdb.mv.db`, restart |
| DB | H2 file, `jdbc:h2:file:./data/projectdb`, user `sa`, no pw |
| Auth | JWT HS256, header `Authorization: Bearer <token>`, 2h expiry |
| Token store (frontend) | `localStorage`: `designer_jobs_token` / `_userId` / `_role` |
| Secret override | env `APP_JWT_SECRET` (≥32 chars) |
| Identity in a controller | `auth.getName()` = userId, `auth.getAuthorities()` = `ROLE_<role>` |
| Add a public endpoint | add matcher to `SecurityConfig` **then** write controller |
| Tests | `mvn test` (toolchains picks JDK 17 — needs `~/.m2/toolchains.xml`, see backend README) — JUnit suite + Postman/Newman in `postman/`; intentionally red: **2 left (`b3`, `b4`)**. See `test.md` |

---

*Built by reading the actual source on 2026-06-10; updated 2026-06-11 after the B1/B2/B5/B7 fixes and the delete-endpoint / search / random-job / locations work landed. Where this document and a package README disagree, the code wins — re-verify before relying on either.*