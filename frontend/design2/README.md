# design2

Bootstrap-first frontend for DesignerJobs.com.
Maximises Bootstrap 5.3 utilities · Space Mono + Fraunces · vanilla JS · no build step.

## Run

```sh
cd frontend
python3 -m http.server 8080
# Visit http://localhost:8080
```

`frontend/index.html` redirects automatically to `design1/index.html`; change the redirect to `design2/` to preview this design.

## How it works

Navigation uses an **iframe shell**: `index.html` renders the navbar and footer once and swaps only the inner content by changing the `<iframe src>`. Page links carry `data-page="filename.html"` attributes — no full-page reloads. Theme toggle and browser back/forward work across the boundary.

The CSS philosophy differs from design1: layout spacing (margins, padding, flex) is expressed with Bootstrap utility classes directly in HTML (`mb-3`, `d-flex`, `gap-2`, etc.) rather than in the stylesheet. `styles_bootstrap+.css` only adds what Bootstrap cannot — custom colors, fonts, component shapes, and the visual identity.

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
- **Bootstrap-first** — utility classes in HTML for all spacing; custom CSS only for identity
- **Dark / light mode** — toggle in navbar, persisted in `localStorage`, synced into iframe, no flash on reload
- **Search banner** — large top block with search form, routes to `jobs.html?q=...`
- **Responsive** — Bootstrap grid; navbar collapses to hamburger
- **Accessible** — WCAG AA contrast, keyboard-reachable, `prefers-reduced-motion` respected

## Files

```
design2/
├── index.html              ← shell (entry point)
├── shell.html              ← redirect to index.html (backwards compat)
├── homepage.html           ← homepage content
├── jobs.html               ← listings page
├── job-random.html         ← job detail page
├── profile.html            ← designer profile
├── profile-edit.html       ← edit profile form
├── register.html           ← registration page
├── about.html              ← about us page (lorem ipsum)
├── impressum.html          ← legal page (lorem ipsum)
├── styles_bootstrap+.css   ← custom styles only (Bootstrap handles layout)
├── app.js                  ← theme toggle + search submit
└── frontend-specs.md       ← design specification
```