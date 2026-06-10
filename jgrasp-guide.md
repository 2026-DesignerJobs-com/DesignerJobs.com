# Using jGRASP with DesignerJobs.com — Visualizing & Tracing Code

> Goal: make the code understandable to **everyone** — from a first-time coder to a senior dev — by *seeing* control flow and *watching* execution step by step.

**Read this first:** jGRASP is **not** a dependency you add to the project. It is a free standalone educational IDE from Auburn University. You install it separately and open our source files in it; nothing is added to `pom.xml` or committed to the repo. This guide covers two levels:

- **Part A — Readability (near-zero setup):** Control Structure Diagrams + UML. Works for anyone, no classpath needed.
- **Part B — Execution tracing (some setup):** step-debug our JUnit tests inside jGRASP with live data viewers.

There is also an **honest-limits** section (Part D) — jGRASP is a teaching lens, not a replacement for IntelliJ when chasing live Spring HTTP requests.

---

## 0. Install (one-time)

1. Install **JDK 17** (already on this machine: Amazon Corretto 17). jGRASP needs a JDK, not just a JRE.
2. Download jGRASP from `https://www.jgrasp.org/` (or `brew install --cask jgrasp` on macOS) and launch it.
3. **Settings → PATH/CLASSPATH → Java** (or *Tools → JVM Settings*): point jGRASP's JDK at Corretto 17:
   ```
   /Users/qw13/Library/Java/JavaVirtualMachines/corretto-17.0.19/Contents/Home
   ```
   This matters for the same reason the tests do: JDK 26 breaks Mockito, and jGRASP should compile/run with 17 to match the project.

---

## Part A — Readability (no classpath, works immediately)

This part needs **zero** build setup. jGRASP parses the `.java` file directly.

### A1. Control Structure Diagram (CSD) — see the control flow

1. **File → Open** and pick any source file, e.g.
   `backend/src/main/java/at/ac/fhcampuswien/chat/ChatService.java`.
2. Click the **CSD button** (the "Generate CSD" toolbar icon) or press the CSD shortcut.
3. jGRASP draws nesting brackets and flow markers in the gutter: `if`/`else`, loops, and early returns become visual shapes. A beginner can now *see* that `sendMessage` validates content, then checks participation, then saves — without reading every token.

Good files to demo the CSD on (rich branching, easy to follow):
| File | Why it reads well as a diagram |
|---|---|
| `chat/ChatService.java` | guard clauses → 400/403, then the happy path |
| `auth/AuthController.java` | register's validation ladder is a clear staircase in CSD |
| `application/ApplicationController.java` | the status-transition rules (PENDING → ACCEPTED → HIRED) |
| `job/JobRepository.java` | the `search()` method's filter chain |

### A2. UML class diagram — see the structure

1. **File → New → Project** (a `.gpj` project file — keep it *outside* the repo or it'll show up in `git status`; e.g. save it in `~/jgrasp-projects/`).
2. **Add** the folder `backend/src/main/java` to the project.
3. Open the **UML** tab. jGRASP auto-draws classes and their dependencies. Double-click a class to jump to it; arrows show who depends on whom (e.g. `ChatController → ChatService → ConversationRepository`).

This gives the "10,000-foot view" that helps a newcomer place any class in the whole system.

> Tip: the CSD and UML are the **highest-value, lowest-effort** part of jGRASP for this project. If you only do Part A, you've already made the code far more approachable.

---

## Part B — Step-debugging our JUnit tests (execution tracing)

This is where you *watch* code run line by line. The catch: jGRASP doesn't run Maven, so you must tell it where the compiled classes and the dependency jars are. We use the JUnit tests as entry points because they're **deterministic and need no running server**.

### B1. Compile everything with Maven (once per code change)

```sh
cd backend
JAVA_HOME="$(/usr/libexec/java_home -v 17)" mvn -q test-compile
```
This produces `backend/target/classes` (main) and `backend/target/test-classes` (tests).

### B2. Generate the dependency classpath

```sh
cd backend
JAVA_HOME="$(/usr/libexec/java_home -v 17)" \
  mvn -q dependency:build-classpath \
  -Dmdep.includeScope=test \
  -Dmdep.outputFile=target/test-classpath.txt
```
This writes one long `:`-separated list of every jar (JUnit, Mockito, AssertJ, H2, Spring) to `backend/target/test-classpath.txt`. Re-run it only when dependencies change.

### B3. Point jGRASP's CLASSPATH at all three

Open **Settings → PATH/CLASSPATH → Workspace** (or per-project CLASSPATH) and add, in order:

1. `backend/target/classes`
2. `backend/target/test-classes`
3. the full contents of `backend/target/test-classpath.txt` (paste the jar list)

So the effective CLASSPATH is:
```
backend/target/classes : backend/target/test-classes : <everything in test-classpath.txt>
```
The jars resolve to your local Maven cache, e.g.:
```
~/.m2/repository/org/junit/jupiter/junit-jupiter/5.10.1/junit-jupiter-5.10.1.jar
~/.m2/repository/org/mockito/mockito-core/5.7.0/mockito-core-5.7.0.jar
~/.m2/repository/com/h2database/h2/2.3.232/h2-2.3.232.jar
... (≈40 jars total)
```

### B4. Pick a test that traces cleanly

**Best first target: `JobRepositoryTest`** — it exercises real SQL against in-memory H2 and has **no Mockito** (Mockito's runtime bytecode tricks are confusing to watch step-by-step; plain objects are not).

> One gotcha: our repository tests read the `db.url` system property to find the in-memory database. In jGRASP, open **Run → Run Arguments / JVM args** for the test and add:
> ```
> -Ddb.url=jdbc:h2:mem:jgrasp;DB_CLOSE_DELAY=-1 -Ddb.user=sa -Ddb.password=
> ```
> (Maven Surefire sets this automatically; jGRASP needs it explicitly.)

### B5. Step through it with viewers open

1. Open `backend/src/test/java/at/ac/fhcampuswien/job/JobRepositoryTest.java`.
2. Click in the gutter to set a **breakpoint** on the first line of `create_generatesIdAndTimestamp_andPersists()`.
3. Press **Debug** (the bug icon) → choose **Run as JUnit test** (jGRASP bundles a JUnit runner).
4. Use the step controls:
   - **Step In** — descend into `repository.create(...)` → `insert(...)` and watch the `PreparedStatement` get filled.
   - **Step Over** — run a call without descending.
   - **Step Out** — pop back to the caller.
5. **Open a Viewer / drag to the Canvas:** in the Debug **Variables** pane, double-click `job` (or `saved`) to open a viewer. As you step, watch `job.id` go from `null` → a UUID and `job.createdAt` get stamped — the exact behavior the test asserts, now *visible*.
6. Drag a `List<Job>` (from a `findAll()` / `search()` test) onto the **Canvas** to see the collection render and grow.

That loop — breakpoint → step → watch the viewer — is the concrete "track code execution" experience, and it's understandable without prior debugger knowledge.

### B6. Other good step-through targets

| Test | What you'll see |
|---|---|
| `JobRepositoryTest` | SQL parameter binding, result-set → object mapping (no Mockito — start here) |
| `session/JwtServiceTest` | a real JWT being built then decoded; inspect the `Jwt` claims viewer |
| `chat/ConversationRepositoryTest` | the idempotent "find-or-create" branch |
| `worldclock/WorldClockServiceTest` | the JSON → `WorldClockResponse` mapping loop (uses Mockito — flow is slightly noisier) |

---

## Part C — The Interactions tab (a Java REPL for exploring)

jGRASP's **Interactions** tab (bottom panel) lets you type Java and run it immediately against the workspace classpath — like a scratchpad. Great for "what does this do?" moments:

```java
// with backend/target/classes on the CLASSPATH:
at.ac.fhcampuswien.session.JwtService // explore types
"DESIGNER".equals("ROLE_DESIGNER".substring(5))   // true — how role checks work
java.util.UUID.randomUUID().toString()            // see the id format used everywhere
```
No `main()` needed. This is a low-stakes way for a beginner to poke at the building blocks.

---

## Part D — Honest limits (so nobody's surprised)

jGRASP is a **teaching/visualization lens**, not a full replacement for our normal tooling:

- **It won't trace a live Spring HTTP request well.** The app runs as a server; following a request through Spring's filter chain, dependency injection, and proxies is opaque in jGRASP. For that, use **IntelliJ's debugger** (attach to the running `mvn spring-boot:run` process) or add logging. The `SecurityIntegrationTest` we wrote is the readable, repeatable substitute for "watch a request get authorized."
- **Mockito-based tests look noisy when stepped.** Mocks are synthetic proxies; stepping into them shows generated bytecode, not your logic. Prefer the non-Mockito tests (Part B4) for visualization.
- **You must recompile via Maven** (`mvn test-compile`) after editing, because jGRASP isn't driving our build. Stale `target/classes` = you're debugging old code.
- **Keep jGRASP's `.gpj` project file out of the repo** (or add it to `.gitignore`) — it's machine-specific.

---

## Quick reference

| Want to… | Do this |
|---|---|
| Make one file readable | Open it → **CSD** button (Part A1) |
| See the whole structure | New Project → add `src/main/java` → **UML** tab (A2) |
| Watch code actually run | `mvn test-compile` + classpath setup (B1–B3) → debug `JobRepositoryTest` (B4–B5) |
| Explore a snippet quickly | **Interactions** tab (Part C) |
| Trace a live web request | Use IntelliJ, not jGRASP (Part D) |

| Path | Value |
|---|---|
| JDK for jGRASP | `/Users/qw13/Library/Java/JavaVirtualMachines/corretto-17.0.19/Contents/Home` |
| Main classes | `backend/target/classes` |
| Test classes | `backend/target/test-classes` |
| Dependency jars list | `backend/target/test-classpath.txt` (regenerate with B2) |
| Best first debug target | `backend/src/test/java/at/ac/fhcampuswien/job/JobRepositoryTest.java` |

---

*Companion docs: `PROJECT_REVIEW.md` (architecture + bugs), `test.md` (the automated suite this guide steps through).*
