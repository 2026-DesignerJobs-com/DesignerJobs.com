# `infrastructure/config/` — Spring configuration

Spring `@Configuration` beans that shape how the application behaves at the framework level: security filter chain, CORS, password encoder, static file serving.

---

## files

| file | role |
|---|---|
| `SecurityConfig.java` | Filter chain, authorisation rules, OAuth2 Resource Server configuration, CORS source, password encoder bean |
| `WebConfig.java` | Maps `/**` to the frontend static directory configured by `app.frontend.path` |

---

## `SecurityConfig`

### filter chain (request order)

1. `CorsFilter` — populated from `corsConfigurationSource` bean.
2. `LogoutFilter` — currently unused (we don't use Spring's logout machinery).
3. **`BearerTokenAuthenticationFilter`** — Spring's OAuth2 Resource Server filter. Reads `Authorization: Bearer …`, verifies the JWT, and sets the `SecurityContext`. See `infrastructure/session/README.md`.
4. `AuthorizationFilter` — enforces the `authorizeHttpRequests` rules below.

### authorisation rules

| matcher | rule |
|---|---|
| `/auth/**` | public — register, login, logout, me (note: `me` itself returns 401 if anonymous, even though the URL is public) |
| `GET /jobs/**` | public — browsing jobs requires no account |
| `GET /designers/**` | public — browsing designer profiles requires no account |
| `/`, `*.html`, `*.css`, `*.js`, `images/**`, `css/**`, `js/**`, `assets/**`, `favicon.ico` | public — the frontend assets served by `WebConfig` |
| anything else | authenticated — needs a valid JWT |

Anything `POST/PUT/DELETE` against `/jobs` or `/designers` is **authenticated** — only `GET` is public on those paths.

### session policy

`SessionCreationPolicy.STATELESS` — no `HTTPSession`, no `JSESSIONID` cookie, no server-side principal storage. Each request carries its own JWT or it's anonymous.

### CSRF

Disabled. Stateless APIs that authenticate via `Authorization` headers (not cookies) are not subject to CSRF — the cross-site attacker can't read or set the `Authorization` header from another origin's JavaScript.

### frame options

`X-Frame-Options: SAMEORIGIN`. Required by the iframe shell pattern used in the `landing` frontend.

### CORS — central configuration

A single `CorsConfigurationSource` bean defines policy for the whole API. The list of allowed origins is **read from `application.properties`**, not hardcoded:

```properties
# application.properties
app.cors.allowed-origins=${APP_CORS_ALLOWED_ORIGINS:http://localhost:8080,http://localhost:63342,http://localhost:63343,http://127.0.0.1:8080,http://127.0.0.1:5500}
```

```
allowedOrigins   = read from app.cors.allowed-origins (comma-separated)
allowedMethods   = GET, POST, PUT, DELETE, PATCH, OPTIONS
allowedHeaders   = Authorization, Content-Type, Accept, Origin
exposedHeaders   = Authorization
allowCredentials = true
maxAge           = 3600 seconds
```

The methods, headers, credentials, and max-age are intentionally still in code — they don't vary per environment. Origins do.

Default origins (development):
- `8080` — Spring Boot's own port. The frontend's `fetch` calls can hit the same origin with `mode: 'cors'` and the preflight still needs an entry.
- `63342`, `63343` — IntelliJ's built-in HTTP server (used by the team to preview frontend files via "Open in Browser").
- `5500` — VS Code Live Server default.
- The `127.0.0.1` variants exist because browsers treat `localhost` and `127.0.0.1` as different origins.

To override in deployment, set the environment variable `APP_CORS_ALLOWED_ORIGINS` to a comma-separated list of production origins.

The previous setup used `@CrossOrigin` annotations on individual controllers (`JobController`, `AuthController` only). This was inconsistent — `ApplicationController`, `ChatController`, etc. had no CORS at all. The central source applies the same rules to every endpoint, including ones we haven't implemented yet.

Per-controller `@CrossOrigin` annotations have been removed in this audit. Don't reintroduce them — change `app.cors.allowed-origins` (or the methods/headers in the bean) instead.

### `PasswordEncoder` bean

`BCryptPasswordEncoder` with default strength (10 rounds). Used by `AuthController` to hash on register and verify on login. Single source of truth — never instantiate `new BCryptPasswordEncoder()` outside this config.

---

## `WebConfig`

Maps every URL not picked up by a `@RestController` to a file under `app.frontend.path` (default `../frontend/landing/`). This is what makes `http://localhost:8080/` serve `index.html`, `http://localhost:8080/theme.css` serve `theme.css`, etc. — the API and the SPA run from one Spring process during development.

Change the path via `application.properties`, not by editing this class.

---

## adding a new public route

If a new endpoint should be reachable without a token, **add it to `SecurityConfig` first**:

```java
.requestMatchers(HttpMethod.GET, "/health").permitAll()
```

then implement the controller. If you forget the matcher rule, requests will hit the `AuthorizationFilter` and bounce with 401 even though your controller does nothing security-related.

---

## adding a role-gated route

1. Enable method security (not yet done): add `@EnableMethodSecurity` to `SecurityConfig`.
2. Annotate the controller method: `@PreAuthorize("hasRole('DESIGNER')")`.
3. The role is read from the `ROLE_<role>` authority that `JwtAuthenticationConverter` (in `SecurityConfig`) sets. No additional plumbing needed.

---

## see also

- `infrastructure/session/README.md` — the JWT filter this config registers.
- `account/README.md` — endpoint catalog and password handling.
