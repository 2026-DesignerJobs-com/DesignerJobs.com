// Wartet, bis das gesamte HTML geladen ist
document.addEventListener('DOMContentLoaded', () => {
    loadJobsFromServer();
});

document.addEventListener('DOMContentLoaded', () => {
    loadUsersFromServer();
});

async function loadJobsFromServer() {
    const tableBody = document.getElementById('job-table-body');

    try {
        // Ruft GET /jobs auf deinem Spring Boot Server auf
        // Wenn Frontend & Backend auf dem gleichen Port laufen, reicht '/jobs'
        // Wenn sie getrennt laufen, nutzt du z.B. 'http://localhost:8080/jobs'
        const response = await fetch('/jobs', {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        });

        // Prüfen, ob der Server mit Status 200 geantwortet hat
        if (!response.ok) {
            throw new Error(`Server-Fehler: ${response.status}`);
        }

        // JSON-Daten extrahieren
        const jobs = await response.json();

        // Tabelle leeren
        tableBody.innerHTML = '';

        // Falls die Liste leer ist
        if (jobs.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center text-muted py-4">
                        <i class="bi bi-info-circle me-2"></i> Keine Jobs in der Datenbank vorhanden.
                    </td>
                </tr>`;
            return;
        }

        // Schleife durch alle empfangenen Jobs
        jobs.forEach(job => {
            const row = document.createElement('tr');

            // HTML-Struktur passend zu deinem Theme generieren
            // job.id, job.title und job.clientId kommen direkt aus deiner Java-Klasse 'Job'
            row.innerHTML = `
                <td class="font-monospace text-muted">#${job.id || 'N/A'}</td>
                <td class="fw-semibold">${escapeHtml(job.title)}</td>
                <td>${escapeHtml(job.clientId || 'Anonym')}</td>
                <td><span class="badge bg-light text-success border px-2 py-1">Live</span></td>
                <td>
                    <button class="btn btn-sm btn-outline-primary me-1" onclick="editJob('${job.id}')">
                        <i class="bi bi-pencil"></i> Edit
                    </button>
                    <button class="btn btn-sm btn-light border text-danger" onclick="deleteJob('${job.id}')">
                        <i class="bi bi-trash"></i> Löschen
                    </button>
                </td>
            `;

            tableBody.appendChild(row);
        });

    } catch (error) {
        console.error('Fehler beim Laden der Jobs:', error);
        tableBody.innerHTML = `
            <tr>
                <td colspan="5" class="text-center text-danger py-4">
                    <i class="bi bi-exclame-triangle me-2"></i> Fehler beim Laden der Daten vom Server.
                </td>
            </tr>`;
    }
}

async function loadUsersFromServer() {
    const tableBody = document.getElementById('user-table-body');

    try {
        // Ruft den Endpunkt aus deinem UserController auf
        const response = await fetch('/designers', {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`Server-Fehler: ${response.status}`);
        }

        const data = await response.json();
        tableBody.innerHTML = '';

        let users = [];

        // FALLBACK: Wenn die API noch "not_implemented" ist, laden wir Testdaten für die Optik
        if (data.status === 'not_implemented') {
            console.warn("API /designers ist noch nicht implementiert. Zeige Testdaten.");
            users = [
                { id: "U-882", name: "Alex Morgan", email: "alex@designstudio.com", type: "Designer" },
                { id: "U-881", name: "TechCorp GmbH", email: "hr@techcorp.de", type: "Unternehmen" }
            ];
        } else {
            // Wenn die API fertig ist, nimm die echten Daten (erwartet ein Array)
            users = Array.isArray(data) ? data : [];
        }

        if (users.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center text-muted py-4">
                        <i class="bi bi-info-circle me-2"></i> Keine Benutzer gefunden.
                    </td>
                </tr>`;
            return;
        }

        // User in die Tabelle rendern
        users.forEach(user => {
            const row = document.createElement('tr');

            // Dynamische Badge-Farbe je nach User-Typ
            const isDesigner = user.type?.toLowerCase() === 'designer';
            const badgeClass = isDesigner ? 'text-primary' : 'text-success'; // Nutzt deine Theme-Farben (Wine oder Terracotta)

            row.innerHTML = `
                <td class="font-monospace text-muted">#${user.id}</td>
                <td class="fw-semibold">${escapeHtml(user.name)}</td>
                <td>${escapeHtml(user.email)}</td>
                <td><span class="badge bg-light ${badgeClass} border px-2 py-1">${escapeHtml(user.type)}</span></td>
                <td>
                    <button class="btn btn-sm btn-outline-primary me-1" onclick="editUser('${user.id}')">
                        <i class="bi bi-pencil"></i> Bearbeiten
                    </button>
                    <button class="btn btn-sm btn-light border text-danger" onclick="banUser('${user.id}')">
                        <i class="bi bi-slash-circle"></i> Sperren
                    </button>
                </td>
            `;
            tableBody.appendChild(row);
        });

    } catch (error) {
        console.error('Fehler beim Laden der User:', error);
        tableBody.innerHTML = `
            <tr>
                <td colspan="5" class="text-center text-danger py-4">
                    <i class="bi bi-exclamation-triangle me-2"></i> Fehler beim Laden der Benutzerdaten.
                </td>
            </tr>`;
    }
}

// Kleine Sicherheitsfunktion, um zu verhindern, falls User böswilligen Code in den Jobtitel geschrieben haben.
function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;")
              .replace(/</g, "&lt;")
              .replace(/>/g, "&gt;")
              .replace(/"/g, "&quot;")
              .replace(/'/g, "&#039;");
}


// Admin-Aktionen
function editJob(id) {
    alert('Job bearbeiten: ' + id);
}

function deleteJob(id) {
    alert('Job löschen getriggert für ID: ' + id);
    // Hier könntest du später einen fetch() mit method: 'DELETE' auf '/jobs/' + id abfeuern!
}

function editUser(id) {
    alert('User bearbeiten: ' + id);
}

async function banUser(id) {
    if (confirm(`Möchtest du den User #${id} wirklich sperren?`)) {
        // Bereit für Phase 2: DELETE /users/{id} aufrufen
        alert(`User ${id} gesperrt (Hier wird später DELETE /users/${id} aufgerufen)`);
    }