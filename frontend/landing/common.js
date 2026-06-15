/* ================================================================
   DESIGNJOBS.COM — common.js
   Tiny shared helpers used across pages. Load this BEFORE any
   per-page <page>.js so the global below is available.

   Pure vanilla JS, no dependencies. Exposes two globals:
   - API_BASE   : the backend origin — single source of truth, used
                  by every page's fetch() calls as `${API_BASE}/...`
   - escapeHtml : HTML-escape a value for safe interpolation
   ================================================================ */
(() => {
  "use strict";

  // Single source of truth for the backend origin. Change it here
  // (or point it at a deployed host) and every page follows.
  const API_BASE = "http://localhost:8080";

  /* HTML-escape a value before dropping it into innerHTML / template
     strings. Was duplicated verbatim in chat / jobs / job-detail. */
  function escapeHtml(value) {
    if (value === null || value === undefined) return "";
    return String(value)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  window.API_BASE = API_BASE;
  window.escapeHtml = escapeHtml;
})();
