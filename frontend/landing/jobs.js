/* DESIGNJOBS.COM — jobs.js
   All-jobs listing page: load GET /jobs, render cards with a
   client-side "load more", and apply to a job. escapeHtml comes
   from common.js. */
let allJobs = [];
let visibleJobCount = 8;

function getJobId(job) {
  return job.id || job.jobId || job.uuid || "";
}

function renderJobs() {
  const jobsContainer = document.getElementById("jobs-container");
  const listingsCounter = document.getElementById("listings-counter");
  const loadMoreButton = document.getElementById("load-more-button");

  jobsContainer.innerHTML = "";

  listingsCounter.textContent = allJobs.length + " listings";

  if (allJobs.length === 0) {
    jobsContainer.innerHTML = `
      <div class="col-12">
        <div class="alert alert-secondary">
          No jobs available yet.
        </div>
      </div>
    `;

    loadMoreButton.classList.add("d-none");
    return;
  }

  const visibleJobs = allJobs.slice(0, visibleJobCount);

  visibleJobs.forEach(job => {
    const jobId = getJobId(job);

    const title = escapeHtml(job.title || "Untitled Job");
    const description = escapeHtml(job.description || "No description available.");
    const location = escapeHtml(job.location || "Remote");
    const budget = escapeHtml(job.budget || "€€");
    const category = escapeHtml(job.category || "Design");

    const card = document.createElement("div");
    card.className = "col-12";

    card.innerHTML = `
      <article class="card bg-primary text-white border-0 rounded-4">
        <div class="card-body d-flex flex-column p-4">

          <h3 class="font-monospace text-uppercase fw-bold text-white mb-3"
              style="font-size:1.2rem; letter-spacing:.06em">
            ${title}
          </h3>

          <p class="card-text opacity-90 flex-grow-1">
            ${description}
          </p>

          <div class="d-flex gap-2 font-monospace text-uppercase mt-3 mb-3 opacity-70 flex-wrap"
               style="font-size:.75rem; letter-spacing:.18em">
            <span>${location}</span>
            <span>·</span>
            <span>${budget}</span>
            <span>·</span>
            <span>${category}</span>
          </div>

          <div class="d-flex gap-2 justify-content-end flex-wrap">
            <a href="job-detail.html?id=${encodeURIComponent(jobId)}"
               class="btn btn-outline-light rounded-pill btn-sm fw-bold text-uppercase px-3"
               style="letter-spacing:.12em">
              Read More
            </a>

            <button type="button"
                    class="btn btn-light rounded-pill btn-sm fw-bold text-uppercase text-primary px-3 apply-button"
                    data-job-id="${escapeHtml(jobId)}"
                    style="letter-spacing:.12em">
              Apply
            </button>
          </div>

        </div>
      </article>
    `;

    jobsContainer.appendChild(card);
  });

  if (visibleJobCount >= allJobs.length) {
    loadMoreButton.classList.add("d-none");
  } else {
    loadMoreButton.classList.remove("d-none");
  }
}

async function loadJobs() {
  const jobsContainer = document.getElementById("jobs-container");
  const listingsCounter = document.getElementById("listings-counter");

  try {
    const response = await fetch(`${API_BASE}/jobs`);

    if (!response.ok) {
      throw new Error("Could not load jobs.");
    }

    allJobs = await response.json();

    if (!Array.isArray(allJobs)) {
      allJobs = [];
    }

    renderJobs();

  } catch (error) {
    console.error("Error while loading jobs:", error);

    listingsCounter.textContent = "0 listings";

    jobsContainer.innerHTML = `
      <div class="col-12">
        <div class="alert alert-danger">
          Jobs could not be loaded. Please check if the backend is running.
        </div>
      </div>
    `;
  }
}

async function applyToJob(jobId) {
  const token = localStorage.getItem("designer_jobs_token");

  if (!token) {
    alert("Please log in before applying to a job.");
    window.location.href = "login.html";
    return;
  }

  if (!jobId) {
    alert("This job has no valid ID.");
    return;
  }

  try {
    const response = await fetch(`${API_BASE}/jobs/${encodeURIComponent(jobId)}/apply`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + token
      },
      body: JSON.stringify({
        coverLetter: "I would like to apply for this job."
      })
    });

    const result = await response.json().catch(() => ({}));

    if (!response.ok) {
      throw new Error(result.error || "Application failed.");
    }

    alert("Application sent successfully.");

  } catch (error) {
    console.error("Error while applying:", error);
    alert("Application could not be sent. Please check backend, login and application endpoint.");
  }
}

document.addEventListener("click", event => {
  const applyButton = event.target.closest(".apply-button");

  if (!applyButton) {
    return;
  }

  applyToJob(applyButton.dataset.jobId);
});

document.getElementById("load-more-button").addEventListener("click", () => {
  visibleJobCount += 8;
  renderJobs();
});

loadJobs();
