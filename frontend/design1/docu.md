# DESIGNJOBS.COM — Developer Documentation

**Bootstrap 5.3** for grid, navbar collapse, and dark mode base.
**Vanilla JS** for theme toggle and search routing. 
**Custom CSS** for the visual identity.
No build step.

---

## Quick Start

```bash
cd frontend
python3 -m http.server 8080   # or any static server
# Visit http://localhost:8080
```

`frontend/index.html` redirects automatically to `design1/`. No bundler, no transpiler, no `node_modules`.

---

## Project Tree

```
frontend/design1/
├── index.html          ← Landing page (search banner + 3 recent job cards)
├── jobs.html           ← Full job listings page
├── about.html          ← About us + team + impressum section
├── impressum.html      ← Legal information page
├── styles.css          ← All custom styles (layered on Bootstrap 5.3)
├── app.js              ← Theme toggle, search submit
├── frontend-specs.md   ← Design specification (read first)
└── docu.md             ← This file
```

---

## Design System Cheatsheet

### Colors (CSS variables in `:root`)

```
--primary       #7C3AED   violet — navbar, search banner, cards, buttons
--primary-deep  #6D28D9   hover / active states
--bg            #FAFAFA   page background  (dark: #0C0A14)
--bg-soft       #F0EBFF   subtle accents   (dark: #161225)
--ink           #1E1B2E   headlines, body  (dark: #EDE9F8)
--ink-soft      #4B4369   body copy        (dark: #A89DC0)
--accent        #10B981   emerald — divider, count labels, card hover ring
--on-primary    #EDE9F8   text on violet surfaces (constant across both modes)
```

To re-skin: edit `--primary` and `--primary-deep` in `:root`. Everything cascades.

### Dark Mode

Dark mode is driven by the `data-bs-theme` attribute on `<html>`:

- Bootstrap reads it and applies its own dark-theme component styles.
- Our CSS overrides `--bg`, `--bg-soft`, `--ink`, `--ink-soft` in `[data-bs-theme="dark"]`.
- The search banner SVG cutout uses `fill: var(--bg)` so it adapts automatically.
- The navbar always has `data-bs-theme="dark"` on the `<nav>` itself — it stays violet regardless of the global theme.

A tiny inline `<script>` in `<head>` restores the saved theme from `localStorage` before first paint (no flash). `app.js` handles the toggle button.

### Typography

| Family     | Used for                   | Why                               |
|------------|----------------------------|-----------------------------------|
| Space Mono | UI, headlines, buttons     | Geometric monospace — poster feel |
| Fraunces   | Body copy in cards         | Warm modern serif — balances mono |

Letter-spacing is the design lever: `0.06em` headings, `0.16–0.20em` buttons/labels.

---

## Class Names

All custom CSS classes use descriptive names — no abbreviation prefix. Bootstrap classes (e.g. `navbar`, `col-md-6`) are used as-is for layout and collapse behaviour.

| Class | What it is |
|-------|-----------|
| `.top-nav` | The violet navigation bar at the top of every page |
| `.search-banner` | The large violet block on the landing page containing the search |
| `.search-banner__panel` | Inner rectangle of the search banner |
| `.search-banner__cutout` | Container for the SVG blob shape and the content inside it |
| `.search-banner__cutout-fill` | The SVG `<path>` — CSS sets `fill: var(--bg)` so it adapts to dark mode |
| `.search-banner__cutout-content` | The search bar, buttons, and divider layered on top of the blob |
| `.search-banner__row` | Row of secondary action buttons inside the banner |
| `.search-banner__or` | The "OR" divider with accent-color rule lines |
| `.search-bar` | Pill-shaped search form |
| `.search-bar__icon` | Inline SVG magnifier icon |
| `.search-bar__input` | Text input inside the search bar |
| `.page-section` | A full-width content section with max-width and padding |
| `.page-section__head` | Section title row with bottom border and optional count |
| `.job-card` | Violet listing card (title, description, meta, link) |
| `.job-card__title` | Card headline in monospace caps |
| `.job-card__body` | Card description in serif, clamped to 3 lines |
| `.job-card__meta` | Small caps row (location · budget · date) anchored bottom-left |
| `.job-card__more` | "Read More" link anchored bottom-right |
| `.site-footer` | Violet footer bar |
| `.site-footer__inner` | Footer content row (brand · nav · copyright) |
| `.site-footer__brand` | Bold brand name in footer |
| `.stamp-btn` | Outlined rounded button — the universal button style |
| `.stamp-btn--inverse` | Cream outline variant, used on violet surfaces |
| `.stamp-btn--filled` | Solid violet CTA variant |

### Hamburger menu

On narrow screens the full navigation links don't fit in the navbar. Bootstrap hides them and shows a small button instead — three horizontal lines stacked on top of each other, which visually resembles a hamburger, hence the name. Clicking it expands the hidden nav links below the bar.

No custom JS is needed for this. The button has `data-bs-toggle="collapse"` and `data-bs-target="#nav-menu"` — Bootstrap reads those attributes and handles the show/hide automatically.

```html
<button data-bs-toggle="collapse" data-bs-target="#nav-menu">
  <span class="navbar-toggler-icon"></span>  ← this renders the ☰ icon
</button>

<div class="collapse navbar-collapse" id="nav-menu">
  <!-- links hidden on mobile, shown when hamburger is clicked -->
</div>
```

The icon itself (`☰`) is a background-image SVG injected by Bootstrap's CSS — nothing to maintain manually.

### The SVG blob shape

The search banner contains a lobed blob shape — an SVG `<path>` with hand-tuned bezier curves. The `d` attribute holds the shape coordinates. To reshape it visually, paste the `d` string into https://yqnn.github.io/svg-path-editor/, drag the handles, copy the result back.

On mobile (`< 768px`) the SVG is hidden and the cutout becomes a flat rounded rectangle.

---

## How To…

### …add a new job card

Copy any `<article class="job-card">` block in `jobs.html`, swap the content. No JS changes needed.

### …add a new page

1. Duplicate `jobs.html`.
2. Update `<title>` and `aria-current="page"` on the right nav link.
3. Drop content inside `<main>` using `.page-section` for vertical rhythm.

### …change the primary color

Edit `--primary` and `--primary-deep` in `:root`. All violet surfaces, borders, and buttons update.

### …connect BE & FE

`app.js` has the integration point — the `#search-form` submit handler. Replace `window.location.href` with:

```js
const res = await fetch(`http://localhost:8080/jobs?q=${encodeURIComponent(q)}`);
const jobs = await res.json();
// render jobs into the DOM
```

---

## Accessibility

- Contrast: `--on-primary` on `--primary` = well above WCAG AA.
- All interactive elements are real `<button>` / `<a>` tags.
- Focus ring: 3px emerald, visible on all backgrounds.
- `prefers-reduced-motion` disables the search banner stagger-reveal.
- Navbar toggle uses `aria-expanded` / `aria-controls` (Bootstrap manages this automatically).
