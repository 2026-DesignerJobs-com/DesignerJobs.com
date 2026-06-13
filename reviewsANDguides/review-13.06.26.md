# Code review — 2026-06-13 (high effort)

Scope: all pushes in the last 24h to **`origin/main`** (range `a4a2261^…`) and the
**`kat-second-frontend`** admin dashboard. Method: 3 independent finder angles
(backend / frontend / removed-behavior+cross-file) → dedup → verified against the
pushed code. New since the 2026-06-12 review: TachyonCc's **Pexels "design inspiration"
API** + a **job view-count** feature; Lika's chat/time-API refactors; Kat's dashboard.

## The two criticals are linked

`JobRepositoryTest.update_changesFields` would have caught the malformed `UPDATE` SQL
(finding 1) — but `ChatServiceTest` references the **deleted** `ExternalChatApiClient`
(finding 2), so the whole test module won't compile, the suite never runs, and the SQL
regression shipped. Fix the test first; it immediately surfaces the SQL bug.

## Findings (most severe first)

```json
[
  {
    "file": "backend/src/main/java/at/ac/fhcampuswien/job/JobRepository.java",
    "line": 255,
    "summary": "CRITICAL: trailing comma in the UPDATE jobs SQL ('created_at = ?,' immediately before 'WHERE id = ?') — invalid SQL.",
    "failure_scenario": "Every PUT /jobs/{id} -> H2 syntax error -> RuntimeException -> 500; the job-edit endpoint is dead. Uncaught because the test module doesn't compile (finding 2)."
  },
  {
    "file": "backend/src/test/java/at/ac/fhcampuswien/chat/ChatServiceTest.java",
    "line": 23,
    "summary": "CRITICAL: test declares @Mock ExternalChatApiClient (and verifies it at line 140) but that class was deleted with the chat refactor.",
    "failure_scenario": "src/test references a removed type -> test-compilation fails -> mvn test / mvn package / CI all fail, masking other regressions (e.g. finding 1)."
  },
  {
    "file": "backend/src/main/java/at/ac/fhcampuswien/worldclock/WorldClockService.java",
    "line": 33,
    "summary": "HIGH: loadCityTime dereferences apiResponse.path(...) with no null check, but ExternalTimeApiClient now returns null on failure (used to throw). [= B25]",
    "failure_scenario": "timeapi.io down/slow/5xx -> client returns null -> null.path('date') -> NullPointerException -> GET /world-clock 500s. The 'avoid crashes' refactor made the crash unconditional on upstream failure."
  },
  {
    "file": "backend/src/main/java/at/ac/fhcampuswien/pexels/PexelsController.java",
    "line": 19,
    "summary": "HIGH: live Pexels API key hardcoded in source and committed; /api/** is permitAll (public).",
    "failure_scenario": "Secret exposed in the shared repo's history; combined with the public proxy, anyone can drive /api/design-inspiration and exhaust/abuse the key. Move to an env var and rotate."
  },
  {
    "file": "backend/src/main/java/at/ac/fhcampuswien/pexels/PexelsController.java",
    "line": 16,
    "summary": "MEDIUM: @CrossOrigin(origins=\"*\") re-introduces a per-controller wildcard CORS source, against the single centralized SecurityConfig CORS.",
    "failure_scenario": "A second, wildcard CORS policy diverges from the central allowlist (same regression class already fixed once on AuthController); any origin can read /api/** responses."
  },
  {
    "file": "backend/src/main/java/at/ac/fhcampuswien/pexels/PexelsController.java",
    "line": 21,
    "summary": "MEDIUM: HttpClient.newHttpClient() + no request timeout on the blocking call to api.pexels.com (same defect remains in ExternalTimeApiClient).",
    "failure_scenario": "If Pexels hangs, the servlet thread blocks indefinitely; a few concurrent calls exhaust the Tomcat worker pool and stall the app."
  },
  {
    "file": "backend/src/main/java/at/ac/fhcampuswien/auth/AuthController.java",
    "line": 177,
    "summary": "MEDIUM: ProfileUpdateRequest uses primitive int rates and updateProfile assigns them unconditionally, dropping the old containsKey guard. [= B24]",
    "failure_scenario": "A PUT /auth/me that omits hourlyMin/hourlyMax/projectMin deserializes them to 0 and overwrites the stored rates with 0 — silent pricing data loss on any partial update."
  },
  {
    "file": "backend/src/main/java/at/ac/fhcampuswien/auth/UserRepository.java",
    "line": 132,
    "summary": "MEDIUM: deleteById is now a soft-delete (role='DELETED') but findById/findByEmail don't exclude soft-deleted rows. [= B23 gap]",
    "failure_scenario": "JWTs are stateless (logout noop, 2h expiry). After DELETE /auth/me the user keeps a valid token; GET/PUT /auth/me still resolve the anonymized row -> a 'deleted' account can still read and edit its profile until expiry."
  },
  {
    "file": "frontend/admin/dashboard.js",
    "line": 110,
    "summary": "MEDIUM (kat-second-frontend): loadUsersFromServer throws on !response.ok before the not_implemented fallback (GET /designers returns 501); also no Auth.requireAuth() gate and no token attached.",
    "failure_scenario": "User-Verwaltung always shows the red error row (fallback unreachable); and anyone reaching /admin/dashboard.html sees the full admin UI + live data with no login enforced."
  },
  {
    "file": "frontend/design3/profile.html",
    "line": 264,
    "summary": "LOW: loadDesignInspiration injects Pexels photo.alt / photographer into innerHTML and photo.url into an href, unescaped, while the rest of the file uses escapeHtml.",
    "failure_scenario": "A field with a quote/angle-bracket (or a future proxy substitution) breaks the card markup or escapes the attribute; inconsistent with the file's own escaping."
  }
]
```

## Already fixed on `bugHunt`

Findings **3 (B25), 7 (B24), 8 (B23 gap)** are already fixed on the `bugHunt` branch
(null-guard, boxed-Integer rates, `role <> 'DELETED'` read filter), along with the
`ChatServiceTest` compile break (a different partial state there). Merging `bugHunt`
into `main` clears those four; the **JobRepository SQL** and the **Pexels key/CORS/timeout**
findings are new and not yet addressed anywhere.
