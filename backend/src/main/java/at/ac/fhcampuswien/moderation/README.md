# `moderation/` — reports & content moderation

Lets users flag jobs, chat messages, and other users; gives admins a queue to triage. **Implemented** — `ModerationController` + `ReportRepository` persist reports to the H2 `reports` table.

> ⚠️ **Known divergences from the intended design (see § known issues):** reports do **not** derive `reporterId` from the JWT, the admin endpoints are **not** role-gated, and `POST .../messages/{id}/report` and `.../users/{id}/report` currently **500** because they don't supply a `reporterId` for the `NOT NULL` column.

---

## files

| file | role |
|---|---|
| `ModerationController.java` | REST endpoints under `/moderation` |
| `Report.java`               | model — `id`, `reporterId`, `targetType`, `targetId`, `reason`, `status`, `createdAt` |
| `ReportRepository.java`     | hand-rolled JDBC against the H2 `reports` table; creates it in its constructor |

A separate `ModerationService` for the synchronous chat hook (see § integration) is **not** built yet.

---

## endpoints

| method | path | who calls | behaviour today |
|---|---|---|---|
| `POST` | `/moderation/messages/{id}/report` | authenticated | `targetType=MESSAGE`; `400` if `reason` blank; **`500` unless the body carries `reporterId`** |
| `POST` | `/moderation/jobs/{id}/report`     | authenticated | `targetType=JOB`; defaults a missing `reporterId` to `"anonymous"`; `400` if `reason` blank → `200` |
| `POST` | `/moderation/users/{id}/report`    | authenticated | `targetType=USER`; `400` if `reason` blank; **`500` unless the body carries `reporterId`** |
| `GET`  | `/moderation/reports`              | authenticated (not admin-gated) | list reports; optional `?status=OPEN` filter (case-insensitive), default all |
| `PUT`  | `/moderation/reports/{id}`         | authenticated (not admin-gated) | set status — body `{ "status": "OPEN"\|"RESOLVED"\|"DISMISSED" }`; `404` if no such report |

Each `POST` sets `targetType` from the URL, `targetId` from the path variable, and persists the rest of the `Report` from the request body. Successful reports return `200` with `{ "message": "...", "id": "..." }` (not `201`).

---

## status state machine

```
OPEN ─┬─► RESOLVED   (admin agreed: action taken on the target)
      └─► DISMISSED  (admin disagreed: report closed without action)
```

`RESOLVED` and `DISMISSED` are terminal. Resolving a `MESSAGE` report should also flip `messages.flagged = true` on the referenced row — see `chat/README.md` § moderation integration.

---

## integration with other packages

### chat — synchronous hook

`chat/ChatController` will call a `ModerationService.scan(content)` method on every `POST /conversations/{id}/messages` **before persisting**:

- If the service decides the content is borderline, `flagged = true` is set on the row but the message goes through.
- If the service decides it's a hard reject (slurs, doxxing, etc.), the controller returns `400 Bad Request` and the message is not stored.

Inject `ModerationService` into `ChatController`; do not call `ModerationController` from another controller.

### jobs / users — async only

Moderation for `JOB` and `USER` targets is **after-the-fact**: the `POST /moderation/*` endpoints exist only to file reports. There is no pre-publish scan on job posts in V1.

---

## auth model

All `/moderation/**` routes fall through to `authenticated()` in `SecurityConfig`, so a valid JWT is required.

**Intended (not yet enforced):**
- `GET /moderation/reports` and `PUT /moderation/reports/{id}` should be **admin only** — add `@PreAuthorize("hasRole('ADMIN')")` once method security is enabled. Today any authenticated user can list and resolve reports.
- `reporterId` should be taken from `auth.getPrincipal()`, never the request body — `ModerationController` doesn't take an `Authentication` param yet, so it trusts the body (or defaults to `"anonymous"` for jobs).
- Rate-limit `POST /moderation/*/report` per reporter — still a `// TODO`.

---

## known issues

1. **`reportMessage` / `reportUser` return `500`** — the `reports.reporter_id` column is `NOT NULL`, but only `reportJob` defaults a missing `reporterId` (to `"anonymous"`). The other two persist `null` and trip the constraint. Fix: derive `reporterId` from the JWT (preferred) or apply the same default.
2. **No admin gate** on the read/resolve endpoints (see auth model).
3. **No chat hook** — `ModerationService.scan(...)` (below) was never built.

---

## persistence sketch

```sql
CREATE TABLE IF NOT EXISTS reports (
    id           VARCHAR(36) PRIMARY KEY,
    reporter_id  VARCHAR(36) NOT NULL,
    target_type  VARCHAR(20) NOT NULL,   -- JOB | MESSAGE | USER
    target_id    VARCHAR(36) NOT NULL,
    reason       TEXT,
    status       VARCHAR(20) NOT NULL,   -- OPEN | RESOLVED | DISMISSED
    created_at   VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reports_status
    ON reports (status);
```

The index keeps the admin queue query (`WHERE status = 'OPEN'`) fast.

---

## see also

- `chat/README.md` — moderation hook on message creation, `messages.flagged` column.
- `account/README.md` — caller identity, role authority.
