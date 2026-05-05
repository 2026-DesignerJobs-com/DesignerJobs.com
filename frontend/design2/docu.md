# DESIGNJOBS.COM — design2 Developer Documentation

Bootstrap-first frontend for the freelance design-jobs portal.
**Bootstrap 5.3** handles grid, spacing, flex layout, nav collapse, and dark mode base.
**styles_bootstrap+.css** adds only what Bootstrap cannot — brand colours, fonts, component shapes, the SVG blob, and animations.
**Vanilla JS** for theme toggle, search routing, and iframe navigation.

---

## Quick Start

```bash
cd frontend
python3 -m http.server 8080
# Visit http://localhost:8080/design2/
```

No bundler, no transpiler, no `node_modules`.

---

## Project Tree

```
frontend/design2/
├── index.html              ← Shell — navbar + iframe + footer (entry point)
├── shell.html              ← Redirect to index.html
├── homepage.html           ← Homepage content (search banner + recent jobs)
├── jobs.html               ← Full job listings page
├── job-random.html         ← Single job detail page
├── profile.html            ← Designer public profile
├── profile-edit.html       ← Edit profile form
├── register.html           ← Registration / login page
├── about.html              ← About us + team + impressum section
├── impressum.html          ← Legal information page
├── styles_bootstrap+.css   ← Custom styles — only what Bootstrap cannot do
├── app.js                  ← Theme toggle, search submit
└── docu.md                 ← This file
```

---

## CSS Philosophy: Bootstrap-First

design2 sits between design1 and design3 in its approach to CSS.

| | design1 | design2 | design3 |
|---|---|---|---|
| Layout (flex, gap, margin, padding) | Custom CSS classes | **Bootstrap utilities in HTML** | Bootstrap utilities in HTML |
| Component shapes (border-radius, border, bg) | Custom CSS classes | **Custom CSS classes** | Bootstrap utilities in HTML |
| Brand colours | CSS custom properties | CSS custom properties | Bootstrap variable overrides |
| Custom CSS file | `styles.css` | **`styles_bootstrap+.css`** | `theme.css` (vars only) |

The rule of thumb for where a property lives:

> If Bootstrap has a utility class for it — `mb-3`, `d-flex`, `gap-2`, `align-self-end`, `py-4` — write it in the HTML.
> If Bootstrap cannot express it — a specific colour, a pill border, a font with letter-spacing, an SVG animation — write it in `styles_bootstrap+.css`.

This means HTML is slightly more verbose (more classes per element) but `styles_bootstrap+.css` is lean — it only contains genuinely custom rules, each with a comment explaining why Bootstrap is insufficient.

### Before / After comparison

design1 puts spacing in CSS:
```css
/* design1 — styles.css */
.job-meta {
  display: flex;
  gap: .6rem;
  margin-top: auto;
  padding-top: 1.4rem;
}
```

design2 puts spacing in HTML:
```html
<!-- design2 — homepage.html -->
<div class="job-meta mt-auto pt-3 d-flex gap-2">
```
`styles_bootstrap+.css` only keeps the font, letter-spacing, and opacity — things Bootstrap has no utility for.

---

## Design Tokens (CSS Custom Properties)

`styles_bootstrap+.css` opens with a token block shared across all components:

```css
:root {
  --primary:      #7C3AED;   /* violet — navbar, cards, buttons */
  --primary-deep: #6D28D9;   /* darker violet — hover states */
  --bg:           #FAFAFA;   /* page background */
  --bg-soft:      #F0EBFF;   /* subtle tinted background */
  --ink:          #1E1B2E;   /* headings and body text */
  --ink-soft:     #4B4369;   /* secondary text */
  --accent:       #10B981;   /* emerald — divider, count labels */
  --on-primary:   #EDE9F8;   /* text on violet surfaces */
  --font-ui:      "Space Mono", monospace;
  --font-body:    "Fraunces", Georgia, serif;
  --r-pill:       999px;     /* border-radius for pill shapes */
  --r-lg:         28px;      /* large cards */
  --r-md:         22px;      /* medium cards, inputs */
  --stroke:       2px;       /* standard border width */
  --max-w:        1280px;    /* max content width */
}
```

Dark mode swaps only bg and text tokens — violet stays violet:

```css
[data-bs-theme="dark"] {
  --bg:       #0C0A14;
  --bg-soft:  #161225;
  --ink:      #EDE9F8;
  --ink-soft: #A89DC0;
}
```

To re-skin: edit `--primary` and `--primary-deep`. Everything cascades.

---

## What Bootstrap Handles vs What `styles_bootstrap+.css` Handles

### Navigation bar

| Property | Where |
|---|---|
| Collapse on mobile, hamburger toggler | Bootstrap `navbar navbar-expand-lg` |
| Responsive layout of nav items | Bootstrap `navbar-nav mx-auto gap-4` |
| Right-side flex group | Bootstrap `d-flex align-items-center gap-2 mt-2 mt-lg-0` |
| Violet background colour | `.top-nav` in CSS |
| Brand font, tracking, colour | `.top-nav .navbar-brand` in CSS |
| Link font, tracking, colour | `.top-nav .nav-link` in CSS |
| Sliding underline on hover/active | `.top-nav .nav-link::after` in CSS — no Bootstrap equivalent |

### Footer

| Property | Where |
|---|---|
| Flex layout, gap, alignment | Bootstrap `d-flex flex-wrap gap-4 align-items-center justify-content-between` |
| Padding | Bootstrap `py-4 px-4` |
| Violet background, font, tracking | `.main-footer` in CSS |
| Link colours | `.main-footer a` in CSS |

### Job cards

| Property | Where |
|---|---|
| Grid columns | Bootstrap `row g-4` + `col-md-6 col-lg-4` |
| Internal flex column | `.job-card { display: flex; flex-direction: column }` in CSS |
| Bottom-aligning meta row | Bootstrap `mt-auto` on `.job-meta` |
| Top padding above meta row | Bootstrap `pt-3` on `.job-meta` |
| Meta row flex + gap | Bootstrap `d-flex gap-2` on `.job-meta` |
| "Read More" alignment | Bootstrap `align-self-end mt-3` |
| Violet background, border-radius, border, hover lift | `.job-card` in CSS |
| Title font, casing, tracking | `.job-title` in CSS |
| Body line-clamp | `.job-body` in CSS (webkit-specific, no Bootstrap equivalent) |
| Meta font, tracking, opacity | `.job-meta` in CSS |

### Section headings

design2 uses Bootstrap utilities directly rather than a custom `.section-head` class:

```html
<div class="d-flex align-items-baseline gap-3 pb-2 mb-4"
     style="border-bottom: var(--stroke) solid var(--ink)">
  <h2>Recent Postings</h2>
  <span class="count ms-auto">03 / 24</span>
</div>
```

The inline `style` for `border-bottom` is a deliberate one-off — it uses two custom tokens that Bootstrap has no utility for.

### Auth form

| Property | Where |
|---|---|
| Page centering | `.auth-page { display: grid; place-items: center }` in CSS |
| Card border, border-radius, background | `.auth-box` in CSS |
| Card padding | Bootstrap `p-4` or `p-md-5` in HTML |
| Field label font, tracking | `.field-label` in CSS |
| Input border, border-radius, font | `.field-input`, `.field-textarea`, `.field-select` in CSS |
| Field margin below each group | Bootstrap `mb-3` in HTML |

### Sidebar panel (job detail)

| Property | Where |
|---|---|
| Sticky positioning | `.side-panel { position: sticky; top: 1.5rem }` in CSS |
| Border, border-radius, background | `.side-panel` in CSS |
| Internal padding | Bootstrap `p-4` in HTML |
| Panel row flex + border-bottom | `.panel-row` in CSS |

### Profile page

| Property | Where |
|---|---|
| Avatar + info + actions layout | `.designer-header { display: grid; grid-template-columns: auto 1fr auto }` in CSS — Bootstrap's 12-col system doesn't auto-size the outer columns |
| Stats row flex + gap | Bootstrap `d-flex flex-wrap gap-4 py-3` in HTML |
| Stats border top + bottom | `.stats-strip` in CSS |
| Portfolio gallery auto-fill grid | `.gallery { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)) }` in CSS — no Bootstrap equivalent for auto-fill at a minimum column width |

---

## Class Reference

Custom classes defined in `styles_bootstrap+.css`. Bootstrap utility classes used alongside these are listed separately in the section above.

| Class | What it is | Bootstrap handles |
|---|---|---|
| `.top-nav` | Violet navbar bar | collapse, toggler, responsive layout |
| `.wide-section` | Max-width centering wrapper (1280px) | horizontal padding (`px-3 px-md-4`), vertical margin (`my-5`) |
| `.count` | Small accent count label beside headings | alignment (`ms-auto`) |
| `.search-panel` | Violet panel with grid centering | outer padding (`px-3 px-md-4`), margin (`my-5`) |
| `.stamp-corner` | Decorative absolute-positioned corner label | — |
| `.blob-area` | Container for SVG blob and content overlay | — |
| `.blob-outline` | SVG path — wide light stroke (outermost ring) | — |
| `.blob-gap` | SVG path — violet stroke creating the gap | — |
| `.blob-fill` | SVG path — solid fill in `var(--bg)` | — |
| `.blob-content` | Content layer above the SVG | — |
| `.or-divider` | "OR" text with flanking lines | — |
| `.search-bar` | Pill-shaped search form on dark violet | — |
| `.search-icon` | Inline SVG magnifier | — |
| `.search-input` | Transparent input inside search bar | — |
| `.job-card` | Violet listing card | grid columns, `mt-auto pt-3 d-flex gap-2` on meta, `align-self-end mt-3` on button |
| `.job-title` | Card headline | `mb-3` top margin in HTML |
| `.job-body` | Card description (line-clamped) | `flex-grow-1` if needed |
| `.job-meta` | Small caps location/budget/date row | `mt-auto pt-3 d-flex gap-2` |
| `.job-header` | Violet hero block on job detail page | vertical margin (`mt-*`) |
| `.section-label` | Accent-coloured label above a content block | `mb-3` in HTML |
| `.job-description` | Body copy on job detail page | — |
| `.side-panel` | Sticky sidebar info card | internal padding (`p-4`) |
| `.panel-label` | Heading inside a side panel | `mb-3` in HTML |
| `.panel-row` | Key/value row with bottom border | — |
| `.panel-key` | Left cell of a panel row | — |
| `.panel-val` | Right cell of a panel row | — |
| `.designer-header` | Profile header: avatar + info + actions grid | — |
| `.avatar` | Square avatar tile with initials | — |
| `.specialty` | Designer role label below name | `mb-3` in HTML |
| `.stats-strip` | Border-top and border-bottom around stats | `d-flex flex-wrap gap-4 py-3` in HTML |
| `.stat` | Single stat block | — |
| `.gallery` | Auto-fill portfolio grid | — |
| `.work-item` | A single portfolio card | — |
| `.work-preview` | Thumbnail/placeholder area | — |
| `.work-title` | Title of a portfolio item | `mb-2` in HTML |
| `.tag` | Pill chip label; modifiers: `.on-violet`, `.green`, `.outline` | `d-flex flex-wrap gap-2` on tag rows |
| `.auth-page` | Full-screen centering wrapper | — |
| `.auth-box` | Card box | internal padding (`p-4 p-md-5`) |
| `.eyebrow` | Small green label above auth heading | `mb-1` in HTML |
| `.field-label` | All-caps form label | `mb-3` on field groups |
| `.field-input` | Text/email/password/number input | — |
| `.field-textarea` | Textarea | — |
| `.field-select` | Select dropdown | — |
| `.type-switcher` | Segmented 2-option toggle | — |
| `.option-group` | Segmented 3-option toggle | — |
| `.text-divider` | "or" divider with horizontal rule lines | `my-4` in HTML |
| `.main-footer` | Violet footer bar | layout (`d-flex flex-wrap …`), padding (`py-4 px-4`) |
| `.footer-brand` | Bold brand name | — |
| `.stamp-btn` | Pill button; modifiers: `.filled`, `.on-violet` | `align-self-end mt-3` when inside a card |

### Hamburger menu

Bootstrap's `data-bs-toggle="collapse"` / `data-bs-target="#nav-menu"` handles the show/hide entirely — no custom JS needed. The shell's JS closes the menu after a nav link is clicked so the iframe content is immediately visible.

### SVG blob

Three identical `<path>` elements stacked in paint order inside `.blob-area`:

1. `.blob-outline` — wide light stroke; only its outermost edge is visible
2. `.blob-gap` — narrower violet stroke; covers everything up to ~10px inside the outline, creating a gap
3. `.blob-fill` — solid fill in `var(--bg)`; adapts to dark mode automatically

To reshape the blob: paste the `d` attribute into an SVG path editor, drag handles, copy the result back into all three `<path>` elements (they share the same `d`).

On mobile (`< 768px`) the SVG is hidden and the cutout becomes a flat rounded rectangle.

---

## Iframe Shell Architecture

`index.html` is a persistent **shell** — the navbar and footer render once and never reload. Only the `<iframe>` content changes on navigation.

```
┌─────────────────────────────────────────┐
│  index.html  (shell — never reloads)    │
│  ┌───────────────────────────────────┐  │
│  │  <nav class="navbar top-nav">    │  │
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

### Navigation

Nav links carry `data-page="filename.html"` instead of a real `href`. The shell script intercepts clicks and updates `frame.src`:

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

The shell owns the theme toggle. When the user switches, `data-bs-theme` is written onto both the shell and the iframe's `<html>` element:

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
4. Use `wide-section my-5 px-3 px-md-4` as the outer wrapper for vertical and horizontal rhythm.

---

## How To…

### …add a new job card

Copy any `<article class="job-card">` block, swap the content. Remember to keep Bootstrap utilities on the inner elements (`mt-auto pt-3 d-flex gap-2` on the meta row, `align-self-end mt-3` on the link).

### …add a new page section

```html
<section class="wide-section my-5 px-3 px-md-4" aria-labelledby="my-h">
  <div class="d-flex align-items-baseline gap-3 pb-2 mb-4"
       style="border-bottom: var(--stroke) solid var(--ink)">
    <h2 id="my-h">Section Title</h2>
  </div>
  <!-- content -->
</section>
```

### …change the brand colour

Edit `--primary` and `--primary-deep` in `:root` inside `styles_bootstrap+.css`. All violet surfaces, borders, and buttons update automatically.

### …decide if a new property goes in CSS or HTML

- Does Bootstrap have a utility class for it? → put it in the HTML class list.
- Is it specific to one component's visual shape or brand identity? → put it in `styles_bootstrap+.css` with a comment explaining why.

### …wire up the real API

`app.js` holds the integration point — the `#search-form` submit handler. Replace `window.location.href` with:

```js
document.getElementById('search-form')?.addEventListener('submit', async e => {
  e.preventDefault();
  const q = e.target.querySelector('.search-input').value.trim();
  const res = await fetch(`/api/jobs?q=${encodeURIComponent(q)}`);
  const jobs = await res.json();
  // render jobs into the DOM
});
```

The register form (`console.log('register:', …)`) and the profile-edit form (`console.log('Form submitted')`) are the other two integration points.

---

## Accessibility

- Contrast: `--on-primary` on `--primary` (violet) exceeds WCAG AA.
- All interactive elements are real `<button>` or `<a>` tags.
- Focus ring: 3px emerald outline, visible on all backgrounds.
- `prefers-reduced-motion` disables the blob content stagger-reveal animation.
- Bootstrap manages `aria-expanded` / `aria-controls` on the hamburger toggler automatically.
- The iframe has `title="Page content"` for screen readers.