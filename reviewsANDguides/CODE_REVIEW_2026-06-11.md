# Code review — 2026-06-11 (xhigh, full backend + one frontend scan)

Process: 10 finder angles → ~70 candidates → dedup → verified (agents + manual reads; two H2 claims verified empirically on H2 2.3.232) → gap sweep. Top 15 below; verified-but-cut and cleanup backlog follow.

## Status addendum — 2026-06-11 (eod)

Fixed since this review ran (each as its own commit, with tests):

- **Findings 1–4** (ownership): `PUT /applications/{id}/status` and `POST /applications/{id}/hire` are owner-only, `GET /applications/{id}` is owner-or-applicant, `DELETE /jobs/{id}` lost the `|| isDesigner` branch.
- **Finding 10** (`mvn package` always fails): resolved — b3 fixed by the new owner-only `PUT /jobs/{id}` (server-side `clientId`/`createdAt`), b4 retired by team decision (random-job is client-side by design; `getRandomJob()` removed). Suite: **115 tests, BUILD SUCCESS**.
- From "verified but cut": **SecurityConfig blanket `GET /jobs/**`** narrowed to `/jobs` + `/jobs/*`, with security-matrix tests.

Also shipped: **C2** — JSON+XML content negotiation (`jackson-dataformat-xml`, `produces` on the job reads, `ContentNegotiationTest`).

New finding from `backend/logs/app.log` (post-review): `config/RequestLoggingConfig`'s `CommonsRequestLoggingFilter` (includeHeaders + includePayload) plus `logging.file.name=logs/app.log` writes **plaintext register/login passwords (11×) and full bearer JWTs** to disk. Gitignored (`*.log`), so local-only — fix by excluding the `Authorization` header and `/auth/**` payloads. Tracked as **H6** in `PROJECT_REVIEW.md` §9b.

Top remaining (re-ranked): finding 5 (POST /jobs role check + client-supplied `id`/`createdAt`), 6 (login `next` XSS/redirect = F2), 7 (chat counterparty validation = B14), 8 (chat shows only oldest 50 messages), 9 (shell localStorage keys = F1), 11–15, then the cut list below.

## Top 15 findings (ranked)

```json
[
  {"file": "backend/src/main/java/at/ac/fhcampuswien/application/ApplicationController.java", "line": 122, "summary": "PUT /applications/{id}/status has no job-ownership check — any authenticated user (including the applicant) can ACCEPT/REJECT any application.", "failure_scenario": "Designer applies to client C's job, then PUTs {\"status\":\"ACCEPTED\"} on their own application id with their own token → 200 OK, self-accepted without C's involvement."},
  {"file": "backend/src/main/java/at/ac/fhcampuswien/application/ApplicationController.java", "line": 163, "summary": "POST /applications/{id}/hire has no job-ownership check — any authenticated user can move an ACCEPTED application to HIRED.", "failure_scenario": "After self-accepting (finding 1), the same designer POSTs /applications/{id}/hire → status HIRED; once contract generation lands (TODO at line 191), contracts are created without the job owner acting."},
  {"file": "backend/src/main/java/at/ac/fhcampuswien/job/JobController.java", "line": 130, "summary": "deleteJob authorizes `isOwnerClient || isDesigner`, so any DESIGNER account can delete any client's job.", "failure_scenario": "Register a DESIGNER account, enumerate ids via public GET /jobs, DELETE /jobs/{id} for each → every listing removed with 200 responses; no FKs, so applications/conversations for those jobs are orphaned."},
  {"file": "backend/src/main/java/at/ac/fhcampuswien/application/ApplicationController.java", "line": 110, "summary": "GET /applications/{id} returns any application to any authenticated user — IDOR; no applicant-or-owner check, although listApplications (line 87) does enforce ownership.", "failure_scenario": "Any logged-in user requests GET /applications/{uuid} for another user's application → 200 with coverLetter, designerId, status."},
  {"file": "backend/src/main/java/at/ac/fhcampuswien/job/JobController.java", "line": 40, "summary": "POST /jobs has no CLIENT-role check, and JobRepository.create (JobRepository.java:55-65) honors client-supplied id and createdAt instead of always generating server-side.", "failure_scenario": "Any DESIGNER token can create listings; a body with an existing \"id\" hits the PRIMARY KEY → RuntimeException → 500; a forged createdAt sorts the job to the top of the created_at DESC listing."},
  {"file": "frontend/design3/login.html", "line": 120, "summary": "The `next` query param flows unvalidated into window.location.replace (line 120) and window.location.href (line 179) — javascript: URI execution / open redirect after login.", "failure_scenario": "login.html?next=javascript:... executes in-page (with localStorage JWT in scope) immediately if already logged in, or right after a successful login."},
  {"file": "backend/src/main/java/at/ac/fhcampuswien/chat/ChatService.java", "line": 42, "summary": "createConversation only checks the caller is one participant; it never validates the other participant exists, the jobId is real, or the clientId owns that job.", "failure_scenario": "POST /conversations with {clientId: self, designerId: \"no-such-user\", jobId: \"fake\"} → 201; arbitrary users can be bound into conversations about any/nonexistent jobs (chat.html:176 even suggests free-text job ids)."},
  {"file": "backend/src/main/java/at/ac/fhcampuswien/chat/MessageRepository.java", "line": 88, "summary": "Messages are paged ORDER BY created_at ASC LIMIT 50, ChatController defaults page=0, and chat.html loadMessages (chat.html:498) never passes a page — past 50 messages, new ones never render.", "failure_scenario": "51st message in a conversation: POST returns 201 but the refresh fetch returns the oldest 50 only; the just-sent message (offset ≥ 50) is permanently invisible in the UI."},
  {"file": "frontend/design3/index.html", "line": 122, "summary": "The iframe shell reads/clears localStorage keys 'token'/'userId'/'role' while auth.js writes designer_jobs_token/_userId/_role — navbar auth state never updates and shell Logout (lines 148-150) leaves the real session intact.", "failure_scenario": "User logs in → updateAuthNavigation checks localStorage.getItem('token') → null → Login/Register stay visible, Profile/Logout stay hidden; Logout (if shown) removes nonexistent keys, user stays authenticated."},
  {"file": "backend/src/test/java/at/ac/fhcampuswien/bugs/KnownBugsWebTest.java", "line": 57, "summary": "Deliberately-red TDD tests (b4 expects GET /jobs/random → 200; b3 expects PUT /jobs/{id} → 200) run in the default surefire phase with no @Disabled/@Tag/skip config — `mvn package` can never succeed.", "failure_scenario": "cd backend && mvn package → /jobs/random 404s (captured by /{id}) and PUT 405s (no @PutMapping) → test failures → BUILD FAILURE; no jar despite healthy app code."},
  {"file": "backend/src/main/java/at/ac/fhcampuswien/chat/MessageRepository.java", "line": 31, "summary": "created_at is VARCHAR holding Instant.toString() (variable fractional-second precision) sorted lexicographically — temporally later strings can sort earlier ('Z' 0x5A > '.' 0x2E). Same pattern in jobs and conversations ordering.", "failure_scenario": "Empirically verified on H2 2.3.232: '...10:00:00.500Z' (later) sorts BEFORE '...10:00:00Z' (earlier) under ORDER BY ASC → chat messages, jobs newest-first, and conversation lists mis-order for same-second rows."},
  {"file": "backend/src/main/java/at/ac/fhcampuswien/external/ExternalTimeApiClient.java", "line": 38, "summary": "Neither external HTTP client (time, location) sets connect or request timeouts, and both serve permitAll endpoints (/world-clock makes 4 sequential blocking sends per request; /locations/** likewise) — hung upstream pins Tomcat worker threads indefinitely.", "failure_scenario": "timeapi.io/countriesnow.space accepts TCP but never responds → each anonymous request holds a worker thread forever; a handful of concurrent requests exhausts the pool and the whole app stops responding. ExternalLocationApiClient.java:52 also prints raw upstream error bodies to stdout."},
  {"file": "backend/src/main/java/at/ac/fhcampuswien/Database/DatabaseInitializer.java", "line": 10, "summary": "jobs DDL is duplicated and drifted vs JobRepository (client_id 36 vs 255, category/design_type 100 vs 255, budget/work_mode/deadline 50 vs 255, tags TEXT vs VARCHAR(1000), created_at nullable vs NOT NULL); Main runs the initializer first so its narrower schema silently wins on every fresh DB.", "failure_scenario": "Fresh DB + POST /jobs with a 60-char budget/deadline or 150-char category → H2 'value too long' SQLException → RuntimeException → opaque 500, despite the repository DDL promising 255; created_at NOT NULL is silently unenforced."},
  {"file": "frontend/design3/search-results.js", "line": 118, "summary": "Search filter values never match stored job values under the backend's exact LOWER(x)=LOWER(?) match: budget tier '3'→'large' but post-a-job stores 'big' (post-a-job.html:180); advanced-search sends 'onsite' but jobs store 'on site' (post-a-job.html:114); discipline values ('web','ux',…) never equal stored categories ('Web Design','UI/UX Design').", "failure_scenario": "Post a €€€ on-site Web Design job, then filter by any of those three facets → GET /jobs?budget=large&workMode=onsite&category=web matches nothing → 'No Results Found' despite the job existing."},
  {"file": "frontend/design3/app.js", "line": 30, "summary": "The homepage hero search redirects to jobs.html?q=…, but jobs.html contains no query-param handling at all — the keyword is silently discarded and the full list shown.", "failure_scenario": "Type 'logo' in the hero search → jobs.html?q=logo loads → loadJobs() fetches /jobs with no params → all jobs render unfiltered with no indication the search was ignored."}
]
```

## Verified but cut at the 15-cap (do these next)

- SecurityConfig.java:59 — blanket `GET /jobs/**` permitAll silently covers GET /jobs/{jobId}/applications; that endpoint's privacy rests on a hand-rolled `auth == null` check; any future GET under /jobs/** is public by default. CONFIRMED.
- Check-then-insert races return 500 instead of 409: POST /jobs/{id}/apply (JobApplicationRepository.create wraps the UNIQUE violation in RuntimeException — ConversationRepository.create handles the same race gracefully) and POST /auth/register (existsByEmail then save). CONFIRMED.
- MessageRepository.java:89 — `page * 50` int overflow → negative OFFSET → empirically a 500 on H2 2.3.232 for page ≥ ~43M. CONFIRMED.
- WorldClockService/Controller — no upstream-error mapping; one failed city → whole /world-clock 500s (LocationController maps the same to 502). CONFIRMED.
- ChatService.java:63-88 — USE_EXTERNAL_CHAT_API branches in getMessages/sendMessage run BEFORE validateParticipant and before senderId is server-set (latent; flag is a compile-time false). CONFIRMED ordering.
- WebConfig.java:16 — second hardcoded CORS mapping (63342 only) shadowed by SecurityConfig's CorsFilter; diverges if security CORS is touched. WebConfig.java:12 default `../frontend/design1/` doesn't exist on disk. CONFIRMED.
- SecurityConfig hmacSecretKey — no length validation; APP_JWT_SECRET < 32 bytes boots fine, first login throws KeyLengthException → 500. (From re-run angle D; not separately verified.)
- updateStatus FSM TOCTOU — unconditional `UPDATE … SET status=?` lets concurrent PUTs both pass the PENDING check (last-write-wins) / double-hire. Mechanism confirmed, concurrency-triggered.
- jobs.html / job-detail.html — no auth.js include, no 401/expiry redirect → expired token gives generic alerts. CONFIRMED (minor).
- homepage.html:188 + login.html:223 — world-clock entries rendered via innerHTML unescaped (only render path not using escapeHtml). PLAUSIBLE (data is third-party-API-controlled, not user-controlled).
- profile-edit.html:283/293 — Save Changes and Delete Profile are stubs (message/console.log only). CONFIRMED (self-announcing stub).
- index.html:93 — pushState '#page.html' but hash never read on load → refresh/deep-link resets to homepage. CONFIRMED by finder (not re-verified).
- job/README.md documents PUT /jobs/{id} (missing) and DELETE → 204 (code returns 200+JSON). Doc drift.

## Sweep candidates (found last round, NOT yet verified)

- AuthController.java:36 — no password length/strength check (register.html promises ≥8 chars; enforced nowhere).
- AuthController.java:45/56/103 — default-locale toUpperCase/toLowerCase for role/email (Turkish-i breakage); use Locale.ROOT.
- AuthController.java:82 — no length caps before INSERT → 'value too long' → 500 on long fullName/email/skills.
- H2TestSupport.java:21 — @BeforeEach sets global db.* system properties with no restore; @SpringBootTest shared DB never cleaned → class-order-dependent tests.
- register.html:171 — doesn't post {type:'auth-changed'} to the shell after auto-login (login.html does).
- profile-edit.html:337 — loadCities leaves city select disabled on early-return/catch paths.
- DatabaseInitializer.java:31 — catch+printStackTrace; note an earlier verifier REFUTED the "boots healthy with no tables" consequence (repository constructors fail fast), so treat only as a code smell.

## Cleanup backlog (rank below all correctness)

- 8× copy-pasted `auth == null` 401 guards (only the listApplications copy is reachable, due to the /jobs/** permitAll); ChatController's getCurrentUserId + a shared helper is the pattern.
- isDesigner duplicated (ApplicationController:196, JobController:124); bare "DESIGNER" branch is dead (converter always emits ROLE_).
- No @RestControllerAdvice — error JSON hand-built at ~25 sites; ChatController's @ExceptionHandler is controller-local.
- No connection pooling (Database.java:19 DriverManager per call; no Hikari on classpath). WorldClock makes 4 sequential upstream calls, no cache.
- Dead JobRepository.add() alias; update() writes caller-supplied created_at; flagged=false set in 3 places; ExternalLocationApiClient news up its own ObjectMapper + duplicates LocationService validation; existsByEmail does SELECT * ; isParticipant fetches the whole row; updateStatus re-reads the row after UPDATE.
- Email normalization lives inline in AuthController only — belongs in UserRepository (or model) + case-insensitive unique index, or the next users-writer reintroduces B1/B5.

## Process notes

- 3 finder/verifier agents died on API usage-policy false positives (security-flavored wording); their clusters were re-verified manually from source — all key claims held.
- PROJECT_REVIEW.md in the repo already tracks some of these as B14/B19/B22 — reconcile before filing duplicates.