//globalen Maps für die Zuordnung von IDs zu lesbaren Namen
const userMap = {};
const jobMap = {};

//Startpunkt beim Laden der Seite
document.addEventListener('DOMContentLoaded', async () => {
    // Dieser Ansatz wartet ERST komplett auf die User und Jobs,
    // damit die Maps befüllt sind, wenn die Reports geladen werden.
    await loadUsersFromServer();
    await loadJobsFromServer();
    await loadReportsFromServer();
});

document.addEventListener('DOMContentLoaded', () => {
    const navLinks = document.querySelectorAll('#sidebar-menu .nav-link');

    // 1. AKTIV SETZEN BEI KLICK
    navLinks.forEach(link => {
        link.addEventListener('click', function() {
            navLinks.forEach(item => item.classList.remove('active'));
            this.classList.add('active');
        });
    });

    // Sektionen aus dem DOM sammeln
    const sections = [];
    navLinks.forEach(link => {
        const targetId = link.getAttribute('href');
        if (targetId && targetId.startsWith('#')) {
            const sectionElement = document.querySelector(targetId);
            if (sectionElement) {
                sections.push({ id: targetId, element: sectionElement });
            }
        }
    });

    // Hilfsfunktion zum Umschalten der Klassen
    function setActiveLink(targetHref) {
        navLinks.forEach(link => {
            if (link.getAttribute('href') === targetHref) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });
    }

    // Scroll-Logik mit zwei harten "Anschlag-Stopps" für oben und unten
    window.addEventListener('scroll', () => {
        const scrollPosition = window.scrollY;

        // LÖSUNG FÜR GANZ OBEN: Wenn weniger als 60px vom oberen Rand entfernt -> Übersicht aktiv
        if (scrollPosition < 60) {
            setActiveLink('#overview');
            return;
        }

        // LÖSUNG FÜR GANZ UNTEN: Wenn der unterste Rand des Viewports erreicht ist -> Meldungen aktiv
        // Wir ziehen einen kleinen Toleranzpuffer von 5 Pixeln ab, falls der Browser rundet
        const isAtBottom = (window.innerHeight + scrollPosition) >= (document.documentElement.scrollHeight - 5);
        if (isAtBottom) {
            setActiveLink('#reports');
            return;
        }

        // NORMALER SCROLLSPY: Wenn man sich im Mittelfeld bewegt, berechnen wir die nächste Sektion
        let currentActiveSectionId = '#overview';
        let minDistance = Infinity;

        sections.forEach(section => {
            const rect = section.element.getBoundingClientRect();

            // Reagiert, sobald die Sektion ins obere Drittel des Bildschirms geschoben wird
            if (rect.top <= 250) {
                const distance = Math.abs(rect.top - 100);
                if (distance < minDistance) {
                    minDistance = distance;
                    currentActiveSectionId = section.id;
                }
            }
        });

        setActiveLink(currentActiveSectionId);
    });
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
                // Wir mappen die ID direkt auf den vollen Namen (Fallback auf E-Mail oder ID)
                userMap[user.id] = user.fullName || user.email || `User #${user.id.substring(0, 5)}`;
            }
        });

        // Tabelle leeren
        tableBody.innerHTML = '';

        if (users.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="5" class="text-center text-muted py-4">Keine User vorhanden.</td></tr>`;
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

        // --- MAP BEFÜLLEN ---
        jobs.forEach(job => {
            if (job.id) {
                jobMap[job.id] = job.title || `Job #${job.id.substring(0, 5)}`;
            }
        });

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

            // --- NEU: ANONYME ODER UNBEKANNTE CLIENTS ABFANGEN ---
            let clientName = `Unbekannter Ersteller`;
            if (job.clientId) {
                if (job.clientId.toLowerCase().startsWith('anonym')) {
                    clientName = "Anonymer Ersteller";
                } else {
                    // Sucht in der userMap, die durch loadUsersFromServer() befüllt wurde
                    clientName = userMap[job.clientId] || `ID: ${job.clientId.substring(0, 8)}`;
                }
            }

            row.innerHTML = `
                <td class="font-monospace text-muted">#${job.id || 'N/A'}</td>
                <td class="fw-semibold">${escapeHtml(job.title)}</td>
                <td>
                    <span class="fw-medium text-primary">${escapeHtml(clientName)}</span>
                </td>
                <td><span class="badge bg-light text-success border px-2 py-1">Live</span></td>
                <td>
                    <button class="btn btn-sm btn-outline-primary me-1" onclick="showJob('${job.id}')">
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
// REPORTS AUS DER DATENBANK LADEN & TAVELLEE FÜLLEN
// ==========================================

async function loadReportsFromServer() {
    const tableBody = document.getElementById('report-table-body');
    if (!tableBody) return;

    try {
        const token = localStorage.getItem("designer_jobs_token");

        // Headers dynamisch und sauber vorbereiten (Verhindert 401-Fehler bei null-Tokens)
        const headers = { 'Accept': 'application/json' };
        if (token && token !== "null" && token !== "undefined") {
            headers['Authorization'] = 'Bearer ' + token;
        }

        const response = await fetch('http://localhost:8080/moderation/reports', {
            method: 'GET',
            headers: headers
        });

        if (!response.ok) throw new Error(`Server-Fehler: ${response.status}`);
        const reports = await response.json();

        const openReportsCount = reports.filter(report => report.status === 'OPEN').length;

        // Sucht das Element, dem wir gerade eben im HTML die ID gegeben haben
        const reportsCounter = document.getElementById('stats-reports-count');
        if (reportsCounter) {
            reportsCounter.textContent = openReportsCount;
        }

        tableBody.innerHTML = '';

        if (reports.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="7" class="text-center text-muted py-4">Keine Meldungen vorhanden.</td></tr>`;
            return;
        }

        reports.forEach(report => {
            const row = document.createElement('tr');

            // 1. Reporter-Name aus der userMap ermitteln
            let reporterName = "Anonym gemeldet";

            // Nur wenn eine valide ID existiert UND diese ID nicht das System-Fallback für anonyme User ist
            if (report.reporterId &&
                report.reporterId !== "anonymous" &&
                report.reporterId !== "anonymous_user") {

                // Sucht die echte UUID im geladenen Telefonbuch (userMap)
                reporterName = userMap[report.reporterId] || `Ehemaliger Benutzer (${report.reporterId.substring(0, 5)})`;
            }

            // 2. Ziel-Name ermitteln & DEZENTEN LINK GENERIEREN (Altes Schriftdesign)
            let targetDisplayName = `Unbekanntes Objekt`;

            if (report.targetType === 'USER') {
                const name = userMap[report.targetId] || `User (ID: ${report.targetId.substring(0,8)})`;
                // Link zum Profil im alten Textdesign (Farbe passt sich der normalen Tabelle an)
                targetDisplayName = `<a href="/profile.html?userId=${report.targetId}" target="_blank" class="text-reset text-decoration-none" style="cursor: pointer;" title="Profil ansehen">
                                        <i class="bi bi-person me-1"></i><strong>${escapeHtml(name)}</strong>
                                     </a>`;
            } else if (report.targetType === 'JOB') {
                const title = jobMap[report.targetId] || `Job (ID: ${report.targetId.substring(0,8)})`;
                // Link zum Job im alten Textdesign
                targetDisplayName = `<a href="/job-detail.html?id=${report.targetId}" target="_blank" class="text-reset text-decoration-none" style="cursor: pointer;" title="Job-Details ansehen">
                                        <i class="bi bi-briefcase me-1"></i><strong>${escapeHtml(title)}</strong>
                                     </a>`;
            } else if (report.targetType === 'MESSAGE') {
                targetDisplayName = `<span><i class="bi bi-chat-left-text me-1"></i>${escapeHtml(report.targetId.substring(0,8))}</span>`;
            }

            // Status Badges
            let statusBadge = '';
            if (report.status === 'OPEN') statusBadge = '<span class="badge bg-danger">Offen</span>';
            else if (report.status === 'RESOLVED') statusBadge = '<span class="badge bg-success">Gelöst</span>';
            else statusBadge = '<span class="badge bg-secondary">Abgewiesen</span>';

            let typeBadge = `<span class="badge bg-light text-dark border">${report.targetType}</span>`;

            row.innerHTML = `
                <td class="text-muted" style="font-size: 0.9rem;">${report.createdAt || 'Unbekannt'}</td>
                <td class="fw-semibold text-primary">${escapeHtml(reporterName)}</td>
                <td>${typeBadge}</td>
                <td>
                    <span class="fw-medium">${targetDisplayName}</span><br>
                    <small class="font-monospace text-muted" style="font-size: 0.75rem;">#${report.targetId}</small>
                </td>
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

// ==========================================
// REPORT STATUS ROTIEREN
// ==========================================

async function toggleReportStatus(reportId, currentStatus) {
    if (currentStatus !== 'OPEN') {
        alert('Diese Meldung wurde bereits final bearbeitet.');
        return;
    }

    const chooseResolved = confirm(
        `Meldung bearbeiten:\n\n` +
        `• Klicke [OK] für RESOLVED (Gelöst)\n` +
        `• Klicke [Abbrechen] für DISMISSED (Abgewiesen)`
    );

    const nextStatus = chooseResolved ? 'RESOLVED' : 'DISMISSED';
    const token = localStorage.getItem("designer_jobs_token");

    try {
        const response = await fetch(`http://localhost:8080/moderation/reports/${reportId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify({ status: nextStatus })
        });

        if (response.ok) {
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
    window.location.href = `/profile-edit.html?userId=${id}`;
}

// 3. JOB ANSCHAUEN
function showJob(id) {
    window.location.href = `/job-detail.html?id=${id}`;
}

// 4. JOB LÖSCHEN
async function deleteJob(id) {
    if (!confirm(`Möchtest du den Job #${id} wirklich unwiderruflich löschen?`)) {
        return;
    }

    try {
        const response = await fetch(`/jobs/${id}`, {
            method: 'DELETE',
            headers: {
                'Accept': 'application/json'
            }
        });

        const data = await response.json();

        if (response.ok) {
            alert('Job erfolgreich gelöscht!');
            await loadJobsFromServer();
        } else {
            alert(`Fehler beim Löschen: ${data.error || 'Unbekannter Fehler'}`);
        }
    } catch (error) {
        console.error('Netzwerkfehler beim Löschen des Jobs:', error);
        alert('Server temporär nicht erreichbar.');
    }
}