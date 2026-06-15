document.addEventListener("DOMContentLoaded", async () => {
  const heading = document.getElementById("results-heading");
  const count = document.getElementById("results-count");
  const resultsContainer = document.getElementById("jobs-results");
  const noResults = document.getElementById("no-results");

  const pageParams = new URLSearchParams(window.location.search);
  const backendParams = new URLSearchParams();

  const q = pageParams.get("q");
  const discipline = pageParams.get("discipline");
  const type = pageParams.get("type");
  const location = pageParams.get("location");
  const budget = pageParams.get("budget");

  if (heading) {
    heading.textContent = q ? `"${q}"` : "All Jobs";
  }

  if (q) backendParams.set("q", q);
  if (location) backendParams.set("location", location);

  if (discipline) backendParams.set("category", discipline);
  if (type) backendParams.set("workMode", type);
  if (budget) backendParams.set("budget", mapBudgetToBackendValue(budget));

  const requestUrl = `${API_BASE}/jobs?` + backendParams.toString();

  try {
    if (count) count.textContent = "Loading...";
    if (resultsContainer) resultsContainer.innerHTML = "";

    const response = await fetch(requestUrl);

    if (!response.ok) {
      throw new Error("Backend returned " + response.status);
    }

    const jobs = await response.json();

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
    "3": "big"      // matches the value post-a-job.html stores (not "large")
  };

  return budgets[value] || value;
}

// escapeHtml is provided globally by common.js