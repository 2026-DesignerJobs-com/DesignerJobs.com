/* DESIGNJOBS.COM — advanced-search.js
   Prefill the keyword field from the ?q= query param. */
document.addEventListener("DOMContentLoaded", () => {
  const params = new URLSearchParams(window.location.search);
  const keyword = params.get("q");

  const keywordInput = document.querySelector('input[name="q"]');

  if (keywordInput && keyword) {
    keywordInput.value = keyword;
  }
});
