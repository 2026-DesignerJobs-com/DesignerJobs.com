# DesignerJobs.com — Projekt-Review & Durchgang

> Eine gemeinsame Referenz für unseren 6-Tage-Endspurt. Sie erklärt das gesamte Projekt für **jedes** Publikum — von jemandem, der noch nie Spring Boot gesehen hat, bis zur Senior-Entwicklerin, die zum Review dazustößt — mit Tiefenkapiteln zu **Spring-Boot-Nutzung** und **Session-/Auth-Verwaltung**, plus einer ehrlichen Liste von **Bugs, Lücken und Restarbeit**.

> **Update 2026-06-11:** Großer Tag. Behoben: **B1, B5** (E-Mail-Groß-/Kleinschreibung), **B2** (teilweise — `listApplications`-Eigentümer-Prüfung), **B7** (größtenteils — eindeutige Bewerbungen, sauberes 409, Conversation-Race). Neu seit gestern: **`DELETE /jobs/{id}`** + FE-Löschen-Button (schließt **M6**, halbes M7), **Suche** durchgängig verdrahtet, **job-random/job-detail-Seiten gefixt** (B4-Symptom weg) und eine **zweite externe REST-API** (countriesnow.space → schließt **S1**) mit Standort-Autofill im Profil-Editor. `mvn test` wählt jetzt automatisch JDK 17 via Maven Toolchains. Das rote TDD-Board ist auf **2 Tests runter: b3 (PUT /jobs) und b4 (GET /jobs/random)**. Details inline unten — jeder betroffene Punkt ist markiert.

---

## 0. Wie man dieses Dokument nutzt

Lies den Abschnitt, der zu deinem Gegenüber passt:

| Publikum | Beginne bei | Überspringe |
|---|---|---|
| **Absoluter Anfänger** (Nicht-Coder, Stakeholder) | §1 „Was es ist" + §2 „Der 60-Sekunden-Rundgang" | die Code-Tiefenkapitel |
| **Junior-Dev / Studierende** | §1–§5 | nichts |
| **Senior-Dev / Reviewer** | §3 (Architektur), §6 (Spring-Tiefenkapitel), §7 (Session-Tiefenkapitel), §9/§9b/§9c (Bugs: Backend, Harness, Frontend) | §2 |
| **Bewertende / Team-Lead** | der ⭐-Abschnitt zu den Bewertungsanforderungen, dann §9–§9c und §10 (Plan) | die Tiefenkapitel |

**Begleitdokumente:** `test.md` (die JUnit- + Postman-Test-Suite, Coverage, das TDD-Rot-Board), `jgrasp-guide.md` (Ausführung visualisieren/verfolgen), `postman/` (Black-Box-API-Tests).

Drei Erklärungsstufen sind inline markiert:
- 🟢 **Anfänger** — einfache Sprache, kein Fachjargon.
- 🟡 **Mittel** — setzt grundlegendes Web-/Programmierwissen voraus.
- 🔴 **Profi** — Implementierungsdetails und Trade-offs.

---

## 1. Was dieses Projekt ist

**DesignerJobs.com** ist ein studentisches Webprojekt (FH Campus Wien) — ein kleiner Job-Marktplatz, der **Clients** (die Design-Jobs ausschreiben) mit **Designern** (die sich bewerben und engagiert werden) verbindet.

🟢 **Anfänger-Version:** Stell es dir wie ein winziges „Upwork für Designer" vor. Firmen schreiben Jobs aus; Designer durchsuchen sie, bewerben sich, chatten mit der Firma und werden engagiert. Es gibt eine Website (den Teil, den du siehst) und einen Server (den Teil, der alles speichert und die Regeln durchsetzt).

🟡 **Was gebaut wurde:** eine REST-API in **Java + Spring Boot** (das `backend/`) und eine Website aus **Vanilla-JavaScript + Bootstrap** (`frontend/design3/`). Ein einziges Java-Programm betreibt beides — es beantwortet Datenanfragen *und* liefert die Webseiten aus. In der Entwicklung läuft alles unter `http://localhost:8080`.

🔴 **Stack:** Spring Boot 3.2, Java 17, eingebettete H2-Datei-Datenbank, **rohes JDBC** (kein JPA/Hibernate), zustandsloses **JWT**-Auth über Springs OAuth2 Resource Server, BCrypt-Passwort-Hashing. Kein Build des Frontends — schlichte `.html`/`.js`-Dateien werden als statische Ressourcen ausgeliefert.

---

## ⭐ Status der Bewertungsanforderungen (MUST / SHOULD / COULD)

Das ist das offizielle Bewertungsraster, abgeglichen mit dem **tatsächlichen Code von heute** (verifiziert am 2026-06-10). Legende: ✅ erfüllt · ⚠️ teilweise/gefährdet · ❌ nicht erfüllt · ❓ muss verifiziert werden.

### MUST — 21 Punkte (alle erforderlich; zwei sind derzeit gefährdet)

| # | Anforderung | Status | Beleg / was fehlt |
|---|---|---|---|
| **M1** | BE ist eine eigenständige Komponente | ✅ | `backend/` Spring-Boot-App, getrennt vom FE |
| **M2** | FE in HTML5 + CSS + JS | ✅ | `frontend/design3/` Vanilla-JS + Bootstrap |
| **M3** | FE↔BE über HTTP(S) | ✅ | alle Aufrufe an `http://localhost:8080` |
| **M4** | Asynchrone Übertragung (AJAX) | ✅ | FE nutzt `fetch()` / `Auth.authFetch()` (21 Aufrufe) |
| **M5** | BE liefert JSON oder XML | ✅ | JSON via `@RestController` |
| **M6** | BE nutzt GET, POST, PUT **und DELETE**, jeweils auf ≥1 Endpunkt | ✅ | ~~DELETE nur 501-Stubs~~ **Behoben 2026-06-10:** funktionierendes `DELETE /jobs/{id}` mit Auth + Autorisierungs-Prüfung (Commit `42fb250`). PUT war bereits funktional (`PUT /applications/{id}/status`). |
| **M7** | FE konsumiert GET, POST, PUT **und DELETE** von ≥1 Endpunkt | ⚠️ | **Halb geschlossen 2026-06-10:** FE setzt jetzt **DELETE** ab (Job-löschen-Button in `job-detail.html`, Commit `f3d26e9`). **Weiterhin 0 PUT-Aufrufe aus dem FE** — z. B. Job bearbeiten → `PUT /jobs/{id}` (braucht B3) oder Bewerbung annehmen → `PUT /applications/{id}/status` ergänzen. |
| **M8** | ≥1 externen REST-Service konsumieren | ✅ | `ExternalTimeApiClient` → `timeapi.io` (`GET /world-clock`) |
| **M9** | Session-Verwaltung (Login/JWT) | ✅ | zustandsloses JWT — siehe §7 |

> **Aktion für volle 21 Punkte:** nur noch **M7** offen — das FE muss ein **PUT** absetzen. Billigster Weg: ein Annehmen-/Ablehnen-Button auf der Bewerberliste, der das existierende `PUT /applications/{id}/status` aufruft. Der schönere Weg: `PUT /jobs/{id}` umsetzen (**B3**, der letzte rote Backend-Test) plus ein Job-bearbeiten-Formular.

### SHOULD — 8 Punkte

| # | Anforderung | Status | Beleg / was fehlt |
|---|---|---|---|
| **S1** | ≥2 externe REST-Services konsumieren | ✅ | **Geschlossen 2026-06-10:** `ExternalLocationApiClient` → `countriesnow.space` (`GET /locations/cities?country=…`, Commit `371b55f`), konsumiert vom Standort-Autofill im Profil-Editor. Plus `timeapi.io`. (`ExternalChatApiClient` bleibt ein deaktivierter Platzhalter.) |
| **S2** | Eine zweite FE-Komponente, die ≥3 BE-Endpunkte nutzt | ❌ | nur ein FE (`design3/`). Ein zweites kleines FE bauen (z. B. ein Admin-/Moderations-Dashboard oder eine Designer-Portfolio-Seite), das ≥3 Endpunkte anspricht. |
| **S3** | FE ist W3C-konform | ❌ | **Verifiziert am 2026-06-10 via validator.w3.org/nu — 6 Seiten scheitern** (~17 Fehler). Siehe §9c für die Liste. Sauber: login, profile, chat, job-random, homepage/jobs/job-detail (nur Warnungen). |
| **S4** | FE responsiv (Mobil- + Desktop-Ansicht) | ⚠️ | Bootstrap-Grid ist responsiv; eine **dedizierte** Mobil- vs. Desktop-Ansicht bestätigen (Breakpoints, Nav-Collapse) und dokumentieren. |

### COULD — 5 Punkte

| # | Anforderung | Status | Beleg / was fehlt |
|---|---|---|---|
| **C1** | ≥3 externe REST-Services konsumieren | ❌ | braucht drei — **zwei jetzt vorhanden** (`timeapi.io`, `countriesnow.space`). Eine weitere echte API schließt es. |
| **C2** | BE liefert JSON **und** XML | ❌ | nur JSON. XML via `jackson-dataformat-xml` + Content Negotiation ergänzen (`produces = {JSON, XML}`). |
| **C3** | BE-PATCH-Endpunkt, vom FE konsumiert | ❌ | kein `@PatchMapping` vorhanden (das einzige „PATCH" im Code ist ein CORS-*Allowed-Method*-Eintrag in `SecurityConfig`, kein Endpunkt). Einen PATCH-Endpunkt ergänzen (z. B. partielles Job-/Profil-Update) und vom FE aufrufen. |

### Punkte-Zusammenfassung (ehrliche Selbsteinschätzung)

- **MUST (21):** 8 von 9 voll erfüllt *(M6 am 2026-06-10 geschlossen)*; nur noch **M7 gefährdet** → sichere 21, sobald das FE ein `PUT` absetzt.
- **SHOULD (8):** **S1 erfüllt** (2 externe APIs); S4 wahrscheinlich; S3 scheitert weiterhin (6 Seiten); S2 braucht Arbeit.
- **COULD (5):** noch keiner erfüllt (C1 ist eine API entfernt).

Diese ordnen sich direkt in den 6-Tage-Plan in §10 ein — die Anforderungslücken sind dort priorisiert eingearbeitet.

---

## 2. Der 60-Sekunden-Rundgang (für alle)

1. **Ein Client registriert sich** → gibt Name, E-Mail, Passwort ein, wählt die Rolle „CLIENT". Der Server speichert das Konto (das Passwort wird verschlüsselt, nie im Klartext gespeichert) und gibt ein **Token** zurück (ein digitales Armband, das beweist, wer du bist).
2. **Der Client schreibt einen Job aus** → „Brauche ein Logo, 500 €." Der Server speichert ihn und stempelt ihn mit der Client-ID.
3. **Ein Designer registriert sich** als „DESIGNER", durchsucht Jobs (zum bloßen *Anschauen* ist kein Login nötig) und **bewirbt sich** auf einen.
4. **Sie chatten** plattformintern über den Job.
5. **Der Client nimmt an/engagiert** den Designer. (Das Engagieren *soll* einen Vertrag erzeugen — dieser Teil ist noch nicht fertig.)

Was heute voll funktioniert: **Konten/Login, Jobs ausschreiben & durchsuchen, Bewerbungs-/Engagement-Flow und Chat.** Noch als Stub: **Designer-Profile & Portfolios, Verträge und Moderation.** (Siehe §8 für die genaue Status-Tabelle.)

---

## 3. Architektur auf einen Blick

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

🟡 **Zwei Rollen, ein Programm.** `Main.java` ruft zuerst `DatabaseInitializer.init()` auf (erstellt die H2-Tabellen), *dann* `SpringApplication.run()`. Danach trifft jeder HTTP-Request entweder:
- einen **REST-Controller** (z. B. `/jobs`, `/auth/login`), oder
- fällt durch zu **`config/WebConfig`**, das eine Datei aus `frontend/design3/` ausliefert (so liefert `/jobs.html` die Seite, `/jobs` liefert JSON).

🔴 **Wichtige Designentscheidungen und wo sie leben:**
- **Persistenz = rohes JDBC.** Kein Spring Data, kein `JdbcTemplate`. Jedes Repository öffnet eine `Connection` via `Database.getConnection()` und schreibt `PreparedStatement`-SQL von Hand. Kanonisches Beispiel: `job/JobRepository.java`.
- **DB = eingebettetes H2, Dateimodus** unter `data/projectdb.mv.db` (`jdbc:h2:file:./data/projectdb`, User `sa`, kein Passwort — nur Dev). **Kein Migrations-Framework** — Schema-Änderungen bedeuten, ein `CREATE TABLE` zu editieren und die DB-Datei zu löschen (oder `ALTER TABLE` von Hand zu schreiben).
- **Security zentralisiert** in `config/SecurityConfig` — eine Filterkette, ein `PasswordEncoder`-Bean, eine CORS-Konfiguration. Nicht `@CrossOrigin` oder `new BCryptPasswordEncoder()` woanders verstreuen.
- **READMEs je Paket sind maßgeblich** — jedes Paket unter `at.ac.fhcampuswien/` dokumentiert seine eigenen Endpunkte. Vor dem Editieren lesen, aber **gegen den Code verifizieren** (manche sind veraltet — siehe §9).

---

## 4. Repository-Aufbau

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

## 5. Ausführen, testen & verifizieren

```sh
cd backend
mvn spring-boot:run        # serves API + frontend on http://localhost:8080
mvn package                # build the jar
mvn test                   # run the test suite (see test.md)
```

Erfordert **JDK 17 + Maven**. *(Aktualisiert 2026-06-11:)* Der Build nutzt jetzt **Maven Toolchains**, um mit JDK 17 zu kompilieren und zu testen, selbst wenn Maven selbst auf einem neueren JDK läuft (das Mockito/JaCoCo kaputt macht). Einmalige Einrichtung pro Maschine: eine `~/.m2/toolchains.xml`, die auf ein lokales JDK 17 zeigt — siehe `backend/README.md`. Ohne sie schlägt der Build sofort mit einer klaren Meldung fehl.

> ✅ **Es gibt jetzt eine JUnit-Test-Suite** unter `backend/src/test` (JUnit 5 + Mockito + AssertJ, plus `@SpringBootTest`/MockMvc für Security) und eine Postman/Newman-Black-Box-Suite in `postman/`. Alle Details, Coverage-Zahlen und das TDD-Rot-Board stehen in **`test.md`**.
>
> ⚠️ **`mvn test` ist gerade absichtlich ROT.** Wir arbeiten test-first: Es gibt einen fehlschlagenden Test pro offenem Bug (das Board in §5 von `test.md`), sodass der Build *absichtlich* fehlschlägt, bis die Bugs behoben sind. Nicht „reparieren", indem man Tests löscht. *(Stand 2026-06-11: nur noch **2 rot** — `b3` PUT /jobs, `b4` GET /jobs/random. `b1`, `b2`, `b5`, `b7` sind grün.)* Hinweis: Weil die roten Tests die `test`-Phase scheitern lassen, wird der JaCoCo-Report bei einem schlichten `mvn test` nicht erzeugt — siehe Harness-Befund **H1** in §9b.

**Gesamten Zustand zurücksetzen:** `backend/data/projectdb.mv.db` löschen und neu starten. (Hinweis: Diese Datei ist derzeit in git getrackt und ändert sich ständig — siehe **H3**.)

**Smoke-Test-Rezept (manuell / Black-Box, zum Kopieren während des Reviews):**
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

## 6. Spring Boot — das Tiefenkapitel

Das ist der Teil, den man am meisten verstehen sollte, denn das ganze Backend ist „einfach Spring".

### 6.1 🟢 Was Spring Boot überhaupt ist

Spring Boot ist ein Framework, das Boilerplate beim Bau eines Java-Webservers entfernt. Statt manuell einen HTTP-Server zu verdrahten, Requests zu parsen, URLs zu routen und JSON zu wandeln, **annotierst** du schlichte Java-Klassen, und Spring übernimmt die Verkabelung.

Ein hilfreiches Denkmodell: Du schreibst das *„Was"* (hier ist eine Methode, die `POST /jobs` behandelt), und Spring kümmert sich um das *„Wie"* (auf einem Port lauschen, die TCP-Verbindung annehmen, das HTTP parsen, deine Methode finden, ihr die geparsten Daten geben, deinen Rückgabewert wieder in JSON wandeln).

### 6.2 🟡 Die Annotationen, die wir tatsächlich nutzen

| Annotation | Wo | Was sie tut |
|---|---|---|
| `@SpringBootApplication` | `Main.java` | Markiert die Einstiegsklasse; aktiviert Auto-Configuration + Component-Scanning des gesamten `at.ac.fhcampuswien`-Paketbaums. |
| `@RestController` | jeder Controller | „Diese Klasse behandelt HTTP und liefert JSON." Kombiniert `@Controller` + `@ResponseBody`. |
| `@RequestMapping("/jobs")` | Klassenebene | Gemeinsames URL-Präfix für den Controller. |
| `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` | Methoden | Bilden HTTP-Methode + Pfad auf eine Java-Methode ab. |
| `@PathVariable` | Parameter | Holt `{id}` aus der URL. |
| `@RequestParam` | Parameter | Holt `?q=logo`-Query-Parameter. |
| `@RequestBody` | Parameter | Deserialisiert den JSON-Request-Body in ein Java-Objekt (via Jackson). |
| `@Service` | `JwtService`, `ChatService` | Markiert ein Business-Logik-Bean, das Spring erstellen und injizieren soll. |
| `@Configuration` / `@Bean` | `SecurityConfig`, `WebConfig` | Java-basierte Konfiguration; `@Bean`-Methoden erzeugen von Spring verwaltete Singletons. |
| `@Value("${app.jwt.secret}")` | Konfiguration | Injiziert einen Wert aus `application.properties`. |

### 6.3 🟡 Dependency Injection (DI) — die Kernidee

Beachte, dass kein Controller je `new JobRepository()` aufruft. Stattdessen:

```java
public JobController(JobRepository jobRepository) {   // constructor injection
    this.jobRepository = jobRepository;
}
```

🟢 **Einfache Version:** Du baust dein Werkzeug nicht selbst; du forderst es im Konstruktor an, und Spring reicht dir fertige, gemeinsam genutzte Instanzen. Das hält die Teile lose gekoppelt und austauschbar.

🔴 Spring baut beim Start einen Abhängigkeitsgraphen: Es sieht, dass `JobController` ein `JobRepository` braucht, erstellt eines (ein Singleton-Bean) und reicht es hinein. Derselbe `ChatService`-Konstruktor erhält `ConversationRepository`, `MessageRepository` und `ExternalChatApiClient` — alle automatisch verdrahtet. Es gibt **keine `@Autowired`-Feld-Annotationen**; wir nutzen überall **Constructor Injection**, den empfohlenen Stil (unveränderlich, testbar, schlägt früh fehl, wenn eine Abhängigkeit fehlt).

### 6.4 🔴 Der Request-Lebenszyklus (was bei jedem Aufruf passiert)

```
TCP → Embedded Tomcat → Spring Security filter chain → DispatcherServlet
    → HandlerMapping picks the @…Mapping method
    → argument resolvers fill @PathVariable / @RequestParam / @RequestBody / Authentication
    → your controller method runs (calls Service → Repository → JDBC → H2)
    → return value (object or ResponseEntity) → Jackson → JSON → HTTP response
```

Zwei Dinge, die für Reviewer erwähnenswert sind:
- **`Authentication` als Methodenparameter** wird von Spring Security aus dem `SecurityContext` injiziert (vom JWT-Filter gesetzt). So bekommen Controller die Identität des Aufrufers, *ohne dem Request-Body zu vertrauen*.
- **`ResponseEntity<?>`** lässt eine Methode ihren eigenen Statuscode wählen (`201 Created`, `403 Forbidden`, usw.). Methoden, die einfach eine `List<Job>` zurückgeben (wie `JobController.search`), liefern implizit `200 OK`.

### 6.5 🔴 Auto-Configuration-Entscheidungen, die wir getroffen haben

- `@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })` in `Main.java` — wir **deaktivieren** Spring Securitys Standard-In-Memory-User/Passwort-Generator, weil wir via JWT authentifizieren, nicht über Username/Passwort-Formular-Login.
- `DatabaseInitializer.init()` läuft **vor** `SpringApplication.run()` — Tabellen müssen existieren, bevor ein Repository sie anfasst. Diese Reihenfolge ist Absicht, kein Zufall.
- **Statisches Datei-Serving** übernimmt `config/WebConfig` (ein `WebMvcConfigurer`), der nicht gematchte URLs auf `frontend/design3/` abbildet. Deshalb ist es relevant, einen Controller-Pfad hinzuzufügen, der mit einem Seitennamen kollidiert.

### 6.6 🔴 Warum rohes JDBC statt JPA?

Es ist eine **Lehr-Entscheidung** — das Projekt kam von einem selbstgebauten `HttpServer` (prog2), daher hält das Nahebleiben am SQL die Datenschicht transparent. Trade-off: mehr Boilerplate, manuelles `PreparedStatement`-Mapping, keine automatische Schema-Verwaltung. Jedes Repository folgt dem Muster von `job/JobRepository.java`; kopiere es beim Implementieren eines neuen.

---

## 7. Session & Authentifizierung — das Tiefenkapitel

> **Das ist der zweite Bereich, den man gründlich verstehen sollte.** Die Kernaussage: **Es gibt keine serverseitige Session.** Keine `HttpSession`, keine `JSESSIONID`, nichts darüber, „wer eingeloggt ist", wird auf dem Server gespeichert.

### 7.1 🟢 Die Armband-Analogie

Wenn du dich einloggst, gibt dir der Server ein **signiertes Armband** (ein JWT). Jedes Mal, wenn du den Server um etwas bittest, zeigst du das Armband. Der Server prüft, dass die Signatur echt ist, liest Name und Rolle daraus ab und handelt — *ohne* etwas nachzuschlagen oder sich zwischen Requests an dich zu erinnern. Wenn du dich „ausloggst", wirfst du das Armband einfach weg; der Server verfolgt es nicht.

### 7.2 🟡 Was ein JWT ist

Ein JWT (JSON Web Token) ist eine Zeichenkette aus drei Teilen: `header.payload.signature`.
- **payload** trägt Claims — hier: `sub` (die User-ID), `role`, `iat` (ausgestellt am), `exp` (Ablauf).
- **signature** ist ein HMAC-SHA256-Hash aus Header+Payload mit einem **geheimen Schlüssel, den nur der Server kennt**. Manipuliert jemand den Payload, passt die Signatur nicht mehr und das Token wird abgelehnt.

Das Token ist also *für jeden lesbar* (keine Geheimnisse hineinpacken), aber *ohne den Schlüssel nicht fälschbar*.

### 7.3 🟡 Der End-to-End-Ablauf

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

### 7.4 🔴 Wo jedes Teil im Code lebt

- **Ausstellen** — `session/JwtService.issue()`. Baut Claims (`subject = userId`, `claim("role", role)`, `issuedAt`, `expiresAt = now + app.jwt.expiry-millis`), signiert HS256 via `JwtEncoder`.
- **Signier-Schlüssel** — `config/SecurityConfig`: `hmacSecretKey()` macht aus `app.jwt.secret` einen `SecretKeySpec`; `jwtEncoder()` (Nimbus) signiert; `jwtDecoder()` verifiziert. Derselbe Secret in beide Richtungen (symmetrisches HS256).
- **Verifizieren** — wir schreiben **keinen** Verifizierungs-Code. `oauth2ResourceServer().jwt(...)` verdrahtet Springs `BearerTokenAuthenticationFilter`, der unseren `JwtDecoder` nutzt. Ungültiges/abgelaufenes/fehlendes Token → 401, bevor der Controller läuft.
- **Claims → Identität abbilden** — `jwtAuthenticationConverter()`:
  - `setPrincipalClaimName("sub")`, damit `auth.getName()` die **userId** zurückgibt.
  - der `role`-Claim ist ein einzelner String, verpackt als `ROLE_<role>` zu einer `GrantedAuthority`.
- **Stateless-Policy** — `sessionManagement(SessionCreationPolicy.STATELESS)`: Spring erzeugt nie eine `HttpSession`.
- **Passwort-Sicherheit** — ein `BCryptPasswordEncoder`-Bean; `AuthController.register` ruft `encode()`, `login` ruft `matches()`. Klartext-Passwörter werden nie gespeichert.

### 7.5 🔴 Die goldene Regel: niemals der Identität aus dem Body vertrauen

Controller setzen die Eigentümerschaft aus dem Token, nicht aus dem Payload:
- `JobController.create`: `job.clientId = auth.getName();` — das `clientId` im Body wird ignoriert.
- `ChatService.sendMessage`: `message.senderId = currentUserId;` — serverseitig gesetzt.
- `ApplicationController.apply`: `designerId = auth.getName();`.

Das ist die wichtigste Sicherheits-Invariante in der Codebasis. Jeder neue Endpunkt muss ihr folgen.

### 7.6 🔴 Autorisierungsregeln (wer was darf)

Aus `SecurityConfig.filterChain`, der Reihe nach:
- `permitAll`: `/auth/**`, `/world-clock`, **GET** `/jobs/**`, **GET** `/designers/**`, und statische Assets (`/*.html`, `/*.css`, `/*.js`, `/images/**`, …).
- alles andere → `authenticated()`.

🔴 **Um einen öffentlichen Endpunkt hinzuzufügen, musst du seinen Matcher zuerst hier eintragen**, sonst gibt es 401, bevor der Controller erreicht wird. Rollen-Prüfungen (z. B. „nur Designer dürfen sich bewerben") passieren derzeit **innerhalb der Controller** (`ApplicationController.isDesigner`), nicht via `hasRole(...)` in der Filterkette — für Reviewer erwähnenswert, eine leichte Inkonsistenz.

### 7.7 🔴 Bekannte Grenzen dieses Auth-Modells

- **Keine Token-Widerrufung / Logout ist ein No-op.** Ein gestohlenes oder geleaktes Token ist bis `exp` gültig (2 h Default). Für ein Studienprojekt akzeptabel; in Produktion bräuchte es eine Blacklist oder kurzlebige + Refresh-Tokens.
- **Symmetrischer Secret** — wer `app.jwt.secret` hat, kann Tokens ausstellen. Der Default-Dev-Secret **muss** über `APP_JWT_SECRET` (≥32 Zeichen) in jeder Nicht-Dev-Umgebung überschrieben werden.
- **Tokens im `localStorage`** (Frontend) sind von jedem JS auf der Seite lesbar → XSS-exponiert. Für die Aufgabe in Ordnung; httpOnly-Cookies wären die gehärtete Alternative.

---

## 8. Feature- & Endpunkt-Status (gegen den Code verifiziert, Juni 2026)

> ⚠️ Die „status at a glance"-Tabelle in `backend/README.md` ist **veraltet** — sie listet `application/`, `chat/` als Stubs, obwohl sie implementiert sind. Dieser Tabelle vertrauen (durch Lesen der Controller erstellt).

| Paket | Echter Zustand | Endpunkte | Anmerkungen |
|---|---|---|---|
| `auth/` | ✅ Fertig | `POST /auth/register`, `POST /auth/login`, `POST /auth/logout` (No-op), `GET /auth/me` | Solide. |
| `session/` | ✅ Fertig | — | Nur `JwtService`. |
| `config/` | ✅ Fertig | — | Security + statisches Serving. |
| `job/` | 🟠 Größtenteils | `POST /jobs`, `GET /jobs` (+ Such-Parameter), `GET /jobs/{id}`, `DELETE /jobs/{id}` *(neu 2026-06-10)* | Suche durchgängig verdrahtet (`23d4dda`). **`PUT /jobs/{id}` weiterhin NICHT implementiert** (B3, letzte Backend-Lücke). Delete-Autorisierung zu lasch — siehe **B22**. |
| `application/` | ✅ Fertig (Review) | `POST /jobs/{jobId}/apply`, `GET /jobs/{jobId}/applications`, `GET /applications/{id}`, `PUT /applications/{id}/status`, `POST /applications/{id}/hire` | Hire ist ein Stub-Trigger (noch kein Vertrag). Apply validiert jetzt Job-Existenz + lehnt Duplikate ab (B7 ✅); Liste ist Owner-only (B2 teilweise) — get/status/hire weiterhin ungeprüft, siehe §9. |
| `location/` | ✅ Fertig *(neu 2026-06-10)* | `GET /locations/countries`, `GET /locations/cities?country=…` | Länder als hartkodierte Liste; Städte via `countriesnow.space` proxied (2. externe API → S1). Öffentlich via `SecurityConfig`. |
| `chat/` | ✅ Fertig | `GET/POST /conversations`, `GET/POST /conversations/{id}/messages` | Lokaler H2-Modus; externer API-Pfad hinter `USE_EXTERNAL_CHAT_API=false`. |
| `worldclock/` | ✅ Fertig | `GET /world-clock` | Demo-Proxy zu timeapi.io. |
| `user/` | ❌ Stub | `/designers`, `/designers/{id}`, `/designers/{id}/portfolio…`, `/users/{id}` | **Alle liefern `501 not_implemented`.** Profile + Portfolio. |
| `contract/` | ❌ Stub | `/contracts…` | Phase 2. Hire-Flow hat ein `TODO`, es aufzurufen. |
| `moderation/` | ❌ Stub | `/moderation…`, Reports | Phase 2. |

---

## 9. Bugs & Korrektheitsprobleme (diese zuerst reviewen)

Nach Schweregrad geordnet. Gefunden durch zeilenweises Lesen jeder Backend-Quelldatei (Controller, Services, Repositories) und Verfolgen der Aufrufstellen gegen den tatsächlichen Code (mehrere `xhigh`-Review-Durchgänge — Backend, Frontend und Test-Harness). **Das sind die konkreten Dinge, die in den 6 Tagen zu beheben sind.** Nummerierung: **B1–B13** erster Durchgang, **B14–B21** der tiefe Backend-Review (§9), **B22** neu 2026-06-11, Harness **H1–H5** in §9b, Frontend **F1–F6** in §9c. Status-Markierungen: ✅ behoben · 🟠/🟡 teilweise behoben oder herabgestuft · unmarkiert = weiterhin offen.

### ✅ B1 — ~~Login ist case-sensitiv bei der E-Mail~~ — BEHOBEN 2026-06-11 (`219e278`)
`login` normalisiert die E-Mail jetzt genau wie die Registrierung (`trim().toLowerCase()`) vor `findByEmail`. Regression-Test `KnownBugsTest.b1` ist grün.
<details><summary>Ursprünglicher Befund</summary>
`AuthController.register` speicherte `user.email = req.email.trim().toLowerCase()`, aber `login` fragte `findByEmail(req.email)` mit der **rohen** Eingabe ab. Wer sich als `John@Example.com` registrierte (kleingeschrieben gespeichert), konnte sich mit derselben Schreibweise nie einloggen → `401 invalid email or password`.
</details>

### 🟠 B2 — Autorisierungslücken im Bewerbungs-/Engagement-Flow — TEILWEISE BEHOBEN 2026-06-11 (`22c5c9b`)
In `ApplicationController`:
- ✅ `GET /jobs/{jobId}/applications` — **behoben:** lädt den Job, liefert 404 wenn er fehlt, 403 außer `job.clientId == auth.getName()`. Test `b2` grün + Unit-Tests in `ApplicationControllerTest`.
- ❌ `GET /applications/{id}` — **weiterhin offen:** keine Eigentümer-Prüfung.
- ❌ `PUT /applications/{id}/status` und `POST /applications/{id}/hire` — **weiterhin offen:** jeder authentifizierte Nutzer kann jede Bewerbung annehmen/ablehnen/engagieren.

Verbleibender Fix: dasselbe Muster (Bewerbung laden → ihren Job laden → `job.clientId` gegen `auth.getName()` vergleichen → 403). Das `JobRepository` ist inzwischen bereits in den Controller injiziert, also eine kleine Änderung.

### 🟠 B3 — `PUT`/`DELETE /jobs/{id}` beworben, aber unerreichbar — HALB BEHOBEN 2026-06-10 (`42fb250`)
- ✅ **`DELETE /jobs/{id}` existiert jetzt** (Auth + 404 + Autorisierungs-Prüfung) und das FE ruft es auf (Löschen-Button in `job-detail.html`, `f3d26e9`). Schließt **M6**. *Aber siehe **B22** — die Delete-Autorisierungsregel ist zu lasch.*
- ❌ **`PUT /jobs/{id}` fehlt weiterhin** — `JobController` hat kein `@PutMapping`; `JobRepository.update()` bleibt toter Code; ein Client kann einen geposteten Job nicht bearbeiten. Test `b3` ist rot. Vorsicht für die Person, die es verdrahtet: `JobRepository.update()` überschreibt `client_id` und `created_at` aus dem Request-Body — eine `auth.getName() == job.clientId`-Eigentümer-Prüfung ergänzen und den Body nicht `clientId`/`createdAt` setzen lassen.

### 🟡 B4 — Random-Job-Seite kaputt — SYMPTOM BEHOBEN 2026-06-10 (`7ddc891`), Backend-Route fehlt weiterhin
Der nutzersichtbare Bug ist weg: `job-random.html` wurde von einem hartkodierten Fake-Job umgebaut auf `GET /jobs` mit **clientseitiger** Zufallsauswahl. Derselbe Commit hat die „View Job"-Buttons der Suchergebnisse auf die echte `job-detail.html?id=…`-Seite umgebogen.
**Weiterhin offen (und der Grund, warum Test `b4` rot ist):** Es gibt **keine `GET /jobs/random`-Backend-Route** — ein Request auf `/jobs/random` wird weiterhin von `@GetMapping("/{id}")` mit `id="random"` geschluckt → 404, und `JobRepository.getRandomJob()` bleibt toter Code. Team-Entscheidung nötig: entweder die Route **oberhalb** des `/{id}`-Mappings ergänzen (Reihenfolge zählt) und die Seite sie nutzen lassen, oder den clientseitigen Ansatz akzeptieren und `getRandomJob()` entfernen + den `b4`-Test umwidmen.

### ✅ B5 — ~~Registrierung mit Duplikat-E-Mail liefert 500 statt 409~~ — BEHOBEN 2026-06-11 (`219e278`)
`register` normalisiert die E-Mail jetzt einmal und nutzt den normalisierten Wert sowohl für den `existsByEmail`-Guard als auch fürs Speichern. Dadurch wird `Foo@x.com` gegen ein bestehendes `foo@x.com` sauber im Voraus als **409** abgefangen. Test `b5` ist grün. (Gleiche Ursache wie B1.)

### 🟠 B6 — Hire-Flow erzeugt keinen Vertrag
`ApplicationController.hire` setzt den Status auf `HIRED` und lässt `// TODO: trigger contract creation` stehen. Der in der UI versprochene Kern-„Happy Path" (hire → Vertrag) ist unvollständig, weil `contract/` ein Stub ist.

### 🟡 B7 — Keine Validierung/Eindeutigkeit bei Bewerbungen & Conversations — GRÖSSTENTEILS BEHOBEN 2026-06-11 (`a72592d`)
- ✅ `applications` hat jetzt **`UNIQUE (job_id, designer_id)`** (inkl. `ALTER TABLE … IF NOT EXISTS`-Migration für bestehende DB-Dateien — falls eine alte Datei bereits Duplikate enthält, schlägt der Start fehl: DB zurücksetzen).
- ✅ `apply` validiert, dass der Job existiert (**404**) und lehnt Duplikate im Voraus ab (**409** „you have already applied"), mit dem Constraint als Backstop. Test `b7` ist grün.
- ✅ Das **TOCTOU-Race** in `ConversationRepository.create` ist behoben: Der Insert-Verlierer fängt die Unique-Verletzung ab und gibt die bestehende Conversation zurück, statt 500 zu werfen.
- ❌ **Weiterhin offen:** `ChatService.createConversation` prüft nicht, ob die referenzierten `jobId`/Gegenparteien existieren (auch keine FK-Constraints) → verwaiste Conversations bleiben möglich. Überschneidet sich mit **B14** (Conversation-Spoofing) — beides gemeinsam beheben.

### 🟠 B8 — Veraltete Paket-READMEs vs. Code
`chat/README.md` und die backend-README-„status at a glance" nennen `chat/` und `application/` weiterhin „501 stubs", obwohl sie implementiert sind. Irreführende Doku führt dazu, dass Leute funktionierenden Code neu implementieren oder ihm misstrauen. Aktualisieren (und §8 dieses Dokuments vertrauen).

### 🟡 B9 — Nachrichten-Pagination liefert älteste Nachrichten zuerst
`MessageRepository.findByConversationId` sortiert `created_at ASC` mit `LIMIT 50 OFFSET page*50`, sodass Seite 0 die **ältesten** 50 Nachrichten sind und neue Nachrichten auf immer höheren Seitenzahlen landen. Für eine Chat-UI will man fast immer die jüngste Seite zuerst. *Klein, aber lässt den Chat mit wachsender Historie kaputt wirken.*

### 🟡 B10 — Designer-Profildaten sind aufgeteilt & unerreichbar
Die Registrierung speichert `designType`/`skills` am User (`auth`), aber die **`/designers`-Endpunkte, die sie anzeigen würden, sind alle 501** (Paket `user/`). Designer-bezogene Seiten können also noch keine Profile zeigen.

### 🟡 B11 — Keine globale Exception-Behandlung außerhalb von Chat
Nur `ChatController` hat einen `@ExceptionHandler`. Jede Repository-`RuntimeException` (jedes Repo verpackt eine `SQLException` darin) in einem anderen Controller erscheint als Standard-Spring-500 mit Stacktrace. Einen `@ControllerAdvice` hinzufügen.

### 🟡 B12 — Dev-Secret & DB-Credentials sind committete Defaults
Der `app.jwt.secret`-Default und H2 `sa`/kein-Passwort sind nur für Dev, aber leicht versehentlich auszuliefern. Der symmetrische Secret bedeutet, dass jeder, der ihn hat, gültige Tokens erzeugen kann. Sicherstellen, dass jedes Deploy/jeder Bewertende `APP_JWT_SECRET` (≥32 Zeichen) überschreibt.

### ⚪ B13 — Top-Level-`README.md` verweist auf `design1/`/`design2/`
Das aktive Frontend ist `design3/`. Klein, aber für Neulinge verwirrend. `frontend/design3/README.md` vertrauen.

### Zusätzliche Backend-Befunde (aus dem tiefen `xhigh`-Review, 2026-06-10)

### 🔴 B14 — Conversation-Spoofing in `ChatService.createConversation`
`createConversation` prüft nur `currentUserId.equals(clientId) || currentUserId.equals(designerId)` — d. h. der Aufrufer muss *einer* der beiden Teilnehmer sein, aber der **andere** Teilnehmer, die Job-Eigentümerschaft und die Existenz werden nie validiert. Ein Designer kann `{clientId: <beliebiges Opfer>, designerId: self, jobId: irgendetwas}` posten und einem beliebigen Nutzer eine Conversation aufzwingen, ihm dann unerwünschte Nachrichten schicken. *Fix: prüfen, dass der Job existiert, dass `clientId` der echte Eigentümer des Jobs ist und dass die Gegenseite tatsächlich zum Job gehört.*

### 🔴 B15 — Externe Time-API hat kein HTTP-Timeout
`ExternalTimeApiClient` baut `HttpClient.newHttpClient()` und einen `HttpRequest` **ohne Connect- oder Request-Timeout**. Wenn `timeapi.io` hängt, blockiert `GET /world-clock` einen Tomcat-Worker unbegrenzt; wiederholte Aufrufe erschöpfen den Pool. *Fix: `.connectTimeout(...)` am Client und `.timeout(...)` am Request setzen.*

### 🟠 B16 — Lexikografische Zeitstempel-Sortierung ist an Sekundengrenzen falsch
`created_at` wird als `Instant.toString()` (variable Länge der Sekundenbruchteile) in einem `VARCHAR` gespeichert und mit `ORDER BY created_at` sortiert. `'2026-…T12:00:00Z'` sortiert lexikografisch *nach* `'2026-…T12:00:00.5Z'` (`'.'`=46 < `'Z'`=90), was die falsche chronologische Reihenfolge ist. Betrifft Job-Liste/Suche, Nachrichten, Conversations, sobald ein Zeitstempel auf einer vollen Sekunde landet. *Fix: einen Zeitstempel mit fester Breite / UTC-Millis oder eine sortierbare numerische Spalte speichern.*

### 🟠 B17 — Widersprüchliche CORS-Konfiguration
`config/WebConfig.addCorsMappings` registriert eine **zweite** CORS-Policy (hartkodierte `localhost:63342`-Origins), die mit der einzigen `SecurityConfig.corsConfigurationSource` konkurriert, die das Design beabsichtigt (CORS sollte einmal, zentral konfiguriert werden). Das Verhalten weicht zwischen security-gefilterten API-Pfaden und MVC-/Handler-Pfaden ab. *Fix: `WebConfig.addCorsMappings` löschen; nur die `SecurityConfig`-Source behalten.*

### 🟡 B18 — `/world-clock` ist Alles-oder-nichts und sequenziell
`WorldClockService` macht 4 blockierende externe Aufrufe nacheinander und lässt jeden einzelnen Fehler werfen, sodass eine langsame/fehlerhafte Stadt den ganzen Endpunkt scheitern lässt und die Latenz die Summe von 4 Round-Trips ist. *Fix: parallel abrufen und pro Stadt graceful degradieren.*

### 🟡 B19 — Hire-/Status-Übergang hat ein TOCTOU
`ApplicationController.hire`/`updateStatus` prüfen den aktuellen Status in Java und führen dann ein bedingungsloses `UPDATE` aus. Gleichzeitige Requests können beide die Prüfung passieren und doppelt verarbeiten — und sobald hire einen Vertrag erzeugt (B6), zwei Verträge produzieren. *Fix: bedingtes `UPDATE … WHERE id=? AND status=?` und betroffene Zeilen prüfen.*

### 🟡 B20 — `app.frontend.path`-Default zeigt auf veraltetes `design1/`
`WebConfig`s `@Value("${app.frontend.path:../frontend/design1/}")` defaultet auf ein Verzeichnis, das nicht existiert (aktives FE ist `design3/`). Funktioniert nur, weil `application.properties` es überschreibt; jede Umgebung, der diese Property fehlt, liefert 404er. *Fix: den Default auf `../frontend/design3/` setzen.*

### ⚪ B21 — Kein Connection-Pooling
`Database.getConnection()` öffnet pro Repository-Aufruf eine frische `DriverManager`-Verbindung (kein Pool). Funktional ok für eingebettetes H2, aber verschwenderisch und unter Last unbegrenzt. *Fix: ein gepooltes `DataSource` (HikariCP).* *(Effizienz/Altitude, kein Korrektheits-Bug.)*

### 🟠 B22 — *(neu 2026-06-11)* `DELETE /jobs/{id}` lässt **jeden Designer** **jeden Job** löschen
Der neue Delete-Endpunkt (`42fb250`) autorisiert `isOwnerClient **|| isDesigner**` — d. h. neben dem besitzenden Client darf *jeder* eingeloggte Designer *jeden* Job löschen, auch Jobs, zu denen er keinerlei Beziehung hat. Das Javadoc nennt das absichtlich („A logged-in designer may also delete it"), aber es widerspricht der Eigentümer-Invariante aus §7.5 und gibt einer ganzen Rolle faktisch ein destruktives Recht. *Fix: den `isDesigner`-Zweig entfernen — nur `job.clientId == auth.getName()` (und ggf. eine künftige Admin-Rolle) sollte löschen dürfen.*

---

## 9b. Test- & Build-Harness-Befunde (aus dem Code-Review)

Das sind **keine** Anwendungs-Bugs — es sind Probleme in der Test-Suite, der Build-Konfiguration und der Repo-Hygiene, die beim Hinzufügen des Test-Harness eingeführt/aufgedeckt wurden. Aufgeführt, weil sie das Sicherheitsnetz selbst untergraben. (Verifiziert am 2026-06-10.)

### 🔴 H1 — `mvn test` erzeugt keinen JaCoCo-Coverage-Report mehr
Das JaCoCo-`report`-Goal ist an die `test`-Phase gebunden, aber die absichtlich roten TDD-Tests (das Bug-Board in §5 von `test.md`) lassen Surefire scheitern und **brechen die Phase ab, bevor `jacoco:report` läuft**. Verifiziert: `mvn test` endet mit `1`, und `target/site/jacoco/jacoco.csv` wird nie neu erzeugt.
**Auswirkung:** Der in `test.md` dokumentierte Coverage-Workflow („`target/site/jacoco/index.html` öffnen") produziert still nichts, solange das Board rot ist — also immer, bis die Bugs behoben sind.
**Fix:** Coverage mit `mvn test -Dmaven.test.failure.ignore=true` erzeugen, oder `jacoco:report` an die `verify`-Phase binden, oder ein dediziertes Coverage-Profil ergänzen. Den gewählten Befehl in `test.md` dokumentieren.

### 🟠 H2 — Wirkungslose Passwort-Encoding-Assertion (falsches Vertrauen)
In `AuthControllerTest` ist die Prüfung „Passwort gehasht, nicht roh" `assertThat(saved.passwordHash).isNotEqualTo("secret123")`. Aber `passwordEncoder` ist ein Mockito-Mock, dessen nicht gestubbtes `encode()` `null` zurückgibt, sodass die Assertion `null != "secret123"` → trivialerweise wahr ist. **Sie würde sogar bestehen, wenn `register()` das rohe Passwort speichern würde.**
**Fix:** stattdessen den Aufruf prüfen — `verify(passwordEncoder).encode("secret123")` — damit eine Regression, die das Encoding weglässt, den Test tatsächlich scheitern lässt.

### 🟠 H3 — Die H2-Datenbankdatei ist committet und ändert sich bei jedem Lauf
`backend/data/projectdb.mv.db` ist in git getrackt und wird von jedem `mvn spring-boot:run`, Datei-DB-Test und Newman-Lauf neu geschrieben (derzeit dirty: `36KB → 61KB`). Der `.gitignore`-Eintrag hilft nicht, weil die Datei bereits getrackt ist.
**Auswirkung:** verrauschte Binär-Diffs und versehentliche Commits von Nutzer-/Job-/Testdaten.
**Fix:** `git rm --cached backend/data/projectdb.mv.db` (weiterhin gitignored halten).

### 🟡 H4 — Fragile In-Memory-DB-Auswahl über `@SpringBootTest`-Klassen hinweg
`SecurityIntegrationTest` und `KnownBugsWebTest` wählen die In-Memory-DB, indem sie die System-Property `db.url` in einem `static`-Block setzen. Spring cached **einen** Context über beide, während die Repositories `db.url` **pro Aufruf** lesen. Sie funktionieren nur, weil beide static-Blöcke dieselbe URL setzen (`jdbc:h2:mem:springboottest`).
**Falle:** Fügt man einen dritten `@SpringBootTest` mit einer anderen `db.url` hinzu, bricht es still — die Tabellen des gecachten Contexts leben in der ersten DB, während Requests die zweite treffen → `RuntimeException: Failed to create job` (genau der Fehler, der beim Bauen dieser Suite auftrat).
**Fix:** die Test-DB-URL in einer gemeinsamen Basisklasse/Konstante zentralisieren und die Invariante dokumentieren.

### 🟡 H5 — `H2TestSupport` leakt die System-Property `db.url` (kein Teardown)
`H2TestSupport` setzt das globale `db.url` in `@BeforeEach`, stellt es aber nie wieder her, und jeder Test nutzt eine frische `mem:…;DB_CLOSE_DELAY=-1`-Datenbank, die für die Lebensdauer der JVM erhalten bleibt.
**Auswirkung:** Nach den Repository-Tests zeigt `db.url` weiterhin auf eine zufällige Wegwerf-DB; späterer Code in derselben JVM, der die Standard-*Datei*-DB erwartet, würde still eine leere, veraltete In-Memory-DB lesen. Heute harmlos (nichts hängt in den Tests an der Datei-DB), aber eine latente Cross-Test-Verschmutzungsfalle; sammelt zudem ~25+ In-Memory-DBs pro Lauf an.
**Fix:** die Property in `@AfterEach` löschen/wiederherstellen.

> Hinweis: Die eine **Produktiv**-Änderung zugunsten der Testbarkeit — `Database.getConnection()` liest die System-Properties `db.url`/`db.user`/`db.password` — ist korrekt und bewahrt die ursprünglichen Datei-DB-Defaults exakt. Keine Verhaltensregression der App.

---

## 9c. Frontend-Befunde (aus dem Code-Review, 2026-06-10)

`frontend/design3/` reviewt. Zuerst die gute Nachricht: XSS ist dort behandelt, wo es zählt — `jobs.html`, `job-detail.html`, `chat.html` escapen API-/Nutzerdaten mit `escapeHtml`; andere Seiten nutzen `textContent`/DOM-Aufbau.

### 🔴 F1 — Navbar-Shell nutzt die falschen `localStorage`-Schlüssel
`index.html` (die iframe-Shell, die Navbar + Footer rendert) liest `localStorage.getItem('token')` und entfernt beim Logout `'token'`/`'userId'`/`'role'` — aber die ganze App speichert die Session unter `designer_jobs_token`/`_userId`/`_role` (via `auth.js`). Die `auth-changed`-`postMessage`-Verdrahtung ist korrekt (Listener in `index.html:204`), aber `updateAuthNavigation` liest immer `null`, sodass:
- nach dem Login die Navbar weiter **Login/Register** zeigt und **Profile/Logout** versteckt;
- der Logout-Button nicht existierende Schlüssel entfernt und die echte Session intakt lässt (der Nutzer *wirkt* ausgeloggt, ist es aber nicht).
*Fix: die `designer_jobs_*`-Schlüssel nutzen (oder besser `window.Auth` aufrufen).*

### 🟠 F2 — Open Redirect / `javascript:`-XSS über den Login-`next`-Parameter
`login.html:179` macht `window.location.href = next || "homepage.html"` mit `next` unvalidiert aus dem Query-String. `login.html?next=https://evil.com` leitet nach dem Login auf eine fremde Seite um; `login.html?next=javascript:…` führt Skript im App-Origin aus (und kann das localStorage-Token lesen). *Fix: nur einen relativen Pfad akzeptieren — Werte ablehnen, die `:` enthalten oder mit `//` beginnen.*

### 🟠 F3 — FE setzt noch kein PUT ab (M7 halb erfüllt; CRUD halb gebaut)
Das FE setzt inzwischen **DELETE** ab (Job-löschen-Button in `job-detail.html`, Commit `f3d26e9`), aber weiterhin **kein PUT**. Damit ist **M7** nur halb geschlossen: GET/POST/DELETE werden konsumiert, aber der Pflichtpunkt verlangt auch einen FE-Aufruf mit PUT. *Fix: einen Annehmen-/Ablehnen-Button ergänzen, der `PUT /applications/{id}/status` nutzt, oder nach B3 ein Job-bearbeiten-Formular mit `PUT /jobs/{id}` bauen.*

### 🟡 F4 — World-Clock rendert externe Daten ungeescapet
`homepage.html:168` und `login.html:223` interpolieren `entry.city`/`entry.time` direkt in `innerHTML`. Geringes Risiko (serverseitig fixe Stadt, vertrauenswürdige Quelle), aber es ist die einzige netzwerkgespeiste `innerHTML`-Senke ohne Escaping. *Fix: zur Konsistenz `textContent`/`escapeHtml` nutzen.*

### 🟡 F5 — Einige Seiten umgehen `Auth.authFetch`
`jobs.html:224`, `job-detail.html:250` lesen das Token manuell und rufen rohes `fetch`, sodass ihnen die zentrale 401-/Ablauf- → Login-Weiterleitung von `Auth.authFetch` entgeht. Inkonsistentes Session-Handling + duplizierte Logik. *Fix: geschützte Aufrufe über `Auth.authFetch` leiten.*

### ⚪ F6 — JWT im `localStorage` gespeichert (XSS-exponiert)
Jedes Skript im Origin kann `designer_jobs_token` lesen; in Kombination mit F2 ist es direkt ausnutzbar. Für ein Studienprojekt akzeptabel, aber erwähnenswert; httpOnly-Cookies würden es härten. (Architektonisch bereits in §7.7 angemerkt.)

### S3 — W3C-Validierungsergebnisse (validator.w3.org/nu, alle 15 Seiten)

**Nicht erfüllt — 6 Seiten haben Fehler (~17 insgesamt).** Die Fixes sind schnell:

| Seite | Fehler | Problem |
|---|---|---|
| `post-a-job.html` | 6 | `autocomplete` an Inputs, deren `type` es nicht erlaubt |
| `profile-edit.html` | 6 | gleiche `autocomplete`-Fehlnutzung |
| `register.html` | 2 | gleiche `autocomplete`-Fehlnutzung |
| `index.html` | 1 | verirrtes `</script>`-Endtag |
| `search-results.html` | 1 | `aria-label` an einem `div` ohne `role` |
| `advanced-search.html` | 1 | `label[for]` zeigt auf ein verstecktes/fehlendes Control |

Sauber (0 Fehler): `login`, `profile`, `chat`, `job-random`, `homepage`, `jobs`, `job-detail` (die letzten drei mit einer kleinen Warnung). `about`/`impressum` warnen nur (Lorem-ipsum vs. `lang="en"`). *Diese 6 Seiten fixen, um die Punkte von S3 zu holen.*

---

## 10. Restarbeit bis „fertig" (der 6-Tage-Plan)

Die Reihenfolge wird **zuerst von Bewertungspunkten** getrieben (siehe ⭐-Anforderungs-Abschnitt), dann Sicherheit/Korrektheit, dann Politur. Punkte referenzieren sowohl Bug-IDs (§9) als auch Anforderungs-IDs (M/S/C).

### Stufe 0 — die geforderten 21 Punkte absichern (MUST-Lücken — zuerst erledigen)
1. ~~**B3 + M6 + M7**~~ → **HALB ERLEDIGT 2026-06-10:** `DELETE /jobs/{id}` + FE-Löschen-Button sind da (M6 ✅, DELETE-Hälfte von M7 ✅). **Übrig: `PUT /jobs/{id}`** über `JobController` mit Eigentümer-Prüfung (den Body nicht `clientId`/`createdAt` setzen lassen) **plus ein FE-PUT-Aufruf** (Job-bearbeiten-Formular, oder billiger: Annehmen-/Ablehnen-Button → `PUT /applications/{id}/status`). Außerdem die Delete-Autorisierung verschärfen (**B22**).
2. **Verifizieren, dass M4/M5/M9 intakt bleiben** beim Editieren (AJAX, JSON, JWT) — sie sind heute erfüllt; nicht regredieren.

### Stufe 1 — Sicherheit & billig-aber-kaputte Korrektheit
3. ✅ ~~**B1 / B5 — E-Mail normalisieren**~~ — **ERLEDIGT 2026-06-11** (`219e278`). `b1`/`b5` grün.
4. 🟠 **B2 — Autorisierung fixen** in `ApplicationController`. **Teilweise erledigt 2026-06-11** (`22c5c9b`): `listApplications` ist Owner-only, `b2` grün. **Übrig: get/status/hire** haben weiterhin keine Eigentümer-Prüfung — gleiches Muster, das `JobRepository` ist bereits injiziert.
5. 🟡 **B4 — `GET /jobs/random`.** Die Seite funktioniert seit 2026-06-10 (clientseitiger Zufall, `7ddc891`); die Backend-Route fehlt weiterhin und `b4` ist rot. Entscheidung: Route ergänzen (~5 Zeilen, oberhalb von `/{id}`) oder Test umwidmen.
6. ✅ ~~**B7 — referentielle Validierung + Eindeutigkeit**~~ — **GRÖSSTENTEILS ERLEDIGT 2026-06-11** (`a72592d`): unique `(job_id, designer_id)` + saubere 404/409 in `apply` + idempotentes Conversation-Create. `b7` grün. Übrig: Job-/User-Existenzprüfungen in `createConversation` (in **B14** einarbeiten).

> Tracking: Jeder Fix oben dreht einen roten Test auf dem §5-Board von `test.md` auf grün. **Stand 2026-06-11: 2 rote Tests übrig (`b3`, `b4`).** Das Projekt ist (korrektheitsseitig) „fertig", wenn dieses Board komplett grün ist.

### Stufe 2 — die SHOULD-Punkte holen (8)
7. ✅ ~~**S1 — einen zweiten externen REST-Service ergänzen.**~~ — **ERLEDIGT 2026-06-10** (`371b55f`): `countriesnow.space` via `location/` + Standort-Autofill im Profil-Editor. Zählt Richtung **C1** (eine weitere API nötig).
8. **S2 — eine zweite FE-Komponente**, die ≥3 BE-Endpunkte anspricht (z. B. ein Moderations-Dashboard oder eine öffentliche Designer-Portfolio-Seite).
9. **S3 — jede FE-Seite durch `validator.w3.org` schicken** und HTML-Fehler beheben.
10. **S4 — responsive Mobil- + Desktop-Ansichten bestätigen/fertigstellen**; die Breakpoints dokumentieren.
11. **`user/`-Paket — Designer-Profile & Portfolio** (`GET /designers`, `GET /designers/{id}`, dann `PUT` + Portfolio-CRUD). Bedient S2 und macht die §6-Stub-Tests grün.

### Stufe 3 — nach den COULD-Punkten greifen (5) + restliche Politur
12. **C1 — dritter externer REST-Service** (nach S1).
13. **C2 — XML-Ausgabe** neben JSON (`jackson-dataformat-xml` + `produces`).
14. **C3 — ein `PATCH`-Endpunkt** (partielles Job-/Profil-Update), vom FE konsumiert.
15. **B6 — Vertragserzeugung beim Engagieren**; **B11 — globaler `@ControllerAdvice`**; **B9 — Nachrichten-Pagination neueste zuerst**; **B8 — Paket-READMEs synchronisieren**; minimale **moderation/**.

### Neu durch die tiefen Reviews aufgetaucht — in die obigen Stufen einordnen
- **Stufe 0 (Pflichtpunkte):** **F1** die `index.html`-Navbar-localStorage-Schlüssel fixen (winzig, aber die gesamte Eingeloggt-/Logout-UX ist gerade kaputt); **F3/M7** die FE-PUT-Aktion ergänzen *(DELETE erledigt 2026-06-10)*.
- **Stufe 1 (Sicherheit):** **B14** Conversation-Spoofing-Prüfung in `ChatService` *(schließt auch den B7-Rest)*; **B22** `DELETE /jobs/{id}`-Autorisierung verschärfen *(neu)*; **F2** den Login-`next`-Parameter validieren (Open Redirect / `javascript:`-XSS).
- **Stufe 1–2 (Robustheit/Korrektheit):** **B15** HTTP-Timeouts zu `ExternalTimeApiClient` ergänzen; **B16** lexikografische Zeitstempel-Sortierung fixen; **B17** die doppelte CORS-Konfig in `WebConfig` löschen; **B19** den Hire-/Status-Übergang atomar machen; **B20** den `design1/`-Pfad-Default fixen.
- **Stufe 2 (SHOULD-Punkte):** **S3** die 6 W3C-scheiternden Seiten fixen (meist verirrtes `</script>` + `autocomplete`/`aria`/`label`-Attribute — siehe §9c).
- **Build-Hygiene:** **H1** ein Coverage-Profil ergänzen, damit `mvn test` weiterhin JaCoCo ausgibt, während das Rot-Board rot ist; **H3** die H2-DB-Datei per `git rm --cached` entfernen.

### Ausdrücklich außerhalb des Rahmens (den Bewertenden sagen)
- Externer Chat-Server (`ExternalChatApiClient` bleibt ein Platzhalter, `USE_EXTERNAL_CHAT_API=false`).
- Token-Widerrufung / Refresh-Tokens.
- Echte Datenbank (bleibt bei eingebettetem H2).

---

## 11. Kurzreferenz-Karte

| Sache | Wert |
|---|---|
| Ausführen | `cd backend && mvn spring-boot:run` → `localhost:8080` |
| Bauen | `mvn package` |
| DB zurücksetzen | `backend/data/projectdb.mv.db` löschen, neu starten |
| DB | H2-Datei, `jdbc:h2:file:./data/projectdb`, User `sa`, kein Passwort |
| Auth | JWT HS256, Header `Authorization: Bearer <token>`, 2 h Ablauf |
| Token-Speicher (Frontend) | `localStorage`: `designer_jobs_token` / `_userId` / `_role` |
| Secret überschreiben | env `APP_JWT_SECRET` (≥32 Zeichen) |
| Identität in einem Controller | `auth.getName()` = userId, `auth.getAuthorities()` = `ROLE_<role>` |
| Öffentlichen Endpunkt hinzufügen | Matcher zu `SecurityConfig` **dann** Controller schreiben |
| Tests | `mvn test` (Toolchains wählt JDK 17 — braucht `~/.m2/toolchains.xml`, siehe Backend-README) — JUnit-Suite + Postman/Newman in `postman/`; absichtlich rot: **2 übrig (`b3`, `b4`)**. Siehe `test.md` |

---

*Erstellt durch Lesen des tatsächlichen Quellcodes am 2026-06-10; aktualisiert am 2026-06-11 nach den B1/B2/B5/B7-Fixes und den Arbeiten an Delete-Endpunkt / Suche / Random-Job / Locations. Wo dieses Dokument und eine Paket-README sich widersprechen, gewinnt der Code — vor dem Verlassen auf eines von beiden erneut verifizieren.*
