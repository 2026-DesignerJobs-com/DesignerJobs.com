# `auth/` — register, login, current user

REST surface and persistence for accounts. Issues tokens via `session/JwtService`; verification is handled by Spring's OAuth2 Resource Server (`BearerTokenAuthenticationFilter`).

---

## files

| file | role |
|---|---|
| `AuthController.java` | REST endpoints under `/auth` |
| `AuthRequest.java`    | request body for register/login (`email`, `password`, `role`) |
| `AuthResponse.java`   | response body — `token`, `userId`, `role` |
| `UserModel.java`      | persisted user — flat public fields |
| `UserRepository.java` | JDBC repository against H2 `users` table |

---

## endpoints

Base path: `/auth`

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

{ "email": "ada@example.com", "password": "secret123", "role": "DESIGNER" }
```

Responses:
- `201 Created` → `{ "token": "...", "userId": "...", "role": "DESIGNER" }`
- `400 Bad Request` → `{ "error": "email, password and role are required" }`
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
- `200 OK` → `{ "userId": "...", "email": "...", "role": "...", "createdAt": "..." }`
- `401 Unauthorized` if missing/invalid token, or if the user has been deleted since the token was issued.

**Logout**
```http
POST /auth/logout
```

Always `204 No Content`. The frontend deletes the token from `localStorage` itself. See `session/README.md` for why this is a noop on the server.

---

## persistence

`UserRepository` is a hand-rolled JDBC repository following the same pattern as `JobRepository`:

- Table `users` is created on first instantiation (`CREATE TABLE IF NOT EXISTS`).
- Schema: `id VARCHAR PK, email VARCHAR UNIQUE NOT NULL, password_hash VARCHAR NOT NULL, role VARCHAR NOT NULL, created_at VARCHAR NOT NULL`.
- All methods use prepared statements + try-with-resources.

Methods:
- `save(UserModel)` — INSERT.
- `findByEmail(String)` — for login lookup.
- `findById(String)` — for `/auth/me`.
- `existsByEmail(String)` — for register duplicate check.

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

---

## known gaps / next steps

1. **Email format validation** — currently any non-blank string is accepted. Add Bean Validation (`jakarta.validation`) constraints once we add `spring-boot-starter-validation`.
2. **Password strength** — no minimum length / complexity check.
3. **Account deletion** — `UserController#deleteUser` is still a 501 stub; deleting a user does not cascade to profiles/portfolio.
4. **Refresh tokens / blacklist** — see `session/README.md`.
5. **Lockout / rate limiting** — login endpoint is unprotected against brute force.

---

## see also

- `session/README.md` — JWT issuance/verification details.
- `config/README.md` — filter chain, CORS, BCrypt bean wiring.
