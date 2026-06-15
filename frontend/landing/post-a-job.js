/* DESIGNJOBS.COM — post-a-job.js
   Post-a-job form submit to POST /jobs (authenticated). Requires
   auth.js (Auth.*) and API_BASE from common.js. */

/*
 * Connects the Post a Job form with the Spring backend.
 * The form data is sent as JSON to POST {API_BASE}/jobs.
 */
document.getElementById("post-job-form").addEventListener("submit", async event => {
    // Prevents the normal form submit and page reload.
    event.preventDefault();

    // Reads all form fields that have a name attribute.
    const form = event.target;
    const formData = new FormData(form);

    // This object matches the fields in backend/src/main/java/at/ac/fhcampuswien/Job.java.
    const job = {
        clientId: "client-1",

        title: formData.get("title"),
        description: formData.get("description"),

        category: formData.get("category"),
        designType: formData.get("category"),

        location: formData.get("location"),
        workMode: formData.get("location"),

        budget: formData.get("budget"),
        deadline: formData.get("deadline"),
        tags: ""
    };

    try {
        // POST /jobs is authenticated — use Auth.authFetch so the bearer
        // token rides along, and a 401 (e.g. token expired mid-session)
        // bounces us to login automatically.
        const response = await Auth.authFetch(`${API_BASE}/jobs`, {
            method: "POST",
            body: JSON.stringify(job)
        });

        // If the backend returns an error, stop and show the error message.
        if (!response.ok) {
            throw new Error("Job could not be saved.");
        }

        // Reads the saved job returned by the backend.
        const savedJob = await response.json();

        console.log("Saved job:", savedJob);
        alert("Job was published successfully.");

        // Clears the form after successful saving.
        form.reset();

    } catch (error) {
        console.error("Error while saving job:", error);
        alert("The job could not be published. Please check if the backend is running.");
    }
});
