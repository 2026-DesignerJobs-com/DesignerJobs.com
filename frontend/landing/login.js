/* DESIGNJOBS.COM — login.js
   Login form submit + already-authenticated bounce + expired/required
   hints, plus the world-clock strip. Requires auth.js (Auth.*) and
   API_BASE from common.js. */

// Accept only a same-origin relative path as a post-login redirect target;
// reject javascript:/data: and off-site (//host) URLs — open-redirect/XSS (F2).
function safeNext(raw) {
    if (!raw || raw.includes(":") || raw.startsWith("//")) return "homepage.html";
    return raw;
}

// If the user is already authenticated (token in localStorage, not expired),
// skip the form and bounce straight to the homepage — closes US-04.
if (window.Auth && Auth.isAuthenticated()) {
    const params = new URLSearchParams(window.location.search);
    window.location.replace(safeNext(params.get("next")));
}

// Surface a hint when we were redirected here because a token expired.
(() => {
    const params = new URLSearchParams(window.location.search);
    const reason = params.get("reason");
    if (!reason) return;
    const message = document.getElementById("login-message");
    if (!message) return;
    if (reason === "expired") {
        message.textContent = "Your session expired. Please log in again.";
        message.className = "font-monospace small mb-3 text-warning";
    } else if (reason === "required") {
        message.textContent = "Please log in to continue.";
        message.className = "font-monospace small mb-3 text-info";
    }
})();

document.getElementById("login-form").addEventListener("submit", async event => {
    event.preventDefault();

    const form = event.target;
    const formData = new FormData(form);

    const loginData = {
        email: formData.get("email"),
        password: formData.get("password")
    };

    const message = document.getElementById("login-message");

    try {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(loginData)
        });

        const data = await response.json();

        if (!response.ok) {
            message.textContent = data && data.error ? data.error : "Login failed.";
            message.className = "font-monospace small mb-3 text-danger";
            return;
        }

        // Persist the session — auth.js will keep it across reloads.
        Auth.saveSession({ token: data.token, userId: data.userId, role: data.role });

        message.textContent = "Login successful. Redirecting…";
        message.className = "font-monospace small mb-3 text-success";

        const params = new URLSearchParams(window.location.search);
        const next = safeNext(params.get("next"));

        window.parent.postMessage({
            type: "auth-changed",
            page: "profile.html"
        }, "*");
    } catch (error) {
        console.error("Login error:", error);
        message.textContent = "Login request failed. Please check if the backend is running.";
        message.className = "font-monospace small mb-3 text-danger";
    }
});

async function loadWorldClock() {
    const worldClock = document.getElementById("world-clock");

    if (!worldClock) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/world-clock`);
        const times = await response.json();

        if (!response.ok) {
            throw new Error("World clock could not be loaded.");
        }

        worldClock.innerHTML = times.map(entry => `
    <span>
      <span class="text-secondary">${entry.city}</span>
      <strong class="ms-1 text-primary">${entry.time}</strong>
    </span>
  `).join("");

    } catch (error) {
        console.error("World clock loading failed:", error);

        worldClock.innerHTML = `
    <span>New York --:--</span>
    <span>London --:--</span>
    <span>Vienna --:--</span>
    <span>Tokyo --:--</span>
  `;
    }
}

loadWorldClock();
