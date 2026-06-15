/* DESIGNJOBS.COM — homepage.js
   Homepage glue: route the hero search into advanced-search, and
   load the world-clock strip from GET /world-clock. */
const searchInput = document.querySelector('#search-form input[name="q"]');
const refinedSearchLink = document.getElementById("refined-search-link");

if (searchInput && refinedSearchLink) {
  refinedSearchLink.addEventListener("click", (event) => {
    event.preventDefault();

    const keyword = searchInput.value.trim();
    const targetUrl = keyword
      ? "advanced-search.html?q=" + encodeURIComponent(keyword)
      : "advanced-search.html";

    window.location.href = targetUrl;
  });
}

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
      <span class="opacity-75">${entry.city}</span>
      <strong class="ms-1">${entry.time}</strong>
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
