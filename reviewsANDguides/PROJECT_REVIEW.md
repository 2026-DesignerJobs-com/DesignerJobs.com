# DesignerJobs.com — Project Review & Walkthrough

> A shared reference for our 6-day final push. It explains the whole project to **any** audience — from someone who has never seen Spring Boot, to a senior dev joining the review — with deep dives on **Spring Boot usage** and **session/auth management**, plus an honest list of **bugs, gaps, and remaining work**.

> **Update 2026-06-11:** Big day. Fixed: **B1, B5** (email casing), **B2** (partially — `listApplications` ownership), **B7** (mostly — unique applications, clean 409, conversation race). New since yesterday: **`DELETE /jobs/{id}`** + FE delete button (closes **M6**, half of M7), **search** wired end-to-end, **job-random/job-detail pages fixed** (B4 symptom gone), and a **second external REST API** (countriesnow.space → closes **S1**) with location autofill in profile-edit. `mvn test` now auto-selects JDK 17 via Maven toolchains. The red TDD board is down to **2 tests: b3 (PUT /jobs) and b4 (GET /jobs/random)**. Details inline below — each touched item is marked.

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
| **M7** | FE consumes GET, POST, PUT **and DELETE** from ≥1 endpoint | ⚠️ | **Half closed 2026-06-10:** FE now issues **DELETE** (delete-job button in `job-detail.html`, commit `f3d26e9`). **Still 0 PUT calls from the FE** — add e.g. edit job → `PUT /jobs/{id}` (needs B3) or accept application → `PUT /applications/{id}/status`. |
| **M8** | Consume ≥1 external REST service | ✅ | `ExternalTimeApiClient` → `timeapi.io` (`GET /world-clock`) |
| **M9** | Session management (Login/JWT) | ✅ | stateless JWT — see §7 |

> **Action for full 21 points:** only **M7** remains — the FE must issue one **PUT**. Cheapest path: an accept/reject button on the applicant list calling the existing `PUT /applications/{id}/status`. The nicer path: implement `PUT /jobs/{id}` (**B3**, the last red backend test) plus an edit-job form.

### SHOULD — 8 points

| # | Requirement | Status | Evidence / what's missing |
|---|---|---|---|
| **S1** | Consume ≥2 external REST services | ✅ | **Closed 2026-06-10:** `ExternalLocationApiClient` → `countriesnow.space` (`GET /locations/cities?country=…`, commit `371b55f`), consumed by the profile-edit location autofill. Plus `timeapi.io`. (`ExternalChatApiClient` remains a disabled placeholder.) |
| **S2** | A second FE component using ≥3 BE endpoints | ❌ | only one FE (`design3/`). Build a second small FE (e.g. an admin/moderation dashboard or a designer portfolio page) hitting ≥3 endpoints. |
| **S3** | FE is W3C compliant | ❌ | **Verified 2026-06-10 via validator.w3.org/nu — 6 pages fail** (~17 errors). See §9c for the list. Clean: login, profile, chat, job-random, homepage/jobs/job-detail (warnings only). |
| **S4** | FE responsive (mobile + desktop views) | ⚠️ | Bootstrap grid is responsive; confirm a **dedicated** mobile vs desktop view (breakpoints, nav collapse) and document it. |

### COULD — 5 points

| # | Requirement | Status | Evidence / what's missing |
|---|---|---|---|
| **C1** | Consume ≥3 external REST services | ❌ | needs three — **have two now** (`timeapi.io`, `countriesnow.space`). One more real API closes it. |
| **C2** | BE returns JSON **and** XML | ❌ | JSON only. Add XML via `jackson-dataformat-xml` + content negotiation (`produces = {JSON, XML}`). |
| **C3** | BE PATCH endpoint consumed by FE | ❌ | no `@PatchMapping` exists (the only "PATCH" in code is a CORS *allowed-method* entry in `SecurityConfig`, not an endpoint). Add a PATCH endpoint (e.g. partial job/profile update) and call it from the FE. |

### Points summary (honest self-assessment)

- **MUST (21):** 8 of 9 fully met *(M6 closed 2026-06-10)*; only **M7 at risk** → safe 21 once the FE issues one `PUT`.
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
> ⚠️ **`mvn test` is intentionally RED right now.** We work test-first: there is one failing test per open bug (the §5 board in `test.md`), so the build fails *on purpose* until the bugs are fixed. Don't "fix" it by deleting tests. *(Status 2026-06-11: down to **2 red** — `b3` PUT /jobs, `b4` GET /jobs/random. `b1`, `b2`, `b5`, `b7` are green.)* Caveat: because the red tests fail the `test` phase, the JaCoCo report isn't generated on a plain `mvn test` — see harness finding **H1** in §9b.

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
| `job/` | 🟠 Mostly | `POST /jobs`, `GET /jobs` (+ search params), `GET /jobs/{id}`, `DELETE /jobs/{id}` *(new 2026-06-10)* | Search wired end-to-end (`23d4dda`). **`PUT /jobs/{id}` still NOT implemented** (B3, last backend gap). Delete authz is loose — see **B22**. |
| `application/` | ✅ Done (review) | `POST /jobs/{jobId}/apply`, `GET /jobs/{jobId}/applications`, `GET /applications/{id}`, `PUT /applications/{id}/status`, `POST /applications/{id}/hire` | Hire is a stub-trigger (no contract yet). Apply now validates job existence + rejects duplicates (B7 ✅); list is owner-only (B2 partial) — get/status/hire still unchecked, see §9. |
| `location/` | ✅ Done *(new 2026-06-10)* | `GET /locations/countries`, `GET /locations/cities?country=…` | Countries hardcoded list; cities proxied from `countriesnow.space` (2nd external API → S1). Public via `SecurityConfig`. |
| `chat/` | ✅ Done | `GET/POST /conversations`, `GET/POST /conversations/{id}/messages` | Local H2 mode; external API path behind `USE_EXTERNAL_CHAT_API=false`. |
| `worldclock/` | ✅ Done | `GET /world-clock` | Demo proxy to timeapi.io. |
| `user/` | ❌ Stub | `/designers`, `/designers/{id}`, `/designers/{id}/portfolio…`, `/users/{id}` | **All return `501 not_implemented`.** Profiles + portfolio. |
| `contract/` | ❌ Stub | `/contracts…` | Phase 2. Hire flow has a `TODO` to call it. |
| `moderation/` | ❌ Stub | `/moderation…`, reports | Phase 2. |

---

## 9. Bugs & correctness issues (review these first)

Ordered by severity. Found by reading every backend source file (controllers, services, repositories) line-by-line and tracing call sites against the actual code (multiple `xhigh` review passes — backend, frontend, and test harness). **These are the concrete things to fix in the 6 days.** Numbering: **B1–B13** first pass, **B14–B21** the deep backend review (§9), **B22** new 2026-06-11, harness **H1–H5** in §9b, frontend **F1–F6** in §9c. Status markers: ✅ fixed · 🟠/🟡 partially fixed or downgraded · unmarked = still open.

### ✅ B1 — ~~Login is case-sensitive on email~~ — FIXED 2026-06-11 (`219e278`)
`login` now normalizes the email exactly like registration (`trim().toLowerCase()`) before `findByEmail`. Regression test `KnownBugsTest.b1` is green.
<details><summary>Original finding</summary>
`AuthController.register` stored `user.email = req.email.trim().toLowerCase()`, but `login` queried `findByEmail(req.email)` with the **raw** input. Someone registering as `John@Example.com` (stored lowercase) could never log in typing the same casing → `401 invalid email or password`.
</details>

### 🟠 B2 — Authorization gaps in the application/hire flow — PARTIALLY FIXED 2026-06-11 (`22c5c9b`)
In `ApplicationController`:
- ✅ `GET /jobs/{jobId}/applications` — **fixed:** loads the job, returns 404 if missing, 403 unless `job.clientId == auth.getName()`. Test `b2` green + unit tests in `ApplicationControllerTest`.
- ❌ `GET /applications/{id}` — **still open:** no ownership check.
- ❌ `PUT /applications/{id}/status` and `POST /applications/{id}/hire` — **still open:** any authenticated user can accept/reject/hire on any application.

Remaining fix: same pattern (load application → load its job → compare `job.clientId` against `auth.getName()` → 403). The `JobRepository` is already injected into the controller now, so it's a small change.

### 🟠 B3 — `PUT`/`DELETE /jobs/{id}` advertised but unreachable — HALF FIXED 2026-06-10 (`42fb250`)
- ✅ **`DELETE /jobs/{id}` now exists** (auth + 404 + authz check) and the FE calls it (delete button in `job-detail.html`, `f3d26e9`). Closes **M6**. *But see **B22** — the delete authz rule is too loose.*
- ❌ **`PUT /jobs/{id}` still missing** — `JobController` has no `@PutMapping`; `JobRepository.update()` remains dead code; a client cannot edit a posted job. Test `b3` is red. Caution for whoever wires it up: `JobRepository.update()` overwrites `client_id` and `created_at` from the request body — add an `auth.getName() == job.clientId` ownership check and don't let the body set `clientId`/`createdAt`.

### 🟡 B4 — Random-job page broken — SYMPTOM FIXED 2026-06-10 (`7ddc891`), backend route still missing
The user-facing bug is gone: `job-random.html` was rewritten from a hardcoded fake job to fetching `GET /jobs` and picking a random one **client-side**. The same commit pointed the search-result "View Job" buttons at the real `job-detail.html?id=…` page.
**Still open (and why test `b4` is red):** there is **no `GET /jobs/random` backend route** — a request to `/jobs/random` is still swallowed by `@GetMapping("/{id}")` with `id="random"` → 404, and `JobRepository.getRandomJob()` remains dead code. Team decision needed: either add the route **above** the `/{id}` mapping (ordering matters) and have the page use it, or accept the client-side approach and retire `getRandomJob()` + re-scope the `b4` test.

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

### 🟡 B18 — `/world-clock` is all-or-nothing and sequential
`WorldClockService` makes 4 blocking external calls in sequence and lets any single failure throw, so one slow/failing city fails the whole endpoint and latency is the sum of 4 round-trips. *Fix: fetch in parallel and degrade gracefully per city.*

### 🟡 B19 — Hire/status transition has a TOCTOU
`ApplicationController.hire`/`updateStatus` check the current status in Java then `UPDATE` unconditionally. Concurrent requests can both pass the check and double-process — and once hire generates a contract (B6), produce two contracts. *Fix: conditional `UPDATE … WHERE id=? AND status=?` and check affected rows.*

### 🟡 B20 — `app.frontend.path` default points at stale `design1/`
`WebConfig`'s `@Value("${app.frontend.path:../frontend/design1/}")` defaults to a directory that doesn't exist (live FE is `design3/`). Only works because `application.properties` overrides it; any environment missing that property serves 404s. *Fix: make the default `../frontend/design3/`.*

### ⚪ B21 — No connection pooling
`Database.getConnection()` opens a fresh `DriverManager` connection per repository call (no pool). Functionally OK for embedded H2 but wasteful and unbounded under concurrency. *Fix: a pooled `DataSource` (HikariCP).* *(Efficiency/altitude, not a correctness bug.)*

### 🟠 B22 — *(new 2026-06-11)* `DELETE /jobs/{id}` lets **any designer** delete **any job**
The new delete endpoint (`42fb250`) authorizes `isOwnerClient **|| isDesigner**` — i.e. besides the owning client, *every* logged-in designer may delete *every* job, including jobs they have no relation to. The Javadoc says this is intentional ("A logged-in designer may also delete it"), but it contradicts the ownership invariant used everywhere else (§7.5) and is effectively a destructive-action privilege for a whole role. *Fix: drop the `isDesigner` branch — only `job.clientId == auth.getName()` (and maybe a future admin role) should delete.*

---

## 9b. Test & build harness findings (from code review)

These are **not** application bugs — they're issues in the test suite, build config, and repo hygiene introduced/uncovered while adding the test harness. Listed because they undermine the safety net itself. (Verified 2026-06-10.)

### 🔴 H1 — `mvn test` no longer produces a JaCoCo coverage report
The JaCoCo `report` goal is bound to the `test` phase, but the intentional red TDD tests (the §5 bug board in `test.md`) make Surefire fail and **abort the phase before `jacoco:report` runs**. Verified: `mvn test` exits `1`, and `target/site/jacoco/jacoco.csv` is never regenerated.
**Impact:** the coverage workflow documented in `test.md` ("open `target/site/jacoco/index.html`") silently produces nothing for as long as the board is red — i.e. always, until the bugs are fixed.
**Fix:** generate coverage with `mvn test -Dmaven.test.failure.ignore=true`, or bind `jacoco:report` to the `verify` phase, or add a dedicated coverage profile. Document the chosen command in `test.md`.

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

### 🔴 F1 — Navbar shell uses the wrong `localStorage` keys
`index.html` (the iframe shell that renders navbar + footer) reads `localStorage.getItem('token')` and on logout removes `'token'`/`'userId'`/`'role'` — but the whole app stores the session under `designer_jobs_token`/`_userId`/`_role` (via `auth.js`). The `auth-changed` `postMessage` wiring is correct (listener at `index.html:204`), but `updateAuthNavigation` always reads `null`, so:
- after login the navbar keeps showing **Login/Register** and hides **Profile/Logout**;
- the Logout button removes non-existent keys, leaving the real session intact (user *looks* logged out but isn't).
*Fix: use the `designer_jobs_*` keys (or better, call `window.Auth`).*

### 🟠 F2 — Open redirect / `javascript:` XSS via the login `next` param
`login.html:179` does `window.location.href = next || "homepage.html"` with `next` taken unvalidated from the query string. `login.html?next=https://evil.com` redirects off-site after login; `login.html?next=javascript:…` executes script in the app origin (and can read the localStorage token). *Fix: accept only a relative path — reject values containing `:` or starting with `//`.*

### 🟠 F3 — FE never issues PUT or DELETE (fails M7; CRUD half-built)
Across all pages only `POST` + implicit `GET` are used (grep: 7 × `method:"POST"`). There is no UI to edit (PUT) or delete (DELETE) a job, nor to update a profile via PUT. This **fails required requirement M7** and is a real functional gap once the BE endpoints exist (B3). *Fix: add edit/delete actions wired to `PUT`/`DELETE /jobs/{id}` and `PUT /designers/{id}`.*

### 🟡 F4 — World-clock renders external data unescaped
`homepage.html:168` and `login.html:223` interpolate `entry.city`/`entry.time` straight into `innerHTML`. Low risk (server-fixed city, trusted upstream) but it's the lone network-fed `innerHTML` sink without escaping. *Fix: use `textContent`/`escapeHtml` for consistency.*

### 🟡 F5 — Some pages bypass `Auth.authFetch`
`jobs.html:224`, `job-detail.html:250` read the token manually and call raw `fetch`, so they miss the centralized 401/expiry → login redirect that `Auth.authFetch` provides. Inconsistent session handling + duplicated logic. *Fix: route protected calls through `Auth.authFetch`.*

### ⚪ F6 — JWT stored in `localStorage` (XSS-exposed)
Any script on the origin can read `designer_jobs_token`; combined with F2 it's directly exploitable. Acceptable for a student project but worth noting; httpOnly cookies would harden it. (Already flagged architecturally in §7.7.)

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

## 10. Remaining work to "finish" (the 6-day plan)

Ordering is driven by **grading points first** (see the ⭐ requirements section), then security/correctness, then polish. Items reference both bug IDs (§9) and requirement IDs (M/S/C).

### Tier 0 — secure the required 21 points (MUST gaps — do these first)
1. ~~**B3 + M6 + M7**~~ → **HALF DONE 2026-06-10:** `DELETE /jobs/{id}` + FE delete button shipped (M6 ✅, DELETE half of M7 ✅). **Remaining: `PUT /jobs/{id}`** through `JobController` with an ownership check (don't let the body set `clientId`/`createdAt`) **plus one FE PUT call** (edit-job form, or cheaper: accept/reject button → `PUT /applications/{id}/status`). Also tighten the delete authz (**B22**).
2. **Verify M4/M5/M9 stay intact** while editing (AJAX, JSON, JWT) — they're met today; don't regress them.

### Tier 1 — security & cheap-but-broken correctness
3. ✅ ~~**B1 / B5 — normalize email**~~ — **DONE 2026-06-11** (`219e278`). `b1`/`b5` green.
4. 🟠 **B2 — fix authorization** in `ApplicationController`. **Partially done 2026-06-11** (`22c5c9b`): `listApplications` is owner-only, `b2` green. **Remaining: get/status/hire** still lack ownership checks — same pattern, the `JobRepository` is already injected.
5. 🟡 **B4 — `GET /jobs/random`.** Page works since 2026-06-10 (client-side random, `7ddc891`); the backend route is still missing and `b4` is red. Decide: add the route (~5 lines, above `/{id}`) or re-scope the test.
6. ✅ ~~**B7 — referential validation + uniqueness**~~ — **MOSTLY DONE 2026-06-11** (`a72592d`): unique `(job_id, designer_id)` + clean 404/409 in apply + idempotent conversation create. `b7` green. Remaining: job/user existence checks in `createConversation` (fold into **B14**).

> Tracking: every fix above flips one red test on the §5 board of `test.md` to green. **Status 2026-06-11: 2 red left (`b3`, `b4`).** Project is "done" (correctness-wise) when that board is all green.

### Tier 2 — chase the SHOULD points (8)
7. ✅ ~~**S1 — add a second external REST service.**~~ — **DONE 2026-06-10** (`371b55f`): `countriesnow.space` via `location/` + profile-edit autofill. Pairs toward **C1** (one more API needed).
8. **S2 — a second FE component** hitting ≥3 BE endpoints (e.g. a moderation dashboard or a public designer-portfolio page).
9. **S3 — run every FE page through `validator.w3.org`** and fix HTML errors.
10. **S4 — confirm/finish responsive** mobile + desktop views; document the breakpoints.
11. **`user/` package — designer profiles & portfolio** (`GET /designers`, `GET /designers/{id}`, then `PUT` + portfolio CRUD). Feeds S2 and turns the §6 stub tests green.

### Tier 3 — reach for the COULD points (5) + remaining polish
12. **C1 — third external REST service** (after S1).
13. **C2 — XML output** alongside JSON (`jackson-dataformat-xml` + `produces`).
14. **C3 — a `PATCH` endpoint** (partial job/profile update) consumed by the FE.
15. **B6 — contract generation on hire**; **B11 — global `@ControllerAdvice`**; **B9 — newest-first message pagination**; **B8 — sync package READMEs**; minimal **moderation/**.

### Newly surfaced by the deep reviews — slot into the tiers above
- **Tier 0 (required points):** **F1** fix the `index.html` navbar localStorage keys (tiny, but the whole logged-in/logout UX is broken right now); **F3/M7** add the FE PUT action *(DELETE done 2026-06-10)*.
- **Tier 1 (security):** **B14** conversation-spoofing check in `ChatService` *(also closes the B7 leftover)*; **B22** tighten `DELETE /jobs/{id}` authz *(new)*; **F2** validate the login `next` param (open-redirect / `javascript:` XSS).
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