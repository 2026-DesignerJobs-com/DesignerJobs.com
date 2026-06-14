# `account/` — accounts, authentication & designer profiles

The identity feature, end to end. This package owns the `users` table and everything
about who a user is:

- **Authentication & accounts** (implemented) — register, login, current user, profile
  update/delete (`/auth/**`).
- **Designer profiles & portfolio** (implemented) — public profile pages, profile edits, and
  portfolio items (`/designers/**`, `/users/**`), served by `UserController` over the same
  `users` table plus a `portfolios` table.

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
| `POST`   | `/register` | public | implemented — hashes password (BCrypt), persists, returns token |
| `POST`   | `/login`    | public | implemented — verifies password, returns token |
| `POST`   | `/logout`   | public | 204 noop — stateless JWT, client drops token |
| `GET`    | `/me`       | authenticated | implemented — returns the user behind the bearer token |
| `PUT`    | `/me`       | authenticated | implemented — partial profile update (only non-null fields applied), returns the updated profile |
| `DELETE` | `/me`       | authenticated | implemented — deletes the caller's own account |

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

**Implemented in `UserController`.** Designer profiles are a *projection* of the `users` table:
`UserModel` carries `fullName`, `designType`, `bio`, `skills`, location, rates and portfolio
links (set at registration / profile edit), and `mapToProfile()` reshapes a `UserModel` into a
`DesignerProfile` (`displayName`, `bio`, `skills`, `hourlyRate`, `location = "city, country"`).
There is no separate `designer_profiles` table — only `users` plus a `portfolios` table for
portfolio items.

### designer profiles

| method | path | auth | behaviour |
|---|---|---|---|
| `GET`  | `/designers` | public | list all `DESIGNER` users as profiles. **Note:** the `skills`/`location` query params are accepted but **not yet applied** as filters |
| `GET`  | `/designers/{id}` | public | fetch one profile by **user id**; `404` if missing or not a designer |
| `PUT`  | `/designers/{id}` | authenticated | update the profile fields. **Note:** owner-only enforcement is **not yet implemented** — see known gaps |

### portfolio

Portfolio items live in a `portfolios` table created by `UserRepository.createPortfolioTableIfNotExists()` (called from `UserController`'s constructor).

| method | path | auth | behaviour |
|---|---|---|---|
| `GET`    | `/designers/{id}/portfolio` | public | list items for one designer |
| `POST`   | `/designers/{id}/portfolio` | authenticated | add item; server assigns `id` (if blank) and `createdAt` |
| `DELETE` | `/designers/{id}/portfolio/{itemId}` | authenticated | remove item; `404` if not found |

> ⚠️ The three portfolio endpoints currently **500** at runtime (the suite catches this) — a defect in the `portfolios` persistence path, not a routing issue. Owner-only checks are also still missing.

### generic user

| method | path | auth | behaviour |
|---|---|---|---|
| `GET`    | `/users/{id}` | authenticated | return the `UserModel` with `passwordHash` nulled out; `404` if missing |
| `GET`    | `/users`      | authenticated | list all users (`passwordHash` nulled) |
| `DELETE` | `/users/{id}` | authenticated | delete the account (soft-delete in `UserRepository`). **Note:** self-only enforcement is **not yet implemented** |

### persistence — profiles & portfolio

There is **no `designer_profiles` table**. A designer profile is just a `DESIGNER` row in `users`, reshaped on the fly by `UserController.mapToProfile()` — so the profile id *is* the user id. Only the portfolio gets its own table, `portfolios` (created by `UserRepository.createPortfolioTableIfNotExists()`):

```sql
CREATE TABLE IF NOT EXISTS portfolios (
    id           VARCHAR(36) PRIMARY KEY,
    designer_id  VARCHAR(36) NOT NULL,        -- references users.id
    title        VARCHAR(255),
    description  TEXT,
    image_url    VARCHAR(1024),
    project_url  VARCHAR(1024),
    tags         TEXT,                         -- comma-separated
    created_at   VARCHAR(50)
);
```

(Exact column set lives in `UserRepository`; the portfolio persistence path currently throws at runtime — see known gaps.)

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
3. **Portfolio endpoints 500 at runtime** — `GET/POST/DELETE /designers/{id}/portfolio` throw; the `portfolios` persistence path needs fixing.
4. **Owner-only enforcement missing** — `PUT /designers/{id}`, `DELETE /users/{id}`, and the portfolio writes don't yet check `auth.getName()` against the resource owner; any authenticated user can edit/delete any profile.
5. **`/designers` filters ignored** — the `skills` / `location` query params are accepted but not applied.
6. **Refresh tokens / blacklist** — see `infrastructure/session/README.md`.
7. **Lockout / rate limiting** — login endpoint is unprotected against brute force.

---

## see also

- `infrastructure/session/README.md` — JWT issuance/verification details.
- `infrastructure/config/README.md` — filter chain, CORS, BCrypt bean wiring.
- `application/README.md` — applications reference designer ids; ownership rules use the same pattern.
