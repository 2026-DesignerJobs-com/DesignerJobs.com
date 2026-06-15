/* DESIGNJOBS.COM — profile.js
   Profile page: design-inspiration strip (Pexels proxy), profile load
   (own via /auth/me or another user via /users/{id}), logout, and the
   report-user action. Requires auth.js (Auth.*) and API_BASE from
   common.js. */

async function loadDesignInspiration() {
  const container = document.getElementById("pexels-inspiration");

  if (!container) {
    return;
  }

  try {
    const response = await fetch(`${API_BASE}/api/design-inspiration?query=graphic%20design`);

    if (!response.ok) {
      throw new Error("Design inspiration could not be loaded.");
    }

    const data = await response.json();
    const photos = data.photos || [];

    if (photos.length === 0) {
      container.innerHTML = `
        <div class="col-12">
          <div class="alert alert-warning">No inspiration images found.</div>
        </div>
      `;
      return;
    }

    container.innerHTML = photos.map(photo => `
      <div class="col-md-6 col-lg-4">
        <article class="card border-primary border-2 rounded-4 overflow-hidden h-100">
          <img src="${photo.src.medium}"
               alt="${photo.alt || "Design inspiration image"}"
               class="card-img-top"
               style="height:220px; object-fit:cover">

          <div class="card-body">
            <p class="font-monospace text-uppercase mb-2"
               style="font-size:.72rem; letter-spacing:.16em">
              Photo by ${photo.photographer}
            </p>

            <a href="${photo.url}"
               target="_blank"
               rel="noopener noreferrer"
               class="btn btn-outline-primary rounded-pill btn-sm fw-bold text-uppercase px-3"
               style="letter-spacing:.12em">
              View on Pexels
            </a>
          </div>
        </article>
      </div>
    `).join("");

  } catch (error) {
    console.error("Pexels loading failed:", error);

    container.innerHTML = `
      <div class="col-12">
        <div class="alert alert-danger">
          Design inspiration could not be loaded.
        </div>
      </div>
    `;
  }
}

loadDesignInspiration();

function getInitials(nameOrEmail) {
  if (!nameOrEmail) return "--";

  const parts = nameOrEmail.trim().split(/\s+/);

  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  return nameOrEmail.substring(0, 2).toUpperCase();
}

function formatDate(value) {
  if (!value) return "Created date unknown";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return "Created " + date.toLocaleDateString();
}

function renderSkills(skillsText) {
  const skillsContainer = document.getElementById("profile-skills");
  skillsContainer.innerHTML = "";

  if (!skillsText || !skillsText.trim()) {
    skillsContainer.innerHTML = '<span class="text-secondary">No skills added yet.</span>';
    return;
  }

  const skills = skillsText
          .split(",")
          .map(skill => skill.trim())
          .filter(skill => skill.length > 0);

  skills.forEach(skill => {
    const badge = document.createElement("span");
    badge.className = "badge rounded-pill border border-primary text-primary fw-normal px-3 py-2";
    badge.textContent = skill;
    skillsContainer.appendChild(badge);
  });
}

async function loadProfile() {
  try {
    const urlParams = new URLSearchParams(window.location.search);
    const targetUserId = urlParams.get("userId");

    // 2. Endpunkt dynamisch bestimmen:
    // Wenn eine userId in der URL steht, laden wir diesen User.
    // Wenn nicht, laden wir das eigene Profil (/auth/me).
    const fetchUrl = targetUserId ? `${API_BASE}/users/${targetUserId}` : `${API_BASE}/auth/me`;

    // 3. Daten vom Server abrufen
    const response = await Auth.authFetch(fetchUrl);
    if (!response.ok) {
      throw new Error(`Failed to fetch profile: ${response.status}`);
    }

    const user = await response.json();

    const displayName = user.fullName || user.email || "User";
    const role = user.role || "USER";

    document.getElementById("profile-avatar").textContent = getInitials(displayName);
    document.getElementById("profile-name").textContent = displayName;
    document.getElementById("profile-role").textContent = role + " PROFILE";

    document.getElementById("profile-email").textContent = user.email || "No email";
    document.getElementById("profile-created-at").textContent = formatDate(user.createdAt);

    if (targetUserId && targetUserId !== localStorage.getItem("designer_jobs_userId")) {
      // Es ist ein fremdes Profil -> Button anzeigen
      document.getElementById("profile-report-button").style.display = "block";
    } else {
      // Eigenes Profil -> Button verstecken
      document.getElementById("profile-report-button").style.display = "none";
    }

    document.getElementById("profile-description").textContent =
            role === "DESIGNER"
                    ? "This designer profile is loaded from the logged-in account."
                    : "This client profile is loaded from the logged-in account.";

    document.getElementById("profile-user-id").textContent = user.userId || "-";
    document.getElementById("profile-full-name").textContent = user.fullName || "-";
    document.getElementById("profile-email-detail").textContent = user.email || "-";
    document.getElementById("profile-role-detail").textContent = role;

    document.getElementById("profile-design-type").textContent = user.designType || "-";
    renderSkills(user.skills);

    if (role !== "DESIGNER") {
      document.getElementById("profile-design-type").textContent = "Only for designer accounts";
      document.getElementById("profile-skills").innerHTML =
              '<span class="text-secondary">Skills are only shown for designer accounts.</span>';
    }

  } catch (error) {
    console.error("Profile error:", error);

    document.getElementById("profile-name").textContent = "Profile could not be loaded";
    document.getElementById("profile-role").textContent = "ERROR";
    document.getElementById("profile-description").textContent =
            "Please check if the backend is running and if your session is valid.";
  }
}

const profileLogoutButton = document.getElementById("profile-logout-button");

if (profileLogoutButton) {
  profileLogoutButton.addEventListener("click", () => {
    if (window.Auth) {
      Auth.logout();
    } else {
      localStorage.removeItem("designer_jobs_token");
      localStorage.removeItem("designer_jobs_userId");
      localStorage.removeItem("designer_jobs_role");

      window.location.href = "login.html";
    }
  });
}

// ==========================================
// USER MELDEN LOGIK
// ==========================================
const profileReportButton = document.getElementById("profile-report-button");

if (profileReportButton) {
  profileReportButton.addEventListener("click", async () => {
    const urlParams = new URLSearchParams(window.location.search);
    const targetUserId = urlParams.get("userId");

    if (!targetUserId) {
      alert("Fehler: Keine User-ID zum Melden gefunden.");
      return;
    }

    // Grund für die Meldung vom Admin/User abfragen
    const reason = prompt("Bitte gib den Grund für die Meldung dieses Nutzers ein:");

    // Abbrechen gedrückt oder leerer String
    if (reason === null) return;
    if (reason.trim() === "") {
      alert("Ein Grund ist zwingend erforderlich!");
      return;
    }

    // Aktuelle User-ID (der Reporter) aus dem localStorage auslesen
    const currentUserId = localStorage.getItem("designer_jobs_userId") || "anonymous";

    try {
      const response = await fetch(`${API_BASE}/moderation/users/${targetUserId}/report`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          // Falls dein Endpoint Auth-Tokens verlangt, füge es hier hinzu:
          "Authorization": "Bearer " + localStorage.getItem("designer_jobs_token")
        },
        body: JSON.stringify({
          reporterId: currentUserId,
          reason: reason
        })
      });

      if (response.ok) {
        alert("Der Nutzer wurde erfolgreich gemeldet. Die Administration prüft den Fall.");
      } else {
        const errorData = await response.json().catch(() => ({}));
        alert(`Fehler beim Melden: ${errorData.error || "Unbekannter Fehler"}`);
      }
    } catch (error) {
      console.error("Netzwerkfehler beim Melden:", error);
      alert("Server aktuell nicht erreichbar.");
    }
  });
}
loadProfile();
