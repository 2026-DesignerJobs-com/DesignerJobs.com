# `user/` — designer profiles & portfolio

REST surface for designer profiles and portfolio items. **Currently all endpoints are 501 stubs.**

This README is a starter — it documents the contract the frontend already targets and what implementing it will need. It will grow as the package fills in.

> **Note:** a partial profile surface already exists outside this package — `auth/UserModel` carries `fullName`, `designType`, and `skills` (set at registration), and `GET /auth/me` returns them. The design3 profile pages currently read/display these via `/auth/me` rather than `/designers/{id}`. When this package is implemented, decide whether to migrate those columns into `designer_profiles` or keep core identity in `users` and only put richer profile data (bio, portfolio link, hourly rate, etc.) here.

---

## files

| file | role |
|---|---|
| `UserController.java`  | REST endpoints for `/designers/**` and `/users/**` |
| `DesignerProfile.java` | model — `id`, `userId`, `displayName`, `bio`, `skills` (csv), `hourlyRate`, `location`, `avatarUrl` |
| `PortfolioItem.java`   | model — `id`, `designerId`, `title`, `description`, `imageUrl`, `projectUrl`, `tags` (csv), `createdAt` |

A storage class (or repository pair) is needed: recommend `DesignerProfileRepository` + `PortfolioItemRepository` against the existing H2 file, matching `JobRepository`'s pattern.

---

## endpoints

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

---

## persistence sketch

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

Profile id and user id are kept separate so a user can in principle have multiple profile records (e.g. for moderation history) without changing the primary key. If that flexibility isn't needed, the profile id can just be the user id.

---

## auth rules (when filling stubs)

- Public read for `GET /designers`, `GET /designers/{id}`, `GET /designers/{id}/portfolio`, `GET /users/{id}`. Already permitted in `SecurityConfig` (`/designers/**` GET is public; `/users/**` is currently `authenticated()` — adjust the matcher if you want public `GET /users/{id}`).
- All write operations require `auth.getPrincipal()` to equal the resource owner. Return `403 Forbidden` on mismatch — do not 404 (leaks existence information, but consistent with REST convention).

---

## see also

- `auth/README.md` — how `/auth/me` exposes the current user, how `Authentication.getPrincipal()` carries the user id.
- `application/README.md` — applications reference designer ids; ownership rules use the same pattern.
