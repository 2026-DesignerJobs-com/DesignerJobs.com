# DesignerJobs.com — Test-Suite-Überblick

> Begleitdokument zu `PROJECT_REVIEW.md`. Es dokumentiert die **automatisierte Test-Suite**, die als Sicherheitsnetz für Refactorings hinzugefügt wurde, was jede Testklasse abdeckt, welche Bugs die Tests gezielt festnageln und die **gemessene Code-Coverage**.

Vor dieser Arbeit hatte das Projekt **keine Tests** — kein `src/test` und kein Test-Framework im Classpath. Jetzt gibt es eine JUnit-Suite über mehrere Testklassen hinweg, plus JaCoCo-Coverage-Reporting.

---

## 1. Tests ausführen

```sh
cd backend
JAVA_HOME="$(/usr/libexec/java_home -v 17)" mvn test
```

- **Es muss auf JDK 17 laufen** (die geforderte Version des Projekts). Auf dem Rechner ist außerdem JDK 26 installiert, das `mvn` standardmäßig wählt — aber Mockitos Bytecode-Inliner kann Java 26 nicht instrumentieren, sodass **alle Mock-basierten Tests unter JDK 26 mit `Mockito cannot mock this class` fehlschlagen**. Das ist ein Umgebungsproblem, kein Test-Bug. `JAVA_HOME` wie oben auf 17 setzen.
- Der Coverage-Report wird nach `backend/target/site/jacoco/index.html` (im Browser öffnen) und `jacoco.csv` (Rohdaten) geschrieben.

### Erwarte ROT — das ist Absicht (TDD)

Die Suite ist **bewusst noch nicht komplett grün.** Wir arbeiten test-first: Es gibt einen **fehlschlagenden** Test pro offenem Bug (PROJECT_REVIEW.md §9), und das Ziel ist, das ganze Board grün zu machen, indem man *die Bugs behebt*, nicht indem man Tests löscht.

Letzter Lauf: **93 Tests — 86 grün, 7 rot.** `mvn test` endet daher mit **BUILD FAILURE**, und das ist das korrekte Signal, bis die Bugs behoben sind. Die 7 roten Tests sind in §5 aufgelistet. Coverage des grünen Codes: **86,5 % Instructions / 87,4 % Lines**.

> Kein Test prüft fehlerhaftes Verhalten. Grüne Tests zementieren bereits korrektes Verhalten; rote Tests prüfen das Verhalten, das wir *wollen* und noch nicht haben.

---

## 2. Was zum Testen hinzugefügt wurde

Zwei kleine, produktionssichere Änderungen waren nötig, weil die Codebasis keine Test-Naht hatte:

1. **`pom.xml`** — `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ) und `spring-security-test` im `test`-Scope ergänzt, plus das `jacoco-maven-plugin` für Coverage. Keine Produktiv-Abhängigkeit geändert.
2. **`Database.java`** — URL/User/Passwort der Verbindung waren fest auf die eingebettete H2-Datei-DB verdrahtet. Sie lesen jetzt die **System-Properties** `db.url` / `db.user` / `db.password` und fallen **auf exakt die ursprünglichen Defaults zurück**. Das Produktivverhalten bleibt unverändert (keine Properties gesetzt → Datei-DB). Tests setzen `db.url` auf ein Wegwerf-In-Memory-H2, sodass Repository-Tests nie `data/projectdb.mv.db` anfassen.

> Wer die `Database`-Änderung lieber nicht behalten möchte: Die Alternative wären vollständige `@SpringBootTest`-Integrationstests gegen ein Test-Profil — schwerer und langsamer. Die System-Property-Naht wurde als minimale, risikoarme Option gewählt.

---

## 3. Test-Strategie

Zwei Ebenen, passend zur Struktur der App:

| Ebene | Stil | Wie | Warum |
|---|---|---|---|
| **Controller & Services** | Schnelle Unit-Tests | Mockito-Mocks für Kollaborateure (Repositories, `Authentication`, externe Clients) | Geschäftsregeln, Validierung und Autorisierungslogik isoliert festnageln |
| **Repositories** | Integrationstests | Echtes SQL gegen ein frisches In-Memory-H2 pro Test (`H2TestSupport`) | Die Repos sind rohes JDBC — das SQL selbst ist die Logik, also muss es gegen eine echte DB laufen |

`testsupport/H2TestSupport.java` ist die Basisklasse für Repository-Tests: In `@BeforeEach` zeigt sie `Database` auf eine eindeutig benannte `jdbc:h2:mem:…;DB_CLOSE_DELAY=-1`-Datenbank und gibt damit jeder Testmethode ein unberührtes, isoliertes Schema.

---

## 4. Testklassen, eine nach der anderen

### Unit-Tests (Mockito)

| Testklasse | Ziel | Abgedeckte Kernfälle |
|---|---|---|
| `auth/AuthControllerTest` | `AuthController` | register-Validierung (fehlende Felder, falsche Rolle → 400), doppelte E-Mail → 409, **Rolle auf Großschreibung normalisiert**, **E-Mail kleingeschrieben gespeichert**, Passwort gehasht statt roh; Login-Erfolg, unbekannter Nutzer/falsches Passwort → 401; **Charakterisierungstest für den Case-Sensitivity-Bug (B1)** |
| `chat/ChatServiceTest` | `ChatService` | Feldvalidierung der Conversation → 400, Nicht-Teilnehmer → 403, Teilnehmer persistiert; **`sendMessage` überschreibt gefälschte `senderId`/`conversationId` mit Server-Werten**, leerer Inhalt → 400; Teilnehmer-Guards und Delegation von `getMessages`/`listConversations` |
| `chat/ChatControllerTest` | `ChatController` | Delegation + Statuscodes (201 bei create/send), `page`-Durchreichung, nicht authentifiziert → 401 via `ResponseStatusException`, das `@ExceptionHandler`-Mapping |
| `application/ApplicationControllerTest` | `ApplicationController` | apply erfordert DESIGNER-Rolle (401/403/201), Status-Übergangsregeln (ungültiger Status → 400, nur PENDING akzeptierbar, 404 wenn fehlend), hire nur bei ACCEPTED; **Charakterisierungstest für die fehlende Job-Ownership-Prüfung (B2)** |
| `job/JobControllerTest` | `JobController` | create erfordert Auth (401) und Titel (400), **`clientId` aus dem Token statt aus dem Body**, getById 200/404, search-Delegation |
| `session/JwtServiceTest` | `JwtService` | ausgestelltes Token läuft durch einen echten Nimbus-Decoder zurück: korrekte `sub`, `role`, `iat`, `exp`; Ablauf liegt ~2 h in der Zukunft |
| `worldclock/WorldClockServiceTest` | `WorldClockService` | mappt die Antwort der externen API in 4 Stadt-Einträge; toleriert fehlende JSON-Felder mit leeren Defaults |
| `worldclock/WorldClockControllerTest` | `WorldClockController` | delegiert an den Service |
| `stubs/StubControllersTest` | `UserController`, `ContractController`, `ModerationController` | jeder Endpunkt liefert aktuell **501**; nagelt den Stub-Status fest, sodass die Implementierung eines Pakets den Test bricht und einen echten Test erzwingt |

### Repository-Tests (In-Memory-H2)

| Testklasse | Ziel | Abgedeckte Kernfälle |
|---|---|---|
| `job/JobRepositoryTest` | `JobRepository` | create vergibt id+Zeitstempel, findById Treffer/Fehlschlag, findAll, search (Keyword/Kategorie/Null-Filter), `getRandomJob` Treffer/leer, update, deleteById Treffer/Fehlschlag |
| `auth/UserRepositoryTest` | `UserRepository` | save→findByEmail/findById Roundtrips, existsByEmail, **Charakterisierungstest, dass Lookups case-sensitiv sind (B1/B5)** |
| `chat/ConversationRepositoryTest` | `ConversationRepository` | id-/Zeitstempel-Vergabe, **idempotentes create für gleiche Teilnehmer+Job**, findByUserId auf beiden Seiten, `isParticipant` true/false/unbekannt |
| `chat/MessageRepositoryTest` | `MessageRepository` | save stempelt id/Zeit und löscht das Flag, Beschränkung auf eine Conversation, **Pagination zu 50 pro Seite**, negative Seite als erste behandelt |
| `application/JobApplicationRepositoryTest` | `JobApplicationRepository` | create defaultet auf PENDING, findByJobId-Beschränkung, findById Treffer/Fehlschlag, updateStatus, **Charakterisierungstest für doppelte Bewerbungen (B7)** |
| `Database/DatabaseInitializerTest` | `DatabaseInitializer` | `init()` erstellt die `jobs`-Tabelle auf einer frischen DB |

---

## 5. Das rote Board — ein fehlschlagender Test pro offenem Bug (TDD-To-do-Liste)

`bugs/KnownBugsTest` und `bugs/KnownBugsWebTest` prüfen jeweils das **korrekte** Verhalten für einen bekannten Defekt aus `PROJECT_REVIEW.md` §9. Sie waren **beim Schreiben ROT** und werden erst grün, wenn der Bug tatsächlich behoben ist. Sie laufen im normalen `mvn test` — nichts ist ausgeschlossen oder per Tag weggefiltert. Das ist die ausführbare To-do-Liste des Teams: **Das Projekt ist fertig, wenn dieses Board grün ist.**

> **Stand 2026-06-11: Board komplett.** `b1`, `b5`, `b2`, `b7`, `b3` (put + delete) sind grün; `b4` wurde per **Team-Entscheidung ausgemustert** — Random-Job ist clientseitig by Design, es gibt bewusst keine `GET /jobs/random`-Route (siehe `PROJECT_REVIEW.md` §B4). `mvn test` = BUILD SUCCESS, der JaCoCo-Report wird wieder erzeugt (H1 gelöst). Die Tabelle unten bleibt als historische Spezifikation stehen.

| Fehlschlagender Test | Prüft (das gewünschte Verhalten) | Schlägt heute fehl mit | Bug |
|---|---|---|---|
| `b1_loginShouldSucceedRegardlessOfEmailCase` | Login liefert 200 für die E-Mail, wie der Nutzer sie getippt hat | `401` | B1 |
| `b5_duplicateEmailDifferentCaseShouldReturn409NotCrash` | doppelte E-Mail (egal welche Groß-/Kleinschreibung) → sauberes `409` | `RuntimeException` (UNIQUE-Verletzung = der 500-Absturz) | B5 |
| `b2_listApplicationsByNonOwnerShouldBeForbidden` | ein Nicht-Eigentümer listet Bewerber → `403` | `200` | B2 |
| `b7_duplicateApplicationsShouldNotBeStored` | höchstens 1 Bewerbung pro `(Job, Designer)` | `2 Zeilen` gespeichert | B7 |
| `b4_getRandomJobShouldBeReachable` | `GET /jobs/random` → `200` | `404` (von `/{id}` überdeckt) | B4 |
| `b3_putJobShouldUpdateExistingJob` | `PUT /jobs/{id}` → `200` | `405` (kein Handler) | B3 |
| `b3_deleteJobShouldRemoveExistingJob` | `DELETE /jobs/{id}` → `2xx` | `405` (kein Handler) | B3 |

**Wie man dieses Board nutzt (rot → grün):** Einen fehlschlagenden Test wählen, seine Assertion lesen (das ist die Spezifikation), den Code in `backend/src/main` reparieren, bis er besteht. Nicht den Test an den Bug anpassen — den Code an den Test anpassen. Ergebnis: 6 von 7 wurden grün; `b4` wurde per expliziter Team-Entscheidung ausgemustert (umgewidmet, nicht geschummelt — das Feature ist clientseitig umgezogen).

### Bereits zementiertes Verhalten (grün)

Die grünen Tests prüfen Verhalten, das **bereits korrekt** ist, sodass ein Refactoring es nicht still brechen kann: `clientId`/`senderId` werden serverseitig aus dem Token gesetzt (nie aus dem Request-Body), Rollen-Normalisierung + E-Mail-Kleinschreibung bei register, Passwort-Hashing, Chat-Zugriff nur für Teilnehmer, Korrektheit von JWT-Claims/-Ablauf und die vollständige `SecurityConfig`-Autorisierungsmatrix.

---

## 6. Coverage (mit JaCoCo gemessen)

Gesamt: **86,5 % Instructions / 87,4 % Lines** Coverage (Standard-Suite). Jede Klasse mit echter Verzweigungslogik ist zu ≥ 75 % abgedeckt.

| Klasse | Instr. | Anmerkungen |
|---|---:|---|
| `SecurityConfig` | 99 % | abgedeckt durch `SecurityIntegrationTest` (Filterkette + JWT) |
| `ChatService` | 100 % | |
| `JwtService` | 100 % | |
| `WorldClockService` | 100 % | |
| `JobController` | 100 % | |
| `WorldClockController` | 100 % | |
| `UserController` / `ContractController` / `ModerationController` | 100 % | Stub-501er |
| `ChatController` | 98 % | |
| `MessageRepository` | 89 % | nicht abgedeckt = SQL-`catch`-Blöcke |
| `JobRepository` | 88 % | nicht abgedeckt = `catch`-Blöcke / `add()`-Alias |
| `ConversationRepository` | 88 % | |
| `JobApplicationRepository` | 87 % | |
| `UserRepository` | 87 % | |
| `Database` | 85 % | |
| `AuthController` | 75 % | nicht abgedeckt = `/auth/me`, `/auth/logout` |
| `ApplicationController` | 75 % | nicht abgedeckt = einige Auth-/Frühausstiegs-Zweige |
| `DatabaseInitializer` | 68 % | nicht abgedeckt = `catch`-Block |

### Bewusst **nicht** abgedeckt (0 %) — und warum

| Klasse | Warum kein Unit-Test |
|---|---|
| `SecurityConfig` (49 Zeilen) | Spring-`@Configuration`; sinnvoll nur über einen vollständigen `@SpringBootTest` + `MockMvc`-Integrationstest der Filterkette testbar — siehe §7 |
| `WebConfig` | Static-Resource- + CORS-Verdrahtung; wie oben |
| `Main` | Bootstrap-Einstiegspunkt |
| `ExternalTimeApiClient` (20 Zeilen), `ExternalChatApiClient` (5) | echte ausgehende HTTP-Clients; brauchen einen Mock-HTTP-Server / Netzwerk — außerhalb des Rahmens für Unit-Tests |
| `Contract`, `Report`, `DesignerProfile`, `PortfolioItem` | leere DTO-Feldhalter für noch nicht implementierte Pakete |

---

## 7. Lücken & empfohlene nächste Tests

In Prioritätsreihenfolge, falls wir das Sicherheitsnetz im 6-Tage-Fenster weiter ausbauen wollen:

1. ~~Security-Integrationstest~~ — **erledigt** (`SecurityIntegrationTest`, `SecurityConfig` jetzt zu 99 % abgedeckt).
2. **Schreibe vor jedem neuen Feature den nächsten roten Test.** Dieselbe TDD-Schleife wie in §5: Für `user/`, `contract/`, `moderation/` zuerst den fehlschlagenden Test schreiben, der den Endpunkt spezifiziert, dann bis grün implementieren. (Die 501-Pins in `stubs/StubControllersTest` sind Platzhalter — jeden durch einen echten roten Test ersetzen, sobald du das Paket beginnst.)
3. **`AuthController./auth/me`** — aktuell nicht abgedeckt; einen Unit-Test ergänzen (authentifiziert → Profil-Map, unbekannter Nutzer → 401).
4. **End-to-End-Happy-Path-Test**, sobald `user/` und `contract/` implementiert sind: Client registrieren → Job posten → Designer registrieren → bewerben → annehmen → einstellen → Vertrag.
5. **Externe Clients** — `ExternalTimeApiClient` gegen einen Stub-Server testen (z. B. WireMock oder einen lokalen `HttpServer`), falls diese Integration tragend wird.

---

## 8. Hinweise für Reviewer / Refactorer

- Die Suite läuft in deutlich unter einer Sekunde Testzeit — halte das so; bevorzuge die Aufteilung Unit + In-Memory-DB gegenüber schwergewichtigen Context-Loads.
- Repository-Tests verlassen sich auf H2-spezifisches SQL, das bereits im Code steht (`ORDER BY RAND()`, `ALTER TABLE … ADD COLUMN IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`). Wenn die DB-Engine je wechselt, melden diese Tests (zu Recht) die Inkompatibilität.
- **Kein Test prüft fehlerhaftes Verhalten.** Die roten Tests in §5 prüfen das *gewünschte* Verhalten und schlagen fehl, bis du den Code reparierst. Bring sie zum Bestehen, indem du `src/main` änderst, niemals indem du die Assertion abschwächst.
- Ein rotes `mvn test` ist gerade erwartet (7 bekannte Bugs). „Repariere" den Build nicht, indem du diese Tests überspringst — behebe die Bugs.
- Am Rande beim Schreiben dieser Tests gefunden: `WebConfig.addCorsMappings` fügt eine **zweite, abweichende** CORS-Konfiguration hinzu, obwohl CORS in `SecurityConfig` zentralisiert sein soll. Noch nicht durch Tests abgedeckt; sollte abgeglichen werden (Kandidat für eine Ergänzung in `PROJECT_REVIEW.md` §9).

---

*Erstellt am 2026-06-10. Letztes `mvn test` auf JDK 17: 93 Tests, 86 grün / 7 rot (das Bug-Board aus §5). JaCoCo erneut ausführen, um die Coverage zu aktualisieren.*
