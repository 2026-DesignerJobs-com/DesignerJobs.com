# DesignerJobs.com — User Stories (Draft)

> Companion to `spec.md`. Akzeptanzkriterien live in `akzeptanzkriterien.md`.
> Status: DRAFT.

## Priority legend

| Tag | Meaning |
|-----|---------|
| **P1** | MUST — closes a Moodle M-requirement |
| **P2** | SHOULD — closes a Moodle S-requirement |
| **P3** | COULD — closes a Moodle C-requirement |
| **P4** | DREAM — team vision, not graded |

Each story has an ID (`US-xx`), a priority, and the requirement(s) it closes.

## Actors

- **Designer** — freelancer offering services
- **Client** — buyer posting a job
- **Visitor** — unauthenticated user browsing the site
- **Admin** — operator using the second FE component
- **System** — the platform itself (background tasks, integrations)

---

## 1. Authentication & Session

### US-01 — Register an account · **P1** · closes M6, M9
> As a **visitor**, I want to **create an account with email and password** so that **I can use the platform as a designer or client**.

### US-02 — Log in · **P1** · closes M6, M9
> As a **registered user**, I want to **log in with my credentials and receive a session token** so that **I can use protected features**.

### US-03 — Log out · **P1** · closes M6, M9
> As a **logged-in user**, I want to **log out** so that **my session token is no longer used**.

### US-04 — Session persistence on reload · **P1** · closes M9
> As a **logged-in user**, I want **my session to survive a page reload** so that **I don't have to log in again on every navigation**.

### US-05 — Password reset via email · **P4**
> As a **user who forgot their password**, I want to **reset it via an email link** so that **I can regain access**.

---

## 2. Profile

### US-06 — Create my profile · **P1** · closes M6, M7
> As a **logged-in user**, I want to **create a profile with name, role, bio, location and skills** so that **clients/designers know who I am**.

### US-07 — View a profile · **P1** · closes M6, M7
> As a **visitor or user**, I want to **view someone's public profile** so that **I can evaluate them**.

### US-08 — Edit my profile (full update) · **P1** · closes M6, M7
> As a **profile owner**, I want to **edit my entire profile** so that **I can keep it up to date**.

### US-09 — Delete my profile · **P1** · closes M6, M7
> As a **profile owner**, I want to **delete my profile** so that **my data is removed from the platform**.

### US-10 — Edit only one field of my profile · **P3** · closes C3
> As a **profile owner**, I want to **update only one field (e.g. bio)** so that **I don't have to resend my full profile to change a sentence**.

### US-11 — Upload portfolio links · **P4**
> As a **designer**, I want to **add up to 10 portfolio links/images** so that **clients can judge my style**.

### US-12 — Show my location on a map · **P2** · closes S1 (Nominatim)
> As a **profile owner**, I want **my city to be displayed with an approximate map pin** so that **clients see I'm in the DACH region**.

---

## 3. Job Posts

### US-13 — Post a job · **P1** · closes M6, M7
> As a **client**, I want to **post a job with title, description, category, location, budget, work mode and deadline** so that **designers can find and apply to it**.

### US-14 — Browse the job feed · **P1** · closes M6, M7
> As a **visitor or user**, I want to **scroll a feed of recent job posts** so that **I get a quick overview of what's on offer**.

### US-15 — Filter / search jobs · **P1** · closes M6, M7
> As a **designer**, I want to **filter jobs by keyword, category, location, budget and work mode** so that **I see only relevant offers**.

### US-16 — View job details · **P1** · closes M6, M7
> As a **designer**, I want to **open a single job to see its full description** so that **I can decide whether to apply**.

### US-17 — Edit my job · **P1** · closes M6, M7
> As the **job's author**, I want to **edit a job I posted** so that **I can correct mistakes or refresh the offer**.

### US-18 — Delete my job · **P1** · closes M6, M7
> As the **job's author**, I want to **delete a job I posted** so that **it stops appearing in the feed**.

### US-19 — Patch a single field on a job · **P3** · closes C3
> As the **job's author**, I want to **change only the budget or deadline** so that **I don't have to resend the whole post**.

### US-20 — See the client's company logo · **P2** · closes S1 (Logo.dev)
> As a **designer**, I want to **see the client's company logo on a job card** so that **I trust the listing more quickly**.

### US-21 — Translate a job description · **P3** · closes C1 (DeepL)
> As a **designer who reads only English/German**, I want to **toggle the description language** so that **I can read jobs in my preferred language**.

### US-22 — Apply to a job · **P4**
> As a **designer**, I want to **submit an application with a cover note** so that **the client can consider me**.

### US-23 — In-platform chat after applying · **P4**
> As a **client and designer**, we want to **chat in-platform after an application** so that **we don't need email**.

---

## 4. External Integrations

### US-24 — Geocode a city · **P1** · closes M8 (Nominatim)
> As the **system**, I want to **resolve a city name to coordinates via Nominatim** so that **profiles and jobs can show map pins**.

### US-25 — Fetch company logos · **P2** · closes S1 (Logo.dev)
> As the **system**, I want to **fetch a company logo by domain via Logo.dev** so that **job cards can display branding**.

### US-26 — Translate text · **P3** · closes C1 (DeepL)
> As the **system**, I want to **translate text between DE and EN via DeepL** so that **multilingual users see content in their language**.

### US-27 — Email notification on apply · **P4** (or P3 alt. via Mailgun)
> As a **client**, I want to **receive an email when someone applies** so that **I don't have to refresh the dashboard**.

---

## 5. Second Frontend Component (Dashboard)

### US-28 — Manage my jobs · **P2** · closes S2
> As a **client**, I want to **see all jobs I posted in one place** so that **I can manage them efficiently**.

### US-29 — Manage my applications · **P2** · closes S2
> As a **designer**, I want to **see all jobs I applied to** so that **I track my pipeline**.

### US-30 — Edit my profile from the dashboard · **P2** · closes S2
> As a **user**, I want to **edit my profile from the dashboard** so that **I don't switch sites**.

> US-28 + US-29 + US-30 together hit the **≥ 3 BE endpoints** requirement of S2 (jobs, applications/jobs by user, profile).

---

## 6. Cross-Cutting / Quality

### US-31 — Responsive design · **P2** · closes S4
> As a **mobile user**, I want **the site to work on my phone** so that **I can browse jobs on the go**.

### US-32 — Standards-compliant HTML · **P2** · closes S3
> As a **quality-conscious user**, I want **every page to be W3C-valid HTML** so that **the site is accessible and predictable**.

### US-33 — Receive XML when I ask for it · **P3** · closes C2
> As an **API consumer**, I want **to receive responses as XML when I send `Accept: application/xml`** so that **I can integrate from XML-only tools**.

### US-34 — Error feedback on failed calls · **P1** · closes M3, M4
> As a **user**, I want to **see a friendly error message when the backend is unreachable or rejects me** so that **I know to retry or log in again**.

### US-35 — Loading states · **P4**
> As a **user**, I want to **see a spinner / skeleton while data loads** so that **the page feels responsive**.

---

## 7. Admin / Moderation (P4 — Dream)

### US-36 — Flag / report a job · **P4**
> As a **user**, I want to **flag a suspicious job** so that **moderators can review it**.

### US-37 — AI moderation of new posts · **P4**
> As an **admin**, I want **new job posts to be auto-screened by an AI** so that **scam / underpaid listings are filtered before publication**.

### US-38 — Auto-contracting · **P4**
> As a **client and designer**, we want **a contract PDF generated on hire** so that **the engagement has a paper trail**.

### US-39 — Feature a job · **P4**
> As a **client**, I want to **pay to feature my job at the top of the feed** so that **I attract better candidates**.

---

## 8. Story-to-Requirement Coverage Matrix

| Moodle req. | Covered by user stories |
|-------------|-------------------------|
| M1 BE component | (architectural — no story; covered by repo structure) |
| M2 FE component | (architectural — covered by repo structure) |
| M3 HTTP comms | US-34 (and all FE↔BE stories) |
| M4 AJAX | All FE stories that fetch data |
| M5 JSON/XML | All BE stories; XML via US-33 |
| M6 GET/POST/PUT/DELETE BE | US-01..04, US-06..09, US-13..18 |
| M7 GET/POST/PUT/DELETE FE | US-06..09, US-13..18 |
| M8 ≥ 1 external | US-24 |
| M9 Session | US-01..04 |
| S1 ≥ 2 external | US-24 + US-25 |
| S2 2nd FE | US-28..30 |
| S3 W3C | US-32 |
| S4 Responsive | US-31 |
| C1 ≥ 3 external | US-24 + US-25 + US-26 |
| C2 JSON+XML | US-33 |
| C3 PATCH | US-10 + US-19 |
| **P4 (extras)** | US-05, US-11, US-22, US-23, US-27, US-35, US-36..39 |
