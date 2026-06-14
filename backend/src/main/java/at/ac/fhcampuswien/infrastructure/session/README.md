# `infrastructure/session/` — stateless JWT session management

This package issues JWT bearer tokens at login. Verification is delegated to **Spring Security's OAuth2 Resource Server** (`spring-boot-starter-oauth2-resource-server`).

There is **no server-side session state**. Logout is a noop on the backend; the client drops the token. Session persistence across page reloads is the *client's* responsibility (see `frontend/landing/auth.js`).

---

## files

| file | role |
|---|---|
| `JwtService.java` | Issues tokens via Spring's `JwtEncoder`. |
| `README.md` | this file |

Verification, parsing, and `SecurityContext` population are done by `BearerTokenAuthenticationFilter`, which the resource-server starter wires into the chain automatically. There is no custom filter class in this package anymore.

---

## how a request flows

```
                  ┌────────────────────────────────────────────┐
                  │ Frontend (landing) — login.html            │
                  └──────────────────┬─────────────────────────┘
                                     │ POST /auth/login {email, password}
                                     ▼
                  ┌────────────────────────────────────────────┐
                  │ AuthController.login()                     │
                  │  1. UserRepository.findByEmail             │
                  │  2. PasswordEncoder.matches(raw, hash)     │
                  │  3. JwtService.issue(userId, role)         │
                  │     → JwtEncoder builds + signs the JWT    │
                  └──────────────────┬─────────────────────────┘
                                     │ {token, userId, role}
                                     ▼
                  ┌────────────────────────────────────────────┐
                  │ Frontend stores in localStorage:           │
                  │   designer_jobs_token, _userId, _role      │
                  │ auth.js wraps fetch() to attach            │
                  │ Authorization: Bearer <token> on all calls │
                  └──────────────────┬─────────────────────────┘
                                     │ Authorization: Bearer <token>
                                     ▼
                  ┌────────────────────────────────────────────┐
                  │ BearerTokenAuthenticationFilter (Spring)   │
                  │  • extract Bearer token                    │
                  │  • JwtDecoder verifies signature + exp     │
                  │  • JwtAuthenticationConverter builds the   │
                  │    Authentication:                         │
                  │      name        = sub claim (userId)      │
                  │      authorities = [ROLE_<role>]           │
                  │  • SecurityContextHolder.setAuthentication │
                  └──────────────────┬─────────────────────────┘
                                     │
                                     ▼
                  ┌────────────────────────────────────────────┐
                  │ Any @RestController                        │
                  │   public … me(Authentication auth) {       │
                  │     String userId = auth.getName();        │
                  │   }                                        │
                  └────────────────────────────────────────────┘
```

---

## `JwtService`

```java
@Service
public class JwtService {
    public String issue(String userId, String role) { … }
}
```

- **Algorithm:** HS256 (HMAC-SHA256), declared explicitly in the `JwsHeader`.
- **Subject:** `userId` (UUID string) → ends up as `Authentication.getName()` on the receiving end.
- **Claim:** `role` (`CLIENT` or `DESIGNER`) → becomes `ROLE_<role>` granted authority.
- **Expiry:** controlled by `app.jwt.expiry-millis` (default 2 h = 7 200 000 ms). Stored in the standard `exp` claim — Spring's `JwtDecoder` enforces it automatically.
- **Key:** `app.jwt.secret` from `application.properties`. Must be ≥ 32 chars for HS256. Override via env var `APP_JWT_SECRET` in deployment.

`JwtService` only knows how to **issue**. It does not expose a `parse()` method — verification is the resource server's job and live nowhere in our code.

---

## verification — what the framework does

When a request arrives:

1. `BearerTokenAuthenticationFilter` reads the `Authorization` header. No header / wrong scheme → passes through (request is anonymous; protected routes return 401 from `AuthorizationFilter`).
2. The token is handed to the `JwtDecoder` bean (`NimbusJwtDecoder.withSecretKey(...).macAlgorithm(HS256)` in `SecurityConfig`).
3. Decoder verifies:
   - JOSE header `alg` matches HS256
   - HMAC signature is valid against our secret
   - `exp` claim is in the future
   - (`nbf` if present is in the past)
4. On success, `JwtAuthenticationConverter` runs: `principal claim "sub"` → `Authentication.getName()`; `role` claim → `ROLE_<role>` authority.
5. On failure (bad signature, expired, malformed) the filter sets a 401 response — the request never reaches the controller.

We don't write that filter; we don't write the decoder; we don't catch the exceptions. **Spring does it all.** Compared to a custom `OncePerRequestFilter`, that's an entire class deleted.

---

## stateless session policy

`SecurityConfig` sets `SessionCreationPolicy.STATELESS`. This:

- Stops Spring from creating an `HttpSession`.
- Stops `JSESSIONID` cookies from being set.
- Stops `SecurityContext` from being persisted between requests.

The JWT is the *only* memory the server has of who the caller is. The client holds it in `localStorage`. This is what makes US-04 (session persistence on reload) work — the token is on the *client side*, not in a server-side session.

---

## US-04 — session persistence on reload

> Given an eingeloggter Nutzer
> When er die Seite neu lädt
> Then ist er weiterhin eingeloggt (Token aus localStorage wieder verwendet)
> Abgelaufenes Token (exp < jetzt) → automatischer Redirect zum Login

Implemented by `frontend/landing/auth.js`. On every page that includes it:

1. **Eager expiry guard** — on load, the script base64-decodes the JWT payload from `localStorage`, reads `exp`, and redirects to `login.html?expired=1` if `exp * 1000 < Date.now()`. This catches expired tokens *before* the user clicks anything.
2. **Authenticated fetch helper** — `authFetch(url, options)` attaches `Authorization: Bearer <token>` to every request and watches the response status. A 401 from the server (e.g., signature invalidated by a server-side secret rotation) also triggers a redirect.
3. **Logout helper** — `logout()` clears the three `designer_jobs_*` localStorage keys and bounces to `login.html`.

Because the token lives in `localStorage`, it survives a full page reload (`F5`, browser restart, tab close + reopen — as long as the same origin) without any backend involvement. The server is genuinely stateless; the *browser* persists the session.

---

## endpoints owned by `account/` (callers of `JwtService`)

| method | path | rule | notes |
|---|---|---|---|
| `POST` | `/auth/register` | public | hashes password (BCrypt), persists `UserModel`, issues token |
| `POST` | `/auth/login`    | public | verifies credentials, issues token |
| `POST` | `/auth/logout`   | public | 204 noop — client deletes the token from `localStorage` |
| `GET`    | `/auth/me`     | authenticated | returns the caller's profile from the token's `sub` |
| `PUT`    | `/auth/me`     | authenticated | partial profile update for the caller |
| `DELETE` | `/auth/me`     | authenticated | deletes the caller's own account |

---

## configuration

`application.properties`

```
app.jwt.secret=${APP_JWT_SECRET:designer-jobs-development-secret-key-please-change-me-32}
app.jwt.expiry-millis=7200000
```

- `APP_JWT_SECRET` — environment variable, takes precedence over the default. **Override in deployment.**
- `app.jwt.expiry-millis` — token lifetime. 7 200 000 = 2 h.

---

## extending

- **Asymmetric keys (RS256/ES256)?** Swap `NimbusJwtDecoder.withSecretKey(...)` for `NimbusJwtDecoder.withPublicKey(...)` (or `withJwkSetUri(...)`) in `SecurityConfig#jwtDecoder`. Replace `JwtEncoder` similarly. The rest of the package doesn't need to change.
- **External identity provider (Auth0, Keycloak)?** Drop the `jwtDecoder` bean entirely and set `spring.security.oauth2.resourceserver.jwt.issuer-uri=…`. Spring will auto-configure the decoder from the provider's discovery doc. Drop the `register/login` endpoints — they'd live in the external IdP.
- **Method security?** Add `@EnableMethodSecurity` to `SecurityConfig`. Then `@PreAuthorize("hasRole('DESIGNER')")` on any controller method just works — the converter already produces `ROLE_<role>` authorities.

---

## see also

- `at.ac.fhcampuswien.account.AuthController` — the issuer side.
- `at.ac.fhcampuswien.account.UserRepository` — the user store backing register/login.
- `at.ac.fhcampuswien.infrastructure.config.SecurityConfig` — filter-chain wiring, decoder/encoder beans, converter, CORS.
- `frontend/landing/auth.js` — client-side session persistence (US-04).
