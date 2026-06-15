# landing

Pure Bootstrap frontend for DesignerJobs.com.
Bootstrap 5.3 utilities only · Space Mono + Fraunces · vanilla JS · no custom CSS classes

Simplified version of the original design. Pure bootstrap.

## Run

```sh
cd frontend
python3 -m http.server 8080
# Visit http://localhost:8080/landing/
```

## How it works

Navigation uses an **iframe shell**: `index.html` renders the navbar and footer once and swaps only the inner content by changing the `<iframe src>`. Page links carry `data-page="filename.html"` attributes — no full-page reloads. Theme toggle and browser back/forward work across the iframe boundary. See `docu.md` for details.

## Pages

| file | description |
|------|-------------|
| `index.html` | Shell — navbar + iframe + footer (entry point); navbar swaps Register/Login ↔ Profile/Logout based on `localStorage` token |
| `homepage.html` | Homepage — hero + dynamic recent-job listing pulled from `GET /jobs`; auth buttons adapt to login state |
| `jobs.html` | Full job listings page |
| `job-detail.html` | Single job detail page — reads `GET /jobs/{id}`, bumps `PATCH /jobs/{id}/view-count` |
| `job-random.html` | "Surprise me" — picks a random entry client-side from `GET /jobs` (there is no `/jobs/random` endpoint) |
| `advanced-search.html` | Advanced job search form |
| `search-results.html` | Search results listing |
| `post-a-job.html` | Post-a-job form — submits to `POST /jobs` |
| `chat.html` | In-platform chat — polls `GET /conversations/{id}/messages`, sends via `POST` |
| `profile.html` | Designer profile — populated from `GET /auth/me`, includes home + logout |
| `profile-edit.html` | Edit profile form — submits to `PUT /auth/me` |
| `register.html` | Registration page — POSTs `fullName`, `email`, `password`, `role`, optional `designType` / `skills` to `/auth/register` |
| `login.html` | Login page — POSTs to `/auth/login`, stores token + userId + role in `localStorage`, redirects to `homepage.html` |
| `about.html` | About us, team, impressum section |
| `impressum.html` | Legal information |

> An admin dashboard lives separately at `frontend/admin/dashboard.html` (consumes `/users`, `/jobs`, `/moderation/reports`), outside this `landing/` folder.

## Features

- **Pure Bootstrap** — all layout, spacing, colour, and component styling via Bootstrap 5.3 utility classes
- **Minimal theme.css** — only Bootstrap CSS variable overrides (`--bs-primary-rgb`, `--bs-success-rgb`, font stacks, button scoped vars); no custom component CSS
- **Iframe shell** — nav and footer render once; only the inner page reloads on navigation
- **Dark / light mode** — toggle in navbar, persisted in `localStorage`, synced into iframe, no flash on reload
- **Bootstrap btn-check toggles** — role selector and availability/visibility toggles use native `<input type="radio" class="btn-check">` instead of custom JS toggle classes
- **JWT-based auth flow** — register/login hit the Spring backend; on success the token, `userId`, and `role` are written to `localStorage` under `designer_jobs_token` / `designer_jobs_userId` / `designer_jobs_role`. Navigation reads these keys to decide Register/Login vs. Profile/Logout. Logout is a client-side `localStorage.clear()` + redirect (the `/auth/logout` endpoint is a noop). See `auth.js` and `18.05-auth-changelog.md`.
- **Responsive** — Bootstrap grid collapses naturally; navbar hamburger is built-in Bootstrap behaviour

## Files

```
landing/
├── index.html              ← shell (entry point); auth-aware navbar
├── homepage.html           ← homepage content; dynamic recent-job listing
├── jobs.html               ← listings page
├── job-detail.html         ← single job detail (GET /jobs/{id} + view-count)
├── job-random.html         ← random job picked client-side from GET /jobs
├── advanced-search.html    ← advanced search form
├── search-results.html     ← search results listing
├── post-a-job.html         ← post-a-job form (POST /jobs)
├── chat.html               ← in-platform chat (GET/POST /conversations…)
├── profile.html            ← designer profile (reads GET /auth/me)
├── profile-edit.html       ← edit profile form (PUT /auth/me)
├── register.html           ← registration page (POST /auth/register)
├── login.html              ← login page (POST /auth/login)
├── about.html              ← about us page
├── impressum.html          ← legal page
├── theme.css               ← Bootstrap CSS variable overrides only (no component styles)
├── app.js                  ← theme toggle + search submit
├── auth.js                 ← localStorage helpers for token / userId / role
├── common.js               ← shared helpers: API_BASE (backend origin, single source of truth) + escapeHtml
├── <page>.js               ← one per page (index.js, jobs.js, chat.js, profile.js, …): that page's logic, externalized from its inline <script>
├── 18.05-auth-changelog.md ← notes on profile-nav and logout wiring
└── docu.md                 ← Bootstrap class patterns, theme.css reference, iframe architecture

> **JS conventions.** Each page's logic lives in a matching `<page>.js` loaded at
> the end of `<body>`; only two tiny inline scripts remain in the HTML — the
> `<head>` no-flash theme init and (on protected pages) the `auth.js` requireAuth
> guard. Backend calls always use the full absolute URL via `` `${API_BASE}/…` ``
> from `common.js` — no relative endpoints, no per-page base-URL literals.
```

See `docu.md` for the Bootstrap utility class patterns, how `theme.css` works, and how to extend the design.