# design1

Poster-aesthetic frontend for DesignerJobs.com.
Bootstrap 5.3 grid · Space Mono + Fraunces · vanilla JS

## Run

```sh
cd frontend
python3 -m http.server 8080
# Visit http://localhost:8080
```

`frontend/index.html` redirects automatically to `design1/index.html`.

## How it works

Navigation uses an **iframe shell**: `index.html` renders the navbar and footer once and swaps only the inner content by changing the `<iframe src>`. Page links carry `data-page="filename.html"` attributes — no full-page reloads. Theme toggle and browser back/forward work across the boundary. See `docu.md` for details.

## Pages

| file | description |
|------|-------------|
| `index.html` | Shell — navbar + iframe + footer (entry point) |
| `homepage.html` | Homepage — search banner + 3 recent job cards |
| `jobs.html` | Full job listings page |
| `job-random.html` | Single job detail page |
| `profile.html` | Designer public profile |
| `profile-edit.html` | Edit profile form |
| `register.html` | Registration page |
| `about.html` | About us, team, impressum section |
| `impressum.html` | Legal information |

## Features

- **Iframe shell** — nav and footer render once; only the inner page reloads on navigation
- **Dark / light mode** — toggle in navbar, persisted in `localStorage`, synced into iframe, no flash on reload
- **Search banner** — large top block with search form, routes to `jobs.html?q=...`
- **Responsive** — 3-column cards on desktop, stacked on mobile; navbar collapses to hamburger
- **Accessible** — WCAG AA contrast, keyboard-reachable, `prefers-reduced-motion` respected

## Files

```
design1/
├── index.html          ← shell (entry point)
├── shell.html          ← redirect to index.html (backwards compat)
├── homepage.html       ← homepage content
├── jobs.html           ← listings page
├── job-random.html     ← job detail page
├── profile.html        ← designer profile
├── profile-edit.html   ← edit profile form
├── register.html       ← registration page
├── about.html          ← about us page (lorem ipsum)
├── impressum.html      ← legal page (lorem ipsum)
├── styles.css          ← all custom styles (layered on Bootstrap)
├── app.js              ← theme toggle + search submit
├── frontend-specs.md   ← design specification
└── docu.md             ← component reference, class names, iframe architecture
```

See `docu.md` for the full class reference, color tokens, iframe shell explanation, and how to wire the real backend.