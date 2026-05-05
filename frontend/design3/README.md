# design3

Pure Bootstrap frontend for DesignerJobs.com.
Bootstrap 5.3 utilities only · Space Mono + Fraunces · vanilla JS · no custom CSS classes

Simplified version of the original design. Pure bootstrap.

## Run

```sh
cd frontend
python3 -m http.server 8080
# Visit http://localhost:8080/design3/
```

## How it works

Navigation uses an **iframe shell**: `index.html` renders the navbar and footer once and swaps only the inner content by changing the `<iframe src>`. Page links carry `data-page="filename.html"` attributes — no full-page reloads. Theme toggle and browser back/forward work across the iframe boundary. See `docu.md` for details.

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

- **Pure Bootstrap** — all layout, spacing, colour, and component styling via Bootstrap 5.3 utility classes
- **Minimal theme.css** — only Bootstrap CSS variable overrides (`--bs-primary-rgb`, `--bs-success-rgb`, font stacks, button scoped vars); no custom component CSS
- **Iframe shell** — nav and footer render once; only the inner page reloads on navigation
- **Dark / light mode** — toggle in navbar, persisted in `localStorage`, synced into iframe, no flash on reload
- **Bootstrap btn-check toggles** — role selector and availability/visibility toggles use native `<input type="radio" class="btn-check">` instead of custom JS toggle classes
- **Responsive** — Bootstrap grid collapses naturally; navbar hamburger is built-in Bootstrap behaviour

## Files

```
design3/
├── index.html          ← shell (entry point)
├── shell.html          ← redirect to index.html
├── homepage.html       ← homepage content
├── jobs.html           ← listings page
├── job-random.html     ← job detail page
├── profile.html        ← designer profile
├── profile-edit.html   ← edit profile form
├── register.html       ← registration page
├── about.html          ← about us page
├── impressum.html      ← legal page
├── theme.css           ← Bootstrap CSS variable overrides only (no component styles)
├── app.js              ← theme toggle + search submit
└── docu.md             ← Bootstrap class patterns, theme.css reference, iframe architecture
```

See `docu.md` for the Bootstrap utility class patterns, how `theme.css` works, and how to extend the design.