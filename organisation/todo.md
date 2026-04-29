# DesignerJobs.com — Master To-Do List

> Group project: Bruno, Yarah, Katja, Lika
> FH Campus Wien — Web project (3–4 students)

---

## 1. Organisational / Team

- [ ] Set up a shared Git repository (GitHub/GitLab) and add all 4 members
- [ ] Agree on a branching strategy (e.g. feature branches → main)
- [ ] Create a shared communication channel (Discord/WhatsApp group)
- [ ] Set concrete internal deadlines per milestone (MUST → SHOULD → COULD)
- [ ] Schedule regular check-ins (weekly sync)
- [ ] Confirm presentation date and who presents which part
- [ ] Ensure every member can explain how frameworks/libraries they use work (exam requirement)

---

## 2. Decisions Still Open

- [ ] **Database choice**: Bruno is responsible for DB — decide on technology (SQLite, H2, PostgreSQL, MySQL?)
- [ ] **Hosting / Demo**: Will the project be demoed locally or deployed somewhere? (localhost is fine for presentation)
- [ ] **API keys**: Get real API keys or decide on mock/fallback behavior for:
  - [ ] Mailgun (email service)
  - [ ] DeepL (translation)
  - [ ] Logo.dev — works without key but has rate limits
- [ ] **Second frontend (S2)**: Decide what the second FE component will be (admin panel? mobile view? separate dashboard?)
- [ ] **Domain / project name**: Confirm "DesignerJobs.com" is final or just a working title
- [ ] **Design system**: Agree on colour palette, typography, overall look & feel for frontend consistency between Katja and Lika

---

## 3. Backend — Bruno (Database & API)

- [ ] Set up a real database to replace in-memory `List<>` storage
- [ ] Create DB schema: `users`, `profiles`, `job_posts` tables
- [ ] Implement DB connection and CRUD operations (DAO/repository layer)
- [ ] Migrate seed data from `DummyGenerator.java` into DB init script
- [ ] Add password hashing (currently stored as plain text `"password123"`)
- [ ] Store JWT secret securely (currently hardcoded in `JwtUtils.java`)
- [ ] Add input validation on API endpoints (email format, required fields, length limits)
- [ ] Add error handling for malformed requests (currently minimal)
- [ ] Test all endpoints with Postman/Bruno/Insomnia and document results

---

## 4. Backend — Yarah (API Endpoints & Service Integration)

- [ ] Design and implement all REST API endpoints
- [ ] Set up Java HttpServer as the backend framework
- [ ] Create data models: User, Profile, JobPost
- [ ] Implement Auth endpoints (register, login, logout with JWT)
- [ ] Implement Profile endpoints (CRUD + PATCH)
- [ ] Implement Job Post endpoints (CRUD + PATCH + feed + search)
- [ ] Implement JSON and XML response support (Accept header) → requirement C2
- [ ] Implement PATCH endpoints for partial updates → requirement C3
- [ ] Create seed/dummy data generator for development
- [ ] Integrate external REST services:
  - [ ] Mailgun — email notifications (External API #1)
  - [ ] Nominatim/ipapi — geocoding & IP geolocation (External API #2)
  - [ ] Logo.dev — company logo fetching (External API #3)
  - [ ] DeepL — translation EN↔DE (External API #4)
- [ ] Create service endpoints to expose external integrations
- [ ] Implement JWT-based session management (M9)
- [ ] Set up CORS headers for frontend communication
- [ ] Coordinate with Bruno on DB integration (endpoints currently use in-memory lists)
- [ ] Coordinate with Katja/Lika to confirm frontend calls match endpoint contracts
- [ ] Write a short API documentation (endpoint list, request/response examples) for frontend team

---

## 5. Frontend — Katja & Lika

### Pages to build (from presentation scope)
- [ ] **Homepage** — landing page with site overview
- [ ] **Anmelden (Login/Register)** — authentication forms
- [ ] **Profil erstellen (Create Profile)** — profile creation form for designers/clients
- [ ] **Post erstellen (Create Job Post)** — job posting form
- [ ] **Feed** — scrollable list of job posts
- [ ] **Refined Search** — search/filter functionality for jobs

### Technical requirements
- [ ] Use HTML5, CSS, and vanilla JS (requirement M2)
- [ ] Communicate with backend via HTTP using AJAX/fetch (requirements M3, M4)
- [ ] Consume GET, POST, PUT, DELETE from at least one endpoint (requirement M7)
- [ ] Consume the PATCH endpoint from the frontend (requirement C3)
- [ ] W3C validation — validate all HTML pages at https://validator.w3.org/ (requirement S3)
- [ ] Responsive design — dedicated mobile and desktop views (requirement S4)
- [ ] Build a **second frontend component** that talks to at least 3 BE endpoints (requirement S2)
- [ ] Implement login flow: call `/api/auth/login`, store JWT, send in `Authorization` header
- [ ] Handle loading states and error messages for API calls
- [ ] Display location data (from Nominatim/ipapi) on profiles or job posts
- [ ] Display company logos (from Logo.dev) where relevant
- [ ] Integrate translation feature (DeepL) — e.g. translate job descriptions

---

## 6. Moodle Grading Requirements Checklist

### MUST (21 points) — all required
- [ ] **M1**: Backend is individual component — NOT STARTED
- [ ] **M2**: Frontend is individual component (HTML5, CSS, JS) — NOT STARTED
- [ ] **M3**: FE ↔ BE communication via HTTP(S) — NOT STARTED
- [ ] **M4**: Asynchronous data transfer (AJAX) — NOT STARTED
- [ ] **M5**: BE endpoints return JSON or XML — NOT STARTED
- [ ] **M6**: BE uses GET, POST, PUT, DELETE — NOT STARTED
- [ ] **M7**: FE consumes GET, POST, PUT, DELETE — NOT STARTED
- [ ] **M8**: System consumes at least 1 external REST service — NOT STARTED
- [ ] **M9**: Session management — NOT STARTED

### SHOULD (8 points)
- [ ] **S1**: Consume at least 2 external REST services — NOT STARTED
- [ ] **S2**: Second individual FE component with ≥3 BE endpoints — NOT STARTED
- [ ] **S3**: FE is W3C compliant — NOT STARTED
- [ ] **S4**: FE is responsive (mobile + desktop) — NOT STARTED

### COULD (5 points)
- [ ] **C1**: Consume at least 3 external REST services — NOT STARTED
- [ ] **C2**: BE returns data as JSON **and** XML — NOT STARTED
- [ ] **C3**: PATCH endpoint consumed by FE — NOT STARTED

---

## 7. Integration & Testing

- [ ] End-to-end test: FE login → create profile → post job → search → view feed
- [ ] Test CORS setup — frontend must be able to call backend from different origin/port
- [ ] Test XML responses (send `Accept: application/xml` header)
- [ ] Test with invalid/missing data — ensure proper error responses
- [ ] Test session management — expired tokens, invalid tokens, logout
- [ ] Verify all external services degrade gracefully when API keys are missing or services are down

---

## 8. Presentation Preparation

- [ ] Update presentation slides (ProfPres1x.pdf is the current version)
- [ ] Prepare live demo flow (what to show, in what order)
- [ ] Each member prepares to explain their part and the technologies used
- [ ] Practice the presentation at least once as a group
- [ ] Prepare for Q&A — professor may ask about framework internals
- [ ] "Reden enthusiastischer" — present with more enthusiasm (noted in responsibilities)
- [ ] Highlight the "Unique: online" aspect of the project

---

## 9. Final Polish & Submission

- [ ] Remove all placeholder API keys from committed code (use environment variables or config file)
- [ ] Clean up dead code and unused files
- [ ] Add a README.md with setup instructions (how to build, run, test)
- [ ] Verify the project compiles and runs from a clean checkout (`mvn clean compile exec:java`)
- [ ] Final W3C validation pass on all frontend pages
- [ ] Check all grading criteria one last time before submission
- [ ] Submit on Moodle by the deadline
