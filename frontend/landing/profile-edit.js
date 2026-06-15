/* DESIGNJOBS.COM — profile-edit.js
   Edit-profile form: live preview, load existing profile (own /auth/me
   or admin /users/{id}), country/city dropdowns from /locations/**,
   save via PUT /auth/me, delete via DELETE /auth/me. Requires auth.js
   and API_BASE from common.js. */
function getInitials(nameOrEmail) {
  if (!nameOrEmail) return "--";

  const parts = nameOrEmail.trim().split(/\s+/);

  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  return nameOrEmail.substring(0, 2).toUpperCase();
}

function updatePreview() {
  const fullName = document.getElementById("full-name").value.trim();
  const title = document.getElementById("title").value.trim();
  const city = document.getElementById("city").value.trim();
  const country = document.getElementById("country").value.trim();

  const displayName = fullName || "Your Name";
  const displayTitle = title || "Profile";
  const displayLocation =
          city && country ? city + ", " + country :
                  city ? city :
                          country ? country :
                                  "Location";

  document.getElementById("preview-avatar").textContent = getInitials(displayName);
  document.getElementById("preview-name").textContent = displayName;
  document.getElementById("preview-title").textContent = displayTitle;
  document.getElementById("preview-location").textContent = displayLocation;
}

// Hilfsfunktion, um alle alten und neuen Felder im Formular zu befüllen
async function fillFormFields(user) {
  if (!user) return;

  // Basis-Informationen
  document.getElementById("full-name").value = user.fullName || "";
  document.getElementById("title").value = user.designType || "";
  document.getElementById("bio").value = user.bio || "";
  document.getElementById("skills").value = user.skills || "";

  // Raten (Rates)
  document.getElementById("hourly-min").value = user.hourlyMin || "";
  document.getElementById("hourly-max").value = user.hourlyMax || "";
  document.getElementById("project-min").value = user.projectMin || "";

  // Social Links & Portfolio URL
  document.getElementById("portfolio-url").value = user.portfolioUrl || "";
  document.getElementById("twitter").value = user.twitter || "";
  document.getElementById("linkedin").value = user.linkedin || "";
  document.getElementById("instagram").value = user.instagram || "";

  // Radio-Buttons: Verfügbarkeit (Availability)
  if (user.availability) {
    const radioAvail = document.querySelector(`input[name="availability"][value="${user.availability}"]`);
    if (radioAvail) radioAvail.checked = true;
  }

  // Radio-Buttons: Portfolio-Sichtbarkeit
  if (user.portfolioVisibility) {
    const radioVis = document.querySelector(`input[name="portfolioVisibility"][value="${user.portfolioVisibility}"]`);
    if (radioVis) radioVis.checked = true;
  }

  // Standort-Dropdowns nacheinander befüllen, da Städte vom Land abhängen
  if (user.country) {
    document.getElementById("country").value = user.country;

    // Warte, bis die Städte für dieses Land geladen wurden
    await loadCities(user.country);

    if (user.city) {
      document.getElementById("city").value = user.city;
    }
  }
}

// Profil beim Laden der Seite abrufen
// Profil beim Laden der Seite abrufen
async function loadEditProfile() {
  const message = document.getElementById("edit-profile-message");

  // 1. Prüfen, ob eine userId als Query-Parameter in der URL übergeben wurde (vom Admin)
  const urlParams = new URLSearchParams(window.location.search);
  const adminTargetUserId = urlParams.get('userId');

  // 2. Dynamische URL bestimmen (Wenn ID da ist, nutze Admin-Route, sonst /auth/me)
  const fetchUrl = adminTargetUserId
          ? `${API_BASE}/users/${adminTargetUserId}`
          : `${API_BASE}/auth/me`;

  try {
    // Erst die Länderliste laden, damit die Auswahloptionen existieren
    await loadCountries();

    const response = await fetch(fetchUrl, { // <-- Hier dynamische URL genutzt
      method: "GET",
      headers: {
        "Authorization": "Bearer " + localStorage.getItem("designer_jobs_token")
      }
    });

    if (!response.ok) {
      throw new Error("Profile could not be loaded.");
    }

    const user = await response.json();

    // Alle Felder mit den geladenen Daten füllen (Nutzt deine bestehende Funktion!)
    await fillFormFields(user);

    updatePreview();

  } catch (error) {
    console.error("Edit profile load error:", error);

    if (message) {
      message.textContent = "Profile data could not be loaded.";
      message.className = "text-danger";
    }
  }
}

// NEU & AKTUALISIERT: Profil löschen Funktion
document.querySelector('[data-action="delete"]').addEventListener('click', async () => {
  if (confirm('Are you sure you want to delete your profile? This cannot be undone.')) {
    const message = document.getElementById("edit-profile-message");

    try {
      // Wir nutzen normales fetch mit Token, da Auth.authFetch manchmal Probleme macht
      const response = await fetch(`${API_BASE}/auth/me`, {
        method: "DELETE",
        headers: {
          "Authorization": "Bearer " + localStorage.getItem("designer_jobs_token")
        }
      });

      if (!response.ok) {
        const result = await response.json().catch(() => ({}));
        throw new Error(result.error || "Profile could not be deleted.");
      }

        // Clear local credentials
        localStorage.removeItem("designer_jobs_token");

        if (message) {
            message.textContent = "Profile deleted successfully. Redirecting...";
            message.className = "font-monospace small mb-3 text-success";
        }

        // FIX: Notify the main shell that auth state changed and request navigation
        setTimeout(() => {
            if (window.parent !== window) {
                window.parent.postMessage({ type: "auth-changed", page: "homepage.html" }, "*");
            } else {
                window.location.href = "homepage.html";
            }
        }, 2000);

    } catch (error) {
      console.error("Profile delete error:", error);

      if (message) {
        message.textContent = "Could not delete profile. Please try again.";
        message.className = "font-monospace small mb-3 text-danger";
      }
    }
  }
});

// Alle Formulardaten sammeln und an das Backend senden (Save)
document.getElementById('edit-form').addEventListener('submit', async e => {
  e.preventDefault();

  const message = document.getElementById("edit-profile-message");

  const payload = {
    fullName: document.getElementById("full-name").value.trim(),
    designType: document.getElementById("title").value.trim(),
    bio: document.getElementById("bio").value.trim(),
    country: document.getElementById("country").value,
    city: document.getElementById("city").value,
    availability: document.querySelector('input[name="availability"]:checked')?.value || "available",
    hourlyMin: parseInt(document.getElementById("hourly-min").value) || 0,
    hourlyMax: parseInt(document.getElementById("hourly-max").value) || 0,
    projectMin: parseInt(document.getElementById("project-min").value) || 0,
    skills: document.getElementById("skills").value.trim(),
    portfolioVisibility: document.querySelector('input[name="portfolioVisibility"]:checked')?.value || "public",
    portfolioUrl: document.getElementById("portfolio-url").value.trim(),
    twitter: document.getElementById("twitter").value.trim(),
    linkedin: document.getElementById("linkedin").value.trim(),
    instagram: document.getElementById("instagram").value.trim()
  };

  try {
    const response = await fetch(`${API_BASE}/auth/me`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + localStorage.getItem("designer_jobs_token")
      },
      body: JSON.stringify(payload)
    });

    const result = await response.json().catch(() => ({}));

    if (!response.ok) {
      throw new Error(result.error || "Profile could not be saved.");
    }

    if (message) {
      message.textContent = "Profile changes saved successfully.";
      message.className = "font-monospace small mb-3 text-success";
    }

    // Eingabefelder mit der Antwort vom Server aktualisieren
    await fillFormFields(result);

    updatePreview();

  } catch (error) {
    console.error("Profile save error:", error);

    if (message) {
      message.textContent = "Profile changes could not be saved. Please check backend.";
      message.className = "font-monospace small mb-3 text-danger";
    }
  }
});

["full-name", "title", "city", "country"].forEach(id => {
  const element = document.getElementById(id);

  if (element) {
    element.addEventListener("input", updatePreview);
  }
});

async function loadCountries() {
  const countrySelect = document.getElementById("country");

  try {
    const response = await fetch(`${API_BASE}/locations/countries`);

    if (!response.ok) {
      throw new Error("Countries could not be loaded.");
    }

    const countries = await response.json();

    countrySelect.innerHTML = `<option value="">Select country</option>`;

    countries.forEach(country => {
      const option = document.createElement("option");
      option.value = country;
      option.textContent = country;
      countrySelect.appendChild(option);
    });

  } catch (error) {
    console.error("Error while loading countries:", error);
  }
}

async function loadCities(country) {
  const citySelect = document.getElementById("city");

  citySelect.innerHTML = `<option value="">Loading cities...</option>`;
  citySelect.disabled = true;

  if (!country) {
    citySelect.innerHTML = `<option value="">Select city</option>`;
    return;
  }

  try {
    const response = await fetch(`${API_BASE}/locations/cities?country=` + encodeURIComponent(country));

    if (!response.ok) {
      throw new Error("Cities could not be loaded.");
    }

    const cities = await response.json();

    citySelect.innerHTML = `<option value="">Select city</option>`;

    cities.forEach(city => {
      const option = document.createElement("option");
      option.value = city;
      option.textContent = city;
      citySelect.appendChild(option);
    });

    citySelect.disabled = false;

  } catch (error) {
    console.error("Error while loading cities:", error);
    citySelect.innerHTML = `<option value="">Cities unavailable</option>`;
  }
}

document.getElementById("country").addEventListener("change", async () => {
  const selectedCountry = document.getElementById("country").value;
  await loadCities(selectedCountry);
  updatePreview();
});

document.getElementById("city").addEventListener("change", () => {
  updatePreview();
});

// Startet den gesamten Prozess (Länder laden & Profildaten holen)
loadEditProfile();
