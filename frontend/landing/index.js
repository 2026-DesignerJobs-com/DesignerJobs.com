/* DESIGNJOBS.COM — index.js
   The iframe shell: navbar/footer render once here; only the inner
   <iframe> swaps on navigation. Handles theme toggle (synced into the
   frame), auth-aware nav, history, and auth-changed postMessage from
   inner pages. No backend calls. */
(() => {
  'use strict';

  const frame  = document.getElementById('content-frame');
  const toggle = document.getElementById('theme-toggle');
  const allPageLinks = () => document.querySelectorAll('[data-page]');
  const registerLink = document.getElementById('register-link');
  const loginLink = document.getElementById('login-link');
  const profileLink = document.getElementById('profile-link');
  const logoutButton = document.getElementById('logout-button');

  const updateAuthNavigation = () => {
    const token = localStorage.getItem('designer_jobs_token');

    registerLink.classList.toggle('d-none', !!token);
    loginLink.classList.toggle('d-none', !!token);

    profileLink.classList.toggle('d-none', !token);
    logoutButton.classList.toggle('d-none', !token);
  };

  updateAuthNavigation();

  const applyTheme = t => {
    document.documentElement.setAttribute('data-bs-theme', t);
    toggle.textContent = t === 'dark' ? '☀ Light' : '☾ Dark';
    localStorage.setItem('theme', t);
    try { frame.contentDocument.documentElement.setAttribute('data-bs-theme', t); } catch (_) {}
  };

  applyTheme(localStorage.getItem('theme') || 'light');

  toggle.addEventListener('click', () => {
    const next = document.documentElement.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
    applyTheme(next);
  });
  logoutButton.addEventListener('click', () => {
    localStorage.removeItem('designer_jobs_token');
    localStorage.removeItem('designer_jobs_userId');
    localStorage.removeItem('designer_jobs_role');

    updateAuthNavigation();

    navigate('homepage.html');
  });
  const navigate = (page, push = true) => {
    frame.src = page;
    if (push) history.pushState({ page }, '', '#' + page);
    setActive(page);
  };

  const setActive = page => {
    allPageLinks().forEach(a => {
      const match = a.dataset.page === page;
      a.classList.toggle('active', match);
      match ? a.setAttribute('aria-current', 'page') : a.removeAttribute('aria-current');
    });
  };

  allPageLinks().forEach(a => {
    a.addEventListener('click', e => { e.preventDefault(); navigate(a.dataset.page); });
  });

  document.getElementById('brand-link').addEventListener('click', e => {
    e.preventDefault(); navigate('homepage.html');
  });

  frame.addEventListener('load', () => {
    try {
      frame.contentDocument.documentElement.setAttribute(
        'data-bs-theme', localStorage.getItem('theme') || 'light'
      );
      if (frame.contentDocument.title) document.title = frame.contentDocument.title;
      const page = frame.contentWindow.location.pathname.split('/').pop() || 'homepage.html';
      setActive(page);
    } catch (_) {}
  });

  window.addEventListener('popstate', e => {
    const page = e.state?.page || 'homepage.html';
    frame.src = page;
    setActive(page);
  });
  allPageLinks().forEach(a => {
    a.addEventListener('click', () => {
      const collapse = document.getElementById('nav-menu');

      if (collapse.classList.contains('show')) {
        bootstrap.Collapse.getInstance(collapse)?.hide();
      }
    });
  });

  window.addEventListener("message", event => {
    // Only trust messages from our own origin — otherwise any framed/foreign page
    // could drive navigate() to an attacker URL (F8).
    if (event.origin !== window.location.origin) return;
    if (event.data && event.data.type === "auth-changed") {
      updateAuthNavigation();

      if (event.data.page) {
        navigate(event.data.page);
      }
    }
  });

})();
