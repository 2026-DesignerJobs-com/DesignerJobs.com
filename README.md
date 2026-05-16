# DesignerJobs.com

FH-Campus-Wien/VZ2028/SoSe26/web/2-Group AF

## Frontend

Two static HTML/CSS/JS designs — no build step, no bundler.

```sh
cd frontend
python3 -m http.server 8080
# Visit http://localhost:8080
```

`frontend/index.html` redirects to `design1/`. To preview design2 instead, open `frontend/design2/index.html` directly.

Both designs use an **iframe shell**: `index.html` is the persistent shell (navbar + footer). All other pages are inner content loaded into an `<iframe>` on navigation — no full-page reloads. See `frontend/design1/docu.md` for the full explanation.

| Design | Approach |
|--------|----------|
| `design1/` | CSS-first — all layout defined in `styles.css`; Bootstrap used for grid and collapse |
| `design2/` | Bootstrap-first — spacing via utility classes in HTML; `styles_bootstrap+.css` handles only the visual identity |

## Backend

Spring Boot 3.2 REST API on Java 17. Job listings and user accounts persist to an embedded H2 file database at `backend/data/projectdb.mv.db`. Auth is JWT-based (stateless) — see `backend/src/main/java/at/ac/fhcampuswien/session/README.md`.

## run

```sh
cd backend
mvn spring-boot:run
```

Server starts on `http://localhost:8080`. To reset state: delete `backend/data/projectdb.mv.db` and restart.

## CORS

The backend serves the API and the static frontend from one Spring process, but the team frequently runs the frontend from a different origin during development (IntelliJ's built-in HTTP server on `63342`/`63343`, VS Code Live Server on `5500`, etc.). To make those flows work, CORS is configured centrally in `backend/src/main/java/at/ac/fhcampuswien/config/SecurityConfig.java` via a `CorsConfigurationSource` bean.

The allowed origins are **not hardcoded** — they are read from `application.properties`:

```properties
app.cors.allowed-origins=${APP_CORS_ALLOWED_ORIGINS:http://localhost:8080,http://localhost:63342,http://localhost:63343,http://127.0.0.1:8080,http://127.0.0.1:5500}
```

Override per environment by setting the `APP_CORS_ALLOWED_ORIGINS` environment variable to a comma-separated list. Allowed methods (`GET, POST, PUT, DELETE, PATCH, OPTIONS`), allowed headers (`Authorization, Content-Type, Accept, Origin`), exposed headers (`Authorization`), credentials support, and the 1 h preflight cache live in `SecurityConfig` because they don't vary per environment.

Per-controller `@CrossOrigin` annotations have been removed in favour of this single source — don't reintroduce them. Full details in `backend/src/main/java/at/ac/fhcampuswien/config/README.md`.

## endpoints

| method | path | description |
|--------|------|-------------|
| `POST` | `/jobs` | store a new job |
| `GET`  | `/jobs` | search jobs (all params optional) |

### search query params

| param | match | example |
|-------|-------|---------|
| `q` | substring in title or description | `q=logo` |
| `category` | exact | `category=graphic+design` |
| `location` | substring | `location=Vienna` |
| `budget` | exact | `budget=small` |
| `workMode` | exact | `workMode=remote` |

### job fields

`title`, `description`, `category`, `location`, `budget`, `workMode`
— `id` and `createdAt` are assigned by the server on POST.

### examples

```sh
# store a job
curl -X POST http://localhost:8080/jobs \
  -H "Content-Type: application/json" \
  -d '{"title":"Logo Designer","category":"graphic design","location":"Vienna","budget":"small","workMode":"remote"}'

# get all jobs
curl http://localhost:8080/jobs

# search by keyword and filter
curl "http://localhost:8080/jobs?q=logo&budget=small"

# filter by category and work mode
curl "http://localhost:8080/jobs?category=webdesign&workMode=remote"
```
