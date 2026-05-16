# DesignerJobs.com — Akzeptanzkriterien (Draft)

> Companion to `user-stories.md`. One block per story, Gherkin-style (Given/When/Then) plus a checklist.
> Status: DRAFT.

## Legende

| Tag | Bedeutung |
|-----|-----------|
| **P1** | MUST — Moodle M-Anforderung |
| **P2** | SHOULD — Moodle S-Anforderung |
| **P3** | COULD — Moodle C-Anforderung |
| **P4** | DREAM — nicht benotet |

---

## 1. Authentifizierung & Session

### AC-01 — Registrierung · **P1** (US-01)
- **Given** ein Besucher ist nicht eingeloggt
- **When** er auf `POST /auth/register` mit gültiger E-Mail, Passwort (≥ 8 Zeichen) und Rolle aufruft
- **Then** antwortet das Backend mit **201 Created** und einer Nutzer-ID
- [ ] Doppelte E-Mail → **409 Conflict**
- [ ] Ungültiges E-Mail-Format → **400 Bad Request**
- [ ] Passwort wird mit BCrypt gehasht (nicht im Klartext)

### AC-02 — Login · **P1** (US-02)
- **Given** ein registrierter Nutzer
- **When** er `POST /auth/login` mit korrekten Credentials aufruft
- **Then** erhält er **200 OK** + JWT im Response-Body
- [ ] Falsches Passwort → **401 Unauthorized**, kein Token
- [ ] JWT hat `sub` (User-ID), `exp` (≤ 24 h) und HMAC-Signatur
- [ ] Token wird im Frontend in `localStorage` gespeichert

### AC-03 — Logout · **P1** (US-03)
- **Given** ein eingeloggter Nutzer
- **When** er auf "Logout" klickt
- **Then** wird das Token aus `localStorage` entfernt und der Nutzer auf die Startseite umgeleitet
- [ ] Geschützte API-Calls nach Logout liefern **401**

### AC-04 — Session-Persistenz · **P1** (US-04)
- **Given** ein eingeloggter Nutzer
- **When** er die Seite neu lädt
- **Then** ist er weiterhin eingeloggt (Token aus `localStorage` wieder verwendet)
- [ ] Abgelaufenes Token (`exp` < jetzt) → automatischer Redirect zum Login

### AC-05 — Passwort-Reset · **P4** (US-05)
- [ ] Reset-Link enthält Einmal-Token (UUID), 1 h gültig.
- [ ] Mail wird via Mailgun versendet.
- [ ] Nach erfolgreichem Reset wird das alte Passwort invalidiert.

---

## 2. Profil

### AC-06 — Profil erstellen · **P1** (US-06)
- **Given** ein eingeloggter Nutzer ohne Profil
- **When** er `POST /profile` mit Name, Rolle, Bio, Stadt, Skills sendet
- **Then** antwortet das Backend mit **201 Created** und der Profil-ID
- [ ] Pflichtfelder fehlen → **400 Bad Request**
- [ ] Nutzer hat schon ein Profil → **409 Conflict**

### AC-07 — Profil ansehen · **P1** (US-07)
- **Given** ein existierendes Profil
- **When** `GET /profile/{id}` aufgerufen wird (auch ohne JWT)
- **Then** wird das Profil als JSON ausgeliefert (**200 OK**)
- [ ] Nicht existierende ID → **404 Not Found**

### AC-08 — Profil voll bearbeiten (PUT) · **P1** (US-08)
- **Given** der Profil-Eigentümer ist eingeloggt
- **When** er `PUT /profile/{id}` mit kompletter Profil-Repräsentation sendet
- **Then** wird das Profil ersetzt (**200 OK**)
- [ ] Fremdes Profil → **403 Forbidden**
- [ ] Kein JWT → **401 Unauthorized**

### AC-09 — Profil löschen · **P1** (US-09)
- **Given** der Profil-Eigentümer
- **When** er `DELETE /profile/{id}` aufruft
- **Then** wird das Profil entfernt (**204 No Content**)
- [ ] Fremdes Profil → **403 Forbidden**

### AC-10 — Profil partiell ändern (PATCH) · **P3** (US-10) · schließt **C3**
- **Given** der Profil-Eigentümer
- **When** er `PATCH /profile/{id}` mit nur einem Feld (`{"bio":"…"}`) sendet
- **Then** wird **nur dieses Feld** aktualisiert, alle anderen bleiben
- [ ] PATCH wird vom Frontend tatsächlich genutzt (Inline-Edit der Bio)
- [ ] Unbekannte Felder → ignoriert oder **400** (Team-Entscheidung dokumentieren)

### AC-11 — Portfolio-Links · **P4** (US-11)
- [ ] Max. 10 Links pro Profil.
- [ ] URL-Validation (`https?://`).

### AC-12 — Stadt auf Karte · **P2** (US-12)
- **Given** ein Profil mit Stadt
- **When** das Profil geöffnet wird
- **Then** ruft das Frontend `GET /integrations/geocode?city=…` auf und zeigt einen Map-Pin
- [ ] Stadt nicht gefunden → kein Pin, Profil rendert trotzdem
- [ ] Nominatim down → Fallback "Karte nicht verfügbar"

---

## 3. Jobs

### AC-13 — Job posten · **P1** (US-13)
- **Given** ein eingeloggter Client
- **When** er `POST /jobs` mit Pflichtfeldern sendet
- **Then** **201 Created** + Job-ID + `createdAt`
- [ ] `category ∈ {webdesign, graphic design, interior design, ui/ux}`
- [ ] `budget ∈ {small, medium, big}`
- [ ] `workMode ∈ {remote, on site, hybrid}`
- [ ] Felder fehlen → **400**

### AC-14 — Feed laden · **P1** (US-14)
- **Given** mind. 1 Job im System
- **When** `GET /jobs` ohne Parameter aufgerufen wird
- **Then** **200 OK** + Array (max. 50 Einträge, neueste zuerst)
- [ ] Leerer Datensatz → `[]`, kein Fehler

### AC-15 — Filter / Suche · **P1** (US-15)
- **Given** mehrere Jobs
- **When** `GET /jobs?q=logo&category=graphic+design&budget=small` aufgerufen wird
- **Then** werden nur passende Jobs zurückgegeben
- [ ] `q` matcht Substring in Titel ODER Beschreibung
- [ ] `category`, `budget`, `workMode` matcht exakt (case-insensitive)
- [ ] `location` matcht Substring
- [ ] Kombination von Filtern = AND-Verknüpfung

### AC-16 — Job-Detail · **P1** (US-16)
- **Given** ein existierender Job
- **When** `GET /jobs/{id}` aufgerufen wird
- **Then** **200 OK** + alle Felder
- [ ] Unbekannte ID → **404**

### AC-17 — Job bearbeiten · **P1** (US-17)
- **Given** der Job-Autor
- **When** er `PUT /jobs/{id}` aufruft
- **Then** Job wird ersetzt (**200 OK**)
- [ ] Nicht-Autor → **403**

### AC-18 — Job löschen · **P1** (US-18)
- **Given** der Job-Autor
- **When** er `DELETE /jobs/{id}` aufruft
- **Then** **204 No Content**, Job verschwindet aus Feed
- [ ] Nicht-Autor → **403**

### AC-19 — Job partiell ändern (PATCH) · **P3** (US-19) · schließt **C3**
- **Given** der Job-Autor
- **When** er `PATCH /jobs/{id}` mit `{"budget":"medium"}` sendet
- **Then** nur `budget` ändert sich
- [ ] Frontend nutzt PATCH für Inline-Edit von Budget/Deadline

### AC-20 — Logo auf Job-Karte · **P2** (US-20)
- **Given** ein Job mit Client-Domain (z. B. `acme.com`)
- **When** der Job in der Liste/Detail gerendert wird
- **Then** holt das FE `GET /integrations/logo?domain=acme.com` und zeigt das Logo
- [ ] Logo nicht verfügbar → generischer Platzhalter, kein Fehler

### AC-21 — Beschreibung übersetzen · **P3** (US-21)
- **Given** ein Job in DE
- **When** Nutzer auf "Translate to English" klickt
- **Then** wird `POST /integrations/translate` mit `{text, target:"EN"}` aufgerufen und der Text ersetzt
- [ ] DeepL down → Toast "Übersetzung nicht verfügbar", Originaltext bleibt

### AC-22 — Bewerbung · **P4** (US-22)
- [ ] `POST /jobs/{id}/applications` mit Cover Note (≤ 1000 Zeichen).
- [ ] Doppelte Bewerbung → **409**.

### AC-23 — In-Platform-Chat · **P4** (US-23)
- [ ] Chat öffnet sich erst nach erfolgter Bewerbung.
- [ ] Polling-basiert (kein WebSocket im MVP).

---

## 4. Externe Integrationen

### AC-24 — Geocoding · **P1** (US-24) · schließt **M8**
- **Given** eine Stadt
- **When** `GET /integrations/geocode?city=Vienna` aufgerufen wird
- **Then** liefert das BE `{lat, lon, displayName}` aus Nominatim
- [ ] Fehler von Nominatim → **502 Bad Gateway** oder `{}` mit Status 200 (entscheiden)
- [ ] Backend cached Antworten für ≥ 1 h (Nominatim-Quota schonen)

### AC-25 — Logo · **P2** (US-25) · schließt **S1**
- **Given** eine Domain
- **When** `GET /integrations/logo?domain=acme.com` aufgerufen wird
- **Then** liefert das BE die Logo-URL (Logo.dev erfordert keinen Key, aber Rate-Limit)

### AC-26 — Übersetzung · **P3** (US-26) · schließt **C1**
- **Given** ein Text + Zielsprache
- **When** `POST /integrations/translate` aufgerufen wird
- **Then** liefert das BE den übersetzten Text aus DeepL
- [ ] API-Key kommt aus `.env`, nicht aus dem Code

### AC-27 — Mail · **P4** (US-27)
- [ ] Mailgun-Sandbox-Domain reicht für Demo.
- [ ] Versand failure → Job wird trotzdem gespeichert.

---

## 5. Zweite Frontend-Komponente (Dashboard) — **P2** · schließt **S2**

### AC-28 — Eigene Jobs · **P2** (US-28)
- **Given** ein eingeloggter Client mit Jobs
- **When** er das Dashboard öffnet
- **Then** lädt das FE `GET /jobs?author={meineId}` und zeigt die Liste
- [ ] Jeder Eintrag bietet "Bearbeiten" (PUT/PATCH) und "Löschen" (DELETE)

### AC-29 — Eigene Bewerbungen · **P2** (US-29)
- [ ] FE ruft `GET /applications?applicant={meineId}` (oder Profil-Endpoint) auf.

### AC-30 — Profil im Dashboard · **P2** (US-30)
- [ ] FE ruft `GET /profile/{id}` und `PUT /profile/{id}` (oder PATCH) auf.
- [ ] Damit erreichen die Dashboard-Komponenten ≥ 3 BE-Endpoints → **S2** erfüllt.

---

## 6. Querschnittsthemen

### AC-31 — Responsive · **P2** (US-31) · schließt **S4**
- [ ] Breakpoint Mobile ≤ 600 px: Navigation collapsed, ein-spaltige Layouts.
- [ ] Desktop ≥ 1024 px: Multi-Column-Feed, Sidebar.
- [ ] Manueller Test auf 360 × 640 (Mobile) und 1440 × 900 (Desktop).

### AC-32 — W3C-Validität · **P2** (US-32) · schließt **S3**
- [ ] Alle HTML-Seiten ohne Fehler in https://validator.w3.org/.
- [ ] Warnungen werden dokumentiert, Fehler behoben.

### AC-33 — XML-Antworten · **P3** (US-33) · schließt **C2**
- **Given** ein Client setzt `Accept: application/xml`
- **When** ein GET-Endpoint aufgerufen wird
- **Then** ist `Content-Type: application/xml` und der Body ist valides XML
- [ ] Default ohne Header bleibt JSON.
- [ ] Mindestens `GET /jobs` und `GET /jobs/{id}` liefern beides.

### AC-34 — Fehler-Feedback · **P1** (US-34)
- [ ] Netzwerkfehler → User-sichtbare Meldung ("Verbindung verloren").
- [ ] 401 → Redirect zum Login.
- [ ] 5xx → "Etwas ist schiefgelaufen" + Retry-Button.

### AC-35 — Loading States · **P4** (US-35)
- [ ] Skeleton-Cards beim Feed-Laden.
- [ ] Submit-Buttons mit Spinner während POST/PUT.

---

## 7. Admin / Dream-Features (P4)

### AC-36 — Job melden · **P4** (US-36)
- [ ] Flag-Counter pro Job, bei ≥ 3 → automatisch unsichtbar bis Review.

### AC-37 — AI-Moderation · **P4** (US-37)
- [ ] Neuer Post läuft durch externes LLM-API, Score < 0.5 → Status `pending`.

### AC-38 — Auto-Contract · **P4** (US-38)
- [ ] PDF-Generierung via z. B. iText.
- [ ] Beide Parteien bestätigen per E-Mail-Link.

### AC-39 — Featured Job · **P4** (US-39)
- [ ] Stripe-Test-Mode, 5 € pauschal, 7 Tage Highlight.

---

## 8. Abhakliste pro Moodle-Punkt

| Anforderung | Erfüllt durch | Status |
|-------------|---------------|--------|
| M1 BE component | Repo `/backend` läuft eigenständig | [ ] |
| M2 FE component | Repo `/frontend` HTML5+CSS+JS | [ ] |
| M3 HTTP | Alle FE↔BE-Calls über `fetch()` | [ ] |
| M4 AJAX | Alle Aufrufe asynchron, kein Full-Reload | [ ] |
| M5 JSON | Alle Endpoints liefern JSON | [ ] |
| M6 GET/POST/PUT/DELETE BE | AC-01, 06, 08, 09, 13, 17, 18 | [ ] |
| M7 GET/POST/PUT/DELETE FE | AC-06..09, 13..18 | [ ] |
| M8 ≥ 1 external | AC-24 (Nominatim) | [ ] |
| M9 Session | AC-01..04 | [ ] |
| S1 ≥ 2 external | AC-24 + AC-25 | [ ] |
| S2 2nd FE ≥ 3 endpoints | AC-28..30 | [ ] |
| S3 W3C | AC-32 | [ ] |
| S4 Responsive | AC-31 | [ ] |
| C1 ≥ 3 external | AC-24 + AC-25 + AC-26 | [ ] |
| C2 JSON+XML | AC-33 | [ ] |
| C3 PATCH konsumiert | AC-10 oder AC-19 | [ ] |
