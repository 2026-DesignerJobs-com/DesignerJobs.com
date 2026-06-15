/* DESIGNJOBS.COM — job-random.js
   "Surprise me": pick a random job client-side from GET /jobs and
   redirect to its detail page. There is no /jobs/random endpoint. */
document.addEventListener("DOMContentLoaded", loadRandomJob);

async function loadRandomJob() {
  try {
    const response = await fetch(`${API_BASE}/jobs`);

    if (!response.ok) {
      throw new Error("Could not load jobs.");
    }

    const jobs = await response.json();

    if (!Array.isArray(jobs) || jobs.length === 0) {
      document.querySelector("main").innerHTML = `
        <section class="container-xxl px-3" style="margin-top:3rem">
          <div class="alert alert-warning">
            No jobs were found.
          </div>
        </section>
      `;
      return;
    }

    const randomIndex = Math.floor(Math.random() * jobs.length);
    const randomJob = jobs[randomIndex];

    if (!randomJob.id) {
      throw new Error("Random job has no ID.");
    }

    window.location.replace(
      "job-detail.html?id=" + encodeURIComponent(randomJob.id)
    );

  } catch (error) {
    console.error("Random job loading failed:", error);

    document.querySelector("main").innerHTML = `
      <section class="container-xxl px-3" style="margin-top:3rem">
        <div class="alert alert-danger">
          Random job could not be loaded. Please check if the backend is running and GET /jobs works.
        </div>
      </section>
    `;
  }
}
