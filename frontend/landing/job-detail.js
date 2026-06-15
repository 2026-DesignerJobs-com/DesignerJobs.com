/* DESIGNJOBS.COM — job-detail.js
   Single job detail: PATCH view-count + render, plus apply / delete /
   report / message-client actions. escapeHtml comes from common.js. */
let currentJobId = null;

function getJobIdFromUrl() {
    const params = new URLSearchParams(window.location.search);
    return params.get("id");
}

async function loadJobDetail() {
    const container = document.getElementById("job-detail-container");
    const status = document.getElementById("job-status");

    currentJobId = getJobIdFromUrl();

    if (!currentJobId) {
        status.textContent = "Missing ID";
        container.innerHTML = `
    <div class="alert alert-danger">
      No job ID was provided.
    </div>
  `;
        return;
    }

    try {
        const response = await fetch(
            `${API_BASE}/jobs/${encodeURIComponent(currentJobId)}/view-count`,
            {
                method: "PATCH"
            }
        );

        if (!response.ok) {
            throw new Error("Job could not be loaded.");
        }

        const job = await response.json();
        // 1. Den Button im HTML sicher greifen
        const profileBtn = document.getElementById("dynamic-profile-btn");

        // 3. Poster-ID finden (deckt alle gängigen Bezeichnungen aus deinem Backend ab)
        const posterId = job.clientId || job.userId || job.creatorId || job.client_id || job.user_id || job.id;

        // 4. Button-Logik anwenden
        if (profileBtn) {
            if (posterId) {
                // Wenn wir eine ID haben, Link setzen und Button sicher einblenden
                profileBtn.href = `/profile.html?userId=${posterId}`;
                profileBtn.style.display = "inline-block";
            } else {
                // Wenn absolut keine ID da ist, Button verstecken
                console.warn("Keine User-ID im Job gefunden! Button wird versteckt.");
                profileBtn.style.display = "none";
            }
        }

        console.log("Loaded job detail:", job);

        const title = escapeHtml(job.title || "Untitled Job");
        const description = escapeHtml(job.description || "No description available.");
        const location = escapeHtml(job.location || "Remote");
        const budget = escapeHtml(job.budget || "€€");
        const category = escapeHtml(job.category || "Design");
        const designType = escapeHtml(job.designType || job.design_type || "-");
        const workMode = escapeHtml(job.workMode || job.work_mode || "-");
        const deadline = escapeHtml(job.deadline || "-");
        const viewCount = job.viewCount || job.view_count || 0;
        const clientId = job.clientId || job.client_id || job.ownerId || job.owner_id || "";

        status.textContent = "Ready";

        container.innerHTML = `
    <article class="card bg-primary text-white border-0 rounded-4">
      <div class="card-body p-4 p-lg-5">

        <div class="d-flex justify-content-between gap-3 align-items-start mb-4">
          <h3 class="font-monospace text-uppercase fw-bold text-white mb-0"
              style="font-size:1.6rem; letter-spacing:.06em">
            ${title}
          </h3>

          <span class="badge rounded-pill bg-light text-primary font-monospace text-uppercase px-3 py-2"
                style="font-size:.7rem; letter-spacing:.14em; white-space:nowrap">
            ${viewCount} Views
          </span>
        </div>

        <p class="opacity-90 mb-4" style="font-size:1.05rem; line-height:1.7">
          ${description}
        </p>

        <div class="row g-3 font-monospace text-uppercase opacity-75 mb-4"
             style="font-size:.78rem; letter-spacing:.16em">

          <div class="col-md-6">
            <div class="border border-light border-opacity-25 rounded-4 p-3 h-100">
              <div class="opacity-75 mb-1">Location</div>
              <strong>${location}</strong>
            </div>
          </div>

          <div class="col-md-6">
            <div class="border border-light border-opacity-25 rounded-4 p-3 h-100">
              <div class="opacity-75 mb-1">Budget</div>
              <strong>${budget}</strong>
            </div>
          </div>

          <div class="col-md-6">
            <div class="border border-light border-opacity-25 rounded-4 p-3 h-100">
              <div class="opacity-75 mb-1">Category</div>
              <strong>${category}</strong>
            </div>
          </div>

          <div class="col-md-6">
            <div class="border border-light border-opacity-25 rounded-4 p-3 h-100">
              <div class="opacity-75 mb-1">Design Type</div>
              <strong>${designType}</strong>
            </div>
          </div>

          <div class="col-md-6">
            <div class="border border-light border-opacity-25 rounded-4 p-3 h-100">
              <div class="opacity-75 mb-1">Work Mode</div>
              <strong>${workMode}</strong>
            </div>
          </div>

          <div class="col-md-6">
            <div class="border border-light border-opacity-25 rounded-4 p-3 h-100">
              <div class="opacity-75 mb-1">Deadline</div>
              <strong>${deadline}</strong>
            </div>
          </div>
        </div>

        <div class="d-flex gap-2 justify-content-end flex-wrap">
  <a href="jobs.html"
     class="btn btn-outline-light rounded-pill fw-bold text-uppercase px-4"
     style="letter-spacing:.14em">
    Back
  </a>

  <button type="button"
        id="message-client-button"
        class="btn btn-outline-light rounded-pill fw-bold text-uppercase px-4"
        style="letter-spacing:.14em">
  Message Client
</button>

  <button type="button"
          id="apply-button"
          class="btn btn-light rounded-pill fw-bold text-uppercase text-primary px-4"
          style="letter-spacing:.14em">
    Apply
  </button>
  <button type="button"
        id="delete-job-button"
        class="btn btn-outline-light rounded-pill fw-bold text-uppercase px-4"
        style="letter-spacing:.14em">
  Delete Job
</button>

<button type="button"
        id="report-job-button"
        class="btn btn-outline-light rounded-pill fw-bold text-uppercase px-4"
        style="letter-spacing:.14em">
  Report Job
</button>

</div>


    </div>
        </article>
        `;

    document.getElementById("apply-button").addEventListener("click", () => {
        applyToJob(currentJobId);
    });

    document.getElementById("delete-job-button").addEventListener("click", () => {
        deleteJob(currentJobId);
    });


    document.getElementById("report-job-button").addEventListener("click", () => {
        reportCurrentJob(currentJobId);
    });
    document.getElementById("message-client-button").addEventListener("click", () => {
        if (!currentJobId) {
            alert("Job ID is missing.");
            return;
        }

        if (!clientId) {
            alert("Client ID is missing in this job. Please check the job backend response.");
            return;
        }

        window.location.href =
            "chat.html?jobId=" +
            encodeURIComponent(currentJobId) +
            "&clientId=" +
            encodeURIComponent(clientId);
    });

    } catch (error) {
        console.error("Error while loading job detail:", error);

        status.textContent = "Error";

        container.innerHTML = `
    <div class="alert alert-danger">
      Job detail could not be loaded. Please check if GET /jobs/{id} exists in the backend.
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
async function deleteJob(jobId) {
    const token = localStorage.getItem("designer_jobs_token");

    if (!token) {
        alert("Please log in before deleting a job.");
        window.location.href = "login.html";
        return;
    }

    const confirmed = confirm("Do you really want to delete this job?");

    if (!confirmed) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/jobs/${encodeURIComponent(jobId)}`, {
            method: "DELETE",
            headers: {
                "Authorization": "Bearer " + token
            }
        });

        const result = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(result.error || "Delete failed.");
        }

        alert("Job deleted successfully.");
        window.location.href = "jobs.html";

    } catch (error) {
        console.error("Error while deleting job:", error);
        alert("Job could not be deleted. Please check backend, login and ownership.");
    }
}
async function reportCurrentJob() {
    // 1. Job-ID aus der URL extrahieren
    const urlParams = new URLSearchParams(window.location.search);
    const jobId = urlParams.get('id');

    if (!jobId) {
        alert("Keine gültige Job-ID gefunden.");
        return;
    }

    // 2. Den Admin/User nach dem Grund fragen
    const reason = prompt("Bitte gib den Grund für die Meldung dieses Jobs ein:");

    // Abbrechen, falls nichts eingegeben oder abgebrochen wurde
    if (reason === null) return;
    if (reason.trim() === "") {
        alert("Ein Grund ist zwingend erforderlich, um einen Job zu melden.");
        return;
    }

    const token = localStorage.getItem("designer_jobs_token");

    // KORREKTUR: Nutzt exakt den Key aus auth.js ("designer_jobs_userId")
    const reporterId = localStorage.getItem("designer_jobs_userId") || "anonymous_user";

    try {
        // Nutzt deinen frisch befüllten ModerationController-Endpunkt
        const response = await fetch(`${API_BASE}/moderation/jobs/${jobId}/report`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + token
            },
            body: JSON.stringify({
                reporterId: reporterId,
                reason: reason
            })
        });

        const result = await response.json().catch(() => ({}));

        if (response.ok) {
            alert("Vielen Dank. Der Job wurde erfolgreich gemeldet und wird vom Support überprüft.");
        } else {
            alert(`Fehler beim Melden: ${result.error || 'Unbekannter Fehler'}`);
        }

    } catch (error) {
        console.error("Fehler beim Senden des Reports:", error);
        alert("Die Meldung konnte nicht an den Server übertragen werden.");
    }
}
loadJobDetail();
