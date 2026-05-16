# DesignerJobs.com — Project Specification (Draft)

> FH Campus Wien — Web Technologies, Group AF (SoSe26)
> Team: Bruno Bilaver, Yarah Riedl, Katja Schloißnig, Lika Kevlishvili
> Status: DRAFT — internal working document, not a Moodle deliverable

---

## 1. Project Summary

DesignerJobs.com is a **design-only freelance job portal for the DACH region** (Austria, Germany, Switzerland). Clients post creative projects tagged by budget, location, design type and deadline; designers maintain portfolio-first profiles, apply to jobs, chat in-platform and get hired based on fit rather than lowest bid.

The project is the deliverable for *Web Technologies Grp2* (SoSe26). It is graded against a defined point catalogue (21 MUST + 8 SHOULD + 5 COULD = 34 max). This specification packages the academic requirements together with the product vision and additional "stretch" features the team has dreamed up.

### Positioning (from 2nd presentation)
- Generalist platforms (Upwork, Fiverr) punish freelancers with race-to-the-bottom pricing.
- DACH market lacks a design-specific marketplace.
- Designers want **fit**, not lowest bid.
- DACH design-freelancer pool: ~200,000–300,000 (extrapolated, 2025); avg. hourly rate €82 (Freelancer-Kompass 2025).
- Business model: 5% per-project booking fee, designers free.

---

## 2. Priority Scheme

All features in this spec, in the user stories and in the akzeptanzkriterien are tagged with one of four priorities:

| Tag | Meaning | Source |
|-----|---------|--------|
| **P1** | MUST — required for the first 21 points | Moodle requirements M1–M9 |
| **P2** | SHOULD — required for the next 8 points | Moodle requirements S1–S4 |
| **P3** | COULD — final 5 points | Moodle requirements C1–C3 |
| **P4** | DREAM — extra product value, not graded | Team vision (chat, AI moderation, contracts, etc.) |

Rule of thumb: **no P2 work starts before all P1 is green; no P3 work starts before all P2 is green; P4 is only touched if the rest is shipped and there is calendar room.**

---

## 3. System Architecture

Two clearly separated components communicate over HTTP(S) with asynchronous JSON (and optionally XML) payloads.

```
┌──────────────────────────┐         HTTP(S) + AJAX        ┌──────────────────────────┐
│  Frontend Component(s)   │  ◀──────── JSON/XML ────────▶ │   Backend Component      │
│  HTML5 + CSS + vanilla JS │                              │   Java / Spring Boot     │
│  Bootstrap (grid only)   │                              │   Embedded Tomcat :8080  │
└──────────────────────────┘                              └────────────┬─────────────┘
                                                                       │
                                                          ┌────────────┴─────────────┐
                                                          │  Persistence (DB / JSON) │
                                                          └────────────┬─────────────┘
                                                                       │
                                                          ┌────────────┴─────────────┐
                                                          │  External REST services  │
                                                          │  (Nominatim, Logo.dev,   │
                                                          │   DeepL, Mailgun …)      │
                                                          └──────────────────────────┘
```

- **M1** Backend is its own component (`/backend`, Spring Boot, port 8080).
- **M2** Frontend is its own component (`/frontend`, HTML5 + CSS + JS, served as static files).
- **M3 / M4** All FE→BE communication via `fetch()` (AJAX, HTTPS-capable).
- **S2** A **second** frontend component (admin / dashboard or mobile-first variant) talks to ≥ 3 BE endpoints.

---

## 4. Technology Stack

| Layer | Tech | Rationale |
|-------|------|-----------|
| Backend framework | Spring Boot 3 (Java 17) | Already wired; replaces hand-rolled `HttpServer` boilerplate; every member can explain the annotations used. |
| HTTP server | Embedded Tomcat | Bundled with Spring Boot, no separate install. |
| JSON | Jackson | Default in Spring Boot; auto-binds request/response bodies. |
| XML (P3) | Jackson XML module | Same library, only `produces = application/xml` differs. |
| Persistence | TBD — SQLite / H2 / PostgreSQL (Bruno owns the decision) | Replaces current `jobs.json` flat file. |
| Auth | JWT (HS256) | Stateless session token, matches M9 + lecture content. |
| Password hashing | BCrypt | Plain text seed currently in code → must be replaced before submission. |
| Frontend | HTML5, CSS, vanilla JS, Bootstrap (grid + collapse only) | Matches M2 wording: "HTML5, CSS and JS". |
| AJAX | `fetch()` | M4 — no jQuery needed. |
| Build / serve FE | Static files via `python3 -m http.server 8080` for dev | No bundler; every member can explain how the page loads. |

---

## 5. Component Inventory

### 5.1 Backend (`/backend`)
- `Main.java` — Spring Boot entry point.
- Controllers — `AuthController`, `JobController`, `ProfileController`, `IntegrationController` (external API proxy).
- Services — `JwtService`, `UserService`, `JobService`, `ExternalApiService`.
- Repositories — `UserRepository`, `JobRepository`, `ProfileRepository` (Spring Data once DB is in).
- Models — `User`, `Profile`, `JobPost`, `Application`.

### 5.2 Frontend Component 1 — Public Site (`/frontend/design1`)
- Home / landing
- Anmelden / Registrieren
- Profil erstellen / bearbeiten
- Job-Feed mit Suche & Filtern
- Job-Detail / Bewerben
- Job posten

### 5.3 Frontend Component 2 — Dashboard / Admin (S2, `/frontend/design2`)
- Eigene Jobs verwalten
- Eingegangene Bewerbungen
- Eigene Bewerbungen
- Eigenes Profil
- Talks to ≥ 3 BE endpoints (jobs, profile, applications).

---

## 6. Functional Scope

### 6.1 Authentication & Session (P1 — M9)
- Register, login, logout.
- JWT issued on login, sent in `Authorization: Bearer …` header on every protected call.
- Protected endpoints reject missing / expired tokens with 401.

### 6.2 Profile (P1 — M6/M7)
- Designer or client profile: name, role, bio, location, skills, portfolio links.
- Endpoints: `POST /profile` (create), `GET /profile/{id}` (read), `PUT /profile/{id}` (full update), `DELETE /profile/{id}` (delete).
- **P3 — C3:** `PATCH /profile/{id}` for partial updates (e.g. only bio).

### 6.3 Job Posts (P1 — M6/M7)
- Create / read / update / delete job posts.
- Fields: title, description, category (`webdesign | graphic design | interior design | ui/ux`), location, budget (`small | medium | big`), workMode (`remote | on site | hybrid`), deadline.
- Search by `q`, `category`, `location`, `budget`, `workMode`.

### 6.4 External Services (P1 → P3 — M8/S1/C1)
| # | Service | Used for | Closes |
|---|---------|----------|--------|
| 1 | **Nominatim** (OpenStreetMap) | Geocoding city → coordinates, displayed on job + profile | M8 |
| 2 | **Logo.dev** | Company logo on job posts / client profiles | S1 |
| 3 | **DeepL** | Translate job description DE ↔ EN | C1 |
| (4) | **Mailgun** | Email notifications (apply, hire) — fallback if needed | C1 alt. |
| (5) | **ipapi** | IP-based geolocation prefill on signup | C1 alt. |

### 6.5 XML Responses (P3 — C2)
- Endpoints honour `Accept: application/xml` and return XML; default remains JSON.

### 6.6 Responsive & Standards-Compliant FE (P2 — S3/S4)
- All HTML pages pass https://validator.w3.org/.
- Dedicated mobile and desktop views (CSS media queries; no separate mobile site).

### 6.7 Dream Features (P4)
- In-platform chat between client and applicant.
- Auto-contracting (PDF generation + signature workflow).
- AI moderation of job posts (filter scam / underpriced).
- Reporting / flagging.
- Featured listings (paid boost).
- Analytics dashboard for admins.
- DeepL translation toggle in the UI.

---

## 7. HTTP Endpoint Map (target)

| Method | Path | Auth | Purpose | Closes |
|--------|------|------|---------|--------|
| POST | `/auth/register` | — | Create account | M6, M9 |
| POST | `/auth/login` | — | Issue JWT | M6, M9 |
| POST | `/auth/logout` | JWT | Invalidate token (client-side mostly) | M6 |
| GET | `/profile/{id}` | optional | Read profile | M6 |
| POST | `/profile` | JWT | Create profile | M6 |
| PUT | `/profile/{id}` | JWT (owner) | Replace profile | M6 |
| PATCH | `/profile/{id}` | JWT (owner) | Partial update | C3 |
| DELETE | `/profile/{id}` | JWT (owner) | Delete profile | M6 |
| GET | `/jobs` | — | Search / list jobs | M6 |
| GET | `/jobs/{id}` | — | Job detail | M6 |
| POST | `/jobs` | JWT | Create job | M6 |
| PUT | `/jobs/{id}` | JWT (owner) | Update job | M6 |
| DELETE | `/jobs/{id}` | JWT (owner) | Delete job | M6 |
| GET | `/integrations/geocode?city=…` | JWT | Proxy Nominatim | M8 |
| GET | `/integrations/logo?domain=…` | JWT | Proxy Logo.dev | S1 |
| POST | `/integrations/translate` | JWT | Proxy DeepL | C1 |

FE component 1 consumes ≥ GET, POST, PUT, DELETE → **M7**.
FE component 2 consumes ≥ 3 endpoints → **S2**.

---

## 8. Non-Functional Requirements

- **W3C compliance** of all HTML (P2 — S3).
- **Responsive design** with mobile + desktop breakpoints (P2 — S4).
- **CORS** configured on the backend so the frontend on a different origin works.
- **Secret management** — JWT secret + API keys read from env vars or a non-committed config file, not hardcoded.
- **Password hashing** — BCrypt; no plain-text passwords stored.
- **Graceful degradation** — when an external service is down or its key missing, the page still renders without it.

---

## 9. Out of Scope (for the graded version)

- Payments / Stripe integration.
- Real-time chat infrastructure (WebSockets) — chat UI mock-up only, if P4 reached.
- Production deployment — demo runs on localhost.
- Mobile native apps.
- Full DSGVO/GDPR compliance workflow (data export, deletion requests).

---

## 10. Risks & Open Decisions

- **Database choice** (Bruno): SQLite vs. H2 vs. PostgreSQL. SQLite is fastest to wire and survives between restarts; H2 has in-memory mode useful for tests.
- **Second FE component** (Katja & Lika): admin dashboard vs. mobile-dedicated view. Admin dashboard easier to justify "≥ 3 endpoints" (S2).
- **API keys**: who provisions Mailgun / DeepL / Logo.dev? Decide before P3 work starts.
- **Cold-start** (presentation risk): designer + client supply must grow together — out of scope for grading, but noted for the pitch.
- **AI disruption** to design freelancing — long-term product risk noted in the pitch; no impact on this milestone.

---

## 11. Definition of Done (per requirement)

A Moodle requirement is "done" only when:
1. The implementation exists in code and runs against a clean checkout.
2. The corresponding user stories are implemented.
3. The corresponding akzeptanzkriterien are all checked.
4. Manual demo path is documented in `organisation/planung/iterations.md`.
5. The README explains how to reproduce it.

See `user-stories.md`, `akzeptanzkriterien.md` and `iterations.md` for the breakdown.
