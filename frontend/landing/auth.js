/* ================================================================
   DESIGNJOBS.COM — auth.js
   Client-side session persistence (US-04 / AC-04).

   - On page load: read the JWT from localStorage. If its `exp` claim
     is in the past, clear storage and redirect to login.html?expired=1.
   - authFetch(url, options): wraps fetch() so every authenticated
     request carries Authorization: Bearer <token>. A 401 response
     redirects to login (covers tokens invalidated server-side).
   - requireAuth(): call from pages that must not be reached anonymously
     (e.g. profile, post-a-job). Redirects to login.html?next=<here>
     if there is no usable token.
   - logout(): clears all designer_jobs_* keys, bounces to login.html.

   Pure vanilla JS, no dependencies. Safe to include on every page;
   it does not modify the DOM, only localStorage / window.location.
   ================================================================ */
(() => {
  "use strict";

  const TOKEN_KEY  = "designer_jobs_token";
  const USERID_KEY = "designer_jobs_userId";
  const ROLE_KEY   = "designer_jobs_role";

  const LOGIN_PAGE = "login.html";

  /* --- 1. decode the payload of a compact JWS without verifying it.
     Verification is the server's job — we only need `exp` so we can
     skip the round-trip when the token is already dead. -------- */
  function decodePayload(token) {
    if (!token || typeof token !== "string") return null;
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    try {
      // base64url → base64 → JSON
      const b64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
      const padded = b64 + "=".repeat((4 - b64.length % 4) % 4);
      return JSON.parse(atob(padded));
    } catch {
      return null;
    }
  }

  function isExpired(token) {
    const claims = decodePayload(token);
    if (!claims || typeof claims.exp !== "number") return true;
    return claims.exp * 1000 <= Date.now();
  }

  function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USERID_KEY);
    localStorage.removeItem(ROLE_KEY);
  }

  function redirectToLogin(reason) {
    const params = new URLSearchParams();
    if (reason) params.set("reason", reason);
    // Preserve where the user was trying to go so login can bounce back.
    const here = window.location.pathname.split("/").pop() || "";
    if (here && here !== LOGIN_PAGE) params.set("next", here + window.location.search);
    const qs = params.toString();
    window.location.replace(LOGIN_PAGE + (qs ? "?" + qs : ""));
  }

  /* --- 2. public API on window.Auth ---------------------------- */
  const Auth = {
    /** Read the current JWT, or null. Does not validate `exp`. */
    getToken() {
      return localStorage.getItem(TOKEN_KEY);
    },

    getUserId() {
      return localStorage.getItem(USERID_KEY);
    },

    getRole() {
      return localStorage.getItem(ROLE_KEY);
    },

    /** True if a token exists and its `exp` claim is still in the future. */
    isAuthenticated() {
      const token = this.getToken();
      return !!token && !isExpired(token);
    },

    /** Persist a login response. Call from login.html / register.html. */
    saveSession({ token, userId, role }) {
      if (token)  localStorage.setItem(TOKEN_KEY, token);
      if (userId) localStorage.setItem(USERID_KEY, userId);
      if (role)   localStorage.setItem(ROLE_KEY, role);
    },

    /** Clear the session and bounce to login.html. */
    logout() {
      clearSession();
      window.location.href = LOGIN_PAGE;
    },

    /**
     * Pages that require login call this near the top of their inline
     * script. Returns true if authenticated; otherwise redirects and
     * returns false (caller should short-circuit further work).
     */
    requireAuth() {
      if (this.isAuthenticated()) return true;
      clearSession();
      redirectToLogin("required");
      return false;
    },

    /**
     * fetch() wrapper that attaches the bearer token and redirects to
     * login on 401. Use this for every call to a protected endpoint.
     */
    async authFetch(url, options = {}) {
      const token = this.getToken();
      if (!token || isExpired(token)) {
        clearSession();
        redirectToLogin("expired");
        // Resolve to a never-resolving Promise so callers' await sits
        // in flight while the navigation happens.
        return new Promise(() => {});
      }
      const headers = new Headers(options.headers || {});
      headers.set("Authorization", "Bearer " + token);
      if (options.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
      }
      const response = await fetch(url, { ...options, headers });
      if (response.status === 401) {
        clearSession();
        redirectToLogin("expired");
        return new Promise(() => {});
      }
      return response;
    }
  };

  /* --- 3. AC-04: eager expiry guard on every page load --------
     If a token exists but is past its `exp`, blow it away and
     redirect to login.html?reason=expired. Triggered before any
     page-specific JS runs (auth.js is loaded synchronously in
     <head> on protected pages). ---------------------------------- */
  const existingToken = localStorage.getItem(TOKEN_KEY);
  if (existingToken && isExpired(existingToken)) {
    clearSession();
    // Only auto-redirect if we are NOT already on the login page,
    // to avoid a redirect loop after the user manually navigates there.
    const here = window.location.pathname.split("/").pop() || "";
    if (here !== LOGIN_PAGE) {
      redirectToLogin("expired");
    }
  }

  window.Auth = Auth;
})();
