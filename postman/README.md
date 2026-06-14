# Postman / Newman API tests — DesignerJobs.com

Black-box tests that hit the **running** backend over HTTP and assert the JSON responses — the outside-in counterpart to the in-process JUnit suite (`../backend/src/test`). They follow the **same TDD red board**: the happy path is green; the known bugs and unimplemented stubs are **red on purpose** until fixed.

## Files

| File | What it is |
|---|---|
| `DesignerJobs.postman_collection.json` | the requests + assertions (importable into the Postman app too) |
| `DesignerJobs.local.postman_environment.json` | environment (`baseUrl`, `password`) |
| `package.json` | Newman runner + npm scripts |

## Prerequisites

1. **Start the backend** (on JDK 17 — see `../backend`):
   ```sh
   cd ../backend
   JAVA_HOME="$(/usr/libexec/java_home -v 17)" mvn spring-boot:run
   ```
   It must be reachable at `http://localhost:8080` (the `baseUrl`).
2. **Install Newman** (once):
   ```sh
   cd postman
   npm install
   ```

## Run

```sh
npm run test:api          # CLI output
npm run test:api:html     # also writes newman-report.html
```

Or directly:
```sh
npx newman run DesignerJobs.postman_collection.json -e DesignerJobs.local.postman_environment.json
```

## What to expect (current board)

46 requests in 6 folders. As of the last sync, **49 of 58 assertions pass; 9 remain red** — the red ones are real outstanding issues, not stale assertions.

| Folder | Status |
|---|---|
| `1. Auth` | **GREEN** — register/login/me, validation, token chaining |
| `2. Jobs` | **GREEN** — create (clientId server-set), list, get, validation |
| `3. Applications` | **GREEN** — apply → accept → hire happy path |
| `4. Chat` | **GREEN** — open conversation, send/read messages, participant guard |
| `5. Regression — formerly bugs B1–B7` | **GREEN except B4** — B1, B2, B3, B5, B7 are now fixed; **B4** (`GET /jobs/random`) is still **404** (endpoint never added) |
| `6. Previously-stubbed packages` | **MIXED** — `/designers` (list/get/update), `/users` (get/list/delete) and `/moderation` (job report, list reports) are implemented and **green**; still **red** below |

### The 9 still-red assertions

| Endpoint | Today | Why red |
|---|---|---|
| `GET /jobs/random` (B4) | 404 | endpoint never implemented |
| `GET/POST/DELETE /designers/{id}/portfolio` | **500** | implemented but throwing — genuine bug |
| `POST /contracts`, `GET /contracts/{id}`, `PUT /contracts/{id}/sign` | **501** | `ContractController` is still a stub |
| `POST /moderation/users/{id}/report` | **500** | throwing — genuine bug |
| `PUT /moderation/reports/sample-id` | 404 | uses a placeholder id with no matching report |

A red item is a TODO for the **code**, not the test: make it green by fixing the endpoint (or, for the last row, by chaining a real report id), never by weakening the assertion.

> Coverage note: `/contracts`, the portfolio sub-resource, `PATCH /jobs/{id}/view-count`, `PUT`/`DELETE /auth/me`, `/world-clock`, `/locations/**` and the `/api/**` integration endpoints are not yet exercised here — add them as those areas stabilise.

## How it works (so every team member can explain it)

- **Token chaining.** The first requests register a client, a designer, and a "stranger"; each saves its JWT into environment variables (`clientToken`, `designerToken`, `strangerToken`) via a test script (`pm.environment.set(...)`). Later requests send `Authorization: Bearer {{clientToken}}`.
- **Id chaining.** Created resource ids (`jobId`, `applicationId`, `conversationId`) are captured the same way and reused in later URLs.
- **Unique data per run.** A collection-level pre-request script sets `runId = Date.now()` once per run, so emails like `client_{{runId}}@test.com` never collide across repeated runs (no DB reset needed). To fully reset state anyway: stop the backend, delete `../backend/data/projectdb.mv.db`, restart.
- **Assertions.** Each request has a `test` script using `pm.test(...)` + `pm.expect(...)` / `pm.response.to.have.status(n)`. Newman runs them headless and exits non-zero if any fail.

## Relationship to the JUnit suite

| | JUnit (`mvn test`) | Postman / Newman |
|---|---|---|
| Style | in-process (white-box) | over HTTP (black-box) |
| Needs server running | no | **yes** |
| Proves | logic, branches, SQL | the real wire contract end-to-end |
| Red board | 7 failing bug tests | same bugs + the 501 stubs |

Both should go green together as the bugs are fixed and the stub packages are implemented.
