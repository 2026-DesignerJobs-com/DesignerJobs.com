# `job/` — job listings

REST surface and persistence for job posts. This is the **first feature on the platform to be fully wired** end-to-end: frontend `post-a-job.html` → `JobController` → H2 file database. Everything else (auth, applications, chat, contracts, moderation) was modelled after this package.

---

## files

| file | role |
|---|---|
| `Job.java`           | model — plain public fields, no annotations (Jackson reflects over field names) |
| `JobController.java` | REST endpoints under `/jobs` |
| `JobRepository.java` | hand-rolled JDBC repository against the H2 `jobs` table |

The `jobs` table itself is created by `infrastructure/Database/DatabaseInitializer.init()` at application startup, **not** in `JobRepository`'s constructor. This is different from `account/UserRepository`, which creates its own table — see `infrastructure/Database/README.md` for the rationale.

---

## endpoints

Base path: `/jobs`. `GET /jobs` and `GET /jobs/{id}` are public — deliberately not `GET /jobs/**`, so nested sub-resources like `/jobs/{id}/applications` stay authenticated; everything else requires a valid JWT (see `infrastructure/config/README.md`).

| method | path | auth | what it does |
|---|---|---|---|
| `POST`   | `/jobs`           | authenticated | store a new job — server assigns `id` (UUID) and `createdAt` (ISO-8601), client sends the rest |
| `GET`    | `/jobs`           | public        | list/search — all query params optional; see filter table below. JSON by default, XML with `Accept: application/xml` (C2) |
| `GET`    | `/jobs/{id}`      | public        | fetch one; `404` if missing. JSON/XML negotiated like `GET /jobs` |
| `PUT`    | `/jobs/{id}`      | owner only    | update; `404` if missing, `403` for non-owners; `id`, `clientId` and `createdAt` are preserved server-side regardless of body |
| `DELETE` | `/jobs/{id}`      | owner only    | delete; `404` if missing, `403` for non-owners, otherwise `200` + confirmation JSON |

### content negotiation (C2)

Both public reads serve JSON by default and XML when the request carries `Accept: application/xml` (via `jackson-dataformat-xml`, see the backend README). The representation only affects serialization — search filters, auth rules, and status codes behave identically for JSON and XML.

### search filters

All filters are optional. `q` is a substring match against `title` and `description`. `location` and `tags` are substring matches. The rest are exact matches (case-insensitive).

| param        | match     | example                      |
|--------------|-----------|------------------------------|
| `q`          | substring | `q=logo`                     |
| `category`   | exact     | `category=graphic+design`    |
| `designType` | exact     | `designType=logo`            |
| `location`   | substring | `location=Vienna`            |
| `budget`     | exact     | `budget=small`               |
| `workMode`   | exact     | `workMode=remote`            |
| `tags`       | substring | `tags=branding`              |

Results are ordered by `created_at DESC` (newest first).

> **Note:** the random-job feature is client-side by design (team decision 2026-06-11): `job-random.html` picks a random entry from `GET /jobs`. There is deliberately no `GET /jobs/random` endpoint; the unused `JobRepository#getRandomJob()` was removed.

---

## job fields

| field         | required | notes                                                              |
|---------------|----------|--------------------------------------------------------------------|
| `id`          | server   | UUID, assigned in `JobController#store`                            |
| `clientId`    | yes      | id of the user who posted the job (`users.id`)                     |
| `title`       | yes      | free text                                                          |
| `description` | yes      | free text                                                          |
| `category`    | yes      | e.g. `webdesign`, `graphic design`, `interior design`              |
| `designType`  | optional | finer-grained label, e.g. `logo`, `UI/UX`, `branding`              |
| `location`    | optional | free text                                                          |
| `budget`      | optional | `small` / `medium` / `big` (frontend constraint, not enforced here)|
| `workMode`    | optional | `remote` / `on site` / `hybrid` (same)                             |
| `deadline`    | optional | ISO date string, e.g. `2026-06-15`                                 |
| `tags`        | optional | comma-separated string — substring-matched by search               |
| `createdAt`   | server   | `Instant.now().toString()`, assigned in `JobController#store`      |

No validation framework is in use — empty / malformed values pass straight through.

---

## persistence

```sql
CREATE TABLE IF NOT EXISTS jobs (
    id           VARCHAR(36) PRIMARY KEY,
    client_id    VARCHAR(36),
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    category     VARCHAR(100),
    design_type  VARCHAR(100),
    location     VARCHAR(255),
    budget       VARCHAR(50),
    work_mode    VARCHAR(50),
    deadline     VARCHAR(50),
    tags         TEXT,
    created_at   VARCHAR(50)
);
```

`jobs` lives in the same H2 file (`./data/projectdb.mv.db`) as `users`. Reset the DB by deleting that file and restarting — the table is recreated at boot.

`JobRepository` uses prepared statements + try-with-resources. Errors are currently caught and printed with `e.printStackTrace()` — a known rough edge. For production-shaped code you'd want a logger and a thrown exception instead.

---

## known gaps / next steps

1. **Validation** — `title` is required (POST and PUT reject blank titles), but `category` / `budget` / `workMode` accept any string.
2. **Pagination** — `GET /jobs` returns everything. Fine at demo scale; add `?page=&size=` when it grows.

---

## see also

- `infrastructure/Database/README.md` — Database/DatabaseInitializer wiring and SQL details.
- `application/README.md` — applications reference `jobs.id`.
- `infrastructure/config/README.md` — `/jobs/**` GET is the only public `/jobs` route; everything else falls through to `authenticated()`.
