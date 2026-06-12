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

---

## Frontend review 2026-06-12 (scheduled)

Scope: `frontend/design3/` (19 files) plus the two profile commits that landed on `origin/main` overnight — `826d09c` + `20d3429` (Lika, profile update/save/delete). The scheduled cloud routine `trig_01WAYMvgMhAPoFkRFQTs1ofc` fired at 2026-06-12T01:37Z; it produced the review below but could **not** open a branch/PR (the cloud checkout had no push path to the private repo, `persist_session:false` left no transcript) — the report was retrieved and pasted in manually. Re-verified inline against `origin/main` (local main has since been fast-forwarded to `20d3429`).

**The two profile commits — beneficial, not an overwrite.** Clean fast-forward over `91223ce`; nothing from the 2026-06-11 work was touched (changed files: `auth/AuthController.java`, `auth/UserModel.java`, `auth/UserRepository.java`, `profile-edit.html`, committed H2 file). They add `PUT`/`DELETE /auth/me` + ~15 profile columns with non-destructive `ALTER TABLE … ADD COLUMN IF NOT EXISTS` migrations, and rewire `profile-edit.html` Save/Delete from stubs to real calls. Repository SQL stays parameterised; `PUT /auth/me` cannot change `email`/`role`/`createdAt`. This closes **F1** (already done earlier), **F3/F6**, and the profile half of **M7**. New problems they introduce are F13/F14/F15 + the backend addendum below.

### Known frontend findings — status (verified file:line, canonical PROJECT_REVIEW F-IDs in brackets)

| Report ID | Finding | Status |
|---|---|---|
| F1 [F1] | shell reads wrong `localStorage` keys | ✅ FIXED — `index.html:122` reads `designer_jobs_token`; logout clears all three `designer_jobs_*` (`147-149`). |
| F2/F12 [F2] | login `next` → open-redirect / `javascript:` (two sinks: `:120`, `:179`) | ❌ OPEN — both unvalidated; patch together. |
| F3/F6 [F3] | FE issues no PUT/DELETE (M7) | ✅ FIXED for profile (`PUT`/`DELETE /auth/me`); 🟠 **job** edit/delete UI still missing. |
| F4 [→F7] | search filter value mismatches (budget `3`→`large` vs stored `big`; `onsite` vs `on site`; `web`/`ux` vs `Web Design`/`UI/UX Design`) | ❌ OPEN — `search-results.js:118`, `advanced-search.html`, `post-a-job.html`. |
| F5 [F4] | world-clock unescaped `innerHTML` | ❌ OPEN — `homepage.html:165`, `login.html:222` (low risk; third-party data; compounds with F14). |
| F7 [F5] | `jobs.html`/`job-detail.html` bypass `Auth.authFetch` | ❌ OPEN + WIDENED — also new `profile-edit.html` raw `fetch`+manual Bearer at 3 sites (load/save/delete). |
| F8 [→F9] | index pushState `#page` never restored on load | ❌ OPEN — `index.html:157` navigate(); no `location.hash` read. |
| F9 [→F10] | chat shows only oldest 50 | ❌ OPEN — `chat.html:498` no `?page=`. |
| F10 [→F11] | register no `auth-changed` postMessage | ❌ OPEN — `register.html:171`. |
| F11 [→F12] | `loadCities` leaves city `<select>` disabled | ❌ OPEN — `profile-edit.html` early-return + catch. |

### New findings, 2026-06-12 (canonical PROJECT_REVIEW IDs in brackets)

```json
[
  {"report_id": "F14", "canonical": "F8", "severity": "high", "file": "frontend/design3/index.html", "line": 203, "summary": "Shell `message` listener has no event.origin check and navigates to event.data.page (sets frame.src).", "failure_scenario": "Compounds with F5 world-clock innerHTML: an injected city name posts {type:'auth-changed', page:'https://evil/'} to the shell; navigate() sets the iframe to the attacker URL while the address bar still shows localhost. Add `if (event.origin !== window.location.origin) return;`."},
  {"report_id": "F13", "canonical": "F13", "severity": "medium", "file": "frontend/design3/profile-edit.html", "line": 350, "summary": "Successful Delete Profile removes only designer_jobs_token; designer_jobs_userId/_role stay in localStorage and no auth-changed is posted to the shell.", "failure_scenario": "Auth.getUserId()/getRole() keep returning the deleted account for in-flight callbacks; navbar lags until full reload. Use Auth.clearSession() + postMessage auth-changed before navigating."},
  {"report_id": "F15", "canonical": "F14", "severity": "medium", "file": "frontend/design3/search-results.html", "line": 160, "summary": "Sidebar filter tiles have no name/form/handlers (dead), and search-results.js:11 uses pageParams.get('discipline') so multi-discipline ?discipline=web&discipline=ux silently keeps only the first.", "failure_scenario": "Clicking sidebar facets does nothing; multi-select advanced searches drop all but one discipline. Wire handlers; use URLSearchParams.getAll()."},
  {"report_id": "F16", "canonical": "F15", "severity": "medium", "file": "frontend/design3/search-results.html", "line": 269, "summary": "Seven hardcoded placeholder job cards whose 'View Job' links point to job-random.html; only cleared if search-results.js reaches line 31.", "failure_scenario": "Visible before DOMContentLoaded, or permanently if the JS throws before resultsContainer.innerHTML=''; users click fake cards into job-random.html. Remove the static cards."}
]
```

### Backend addendum — regressions from the two profile commits (FE-only review missed these)

```json
[
  {"canonical": "B17 (extend) / B23", "severity": "high", "file": "backend/src/main/java/at/ac/fhcampuswien/auth/AuthController.java", "line": 16, "summary": "@CrossOrigin(origins=\"*\", allowedHeaders=\"*\", methods={GET,POST,PUT,DELETE}) re-added on AuthController (commit 826d09c) with a leftover '// DAS HIER ERWEITERN' comment.", "failure_scenario": "Violates CLAUDE.md's centralized-CORS rule (per-controller @CrossOrigin was deliberately removed; CORS belongs only in SecurityConfig.corsConfigurationSource); wildcard origin diverges from the app.cors.allowed-origins allowlist. Same root as B17. Remove the annotation."},
  {"canonical": "B23", "severity": "medium", "file": "backend/src/main/java/at/ac/fhcampuswien/auth/AuthController.java", "line": 231, "summary": "DELETE /auth/me hard-deletes the user with no cleanup of owned data.", "failure_scenario": "jobs.client_id / applications.designer_id / conversations / messages still reference the deleted id (no FK cascade in the raw-JDBC schema) -> orphaned listings, counterparties still read the ex-user's data. Soft-delete or cascade."},
  {"canonical": "B24", "severity": "medium", "file": "backend/src/main/java/at/ac/fhcampuswien/auth/AuthController.java", "line": 150, "summary": "PUT /auth/me binds an untyped Map<String,Object> with unchecked (String)/(Number) casts and no length/value validation.", "failure_scenario": "Wrong JSON type (numeric fullName, string hourlyMin) -> ClassCastException -> 500; over-long bio/url past the new VARCHAR caps -> H2 'value too long' -> 500. Bind a typed DTO; validate. Also: UserRepository.updateProfile(id,model) is dead code; AuthController/UserModel lack a trailing newline; data/projectdb.mv.db churns in VCS again."}
]
```

> Numbering note: this dated report keeps the scheduled routine's own F1–F16 labels (self-contained); the canonical registry is PROJECT_REVIEW.md §9c, where statuses are updated and new findings continue from F6 as **F7–F15** (mapping in the brackets above). Backend items are filed under §9 (B17 extended, B23–B24).