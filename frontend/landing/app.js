/* ================================================================
   DESIGNJOBS.COM — app.js
   Minimal vanilla JS. Bootstrap handles the mobile nav collapse.
   ================================================================ */
(() => {
  "use strict";

  /* --- 1. Dark / light mode toggle ---------------------------- */
  const toggle = document.getElementById("theme-toggle");
  if (toggle) {
    const apply = t => {
      toggle.textContent = t === "dark" ? "☀ Light" : "☾ Dark";
    };
    // Button label reflects whatever the <head> script already set
    apply(document.documentElement.getAttribute("data-bs-theme") || "light");

    toggle.addEventListener("click", () => {
      const next = document.documentElement.getAttribute("data-bs-theme") === "dark"
        ? "light" : "dark";
      document.documentElement.setAttribute("data-bs-theme", next);
      localStorage.setItem("theme", next);
      apply(next);
    });
  }

  /* --- 2. Banner search → jobs.html?q=... -------------------- */
  document.getElementById("search-form")?.addEventListener("submit", e => {
    e.preventDefault();
    const q = e.target.querySelector(".search-input").value.trim();
    window.location.href = "jobs.html" + (q ? "?q=" + encodeURIComponent(q) : "");
  });

})();