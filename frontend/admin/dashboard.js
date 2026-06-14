// 1. Die globale Map für die Benutzer-Zuordnung
const userMap = {};

// 2. Startpunkt beim Laden der Seite
document.addEventListener('DOMContentLoaded', async () => {
    // Dieser Ansatz wartet ERST komplett auf die User und startet DANN die Jobs
    await loadUsersFromServer();
    await loadJobsFromServer();
    await loadReportsFromServer();
});

// ==========================================
// USER LADEN & USER-TABELLE BEFÜLLEN
// ==========================================
async function loadUsersFromServer() {
    const tableBody = document.getElementById('user-table-body');
    if (!tableBody) return;

    try {
        const response = await fetch('/users', {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });

        if (!response.ok) {
            throw new Error(`Server-Fehler: ${response.status}`);
        }

        const users = await response.json();

        // Counter für User im Overview setzen
        const userCounter = document.getElementById('stats-user-count');
        if (userCounter) {
            userCounter.textContent = users.length;
        }

        // --- MAP BEFÜLLEN  ---
        users.forEach(user => {
            if (user.id) {
                // Wenn fullName leer ist, sonst 'User ohne Name'
                // Wir mappen die ID direkt auf den Namen
                userMap[user.id] = user.fullName || user.email || `User #${user.id.substring(0, 5)}`;
            }
        });

        // Tabelle leeren
        tableBody.innerHTML = '';

        if (users.length === 0) {
            tableBody.innerHTML = `<tr><td colspan=\"5\" class=\"text-center text-muted py-4\">Keine User vorhanden.</td></tr>`;
            return;
        }

        users.forEach(user => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td class="font-monospace text-muted">#${user.id || 'N/A'}</td>
                <td class="fw-semibold">${escapeHtml(user.fullName || 'Kein Name hinterlegt')}</td>
                <td>${escapeHtml(user.email || 'Keine E-Mail')}</td>
                <td><span class="badge bg-secondary border px-2 py-1">${escapeHtml(user.role || 'USER')}</span></td>
                <td>
                    <button class="btn btn-sm btn-outline-primary me-1" onclick="editUser('${user.id}')">
                        <i class="bi bi-pencil"></i>
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

// ==========================================
// JOBS LADEN & JOB-TABELLE BEFÜLLEN
// ==========================================
async function loadJobsFromServer() {
    const tableBody = document.getElementById('job-table-body');
    if (!tableBody) return;

    try {
        const response = await fetch('/jobs', {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });

        if (!response.ok) {
            throw new Error(`Server-Fehler: ${response.status}`);
        }

        const jobs = await response.json();

        // Counter im Overview anpassen
        const jobCounter = document.getElementById('stats-job-count');
        if (jobCounter) {
            jobCounter.textContent = jobs.length;
        }

        // Tabelle leeren
        tableBody.innerHTML = '';

        if (jobs.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center text-muted py-4">
                        <i class="bi bi-info-circle me-2"></i> Keine Jobs in der Datenbank vorhanden.
                    </td>
                </tr>`;
            return;
        }

        jobs.forEach(job => {
            const row = document.createElement('tr');

            // Holt den Namen aus der Map. Wenn nichts gefunden wird, zeigt er die Roh-ID an
            const clientName = userMap[job.clientId] || `ID: ${job.clientId}`;

            row.innerHTML = `
                <td class="font-monospace text-muted">#${job.id || 'N/A'}</td>
                <td class="fw-semibold">${escapeHtml(job.title)}</td>
                <td class="text-primary fw-medium">${escapeHtml(clientName)}</td>
                <td><span class="badge bg-light text-success border px-2 py-1">Live</span></td>
                <td>
                    <button class="btn btn-sm btn-outline-primary" onclick="showJob('${job.id}')">
                        <i class="bi bi-eye"></i>
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
                    <i class="bi bi-exclamation-triangle me-2"></i> Fehler beim Laden der Daten vom Server.
                </td>
            </tr>`;
    }
}

// ==========================================
// REPORTS LADEN & TABELLE BEFÜLLEN
// ==========================================
async function loadReportsFromServer() {
    const tableBody = document.getElementById('report-table-body');
    if (!tableBody) return;

    try {
        // Nutzt deinen ModerationController Endpunkt: GET /moderation/reports
        const response = await fetch('/moderation/reports', {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });

        if (!response.ok) throw new Error(`Server-Fehler: ${response.status}`);
        const reports = await response.json();

        tableBody.innerHTML = '';

        if (reports.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="7" class="text-center text-muted py-4">Keine Meldungen vorhanden.</td></tr>`;
            return;
        }

        reports.forEach(report => {
            const row = document.createElement('tr');

            // Namen des Reporters auflösen, falls in userMap vorhanden
            const reporterName = userMap[report.reporterId] || `ID: ${report.reporterId.substring(0,8)}`;

            // Badge-Farbe basierend auf dem Report-Status wählen
            let statusBadge = '';
            if (report.status === 'OPEN') statusBadge = '<span class="badge bg-danger">Offen</span>';
            else if (report.status === 'RESOLVED') statusBadge = '<span class="badge bg-success">Gelöst</span>';
            else statusBadge = '<span class="badge bg-secondary">Abgewiesen</span>';

            // Schöner formulierter Typ-Badge
            let typeBadge = `<span class="badge bg-light text-dark border">${report.targetType}</span>`;

            row.innerHTML = `
                <td class="text-muted" style="font-size: 0.9rem;">${escapeHtml(report.createdAt || 'Unbekannt')}</td>
                <td class="fw-medium">${escapeHtml(reporterName)}</td>
                <td>${typeBadge}</td>
                <td class="font-monospace text-muted" style="font-size: 0.85rem;">#${escapeHtml(report.targetId)}</td>
                <td class="text-wrap" style="max-width: 250px;">${escapeHtml(report.reason)}</td>
                <td>${statusBadge}</td>
                <td>
                    <button class="btn btn-sm btn-outline-primary font-ui" onclick="toggleReportStatus('${report.id}', '${report.status}')">
                        <i class="bi bi-arrow-repeat"></i> Status ändern
                    </button>
                </td>
            `;
            tableBody.appendChild(row);
        });
    } catch (error) {
        console.error('Fehler beim Laden der Reports:', error);
        tableBody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4"><i class="bi bi-exclamation-triangle me-2"></i> Fehler beim Laden der Moderationsdaten.</td></tr>`;
    }
}

// 5. REPORT STATUS ROTIEREN
async function toggleReportStatus(reportId, currentStatus) {
    // Wenn die Meldung nicht mehr OPEN ist, darf nichts mehr geändert werden
    if (currentStatus !== 'OPEN') {
        alert('Diese Meldung wurde bereits final bearbeitet.');
        return;
    }

    // Admin entscheidet über die offene Meldung
    const chooseResolved = confirm(
        `Meldung bearbeiten:\n\n` +
        `• Klicke [OK] für RESOLVED (Gelöst)\n` +
        `• Klicke [Abbrechen] für DISMISSED (Abgewiesen)`
    );

    // Zuweisung basierend auf der Entscheidung
    const nextStatus = chooseResolved ? 'RESOLVED' : 'DISMISSED';

    try {
        // Nutzt deinen Endpunkt: PUT /moderation/reports/{id}
        const response = await fetch(`/moderation/reports/${reportId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: nextStatus })
        });

        if (response.ok) {
            // Tabelle sofort aktualisieren
            await loadReportsFromServer();
        } else {
            const data = await response.json().catch(() => ({}));
            alert(`Fehler beim Aktualisieren: ${data.error || 'Status konnte nicht geändert werden.'}`);
        }
    } catch (error) {
        console.error('Netzwerkfehler:', error);
        alert('Server nicht erreichbar.');
    }
}

// Sicherheitsfunktion gegen XSS
function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;")
              .replace(/</g, "&lt;")
              .replace(/>/g, "&gt;")
              .replace(/"/g, "&quot;")
              .replace(/'/g, "&#039;");
}

// ==========================================
// ADMIN - AKTIONENEN
// ==========================================

// 1. USER LÖSCHEN
async function banUser(id) {
    if (!confirm(`Möchtest du den User #${id} wirklich löschen?`)) {
        return;
    }

    try {
        const response = await fetch(`/users/${id}`, {
            method: 'DELETE',
            headers: {
                'Accept': 'application/json'
            }
        });

        const data = await response.json();

        if (response.ok) {
            alert('User erfolgreich gelöscht!');
            // Tabellen aktualisieren
            await loadUsersFromServer();
            await loadJobsFromServer();
        } else {
            alert(`Fehler beim Löschen: ${data.error || 'Unbekannter Fehler'}`);
        }
    } catch (error) {
        console.error('Netzwerkfehler beim Löschen des Users:', error);
        alert('Server temporär nicht erreichbar.');
    }
}

// 2. USER EDITIEREN
function editUser(id) {
    // Leitet zur Profil-Bearbeitungsseite weiter und übergibt die ID des Users
    window.location.href = `/profile-edit.html?userId=${id}`;
}

// 3. JOB ANSCHAUEN
function showJob(id) {
    window.location.href = `/job-detail.html?id=${id}`;
}

// 4. JOB LÖSCHEN
async function deleteJob(id) {
    if (!confirm(`Möchtest du den Job #${id} wirklich unwiderruflich löschen?`)) {
        return; // Abbrechen, falls der Admin verklickt hat
    }

    try {
        const response = await fetch(`/jobs/${id}`, {
            method: 'DELETE',
            headers: {
                'Accept': 'application/json'
                // Falls später JWT/Tokens nutzt,hier Authorization-Header hin
            }
        });

        const data = await response.json();

        if (response.ok) {
            alert('Job erfolgreich gelöscht!');
            // Tabelle sofort neu laden, damit der gelöschte Job verschwindet
            await loadJobsFromServer();
        } else {
            alert(`Fehler beim Löschen: ${data.error || 'Unbekannter Fehler'}`);
        }
    } catch (error) {
        console.error('Netzwerkfehler beim Löschen des Jobs:', error);
        alert('Server temporär nicht erreichbar.');
    }
}
