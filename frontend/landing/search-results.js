document.addEventListener("DOMContentLoaded", async () => {
  const heading = document.getElementById("results-heading");
  const count = document.getElementById("results-count");
  const resultsContainer = document.getElementById("jobs-results");
  const noResults = document.getElementById("no-results");
  const clearAllBtn = document.getElementById("clear-all-btn");
  const filterForm = document.getElementById("desktop-filter-form");

  const pageParams = new URLSearchParams(window.location.search);
  const backendParams = new URLSearchParams();

  const q = pageParams.get("q");
  const location = pageParams.get("location");

  // Gather all checked values from URL into filter arrays
  const disciplines = pageParams.getAll("discipline");
  const types = pageParams.getAll("type");
  const budgets = pageParams.getAll("budget");
  const postedVal = pageParams.get("posted");

  // ========================================================
  // 1. SIDEBAR MANAGEMENT
  // ========================================================
  if (filterForm) {
    // A. Restore states of multiple checkboxes on page load
    const restoreCheckboxes = (name, activeValues) => {
      activeValues.forEach(val => {
        const cb = filterForm.querySelector(`input[name="${name}"][value="${val}"]`);
        if (cb) cb.checked = true;
      });
    };

    restoreCheckboxes("discipline", disciplines);
    restoreCheckboxes("type", types);
    restoreCheckboxes("budget", budgets);

    // Restore select dropdown state
    const postedSelect = document.getElementById("sf-posted");
    if (postedSelect && postedVal) {
      postedSelect.value = postedVal;
    }

    // B. Listen for form input changes and auto-submit live
    filterForm.addEventListener("change", () => {
      // Retain the search bar 'q' string and location parameter if they exist
      if (q && !filterForm.querySelector('input[name="q"]')) {
        const hiddenQ = document.createElement("input");
        hiddenQ.type = "hidden";
        hiddenQ.name = "q";
        hiddenQ.value = q;
        filterForm.appendChild(hiddenQ);
      }
      if (location && !filterForm.querySelector('input[name="location"]')) {
        const hiddenLoc = document.createElement("input");
        hiddenLoc.type = "hidden";
        hiddenLoc.name = "location";
        hiddenLoc.value = location;
        filterForm.appendChild(hiddenLoc);
      }
      filterForm.submit();
    });
  }

  // ========================================================
  // 2. CLEAR ALL FUNCTIONALITY
  // ========================================================
  if (clearAllBtn) {
    clearAllBtn.addEventListener("click", () => {
      // Wipes all filters completely and reloads to show all jobs
      window.location.href = window.location.pathname;
    });
  }

  // ========================================================
  // 3. BACKEND API SEARCH & FETCH
  // ========================================================
  if (heading) {
    heading.textContent = q ? `"${q}"` : "All Jobs";
  }

  if (q) backendParams.set("q", q);
  if (location) backendParams.set("location", location);

  const requestUrl = "http://localhost:8080/jobs?" + backendParams.toString();

  try {
    if (count) count.textContent = "Loading...";
    if (resultsContainer) resultsContainer.innerHTML = "";

    const response = await fetch(requestUrl);

    if (!response.ok) {
      throw new Error("Backend returned " + response.status);
    }

    let jobs = await response.json();

    // ========================================================
    // 4. CLIENT-SIDE FILTERING (Flexible Multi-Select OR Logic)
    // ========================================================
    if (Array.isArray(jobs)) {

      // Filter Discipline (Matches ANY checked discipline)
      if (disciplines.length > 0) {
        jobs = jobs.filter(job => {
          const jobCat = job.category || job.designType || "";
          return disciplines.some(d => d.toLowerCase() === jobCat.toLowerCase());
        });
      }

      // Filter Work Type (Matches ANY checked work mode)
      if (types.length > 0) {
        jobs = jobs.filter(job => {
          const jobMode = job.workMode || "";
          return types.some(t => t.toLowerCase() === jobMode.toLowerCase());
        });
      }

      // Filter Budget (Matches ANY checked budget level)
      if (budgets.length > 0) {
        jobs = jobs.filter(job => {
          const jobBudget = (job.budget || "").toLowerCase();
          return budgets.some(b => {
            const mapped = mapBudgetToBackendValue(b).toLowerCase();
            return jobBudget === mapped || jobBudget === b.toLowerCase();
          });
        });
      }
    }

    // ========================================================
    // 5. RENDER LOGIC
    // ========================================================
    if (!Array.isArray(jobs) || jobs.length === 0) {
      if (count) count.textContent = "0 Results";
      if (resultsContainer) resultsContainer.innerHTML = "";
      if (noResults) noResults.classList.remove("d-none");
      return;
    }

    if (noResults) noResults.classList.add("d-none");
    if (count) count.textContent = jobs.length + " Results";

    resultsContainer.innerHTML = jobs.map(createJobCardHtml).join("");
  } catch (error) {
    console.error("Search failed:", error);

    if (count) count.textContent = "Error";
    if (resultsContainer) {
      resultsContainer.innerHTML = `
        <p class="text-danger font-monospace">
          Could not load jobs. Check if the backend is running.
        </p>
      `;
    }
  }
});

function createJobCardHtml(job) {
  const jobId = job.id;
  const detailUrl = jobId
      ? "job-detail.html?id=" + encodeURIComponent(jobId)
      : "#";

  return `
    <article class="result-card p-4">
      <div class="d-flex flex-column flex-sm-row gap-3 align-items-sm-start">
        <div class="flex-grow-1">
          <div class="d-flex gap-2 flex-wrap mb-2">
            <span class="badge rounded-pill border border-primary text-primary fw-bold px-3 py-1 font-monospace text-uppercase" style="font-size:.65rem; letter-spacing:.12em">
              ${escapeHtml(job.category || job.designType || "Design")}
            </span>
            <span class="badge rounded-pill border border-success text-success fw-bold px-3 py-1 font-monospace text-uppercase" style="font-size:.65rem; letter-spacing:.12em">
              ${escapeHtml(job.workMode || "Remote")}
            </span>
          </div>

          <h2 class="font-monospace text-uppercase fw-bold mb-2" style="font-size:1.15rem; letter-spacing:.06em">
            ${escapeHtml(job.title || "Untitled Job")}
          </h2>

          <p class="mb-3" style="display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; font-size:.95rem">
            ${escapeHtml(job.description || "")}
          </p>

          <div class="d-flex gap-3 flex-wrap font-monospace text-uppercase opacity-60" style="font-size:.72rem; letter-spacing:.18em">
            <span>${escapeHtml(job.workMode || "Unknown")}</span>
            <span>·</span>
            <span>${escapeHtml(job.budget || "Unknown")}</span>
            <span>·</span>
            <span>${escapeHtml(job.location || "Unknown")}</span>
          </div>
        </div>

        <div class="d-flex flex-row flex-sm-column gap-2 align-items-sm-end">
          <a href="${detailUrl}"
             class="btn btn-outline-primary rounded-pill btn-sm fw-bold text-uppercase px-4 py-2"
             style="letter-spacing:.12em; white-space:nowrap">
            View Job
          </a>
        </div>
      </div>
    </article>
  `;
}

function mapBudgetToBackendValue(value) {
  const budgets = {
    "1": "small",
    "2": "medium",
    "3": "big"
  };
  return budgets[value] || value;
}

function escapeHtml(value) {
  return String(value)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
}