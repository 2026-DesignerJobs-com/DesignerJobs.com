# DESIGNJOBS.COM — design3 Developer Documentation

Pure Bootstrap 5.3 frontend for the freelance design-jobs portal.
**Bootstrap 5.3** for everything — grid, utilities, components, dark mode.
**theme.css** for brand colour overrides only — no custom component classes.
**Vanilla JS** for theme toggle, search routing, and iframe navigation.

---

## Quick Start

```bash
cd frontend
python3 -m http.server 8080
# Visit http://localhost:8080/design3/
```

No bundler, no transpiler, no `node_modules`.

---

## Project Tree

```
frontend/design3/
├── index.html          ← Shell — navbar + iframe + footer (entry point)
├── shell.html          ← Redirect to index.html
├── homepage.html       ← Homepage content (search banner + recent jobs)
├── jobs.html           ← Full job listings page
├── job-random.html     ← Single job detail page
├── profile.html        ← Designer public profile
├── profile-edit.html   ← Edit profile form
├── register.html       ← Registration / login page
├── about.html          ← About us + team + impressum section
├── impressum.html      ← Legal information page
├── theme.css           ← Bootstrap CSS variable overrides only
├── app.js              ← Theme toggle, search submit
└── docu.md             ← This file
```

---

## Design Approach: Pure Bootstrap

design3 reproduces the visuals design1 — violet brand, monospace headings, pill buttons, dark cards — but uses **only Bootstrap utility classes** in the HTML. There is no `styles.css` and no custom CSS component classes like `.job-card` or `.stamp-btn`.

### What "pure Bootstrap" means here

| ✅ Allowed | ❌ Not used |
|---|---|
| Bootstrap utility classes (`bg-primary`, `rounded-pill`, `fw-bold`, …) | Custom component classes (`.job-card`, `.stamp-btn`, `.page-section`, …) |
| Bootstrap CSS variable overrides in `theme.css` (`--bs-primary-rgb`, `--bs-btn-bg`, …) | Custom CSS rules for new components |
| Bootstrap components (`card`, `btn`, `badge`, `form-control`, `btn-check`, …) | Bespoke component CSS |
| Inline `style=""` attributes for one-off values Bootstrap has no utility for (`letter-spacing`, `aspect-ratio`, …) | A separate stylesheet for visual identity |

---

## theme.css — Bootstrap Variable Overrides

`theme.css` contains **only** Bootstrap CSS variable overrides. It has no custom selectors beyond Bootstrap's own component scopes. It is loaded after Bootstrap so the overrides take effect.

### Color variables

```css
:root {
  --bs-primary: #7C3AED;       /* violet — replaces Bootstrap's blue */
  --bs-primary-rgb: 124, 58, 237;
  --bs-success: #10B981;       /* emerald accent */
  --bs-success-rgb: 16, 185, 129;
  --bs-link-color: #7C3AED;
  --bs-link-hover-color: #6D28D9;
}
```

Overriding `--bs-primary-rgb` makes all Bootstrap utility classes that depend on it — `bg-primary`, `text-primary`, `border-primary`, `btn-outline-primary` — use the violet colour automatically.

### Button scoped variables

Bootstrap's `.btn-primary` hardcodes its colour in scoped CSS variables that don't inherit from `:root`. `theme.css` overrides those directly:

```css
.btn-primary {
  --bs-btn-bg: #7C3AED;
  --bs-btn-border-color: #7C3AED;
  --bs-btn-hover-bg: #6D28D9;
  /* … */
}
.btn-outline-primary {
  --bs-btn-color: #7C3AED;
  --bs-btn-border-color: #7C3AED;
  --bs-btn-hover-bg: #7C3AED;
  /* … */
}
```

### Font variables

```css
:root {
  --bs-font-monospace: "Space Mono", SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  --bs-body-font-family: "Fraunces", Georgia, serif;
  --bs-body-bg: #FAFAFA;
  --bs-body-color: #1E1B2E;
}
```

`--bs-font-monospace` is what Bootstrap's `font-monospace` utility class applies. Overriding it to Space Mono means `class="font-monospace"` on any element uses the brand font.

### Dark mode

```css
[data-bs-theme="dark"] {
  --bs-body-bg: #0C0A14;
  --bs-secondary-bg: #161225;
  --bs-body-color: #EDE9F8;
}
```

Bootstrap's own dark mode handles card backgrounds, border colours, and form elements. These overrides just adjust the page background to match the deep navy from design1.

### Re-skinning

To change the brand colour, edit `--bs-primary`, `--bs-primary-rgb`, and the `.btn-primary`/`.btn-outline-primary` scoped values in `theme.css`. All violet surfaces update automatically.

---

## Bootstrap Class Patterns

The following table shows the Bootstrap utility combinations used to replicate design1's custom components.

### Navigation bar (shell `index.html`)

```html
<nav class="navbar navbar-expand-lg bg-primary py-3" data-bs-theme="dark">
```

| Element | Bootstrap classes |
|---|---|
| Nav outer | `navbar navbar-expand-lg bg-primary py-3` + `data-bs-theme="dark"` |
| Brand | `navbar-brand fw-bold font-monospace text-white text-uppercase` |
| Nav links | `nav-link fw-bold text-uppercase font-monospace` |
| Nav buttons (Register, Login, Dark) | `btn btn-outline-light rounded-pill btn-sm fw-bold text-uppercase` |
| Mobile toggler | Bootstrap's default `navbar-toggler` — no custom styling |

`data-bs-theme="dark"` on the `<nav>` makes `nav-link`, `navbar-brand`, and the toggler icon render in light colours appropriate for the violet background.

### Footer (shell `index.html`)

```html
<footer class="bg-primary text-white py-4 mt-auto">
  <div class="container-xxl d-flex flex-wrap justify-content-between align-items-center gap-3">
```

### Job cards (homepage, jobs, about)

Replaces design1's `.job-card`:

```html
<article class="card bg-primary text-white border-0 rounded-4 h-100">
  <div class="card-body d-flex flex-column p-4">
    <h3 class="font-monospace text-uppercase fw-bold text-white mb-3">Title</h3>
    <p class="card-text opacity-90 flex-grow-1">Description…</p>
    <div class="d-flex gap-2 font-monospace text-uppercase mt-3 mb-2 opacity-70">
      <span>Remote</span><span>·</span><span>€€</span><span>·</span><span>2d ago</span>
    </div>
    <a href="#" class="btn btn-outline-light rounded-pill btn-sm fw-bold text-uppercase align-self-end">Read More</a>
  </div>
</article>
```

### Section headings

Replaces design1's `.section-head`:

```html
<div class="d-flex align-items-baseline gap-3 border-bottom border-2 pb-3 mb-4">
  <h2 class="font-monospace text-uppercase fw-bold mb-0">Recent Postings</h2>
  <span class="ms-auto text-success fw-bold font-monospace small">03 / 24</span>
</div>
```

### Page content wrapper

Replaces design1's `.page-section`:

```html
<section class="container-xxl px-3 my-5">
```

### Search banner hero (homepage)

Replaces design1's `.page-banner` + `.search-panel` + SVG blob:

```html
<section class="container-xxl my-4 px-3">
  <div class="bg-primary rounded-4 overflow-hidden position-relative" style="min-height:480px">
    <div class="d-flex flex-column align-items-center justify-content-center gap-4 text-center px-4 py-5">
      <!-- search bar, buttons, OR divider, CTA -->
    </div>
  </div>
</section>
```

The SVG blob cutout from design1 is not replicated — the hero is a solid violet rounded card. All content sits directly on the violet background; buttons use `btn-outline-light` (white-bordered) instead of `btn-outline-primary`.

### Search bar (inside hero)

```html
<form id="search-form" role="search" class="d-flex align-items-center rounded-pill gap-3 px-4 py-2 w-100" style="background:rgba(0,0,0,.22); max-width:620px">
  <svg …><!-- search icon --></svg>
  <input type="search" class="search-input form-control bg-transparent border-0 text-white fw-bold font-monospace text-uppercase p-0 shadow-none" …>
</form>
```

`bg-transparent border-0 text-white` use `!important` under the hood so they override Bootstrap's `.form-control` default background and border on both normal and focus states.

### OR divider (inside hero)

```html
<div class="d-flex align-items-center gap-3" style="width:min(100%,340px)">
  <hr class="flex-grow-1 border-success opacity-75 m-0">
  <span class="text-success fw-bold font-monospace fs-5">OR</span>
  <hr class="flex-grow-1 border-success opacity-75 m-0">
</div>
```

Bootstrap's `<hr>` renders via `border-top`. `border-success` overrides `border-color`. `flex-grow-1` makes the lines fill available space symmetrically.

### Page header (about, impressum, profile-edit)

Replaces design1's `.search-panel` used as a decorative page header:

```html
<div class="bg-primary rounded-4 d-flex align-items-center justify-content-center text-center text-white py-5" style="min-height:200px">
  <div>
    <p class="font-monospace text-uppercase mb-2 opacity-70">eyebrow text</p>
    <h1 class="font-monospace text-uppercase fw-bold text-white mb-0">Page Title</h1>
  </div>
</div>
```

### Tags / badges

| Context | Bootstrap | Equivalent to |
|---|---|---|
| On a violet surface | `badge rounded-pill border fw-normal` + inline border-color | `.tag.on-violet` |
| Status badge (green) | `badge bg-success rounded-pill` | `.tag.green` |
| Skill chips (light bg) | `badge rounded-pill border border-primary text-primary fw-normal` | `.tag.outline` |
| Card tags | `badge bg-primary rounded-pill` | `.tag.on-violet` inside card |

### Buttons

| Design1 | design3 Bootstrap |
|---|---|
| `.stamp-btn` | `btn btn-outline-primary rounded-pill fw-bold text-uppercase` |
| `.stamp-btn.filled` | `btn btn-primary rounded-pill fw-bold text-uppercase` |
| `.stamp-btn.on-violet` | `btn btn-outline-light rounded-pill fw-bold text-uppercase` |
| `.stamp-btn.on-violet` filled CTA on violet | `btn btn-light rounded-pill fw-bold text-uppercase text-primary` |
| `.stamp-btn.job-more` | `btn btn-outline-light rounded-pill btn-sm fw-bold text-uppercase align-self-end` |

### Designer profile header

Replaces design1's `.designer-header` grid:

```html
<div class="card bg-primary text-white border-0 rounded-4">
  <div class="card-body p-4 p-md-5">
    <div class="row g-4 align-items-start">
      <div class="col-auto"><!-- avatar --></div>
      <div class="col"><!-- name, specialty, tags, bio --></div>
      <div class="col-12 col-lg-auto d-flex flex-lg-column gap-2"><!-- action buttons --></div>
    </div>
  </div>
</div>
```

The avatar uses inline `style` for exact pixel dimensions (108×108px) and the custom border treatment since Bootstrap has no utility for a specific border-color with reduced opacity.

### Stats strip

Replaces design1's `.stats-strip`:

```html
<div class="d-flex flex-wrap gap-4 border-top border-bottom border-primary border-2 py-3">
  <div class="font-monospace text-uppercase" style="font-size:.78rem; letter-spacing:.2em">
    <strong class="d-block text-primary mb-1" style="font-size:1.25rem">12</strong>
    Projects Completed
  </div>
  …
</div>
```

### Portfolio gallery

Replaces design1's `.gallery` (CSS grid):

```html
<div class="row row-cols-2 row-cols-md-3 g-3">
  <div class="col">
    <div class="card border-primary border-2 rounded-3 overflow-hidden h-100">
      <div class="bg-primary d-flex align-items-center justify-content-center …" style="aspect-ratio:4/3">…</div>
      <div class="card-body d-flex flex-column p-3">…</div>
    </div>
  </div>
</div>
```

`row-cols-2 row-cols-md-3` replaces `grid-template-columns: repeat(auto-fill, minmax(240px, 1fr))`.

### Auth form (register)

Replaces design1's `.auth-page` + `.auth-box`:

```html
<main class="py-5">
  <div class="container">
    <div class="row justify-content-center">
      <div class="col-lg-6 col-md-8">
        <div class="card border-primary border-2 rounded-4 p-2">
          <div class="card-body p-4">…</div>
        </div>
      </div>
    </div>
  </div>
</main>
```

### Form fields

Replaces design1's `.field-input`, `.field-label`:

```html
<label class="form-label font-monospace text-uppercase fw-bold text-secondary" for="…" style="font-size:.72rem; letter-spacing:.22em">Field Name</label>
<input class="form-control border-primary border-2 rounded-3" type="text" …>
```

### Segmented toggle controls

Replaces design1's `.type-switcher` and `.option-group` — uses Bootstrap's native `btn-check` radio pattern:

```html
<div class="d-flex border border-primary border-2 rounded-pill p-1 gap-1" role="group" aria-label="…">
  <input type="radio" class="btn-check" name="role-radio" id="role-designer" value="designer" checked autocomplete="off">
  <label class="btn btn-outline-primary rounded-pill fw-bold text-uppercase flex-grow-1 text-center" for="role-designer">Designer</label>

  <input type="radio" class="btn-check" name="role-radio" id="role-client" value="client" autocomplete="off">
  <label class="btn btn-outline-primary rounded-pill fw-bold text-uppercase flex-grow-1 text-center" for="role-client">Client</label>
</div>
```

`btn-check` is a visually hidden radio input. When checked, the paired `label.btn-outline-primary` automatically renders in its active/filled state (Bootstrap handles this via `:checked + label` CSS). No JS needed for the visual toggle. Using `d-flex` instead of `btn-group` preserves `rounded-pill` on each label independently.

### Job detail sidebar panel

Replaces design1's `.side-panel` + `.panel-row`:

```html
<aside class="card border-primary border-2 rounded-3 position-sticky" style="top:1.5rem">
  <div class="card-body">
    <p class="text-success font-monospace text-uppercase fw-bold mb-3">Job Details</p>
    <div class="d-flex justify-content-between py-2 border-bottom">
      <span class="font-monospace text-uppercase text-secondary">Budget</span>
      <span class="fw-semibold">€€€ (High)</span>
    </div>
    …
  </div>
</aside>
```

### Section label (job detail)

Replaces design1's `.section-label`:

```html
<p class="text-success font-monospace text-uppercase fw-bold border-bottom border-primary pb-2 mb-3">About the Project</p>
```

---

## Iframe Shell Architecture

Same architecture as design1. `index.html` acts as a persistent shell — it renders the navbar and footer once and swaps only the inner content via an `<iframe>`.

```
┌─────────────────────────────────────────┐
│  index.html  (shell — never reloads)    │
│  ┌───────────────────────────────────┐  │
│  │  <nav class="navbar bg-primary">  │  │
│  └───────────────────────────────────┘  │
│  ┌───────────────────────────────────┐  │
│  │  <iframe id="content-frame"       │  │  ← src changes on every nav click
│  │    src="homepage.html">           │  │
│  └───────────────────────────────────┘  │
│  ┌───────────────────────────────────┐  │
│  │  <footer class="bg-primary">     │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

### Navigation

Every nav link carries `data-page="filename.html"` instead of a real `href`. The shell script intercepts clicks:

```js
const navigate = (page, push = true) => {
  frame.src = page;
  if (push) history.pushState({ page }, '', '#' + page);
  setActive(page);
};

allPageLinks().forEach(a => {
  a.addEventListener('click', e => { e.preventDefault(); navigate(a.dataset.page); });
});
```

### Theme sync across the iframe boundary

The shell owns the toggle. When the user switches theme, `data-bs-theme` is written onto both the shell's `<html>` and the iframe's `<html>`:

```js
const applyTheme = t => {
  document.documentElement.setAttribute('data-bs-theme', t);
  localStorage.setItem('theme', t);
  try { frame.contentDocument.documentElement.setAttribute('data-bs-theme', t); } catch (_) {}
};
```

Each inner page has a tiny `<script>` in `<head>` that restores the saved theme from `localStorage` before first paint — no flash on direct URL load.

### Adding a new page

1. Create the HTML file — copy `jobs.html` as a starting point.
2. Do **not** add `<nav>` or `<footer>` — the shell provides those.
3. Add a `data-page="your-page.html"` link in `index.html`.
4. Wrap content in `<main>` and use `container-xxl px-3 my-5` for vertical rhythm.

---

## Dark Mode

Bootstrap 5.3's dark mode is triggered by `data-bs-theme="dark"` on `<html>`. Bootstrap automatically adjusts its component variables — card backgrounds, border colours, form controls, etc. `theme.css` only overrides the body background and secondary background to match design1's deep navy palette.

The navbar always has `data-bs-theme="dark"` on the `<nav>` itself — it stays violet regardless of the global page theme.

---

## How To…

### …add a new job card

Copy any `<article class="card bg-primary …">` block in `jobs.html`, swap the content. No JS or CSS changes needed.

### …change the brand colour

Edit `--bs-primary`, `--bs-primary-rgb`, and the `.btn-primary` / `.btn-outline-primary` scoped variables in `theme.css`. All violet surfaces, borders, and buttons update automatically.

### …add a segmented toggle with more than 3 options

Copy the `btn-check` pattern. Add more `<input type="radio"> + <label>` pairs inside the `d-flex` wrapper. The `flex-grow-1` on each label distributes width evenly.

### …wire up the real API

The `#search-form` submit handler in `app.js` is the main integration point. Replace `window.location.href` with a `fetch` call:

```js
document.getElementById('search-form')?.addEventListener('submit', async e => {
  e.preventDefault();
  const q = e.target.querySelector('.search-input').value.trim();
  const res = await fetch(`/api/jobs?q=${encodeURIComponent(q)}`);
  const jobs = await res.json();
  // render jobs into the DOM
});
```

The register form's submit handler (`console.log('register:', …)`) and the profile-edit form's submit handler (`console.log('Form submitted')`) are the other two integration points.

---

## Accessibility

- Contrast: `text-white` on `bg-primary` (violet) exceeds WCAG AA.
- All interactive elements are real `<button>` / `<a>` / `<label for>` tags.
- `btn-check` toggles are keyboard-operable radio inputs.
- Bootstrap manages `aria-expanded` / `aria-controls` on the hamburger toggler automatically.
- The iframe has `title="Page content"` for screen readers.
- Focus styles come from Bootstrap's default ring, recoloured to green via `--bs-focus-ring-color` override in `theme.css`.