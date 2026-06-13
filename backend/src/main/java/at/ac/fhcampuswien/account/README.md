# `account/` — accounts, authentication & designer profiles

The identity feature, end to end. This package owns the `users` table and everything
about who a user is:

- **Authentication & accounts** (implemented) — register, login, current user (`/auth/**`).
- **Designer profiles & portfolio** (mostly 501 stubs) — public profile pages and portfolio
  items (`/designers/**`, `/users/**`).

It merges what used to be two packages (`auth/` + `user/`); they were split but both modelled
identity, so they now live together. Tokens are issued via
`infrastructure/session/JwtService`; verification is handled by Spring's OAuth2 Resource Server
(`BearerTokenAuthenticationFilter`).

---

## files

| file | role |
|---|---|
| `AuthController.java` | REST endpoints under `/auth` |
| `AuthRequest.java`    | request body for register/login (`email`, `password`, `role`, `fullName`, `designType`, `skills`) |
| `AuthResponse.java`   | response body — `token`, `userId`, `role` |
| `ProfileUpdateRequest.java` | request body for profile edits (validated; `@Size` caps mirror the column limits) |
| `UserModel.java`      | persisted user — flat public fields (`id`, `email`, `passwordHash`, `role`, `createdAt`, `fullName`, `designType`, `bio`, `skills`, location, rates, portfolio links) |
| `UserRepository.java` | JDBC repository against the H2 `users` table |
| `UserController.java`  | REST endpoints for `/designers/**` and `/users/**` (profiles & portfolio) |
| `DesignerProfile.java` | model — `id`, `userId`, `displayName`, `bio`, `skills` (csv), `hourlyRate`, `location`, `avatarUrl` |
| `PortfolioItem.java`   | model — `id`, `designerId`, `title`, `description`, `imageUrl`, `projectUrl`, `tags` (csv), `createdAt` |

---

## endpoints — authentication (`/auth`)

| method | path | auth | status |
|---|---|---|---|
| `POST` | `/register` | public | implemented — hashes password (BCrypt), persists, returns token |
| `POST` | `/login`    | public | implemented — verifies password, returns token |
| `POST` | `/logout`   | public | 204 noop — stateless JWT, client drops token |
| `GET`  | `/me`       | authenticated | implemented — returns the user behind the bearer token |

### request / response shapes

**Register**
```http
POST /auth/register
Content-Type: application/json

{
  "fullName": "Ada Lovelace",
  "email": "ada@example.com",
  "password": "secret123",
  "role": "DESIGNER",
  "designType": "logo",
  "skills": "illustration, branding"
}
```

`fullName`, `email`, `password`, `role` are required. `designType` and `skills` are stored only when `role` is `DESIGNER` — for `CLIENT` accounts they are persisted as empty strings regardless of what the client sends.

Responses:
- `201 Created` → `{ "token": "...", "userId": "...", "role": "DESIGNER" }`
- `400 Bad Request` → `{ "error": "fullName, email, password and role are required" }` or `{ "error": "role must be CLIENT or DESIGNER" }`
- `409 Conflict` → `{ "error": "email already exists" }`

**Login**
```http
POST /auth/login
Content-Type: application/json

{ "email": "ada@example.com", "password": "secret123" }
```

Responses:
- `200 OK` → `{ "token": "...", "userId": "...", "role": "DESIGNER" }`
- `400 Bad Request` → `{ "error": "email and password are required" }`
- `401 Unauthorized` → `{ "error": "invalid email or password" }`

**Me**
```http
GET /auth/me
Authorization: Bearer <token>
```

Responses:
- `200 OK` → `{ "userId": "...", "email": "...", "role": "...", "createdAt": "...", "fullName": "...", "designType": "...", "skills": "..." }`
- `401 Unauthorized` if missing/invalid token, or if the user has been deleted since the token was issued.

`designType` and `skills` are returned as empty strings for `CLIENT` accounts.

**Logout**
```http
POST /auth/logout
```

Always `204 No Content`. The frontend deletes the token from `localStorage` itself. See `infrastructure/session/README.md` for why this is a noop on the server.

---

## endpoints — designer profiles & portfolio (`/designers`, `/users`)

**Currently mostly 501 stubs.** A partial profile surface already exists via auth: `UserModel`
carries `fullName`, `designType`, `bio`, `skills`, location, rates and portfolio links (set at
registration / profile edit), and `GET /auth/me` returns them. The landing frontend's profile
pages currently read/display these via `/auth/me` rather than `/designers/{id}`. When the
profile endpoints below are filled in, decide whether to migrate those columns into
`designer_profiles` or keep core identity in `users` and only put richer profile data here.

### designer profiles

| method | path | auth | what it should do |
|---|---|---|---|
| `GET`  | `/designers` | public | list all designers; query params `skills` (csv match), `location` (substring) |
| `GET`  | `/designers/{id}` | public | fetch one profile by **profile id** |
| `PUT`  | `/designers/{id}` | authenticated, owner-only | update own profile — check `auth.getPrincipal() == profile.userId` |

### portfolio

| method | path | auth | what it should do |
|---|---|---|---|
| `GET`    | `/designers/{id}/portfolio` | public | list items for one designer |
| `POST`   | `/designers/{id}/portfolio` | authenticated, owner-only | add item; server assigns `id` and `createdAt` |
| `DELETE` | `/designers/{id}/portfolio/{itemId}` | authenticated, owner-only | remove item |

### generic user

| method | path | auth | what it should do |
|---|---|---|---|
| `GET`    | `/users/{id}` | public | return **public** fields only — `email`, `role`, `createdAt`. Never `passwordHash`. |
| `DELETE` | `/users/{id}` | authenticated, self-only | delete own account; cascade to profile + portfolio + applications + contracts (decide cascade policy when implementing) |

### persistence sketch (profiles — not yet built)

```sql
CREATE TABLE IF NOT EXISTS designer_profiles (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) UNIQUE NOT NULL,    -- references users.id
    display_name VARCHAR(255),
    bio TEXT,
    skills TEXT,                             -- comma-separated
    hourly_rate DOUBLE,
    location VARCHAR(255),
    avatar_url VARCHAR(1024)
);

CREATE TABLE IF NOT EXISTS portfolio_items (
    id VARCHAR(36) PRIMARY KEY,
    designer_id VARCHAR(36) NOT NULL,        -- references designer_profiles.id
    title VARCHAR(255),
    description TEXT,
    image_url VARCHAR(1024),
    project_url VARCHAR(1024),
    tags TEXT,                               -- comma-separated
    created_at VARCHAR(50)
);
```

Profile id and user id are kept separate so a user can in principle have multiple profile records without changing the primary key. If that flexibility isn't needed, the profile id can just be the user id.

---

## persistence — the `users` table

`UserRepository` is a hand-rolled JDBC repository following the same pattern as `JobRepository`
and uses `Database.getConnection()` from the `infrastructure/Database/` package:

- Table `users` is created on first instantiation (`CREATE TABLE IF NOT EXISTS`).
- All methods use prepared statements + try-with-resources.

Methods:
- `save(UserModel)` — INSERT.
- `findByEmail(String)` — for login lookup (excludes soft-deleted accounts).
- `findById(String)` — for `/auth/me` (excludes soft-deleted accounts).
- `existsByEmail(String)` — for register duplicate check.
- `update(...)` / `updateProfile(...)` — profile edits.
- `deleteById(String)` — soft-delete (anonymizes the row, sets `role = 'DELETED'`).

The `users` table lives in the same H2 file as `jobs` (`./data/projectdb.mv.db`). No migrations framework — schema changes mean editing `createTableIfNotExists` and either dropping the DB file or adding `ALTER TABLE` SQL.

---

## password handling

- Passwords are **never** stored in plaintext.
- `BCryptPasswordEncoder` from `spring-security-crypto` is injected by `SecurityConfig#passwordEncoder` (default 10 rounds).
- `AuthController.register` calls `encoder.encode(rawPassword)` before persisting.
- `AuthController.login` calls `encoder.matches(rawPassword, storedHash)` — never compare hashes directly.
- The `password_hash` column never leaves the backend — `/auth/me` returns only public fields.

---

## role model

`UserModel.role` is `CLIENT` or `DESIGNER`. The string is uppercased and stored as-is. The JWT filter wires it into Spring authorities as `ROLE_<role>`, so:

```java
@PreAuthorize("hasRole('DESIGNER')")
public ResponseEntity<?> updateProfile(...) { … }
```

works out of the box once method-security is enabled. Method-security is **not** enabled yet (`@EnableMethodSecurity` is absent from `SecurityConfig`) — enable it when role-gated endpoints land.

### auth rules for the profile endpoints (when filling stubs)

- Public read for `GET /designers`, `GET /designers/{id}`, `GET /designers/{id}/portfolio`, `GET /users/{id}`. `/designers/**` GET is already public in `SecurityConfig`; `/users/**` is currently `authenticated()` — adjust the matcher if you want public `GET /users/{id}`.
- All write operations require `auth.getPrincipal()` to equal the resource owner. Return `403 Forbidden` on mismatch.

---

## known gaps / next steps

1. **Email format validation** — currently any non-blank string is accepted. Add Bean Validation constraints once we add `spring-boot-starter-validation`.
2. **Password strength** — no minimum length / complexity check.
3. **Profile & portfolio endpoints** — `/designers/**` and `/users/**` are still 501 stubs; no `DesignerProfileRepository` / `PortfolioItemRepository` yet.
4. **Refresh tokens / blacklist** — see `infrastructure/session/README.md`.
5. **Lockout / rate limiting** — login endpoint is unprotected against brute force.

---

## see also

- `infrastructure/session/README.md` — JWT issuance/verification details.
- `infrastructure/config/README.md` — filter chain, CORS, BCrypt bean wiring.
- `application/README.md` — applications reference designer ids; ownership rules use the same pattern.
