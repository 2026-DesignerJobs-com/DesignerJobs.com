# `moderation/` — reports & content moderation

Lets users flag jobs, chat messages, and other users; gives admins a queue to triage. **Currently all endpoints are 501 stubs (Phase 2).**

This README documents the contract the frontend will target and the hooks other packages (`chat/`, `job/`, `account/`) will need.

---

## files

| file | role |
|---|---|
| `ModerationController.java` | REST endpoints under `/moderation` |
| `Report.java`               | model — `id`, `reporterId`, `targetType`, `targetId`, `reason`, `status`, `createdAt` |

A `ReportRepository` is needed when implementing — follow `JobRepository`'s JDBC pattern. A separate `ModerationService` should host the synchronous moderation hook (see § integration below).

---

## endpoints

| method | path | who calls | what it should do |
|---|---|---|---|
| `POST` | `/moderation/messages/{id}/report` | any authenticated user | flag a chat message — body `{ "reason": "..." }`; server fills the rest |
| `POST` | `/moderation/jobs/{id}/report`     | any authenticated user | flag a job listing |
| `POST` | `/moderation/users/{id}/report`    | any authenticated user | flag a user |
| `GET`  | `/moderation/reports`              | admin                  | list reports; `?status=OPEN` filter, default to all |
| `PUT`  | `/moderation/reports/{id}`         | admin                  | resolve — body `{ "status": "RESOLVED"\|"DISMISSED" }` |

The three `POST` endpoints all build a `Report` with `targetType` derived from the URL (`MESSAGE` / `JOB` / `USER`), `targetId` from the path variable, `reporterId` from `auth.getPrincipal()`, `status = "OPEN"`, and `createdAt = Instant.now()`.

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

- `POST /moderation/*/report` — any authenticated caller can file a report.
- `GET /moderation/reports` and `PUT /moderation/reports/{id}` — admin only. Use `@PreAuthorize("hasRole('ADMIN')")` once method security is enabled in `SecurityConfig`. Until then, hard-code a check against a known admin user-id or skip these endpoints in V1.

Rate-limit `POST /moderation/*/report` per reporter to avoid abuse — out of scope for V1, flag in code with a `// TODO: rate limit`.

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
