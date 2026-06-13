# backend — DesignerJobs.com

Spring Boot 3.2 REST API on Java 17. Job listings and user accounts persist to an embedded H2 file database. Auth is stateless JWT — bearer tokens issued at login, verified by Spring's OAuth2 Resource Server.

The same Spring process also serves the `frontend/landing/` static files via `WebConfig`, so the whole app runs on `http://localhost:8080` in dev.

---

## run

```sh
cd backend
mvn spring-boot:run
```

Requires JDK 17 + Maven. The server starts on `http://localhost:8080`. The H2 file at `data/projectdb.mv.db` is created on first run. To reset state: delete that file and restart.

### JDK 17 via Maven toolchains

The build compiles and runs tests with JDK 17 even if your Maven/`JAVA_HOME` points at a newer JDK (newer JDKs break Mockito and JaCoCo). This needs a one-time `~/.m2/toolchains.xml` on your machine pointing at a local JDK 17 install:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains xmlns="http://maven.apache.org/TOOLCHAINS/1.1.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/TOOLCHAINS/1.1.0 https://maven.apache.org/xsd/toolchains-1.1.0.xsd">
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>17</version>
    </provides>
    <configuration>
      <!-- e.g. output of: /usr/libexec/java_home -v 17 -->
      <jdkHome>/path/to/your/jdk-17</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

Without that file the build fails fast with "Cannot find matching toolchain definitions" — that's your cue to create it.

### environment variables

| var | default | what for |
|---|---|---|
| `APP_JWT_SECRET`           | `designer-jobs-development-secret-key-please-change-me-32` (dev only) | HS256 signing key — must be ≥ 32 chars; override in any non-dev environment |
| `APP_CORS_ALLOWED_ORIGINS` | localhost + IntelliJ + Live Server defaults                            | comma-separated list of CORS-allowed origins |

Both are read from `src/main/resources/application.properties` via `${VAR:default}` syntax. Token expiry is `app.jwt.expiry-millis = 7200000` (2 hours), edit there.

---

## packages

```
at.ac.fhcampuswien
├── Main.java          ← @SpringBootApplication entry; calls DatabaseInitializer.init() before SpringApplication.run()
│
│   # feature packages (one domain each: controller + service + repository + model)
├── account/           ← identity: register/login/me + users table + BCrypt (done); designer profiles & portfolio (stubs)
├── job/               ← job listings — fully implemented (POST/GET/PUT/DELETE + search)
├── application/       ← apply & hire flow (stubs)
├── chat/              ← in-platform messaging (stubs)
├── contract/          ← auto-generated freelance contracts (stubs)
├── moderation/        ← reports & content moderation (stubs)
│
│   # third-party API integrations
├── integration/
│   ├── external/      ← HTTP clients (ExternalTimeApiClient, ExternalLocationApiClient)
│   ├── worldclock/    ← proxies timeapi.io (GET /world-clock)
│   ├── location/      ← location lookup endpoint
│   └── pexels/        ← Pexels image API proxy
│
│   # cross-cutting plumbing
└── infrastructure/
    ├── Database/      ← H2 connection + jobs-table bootstrap
    ├── config/        ← Spring config: security filter chain, CORS, WebConfig (static files)
    └── session/       ← JWT issuance (JwtService); verification delegated to Spring's BearerTokenAuthenticationFilter
```

Each package has its own `README.md` documenting endpoints, models, and design choices. **Read those before editing a package** — the top-level view here only catalogs them.

### status at a glance

| package | state |
|---|---|
| `infrastructure/Database/`    | implemented |
| `infrastructure/config/`      | implemented |
| `account/`     | auth done; profiles stubbed |
| `infrastructure/session/`     | implemented |
| `job/`         | implemented |
| `application/` | stubs |
| `chat/`        | stubs |
| `contract/`    | stubs (Phase 2) |
| `moderation/`  | stubs (Phase 2) |

---

## request flow

```
                                                ┌──────────────────────────┐
HTTP request                                    │ Spring Security filters  │
  │                                             │  1. CorsFilter           │
  ▼                                             │  2. BearerTokenAuthN     │  ← reads JWT, sets SecurityContext
[Tomcat embedded] ─► [DispatcherServlet] ─►    │  3. AuthorizationFilter  │  ← matches request against permitAll / authenticated()
                              │                 └────────────┬─────────────┘
                              ▼                              ▼
                       @RestController ─► Repository ─► Database.getConnection() ─► H2 file
```

- **CORS** is centralised in `infrastructure/config/SecurityConfig#corsConfigurationSource`. Origins come from `app.cors.allowed-origins`; methods/headers are in code.
- **Auth** is stateless — no `HttpSession`, no `JSESSIONID`. Each request carries its own JWT or it's anonymous.
- **Static files** outside `/auth/**`, `/jobs/**`, `/designers/**`, `/users/**`, `/applications/**`, `/conversations/**`, `/contracts/**`, `/moderation/**` are served by `WebConfig` from `../frontend/landing/` (configurable via `app.frontend.path`).

---

## database

Embedded H2 in file mode at `data/projectdb.mv.db`. URL: `jdbc:h2:file:./data/projectdb`, user `sa`, no password (dev only).

Two tables today:

| table   | created by                                    | owned by             |
|---------|-----------------------------------------------|----------------------|
| `jobs`  | `infrastructure/Database/DatabaseInitializer.init()` on boot | `job/JobRepository`  |
| `users` | `account/UserRepository` constructor             | `account/UserRepository`|

There is no migrations framework. Schema changes mean editing the relevant `CREATE TABLE` statement and either deleting the DB file or adding `ALTER TABLE` SQL. See `infrastructure/Database/README.md` for the full Job-side documentation.

When new packages (`application/`, `chat/`, `contract/`, `moderation/`) get implemented, decide per package whether the table-creation goes into `DatabaseInitializer` (alongside `jobs`) or into the repository's constructor (the pattern `account/` uses). The current split is historical, not principled.

---

## endpoints

Implemented:

| method | path | auth | package |
|---|---|---|---|
| `POST`   | `/auth/register`     | public        | `account/`     |
| `POST`   | `/auth/login`        | public        | `account/`     |
| `POST`   | `/auth/logout`       | public        | `account/` (noop) |
| `GET`    | `/auth/me`           | authenticated | `account/`     |
| `POST`   | `/jobs`              | authenticated | `job/`      |
| `GET`    | `/jobs`              | public        | `job/`      |
| `GET`    | `/jobs/{id}`         | public        | `job/`      |
| `PUT`    | `/jobs/{id}`         | authenticated | `job/`      |
| `DELETE` | `/jobs/{id}`         | authenticated | `job/`      |

Everything else returns `501 Not Implemented` for now — see each package's README for the contract.

**Response formats (C2, since 2026-06-11):** the API answers in JSON by default and in XML when the client sends `Accept: application/xml` — enabled by `jackson-dataformat-xml` in `pom.xml`, which registers an app-wide XML message converter. `GET /jobs` and `GET /jobs/{id}` declare it explicitly via `produces`; covered by `infrastructure/config/ContentNegotiationTest`. Note: browsers rank `application/xml` above `*/*` in their Accept header, so opening an API URL in a browser tab shows XML, while `fetch()` calls (Accept `*/*`) keep getting JSON.

---

## see also

- `docu.md` — the original "how Spring works" walkthrough written when this project was migrated off the prog2 `HttpServer` implementation. Tutorial-style; useful if you're new to Spring.
- `infrastructure/Database/README.md` — H2 setup and `jobs` table details.
- `infrastructure/config/README.md` — security filter chain, CORS, static-file mapping.
- `../README.md` — repo-wide README (frontend + backend together).
