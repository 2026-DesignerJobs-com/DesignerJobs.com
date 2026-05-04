# design1

Poster-aesthetic frontend for DesignerJobs.com.
Bootstrap 5.3 grid · Space Mono + Fraunces · vanilla JS · no build step.

## Run

```sh
cd frontend
python3 -m http.server 8080
# Visit http://localhost:8080
```

`frontend/index.html` redirects automatically to `design1/`.

## Pages

| file | description |
|------|-------------|
| `index.html` | Landing page — search banner + 3 recent job cards |
| `jobs.html` | Full job listings page |
| `about.html` | About us, team, impressum section |
| `impressum.html` | Legal information |

## Features

- **Dark / light mode** — toggle in navbar, persisted in `localStorage`, no flash on reload
- **Search banner** — large top block with search form, routes to `jobs.html?q=...`
- **Responsive** — 3-column cards on desktop, stacked on mobile; navbar collapses to hamburger
- **Accessible** — WCAG AA contrast, keyboard-reachable, `prefers-reduced-motion` respected

## Files

```
design1/
├── index.html          ← landing page
├── jobs.html           ← listings page
├── about.html          ← about us page lorom ipsum 
├── impressum.html      ← legal page lorom ipsum
├── styles.css          ← all custom styles (layered on Bootstrap)
├── app.js              ← theme toggle + search submit
├── frontend-specs.md   ← design specification
└── docu.md             ← component reference and extension guide
```

See `docu.md` for the full class reference, color tokens, and how to wire the real backend.