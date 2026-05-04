# DESIGNJOBS.COM — Developer Documentation

A minimal, dependency-light frontend for a freelance design-jobs portal.
**Bootstrap 5.3** for grid, navbar collapse, and dark mode base.
**Vanilla JS** for theme toggle, search routing, and iframe navigation. **Custom CSS** for the visual identity.
No build step.

---

## Quick Start

```bash
cd frontend
python3 -m http.server 8080   # or any static server
# Visit http://localhost:8080
```

`frontend/index.html` redirects automatically to `design1/index.html`. No bundler, no transpiler, no `node_modules`.

---

## Project Tree

```
frontend/design1/
├── index.html          ← Shell — navbar + iframe + footer (entry point)
├── shell.html          ← Redirect to index.html (backwards compat)
├── homepage.html       ← Homepage content (search banner + recent jobs)
├── jobs.html           ← Full job listings page
├── job-random.html     ← Single job detail page
├── profile.html        ← Designer public profile
├── profile-edit.html   ← Edit profile form
├── register.html       ← Registration / login page
├── about.html          ← About us + team + impressum section
├── impressum.html      ← Legal information page
├── styles.css          ← All custom styles (layered on Bootstrap 5.3)
├── app.js              ← Theme toggle, search submit
├── frontend-specs.md   ← Design specification (read first)
└── docu.md             ← This file
```

---

## Iframe Shell Architecture

Navigation does not reload the whole page. Instead `index.html` acts as a persistent **shell**: it renders the navbar and footer once and swaps only the inner content via an `<iframe>`.

```
┌─────────────────────────────────────────┐
│  index.html  (shell — never reloads)    │
│  ┌───────────────────────────────────┐  │
│  │  <nav class="top-nav">           │  │
│  └───────────────────────────────────┘  │
│  ┌───────────────────────────────────┐  │
│  │  <iframe id="content-frame"       │  │  ← src changes on every nav click
│  │    src="homepage.html">           │  │
│  └───────────────────────────────────┘  │
│  ┌───────────────────────────────────┐  │
│  │  <footer class="main-footer">    │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

### How navigation works

Every nav link and footer link carries a `data-page="filename.html"` attribute instead of an `href`. The shell script intercepts clicks, sets `frame.src = page`, and pushes a `history.pushState` entry so the browser back/forward buttons work.

```html
<!-- Nav link — no real href, shell handles it -->
<a class="nav-link" href="#" data-page="jobs.html">All Jobs</a>
```

```js
// Shell script (inside index.html)
allPageLinks().forEach(a => {
  a.addEventListener('click', e => { e.preventDefault(); navigate(a.dataset.page); });
});

const navigate = (page, push = true) => {
  frame.src = page;
  if (push) history.pushState({ page }, '', '#' + page);
  setActive(page);
};
```

### Theme sync across the iframe boundary

The shell owns the theme toggle. When the user switches theme, the shell script writes `data-bs-theme` onto the iframe's `<html>` element too, so both the shell and the inner page stay in sync:

```js
const applyTheme = t => {
  document.documentElement.setAttribute('data-bs-theme', t);
  localStorage.setItem('theme', t);
  try { frame.contentDocument.documentElement.setAttribute('data-bs-theme', t); } catch (_) {}
  //    ↑ try/catch because cross-origin frames throw — safe to ignore here
};
```

Each inner page also has the tiny inline script in `<head>` that reads `localStorage` before first paint, so there is no theme flash on direct URL load either.

### Page title sync

On every iframe `load` event the shell copies the inner page's `<title>` to the outer document:

```js
frame.addEventListener('load', () => {
  try {
    if (frame.contentDocument.title) document.title = frame.contentDocument.title;
  } catch (_) {}
});
```

### Adding a new page

1. Create the HTML file — copy `jobs.html` as a starting point. Do **not** add a `<nav>` or `<footer>`; the shell provides those.
2. Add a nav link in `index.html` with `data-page="your-page.html"`.
3. Done — theme, history, and active-link highlighting are handled automatically.

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

A tiny inline `<script>` in `<head>` restores the saved theme from `localStorage` before first paint (no flash). The shell script in `index.html` handles the toggle button and propagates theme changes into the iframe.

### Typography

| Family     | Used for                   | Why                               |
|------------|----------------------------|-----------------------------------|
| Space Mono | UI, headlines, buttons     | Geometric monospace — poster feel |
| Fraunces   | Body copy in cards         | Warm modern serif — balances mono |

Letter-spacing is the design lever: `0.06em` headings, `0.16–0.20em` buttons/labels.

---

## Class Names

All custom CSS classes use descriptive names — no BEM prefixes, no web-jargon abbreviations. Bootstrap classes (e.g. `navbar`, `col-md-6`) are used as-is for layout and collapse behaviour.

| Class | What it is |
|-------|-----------|
| `.top-nav` | The violet navigation bar rendered by the shell |
| `.page-banner` | Outer wrapper for the violet search banner section |
| `.search-panel` | Inner rectangle of the search banner (violet background) |
| `.blob-area` | Container for the SVG blob shape and content overlay |
| `.blob-outline` | SVG `<path>` — wide light-coloured stroke (outermost ring) |
| `.blob-gap` | SVG `<path>` — narrower violet stroke that creates the gap between outline and fill |
| `.blob-fill` | SVG `<path>` — solid background fill, adapts to `var(--bg)` in dark mode |
| `.blob-content` | Search bar, buttons, and divider layered absolutely on top of the blob |
| `.search-bar` | Pill-shaped search form |
| `.search-icon` | Inline SVG magnifier icon |
| `.search-input` | Text input inside the search bar |
| `.btn-row` | Row of secondary action buttons inside the search banner |
| `.or-divider` | "OR" label with accent-color rule lines |
| `.page-section` | A full-width content section with max-width and padding |
| `.section-head` | Section title row with bottom border and optional count |
| `.count` | Small label alongside a section heading (e.g. "03 / 24") |
| `.job-card` | Violet listing card (title, description, meta, link) |
| `.job-title` | Card headline in monospace caps |
| `.job-body` | Card description in serif, clamped to 3 lines |
| `.job-meta` | Small caps row (location · budget · date) anchored bottom-left |
| `.job-more` | "Read More" link anchored bottom-right |
| `.job-header` | Hero block at the top of a job detail page |
| `.detail-section` | Vertical spacing wrapper for sections on a job detail page |
| `.section-label` | All-caps label above a content block |
| `.job-description` | Body text area on a job detail page |
| `.side-panel` | Info sidebar box on a job detail page |
| `.panel-label` | Heading inside a side panel |
| `.panel-row` | Key/value row inside a side panel |
| `.panel-key` | Left cell of a panel row (label) |
| `.panel-val` | Right cell of a panel row (value) |
| `.designer-header` | Profile page header: avatar + name + bio + actions |
| `.avatar` | Square avatar block with initials |
| `.specialty` | Designer's primary role/title below the name |
| `.tag-row` | Flex row of tag chips below the specialty |
| `.bio` | Bio paragraph in the profile header |
| `.action-col` | Column of CTA buttons in the profile header |
| `.stats-strip` | Horizontal row of stat blocks (projects, rating, earnings) |
| `.stat` | A single stat block with a large number and label |
| `.gallery` | Portfolio grid of work cards |
| `.work-item` | A single portfolio card |
| `.work-preview` | Thumbnail / placeholder area of a portfolio card |
| `.work-body` | Text area below the portfolio card thumbnail |
| `.work-title` | Title of a portfolio item |
| `.work-tags` | Tag row at the bottom of a portfolio card |
| `.tag` | A small chip label; modifiers: `.on-violet`, `.green`, `.outline` |
| `.auth-page` | Full-screen centering wrapper for login / register pages |
| `.auth-box` | The white box containing the auth form |
| `.eyebrow` | Small uppercase label above an auth page heading |
| `.auth-title` | Main heading on an auth page |
| `.field-group` | Margin wrapper for a label + input pair |
| `.field-label` | All-caps label above a form input |
| `.field-input` | Text / email / password / number input |
| `.field-textarea` | Textarea input |
| `.field-select` | Select dropdown |
| `.type-switcher` | Segmented 2-option toggle (e.g. Designer / Client) |
| `.option-group` | Segmented 3-option toggle (e.g. Public / Clients Only / Private) |
| `.text-divider` | "or" divider with horizontal rule lines |
| `.main-footer` | Violet footer bar rendered by the shell |
| `.footer-inner` | Footer content row (brand · nav) |
| `.footer-brand` | Bold brand name in the footer |
| `.stamp-btn` | Outlined rounded button — the universal button style; modifiers: `.filled`, `.on-violet` |

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

The shell's JS also closes the mobile menu after a link is clicked, so the iframe content can be seen immediately.

### The SVG blob shape

The search banner contains a lobed blob shape — three identical SVG `<path>` elements stacked in paint order:

1. `.blob-outline` — wide, light-coloured stroke (the outermost visible ring)
2. `.blob-gap` — narrower violet stroke that creates a gap between the outline and the fill
3. `.blob-fill` — solid fill in `var(--bg)`, so it adapts to dark mode automatically

The `d` attribute on each path holds the bezier curve coordinates. To reshape the blob, paste the `d` string into https://yqnn.github.io/svg-path-editor/, drag the handles, and copy the result back into all three paths.

On mobile (`< 768px`) the SVG is hidden and the cutout becomes a flat rounded rectangle.

---

## How To…

### …add a new job card

Copy any `<article class="job-card">` block in `jobs.html`, swap the content. No JS changes needed.

### …add a new page

1. Create the HTML file — copy `jobs.html` as a starting point.
2. Do **not** add `<nav>` or `<footer>` — the shell in `index.html` provides those.
3. Add a link in `index.html` with `data-page="your-page.html"`.
4. Drop content inside `<main>` using `.page-section` for vertical rhythm.

### …change the primary color

Edit `--primary` and `--primary-deep` in `:root`. All violet surfaces, borders, and buttons update.

### …wire up the real API

`app.js` has the integration point — the `#search-form` submit handler. Replace `window.location.href` with:

```js
const res = await fetch(`http://localhost:8080/jobs?q=${encodeURIComponent(q)}`);
const jobs = await res.json();
// render jobs into the DOM
```

The register and profile-edit forms are marked with `// TODO:` comments at their submit handlers — those are the other integration points.

---

## Accessibility

- Contrast: `--on-primary` on `--primary` = well above WCAG AA.
- All interactive elements are real `<button>` / `<a>` tags.
- Focus ring: 3px emerald, visible on all backgrounds.
- `prefers-reduced-motion` disables the search banner stagger-reveal.
- Navbar toggle uses `aria-expanded` / `aria-controls` (Bootstrap manages this automatically).
- The iframe has `title="Page content"` for screen readers.