# jGRASP mit DesignerJobs.com nutzen — Code visualisieren & Ausführung verfolgen

> Ziel: den Code für **alle** verständlich machen — vom Programmier-Neuling bis zur Senior-Entwicklerin — indem man den Kontrollfluss *sieht* und die Ausführung Schritt für Schritt *beobachtet*.

**Zuerst lesen:** jGRASP ist **keine** Abhängigkeit, die man dem Projekt hinzufügt. Es ist eine kostenlose, eigenständige Lehr-IDE der Auburn University. Man installiert sie separat und öffnet darin unsere Quelldateien; nichts wird zur `pom.xml` hinzugefügt oder ins Repo committet. Diese Anleitung deckt zwei Ebenen ab:

- **Teil A — Lesbarkeit (nahezu kein Setup):** Control Structure Diagrams + UML. Funktioniert für jeden, kein Classpath nötig.
- **Teil B — Ausführung verfolgen (etwas Setup):** Unsere JUnit-Tests in jGRASP per Schritt-Debugger mit Live-Daten-Viewern durchgehen.

Es gibt außerdem einen Abschnitt zu den **ehrlichen Grenzen** (Teil D) — jGRASP ist eine Lehr-Linse, kein Ersatz für IntelliJ, wenn man live Spring-HTTP-Requests verfolgen will.

---

## 0. Installation (einmalig)

1. **JDK 17** installieren (auf diesem Rechner bereits vorhanden: Amazon Corretto 17). jGRASP braucht ein JDK, nicht nur ein JRE.
2. jGRASP von `https://www.jgrasp.org/` herunterladen (oder unter macOS `brew install --cask jgrasp`) und starten.
3. **Settings → PATH/CLASSPATH → Java** (oder *Tools → JVM Settings*): jGRASPs JDK auf Corretto 17 zeigen lassen:
   ```
   /Users/qw13/Library/Java/JavaVirtualMachines/corretto-17.0.19/Contents/Home
   ```
   Das ist aus demselben Grund wichtig wie bei den Tests: JDK 26 bricht Mockito, und jGRASP sollte mit 17 kompilieren/laufen, passend zum Projekt.

---

## Teil A — Lesbarkeit (kein Classpath, funktioniert sofort)

Dieser Teil braucht **null** Build-Setup. jGRASP parst die `.java`-Datei direkt.

### A1. Control Structure Diagram (CSD) — den Kontrollfluss sehen

1. **File → Open** und eine beliebige Quelldatei wählen, z. B.
   `backend/src/main/java/at/ac/fhcampuswien/chat/ChatService.java`.
2. Auf den **CSD-Button** klicken (das „Generate CSD"-Symbol in der Toolbar) oder das CSD-Kürzel drücken.
3. jGRASP zeichnet Verschachtelungsklammern und Flussmarkierungen am Rand: `if`/`else`, Schleifen und frühe Returns werden zu visuellen Formen. Ein Anfänger *sieht* nun, dass `sendMessage` zuerst den Inhalt validiert, dann die Teilnahme prüft und anschließend speichert — ohne jedes Token zu lesen.

Gut geeignete Dateien für eine CSD-Demo (viel Verzweigung, leicht nachvollziehbar):
| Datei | Warum sie als Diagramm gut lesbar ist |
|---|---|
| `chat/ChatService.java` | Guard-Clauses → 400/403, dann der Happy Path |
| `auth/AuthController.java` | die Validierungs-Leiter von register ist im CSD eine klare Treppe |
| `application/ApplicationController.java` | die Status-Übergangsregeln (PENDING → ACCEPTED → HIRED) |
| `job/JobRepository.java` | die Filterkette der `search()`-Methode |

### A2. UML-Klassendiagramm — die Struktur sehen

1. **File → New → Project** (eine `.gpj`-Projektdatei — *außerhalb* des Repos ablegen, sonst taucht sie in `git status` auf; z. B. unter `~/jgrasp-projects/` speichern).
2. Den Ordner `backend/src/main/java` zum Projekt **hinzufügen**.
3. Den **UML**-Tab öffnen. jGRASP zeichnet Klassen und ihre Abhängigkeiten automatisch. Ein Doppelklick auf eine Klasse springt zu ihr; Pfeile zeigen, wer von wem abhängt (z. B. `ChatController → ChatService → ConversationRepository`).

Das liefert die „Vogelperspektive", die Neulingen hilft, jede Klasse im Gesamtsystem einzuordnen.

> Tipp: CSD und UML sind der Teil von jGRASP mit dem **höchsten Nutzen bei geringstem Aufwand** für dieses Projekt. Selbst wenn man nur Teil A macht, ist der Code schon deutlich zugänglicher.

---

## Teil B — Schritt-Debugging unserer JUnit-Tests (Ausführung verfolgen)

Hier *beobachtet* man, wie Code Zeile für Zeile läuft. Der Haken: jGRASP führt kein Maven aus, also muss man ihm sagen, wo die kompilierten Klassen und die Abhängigkeits-Jars liegen. Wir nutzen die JUnit-Tests als Einstiegspunkte, weil sie **deterministisch sind und keinen laufenden Server brauchen**.

### B1. Alles mit Maven kompilieren (einmal pro Code-Änderung)

```sh
cd backend
JAVA_HOME="$(/usr/libexec/java_home -v 17)" mvn -q test-compile
```
Das erzeugt `backend/target/classes` (Main) und `backend/target/test-classes` (Tests).

### B2. Den Abhängigkeits-Classpath erzeugen

```sh
cd backend
JAVA_HOME="$(/usr/libexec/java_home -v 17)" \
  mvn -q dependency:build-classpath \
  -Dmdep.includeScope=test \
  -Dmdep.outputFile=target/test-classpath.txt
```
Das schreibt eine lange, mit `:` getrennte Liste aller Jars (JUnit, Mockito, AssertJ, H2, Spring) nach `backend/target/test-classpath.txt`. Nur erneut ausführen, wenn sich Abhängigkeiten ändern.

### B3. jGRASPs CLASSPATH auf alle drei zeigen lassen

**Settings → PATH/CLASSPATH → Workspace** öffnen (oder den projektspezifischen CLASSPATH) und der Reihe nach hinzufügen:

1. `backend/target/classes`
2. `backend/target/test-classes`
3. den gesamten Inhalt von `backend/target/test-classpath.txt` (die Jar-Liste einfügen)

Der effektive CLASSPATH ist also:
```
backend/target/classes : backend/target/test-classes : <alles aus test-classpath.txt>
```
Die Jars verweisen auf deinen lokalen Maven-Cache, z. B.:
```
~/.m2/repository/org/junit/jupiter/junit-jupiter/5.10.1/junit-jupiter-5.10.1.jar
~/.m2/repository/org/mockito/mockito-core/5.7.0/mockito-core-5.7.0.jar
~/.m2/repository/com/h2database/h2/2.3.232/h2-2.3.232.jar
... (≈40 Jars insgesamt)
```

### B4. Einen Test wählen, der sich sauber verfolgen lässt

**Bestes erstes Ziel: `JobRepositoryTest`** — er führt echtes SQL gegen In-Memory-H2 aus und nutzt **kein Mockito** (Mockitos Bytecode-Tricks zur Laufzeit sind beim schrittweisen Durchgehen verwirrend; einfache Objekte nicht).

> Eine Stolperfalle: Unsere Repository-Tests lesen die System-Property `db.url`, um die In-Memory-Datenbank zu finden. In jGRASP unter **Run → Run Arguments / JVM args** für den Test ergänzen:
> ```
> -Ddb.url=jdbc:h2:mem:jgrasp;DB_CLOSE_DELAY=-1 -Ddb.user=sa -Ddb.password=
> ```
> (Maven Surefire setzt das automatisch; jGRASP braucht es explizit.)

### B5. Mit geöffneten Viewern durchsteppen

1. `backend/src/test/java/at/ac/fhcampuswien/job/JobRepositoryTest.java` öffnen.
2. Am Rand klicken, um einen **Breakpoint** auf die erste Zeile von `create_generatesIdAndTimestamp_andPersists()` zu setzen.
3. **Debug** drücken (das Käfer-Symbol) → **Run as JUnit test** wählen (jGRASP bringt einen JUnit-Runner mit).
4. Die Schritt-Steuerung nutzen:
   - **Step In** — in `repository.create(...)` → `insert(...)` hineingehen und beobachten, wie das `PreparedStatement` befüllt wird.
   - **Step Over** — einen Aufruf ausführen, ohne hineinzugehen.
   - **Step Out** — zurück zum Aufrufer springen.
5. **Einen Viewer öffnen / auf das Canvas ziehen:** Im Debug-Bereich **Variables** doppelt auf `job` (oder `saved`) klicken, um einen Viewer zu öffnen. Beim Steppen sieht man, wie `job.id` von `null` → einer UUID wird und `job.createdAt` einen Zeitstempel bekommt — genau das Verhalten, das der Test prüft, nun *sichtbar*.
6. Eine `List<Job>` (aus einem `findAll()`-/`search()`-Test) auf das **Canvas** ziehen, um die Collection darzustellen und wachsen zu sehen.

Diese Schleife — Breakpoint → Step → Viewer beobachten — ist das konkrete „Code-Ausführung verfolgen"-Erlebnis und auch ohne Debugger-Vorwissen verständlich.

### B6. Weitere gute Ziele zum Durchsteppen

| Test | Was man sieht |
|---|---|
| `JobRepositoryTest` | SQL-Parameter-Bindung, ResultSet → Objekt-Mapping (kein Mockito — hier anfangen) |
| `session/JwtServiceTest` | ein echtes JWT, das gebaut und dann decodiert wird; den `Jwt`-Claims-Viewer ansehen |
| `chat/ConversationRepositoryTest` | der idempotente „find-or-create"-Zweig |
| `worldclock/WorldClockServiceTest` | die JSON → `WorldClockResponse`-Mapping-Schleife (nutzt Mockito — der Fluss ist etwas unruhiger) |

---

## Teil C — Der Interactions-Tab (eine Java-REPL zum Erkunden)

jGRASPs **Interactions**-Tab (unteres Panel) erlaubt es, Java zu tippen und sofort gegen den Workspace-Classpath auszuführen — wie ein Notizblock. Ideal für „Was macht das?"-Momente:

```java
// mit backend/target/classes im CLASSPATH:
at.ac.fhcampuswien.session.JwtService // Typen erkunden
"DESIGNER".equals("ROLE_DESIGNER".substring(5))   // true — wie Rollenprüfungen funktionieren
java.util.UUID.randomUUID().toString()            // das überall genutzte ID-Format ansehen
```
Kein `main()` nötig. Eine risikoarme Möglichkeit für Anfänger, an den Bausteinen zu stochern.

---

## Teil D — Ehrliche Grenzen (damit niemand überrascht wird)

jGRASP ist eine **Lehr-/Visualisierungs-Linse**, kein vollwertiger Ersatz für unser normales Tooling:

- **Es verfolgt einen live Spring-HTTP-Request nicht gut.** Die App läuft als Server; einen Request durch Springs Filterkette, Dependency Injection und Proxies zu verfolgen, ist in jGRASP undurchsichtig. Dafür **IntelliJs Debugger** nutzen (an den laufenden `mvn spring-boot:run`-Prozess anhängen) oder Logging ergänzen. Der von uns geschriebene `SecurityIntegrationTest` ist der lesbare, wiederholbare Ersatz für „beobachten, wie ein Request autorisiert wird".
- **Mockito-basierte Tests wirken beim Steppen unruhig.** Mocks sind synthetische Proxies; hineinzusteppen zeigt generierten Bytecode, nicht deine Logik. Für die Visualisierung die Nicht-Mockito-Tests bevorzugen (Teil B4).
- **Nach dem Editieren muss man via Maven neu kompilieren** (`mvn test-compile`), weil jGRASP unseren Build nicht steuert. Veraltete `target/classes` = du debuggst alten Code.
- **jGRASPs `.gpj`-Projektdatei aus dem Repo heraushalten** (oder zu `.gitignore` hinzufügen) — sie ist maschinenspezifisch.

---

## Kurzreferenz

| Du willst… | Mach das |
|---|---|
| Eine Datei lesbar machen | Öffnen → **CSD**-Button (Teil A1) |
| Die ganze Struktur sehen | New Project → `src/main/java` hinzufügen → **UML**-Tab (A2) |
| Code wirklich laufen sehen | `mvn test-compile` + Classpath-Setup (B1–B3) → `JobRepositoryTest` debuggen (B4–B5) |
| Ein Snippet schnell erkunden | **Interactions**-Tab (Teil C) |
| Einen live Web-Request verfolgen | IntelliJ statt jGRASP nutzen (Teil D) |

| Pfad | Wert |
|---|---|
| JDK für jGRASP | `/Users/qw13/Library/Java/JavaVirtualMachines/corretto-17.0.19/Contents/Home` |
| Main-Klassen | `backend/target/classes` |
| Test-Klassen | `backend/target/test-classes` |
| Liste der Abhängigkeits-Jars | `backend/target/test-classpath.txt` (mit B2 neu erzeugen) |
| Bestes erstes Debug-Ziel | `backend/src/test/java/at/ac/fhcampuswien/job/JobRepositoryTest.java` |

---

*Begleitdokumente: `PROJECT_REVIEW.md` (Architektur + Bugs), `test.md` (die automatisierte Test-Suite, die diese Anleitung durchsteppt).*
